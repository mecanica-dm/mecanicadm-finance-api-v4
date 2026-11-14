# Terraform — Banco de dados da Finance API

Infraestrutura AWS do serviço `mecanicadm-finance-api-v4`. Este diretório é
aplicado **depois** do [`mecanicadm-infra-v4`](../../../mecanicadm-infra-v4), que
cria a VPC, o cluster EKS e publica os parâmetros SSM usados aqui.

## O que é criado

| Recurso | Finalidade |
|---|---|
| `aws_db_instance` PostgreSQL | Banco do serviço (`mecanicadmfinancedb`) em subnet privada |
| `aws_db_subnet_group` | Subnets privadas descobertas pela tag `Name` do módulo VPC |
| `aws_security_group` do RDS | Libera 5432 apenas para o SG dos worker nodes do EKS |
| IAM Role de Enhanced Monitoring | Métricas granulares do RDS no CloudWatch |
| `aws_secretsmanager_secret` de credenciais | Host, porta, banco, usuário e senha do RDS |
| Parâmetros SSM | `db_host`, `db_port`, `db_name`, `db_user`, `db_password` e `db_sslmode` |

O RDS é **privado** (`publicly_accessible = false`) e só aceita conexões do
security group dos nodes do EKS (`/mecanicadm/prod/eks_node_sg_id`). Não há
nenhum CIDR liberado por padrão.

## O que **não** é criado aqui

- **JWT compartilhado**: já é criado pelo `mecanicadm-workorder-api-v4`
  (`mecanicadm/prod/jwt-secret` no Secrets Manager e `/mecanicadm/prod/jwt_secret`
  no SSM). Finance e workorder precisam validar os mesmos tokens, então este
  state apenas **consome** o segredo existente. Criá-lo nos dois states
  causaria conflito de recurso.
- **RabbitMQ**: o broker é gerenciado no CloudAMQP (plano compartilhado Little
  Lemur) e **não** é criado por este state. As credenciais ficam no SSM em
  `/mecanicadm/prod/rabbitmq/{user,password,vhost}` e são materializadas no
  Secret `mecanicadm-finance-secrets` pela pipeline de deploy. O host e a porta
  (`5671`, TLS) ficam no ConfigMap. A topologia (exchanges, filas e bindings)
  precisa ser importada no vhost do CloudAMQP antes do deploy.
- **Namespace, Ingress, Kong, Route 53, HPA, image pull**: pertencem ao
  `mecanicadm-infra-v4` e aos manifests de Kubernetes.
- **Credenciais de e-mail, New Relic e Mercado Pago**: continuam vindo dos
  GitHub Secrets do workflow de deploy. Não são recursos AWS.
- **Bucket S3**: a aplicação gera o PDF do orçamento em memória e o envia por
  e-mail (SMTP); não há persistência em S3.

## Pré-requisitos

1. `mecanicadm-infra-v4` aplicado em `prod`.
2. Parâmetros SSM existentes:
   - `/mecanicadm/prod/vpc_id`
   - `/mecanicadm/prod/eks_node_sg_id`
3. `mecanicadm-workorder-api-v4` aplicado (fornece o JWT compartilhado em
   `/mecanicadm/prod/jwt_secret`).
4. Bucket do state (`mecanicadm-v4-01`) e credenciais AWS com permissão para
   RDS, EC2 (SG), IAM, Secrets Manager e SSM.
5. Terraform >= 1.10.

## Backend

O state fica separado do cluster e dos demais serviços:

```hcl
# backend.hcl
bucket       = "mecanicadm-v4-01"
region       = "us-east-1"
key          = "terraform/mecanicadm-finance/terraform.tfstate"
encrypt      = true
use_lockfile = true
```

```bash
terraform init -backend-config=backend.hcl
```

Há um modelo em [`backend.hcl.example`](backend.hcl.example).

## Aplicação

```bash
terraform init -backend-config=backend.hcl
terraform fmt -check -recursive -diff
terraform validate
terraform plan -out=tfplan
terraform apply tfplan
```

Para sobrescrever configurações:

```bash
cp terraform.tfvars.example terraform.tfvars
```

## Como a pipeline consome

O Secret do Kubernetes deve ser criado a partir do SSM, na seguinte ordem:

- Secret do banco (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD`),
  lidos de `/mecanicadm/prod/finance/*`.
- Secret da aplicação (`JWT_SECRET`), lido de `/mecanicadm/prod/jwt_secret`.

Os comandos equivalentes para depuração manual são:

```bash
DB_HOST=$(aws ssm get-parameter --name /mecanicadm/prod/finance/db_host --with-decryption --query Parameter.Value --output text)
DB_PORT=$(aws ssm get-parameter --name /mecanicadm/prod/finance/db_port --query Parameter.Value --output text)
DB_NAME=$(aws ssm get-parameter --name /mecanicadm/prod/finance/db_name --query Parameter.Value --output text)
DB_USER=$(aws ssm get-parameter --name /mecanicadm/prod/finance/db_user --with-decryption --query Parameter.Value --output text)
DB_PASSWORD=$(aws ssm get-parameter --name /mecanicadm/prod/finance/db_password --with-decryption --query Parameter.Value --output text)
JWT_SECRET=$(aws ssm get-parameter --name /mecanicadm/prod/jwt_secret --with-decryption --query Parameter.Value --output text)
```

Os valores precisam bater com o que a aplicação espera
(`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`).

## Descoberta das subnets

O `mecanicadm-infra-v4` ainda não publica os IDs das subnets no SSM. Por isso o
Terraform localiza as subnets privadas pela tag `Name` gerada pelo módulo VPC:

```
mecanicadm-prod-private-us-east-1a
mecanicadm-prod-private-us-east-1b
```

Se a topologia mudar, informe os IDs explicitamente:

```hcl
private_subnet_ids = ["subnet-xxxxxxxx", "subnet-yyyyyyyy"]
```

A validação exige pelo menos 2 subnets em AZs diferentes.

## Custos e destruição

- A classe padrão é `db.t4g.micro`, Single-AZ, 20 GB gp3, backup de 1 dia e
  Performance Insights com 7 dias (free tier). Ajuste em `variables.tf` ou
  `terraform.tfvars`.
- `deletion_protection = false` e `skip_final_snapshot = true` por padrão para
  permitir `terraform destroy` no ambiente de estudo. Em produção real, use
  `deletion_protection = true` e `skip_final_snapshot = false`.
- `secret_recovery_window_in_days = 0` permite recriar os secrets logo após um
  destroy. Em produção real, use 7–30 dias.
- O banco não é compartilhado com o workorder: cada serviço tem seu próprio RDS,
  com credenciais e parâmetros SSM independentes.

## Próximos passos sugeridos

1. Decidir e provisionar o RabbitMQ (Amazon MQ com TLS em 5671 ou RabbitMQ no
   EKS) e publicar host/porta/credenciais no SSM. Enquanto isso não acontece, a
   aplicação sobe, mas os listeners ficam tentando reconectar ao
   `localhost:5672`.
2. Aplicar este state pelo workflow
   [`.github/workflows/terraform.yml`](../../.github/workflows/terraform.yml)
   (`Terraform - Finance DB + Secrets`). Ele roda em `push` na `main` quando
   `devops/terraform/**` muda, e em PR apenas valida (plan sem apply).
3. Depois do apply, o
   [`.github/workflows/app-deploy.yml`](../../.github/workflows/app-deploy.yml)
   roda automaticamente (`workflow_run`) e materializa os Secrets a partir do
   SSM. O deploy dos manifests de [`devops/k8s`](../k8s) também pode ser
   disparado pelo `ci-cd.yml` (`gh workflow run`) a cada publicação de imagem.
