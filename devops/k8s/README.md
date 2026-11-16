# Kubernetes — Finance API

Manifests do serviço `mecanicadm-finance-api-v4` aplicados no namespace
`mecanicadm` do cluster EKS criado pelo `mecanicadm-infra-v4`.

O deploy é feito pelo workflow
[`.github/workflows/app-deploy.yml`](../../.github/workflows/app-deploy.yml).
Nada aqui precisa ser aplicado manualmente em produção.

## Arquivos

| Arquivo | Recurso | Nome |
|---|---|---|
| `serviceaccount.yaml` | ServiceAccount | `mecanicadm-finance` |
| `configmap.yaml` | ConfigMap | `mecanicadm-finance-config` |
| `service.yaml` | Service (ClusterIP) | `mecanicadm-finance` |
| `deployment.yaml` | Deployment | `mecanicadm-finance` |
| `hpa.yaml` | HorizontalPodAutoscaler | `mecanicadm-finance` |
| `ingress.yaml` | Ingress (Kong) | `mecanicadm-finance` |
| `seed-job.yaml` | Job de seed | `mecanicadm-finance-seed` |
| `seed/init.sql` | Script do seed | ConfigMap `mecanicadm-finance-seed-sql` |

## Por que os nomes têm o sufixo `finance`

O workorder usa os nomes `mecanicadm`, `mecanicadm-config` e
`mecanicadm-secrets` no mesmo namespace. Para os dois serviços conviverem sem
colisão, todos os recursos do finance usam `mecanicadm-finance*`.

## Secrets

Não há credencial versionada. Na pipeline:

1. `mecanicadm-finance-db-secrets` é montado a partir dos parâmetros SSM
   publicados pelo Terraform (`/mecanicadm/prod/finance/db_*`).
2. `mecanicadm-finance-secrets` é montado com:
   - `JWT_SECRET` → `/mecanicadm/prod/jwt_secret` (criado pelo
     `mecanicadm-workorder-api-v4`, compartilhado entre os serviços);
   - `RABBITMQ_USER`, `RABBITMQ_PASSWORD` e `RABBITMQ_VHOST` →
     `/mecanicadm/prod/rabbitmq/*`, publicados pela própria pipeline a partir
     dos GitHub Secrets `CLOUDAMQP_*`;
   - `MAIL_USERNAME`, `MAIL_PASSWORD`, `NEW_RELIC_LICENSE_KEY` e
     `MERCADO_PAGO_*` → GitHub Secrets do repositório.

## Pré-requisitos no GitHub

Antes do primeiro deploy, o repositório precisa de:

**Secrets**

| Secret | Usado por |
|---|---|
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | `terraform.yml` e `app-deploy.yml` |
| `DOCKER_USERNAME`, `DOCKER_PASSWORD` | `ci-cd.yml` (push da imagem) |
| `SONAR_TOKEN` | `ci-cd.yml` (Sonar) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | `app-deploy.yml` → Secret da aplicação |
| `NEW_RELIC_LICENSE_KEY` | `app-deploy.yml` → Secret da aplicação |
| `MERCADO_PAGO_ACCESS_TOKEN`, `MERCADO_PAGO_PUBLIC_KEY`, `MERCADO_PAGO_CLIENT_ID`, `MERCADO_PAGO_CLIENT_SECRET` | `app-deploy.yml` → Secret da aplicação |
| `CLOUDAMQP_USER`, `CLOUDAMQP_PASSWORD`, `CLOUDAMQP_VHOST` | `app-deploy.yml` → SSM `/mecanicadm/prod/rabbitmq/*` → Secret da aplicação |

**Variables**

| Variable | Padrão | Uso |
|---|---|---|
| `AWS_REGION` | `us-east-1` | Região do Terraform/EKS |
| `EKS_CLUSTER_NAME` | `mecanicadm-prod` | Nome do cluster |
| `DOCKER_IMAGE_REPO` | `diegopriess/mecanicadm-finance` | Repositório da imagem |
| `TF_STATE_BUCKET` | `mecanicadm-v4-01` | Bucket do state do Terraform |

**Environment** `prod` criado no repositório (é o `environment` dos jobs de
apply e deploy).

## Ingress

`https://finance.mecanicadm.com.br/api` → Kong → Service `mecanicadm-finance`.

O host é próprio do finance porque `api.mecanicadm.com.br/api` já pertence ao
workorder. O ExternalDNS cria o registro no Route 53 a partir do Ingress.

## Imagem

O Deployment traz `diegopriess/mecanicadm-finance:prod-latest` apenas como
placeholder. A pipeline sempre roda `kubectl set image` com
`vars.DOCKER_IMAGE_REPO` e a tag imutável `prod-<sha>` do build.

> **Atenção:** a variável de repositório `DOCKER_IMAGE_REPO` do finance precisa
> apontar para um repositório Docker Hub **próprio do serviço**. O `ci-cd.yml`
> também publica a tag móvel `prod-latest`; se workorder e finance usarem o
> mesmo repositório, um sobrescreve a tag do outro. Os deploys automáticos usam
> `prod-<sha>` e não são afetados, mas deploys manuais com `prod-latest` podem
> subir a imagem errada.

## RabbitMQ (CloudAMQP)

O broker é uma instância compartilhada do CloudAMQP (Little Lemur) em
`us-east-1`, usada também pelo workorder.

- `RABBITMQ_HOST` / `RABBITMQ_PORT` (`5671`, TLS) / `RABBITMQ_SSL_ENABLED` ficam
  no `configmap.yaml`.
- `RABBITMQ_USER` / `RABBITMQ_PASSWORD` / `RABBITMQ_VHOST` vêm do SSM
  (`/mecanicadm/prod/rabbitmq/*`) e entram no Secret
  `mecanicadm-finance-secrets` pela pipeline. Os parâmetros do SSM são
  publicados pela própria pipeline a partir dos GitHub Secrets
  `CLOUDAMQP_USER`, `CLOUDAMQP_PASSWORD` e `CLOUDAMQP_VHOST`.
- O vhost do plano compartilhado **não** é `/`: use exatamente o valor mostrado
  no console do CloudAMQP, e reescreva o vhost nos JSONs de topologia antes de
  importar.
- Copie a senha do campo de credenciais, não da URL `amqps://` (a URL pode vir
  com percent-encoding).

O readiness do pod continua configurado como `readinessState,db`
(`MANAGEMENT_ENDPOINT_HEALTH_GROUP_READINESS_INCLUDE`) para o RabbitMQ fora do
ar não impedir o pod de ficar pronto.
