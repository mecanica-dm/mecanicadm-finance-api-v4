output "db_instance_identifier" {
  description = "Identificador da instância RDS."
  value       = aws_db_instance.main.identifier
}

output "db_host" {
  description = "Endpoint do RDS (host sem porta)."
  value       = aws_db_instance.main.address
}

output "db_port" {
  description = "Porta do PostgreSQL."
  value       = aws_db_instance.main.port
}

output "db_name" {
  description = "Nome do banco criado."
  value       = var.db_name
}

output "db_username" {
  description = "Usuário administrador do banco."
  value       = var.db_username
}

output "db_secretsmanager_secret_arn" {
  description = "ARN do secret com as credenciais do banco."
  value       = aws_secretsmanager_secret.db.arn
}

output "db_security_group_id" {
  description = "Security group do RDS (use como origem em outros recursos, se necessário)."
  value       = aws_security_group.rds.id
}

output "jwt_secretsmanager_secret_arn" {
  description = "ARN do secret do JWT compartilhado (criado pelo mecanicadm-workorder-api-v4)."
  value       = data.aws_secretsmanager_secret.jwt.arn
}

output "jwt_secret_ssm_parameter_name" {
  description = "Nome do parâmetro SSM do JWT compartilhado consumido pela pipeline."
  value       = data.aws_ssm_parameter.jwt_secret.name
}

output "ssm_parameter_prefix" {
  description = "Prefixo dos parâmetros SSM consumidos pela pipeline."
  value       = local.ssm_prefix
}
