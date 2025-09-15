# Replication Group Outputs (Cluster Mode Disabled)
output "redis_replication_group_id" {
  description = "ID of the ElastiCache replication group"
  value       = var.cluster_mode_enabled ? null : aws_elasticache_replication_group.main[0].id
}

output "redis_replication_group_arn" {
  description = "ARN of the ElastiCache replication group"
  value       = var.cluster_mode_enabled ? null : aws_elasticache_replication_group.main[0].arn
}

output "redis_primary_endpoint_address" {
  description = "Address of the endpoint for the primary node in the replication group"
  value       = var.cluster_mode_enabled ? null : aws_elasticache_replication_group.main[0].primary_endpoint_address
}

output "redis_reader_endpoint_address" {
  description = "Address of the endpoint for the reader node in the replication group"
  value       = var.cluster_mode_enabled ? null : aws_elasticache_replication_group.main[0].reader_endpoint_address
}

# Cluster Mode Outputs
output "redis_cluster_replication_group_id" {
  description = "ID of the ElastiCache replication group (cluster mode)"
  value       = var.cluster_mode_enabled ? aws_elasticache_replication_group.cluster[0].id : null
}

output "redis_cluster_replication_group_arn" {
  description = "ARN of the ElastiCache replication group (cluster mode)"
  value       = var.cluster_mode_enabled ? aws_elasticache_replication_group.cluster[0].arn : null
}

output "redis_configuration_endpoint_address" {
  description = "Address of the replication group configuration endpoint when cluster mode is enabled"
  value       = var.cluster_mode_enabled ? aws_elasticache_replication_group.cluster[0].configuration_endpoint_address : null
}

# Common Outputs
output "redis_port" {
  description = "Redis port"
  value       = 6379
}

output "redis_engine_version" {
  description = "Redis engine version"
  value       = var.engine_version
}

# Security Group Outputs
output "redis_security_group_id" {
  description = "ID of the security group for Redis"
  value       = aws_security_group.redis.id
}

output "redis_security_group_arn" {
  description = "ARN of the security group for Redis"
  value       = aws_security_group.redis.arn
}

# Subnet Group Outputs
output "redis_subnet_group_name" {
  description = "Name of the ElastiCache subnet group"
  value       = aws_elasticache_subnet_group.main.name
}

output "redis_subnet_group_description" {
  description = "Description of the ElastiCache subnet group"
  value       = aws_elasticache_subnet_group.main.description
}

# Parameter Group Outputs
output "redis_parameter_group_name" {
  description = "Name of the ElastiCache parameter group"
  value       = aws_elasticache_parameter_group.main.name
}

output "redis_parameter_group_family" {
  description = "Family of the ElastiCache parameter group"
  value       = aws_elasticache_parameter_group.main.family
}

# Connection Information
output "redis_connection_info" {
  description = "Redis connection information"
  value = {
    host = var.cluster_mode_enabled ? (
      aws_elasticache_replication_group.cluster[0].configuration_endpoint_address
    ) : (
      aws_elasticache_replication_group.main[0].primary_endpoint_address
    )
    port                = 6379
    auth_token_enabled  = var.transit_encryption_enabled && var.auth_token != null
    ssl_enabled        = var.transit_encryption_enabled
  }
}

# CloudWatch Log Group (if enabled)
output "redis_log_group_name" {
  description = "Name of the CloudWatch log group for Redis"
  value       = var.enable_logging ? aws_cloudwatch_log_group.redis[0].name : null
}

output "redis_log_group_arn" {
  description = "ARN of the CloudWatch log group for Redis"
  value       = var.enable_logging ? aws_cloudwatch_log_group.redis[0].arn : null
}
