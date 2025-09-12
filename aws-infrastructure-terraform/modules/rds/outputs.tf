output "db_instance_id" {
  description = "RDS instance ID"
  value       = aws_db_instance.main.id
}

output "db_instance_arn" {
  description = "RDS instance ARN"
  value       = aws_db_instance.main.arn
}

output "db_instance_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.main.endpoint
}

output "db_instance_address" {
  description = "RDS instance hostname"
  value       = aws_db_instance.main.address
}

output "db_instance_port" {
  description = "RDS instance port"
  value       = aws_db_instance.main.port
}

output "db_instance_name" {
  description = "RDS instance database name"
  value       = aws_db_instance.main.db_name
}

output "db_instance_username" {
  description = "RDS instance root username"
  value       = aws_db_instance.main.username
  sensitive   = true
}

output "db_subnet_group_id" {
  description = "DB subnet group ID"
  value       = aws_db_subnet_group.main.id
}

output "db_subnet_group_arn" {
  description = "DB subnet group ARN"
  value       = aws_db_subnet_group.main.arn
}

output "db_parameter_group_id" {
  description = "DB parameter group ID"
  value       = aws_db_parameter_group.main.id
}

output "db_parameter_group_arn" {
  description = "DB parameter group ARN"
  value       = aws_db_parameter_group.main.arn
}

output "db_security_group_id" {
  description = "Security group ID for RDS"
  value       = aws_security_group.rds.id
}

output "db_security_group_arn" {
  description = "Security group ARN for RDS"
  value       = aws_security_group.rds.arn
}

# Secrets Manager outputs (only when not using RDS managed passwords)
output "db_credentials_secret_arn" {
  description = "ARN of the Secrets Manager secret containing database credentials"
  value       = var.manage_master_user_password ? null : aws_secretsmanager_secret.db_credentials[0].arn
}

output "db_credentials_secret_name" {
  description = "Name of the Secrets Manager secret containing database credentials"
  value       = var.manage_master_user_password ? null : aws_secretsmanager_secret.db_credentials[0].name
}

# RDS managed master user password secret ARN (when using AWS managed passwords)
output "db_master_user_secret_arn" {
  description = "ARN of the master user secret when using AWS managed master user password"
  value       = var.manage_master_user_password ? aws_db_instance.main.master_user_secret[0].secret_arn : null
}

# Read replica outputs
output "db_read_replica_id" {
  description = "Read replica instance ID"
  value       = var.create_read_replica ? aws_db_instance.read_replica[0].id : null
}

output "db_read_replica_endpoint" {
  description = "Read replica endpoint"
  value       = var.create_read_replica ? aws_db_instance.read_replica[0].endpoint : null
}

output "db_read_replica_address" {
  description = "Read replica hostname"
  value       = var.create_read_replica ? aws_db_instance.read_replica[0].address : null
}

# Enhanced monitoring role ARN
output "enhanced_monitoring_role_arn" {
  description = "Enhanced monitoring role ARN"
  value       = var.monitoring_interval > 0 ? aws_iam_role.rds_enhanced_monitoring[0].arn : null
}
