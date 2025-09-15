# ElastiCache Subnet Group
resource "aws_elasticache_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-redis-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-redis-subnet-group"
  })
}

# ElastiCache Parameter Group
resource "aws_elasticache_parameter_group" "main" {
  family = var.parameter_group_family
  name   = "${var.project_name}-${var.environment}-redis-params"

  dynamic "parameter" {
    for_each = var.parameters
    content {
      name  = parameter.value.name
      value = parameter.value.value
    }
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-redis-params"
  })
}

# Security Group for Redis
resource "aws_security_group" "redis" {
  name_prefix = "${var.project_name}-${var.environment}-redis-"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = var.allowed_security_groups
    cidr_blocks     = var.allowed_cidr_blocks
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-redis-sg"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# ElastiCache Replication Group (Redis Cluster Mode Disabled)
resource "aws_elasticache_replication_group" "main" {
  count = var.cluster_mode_enabled ? 0 : 1

  replication_group_id       = "${var.project_name}-${var.environment}-redis"
  description                = "Redis cache for ${var.project_name} ${var.environment}"
  
  # Node configuration
  node_type            = var.node_type
  port                 = 6379
  parameter_group_name = aws_elasticache_parameter_group.main.name

  # Replica configuration
  num_cache_clusters = var.num_cache_clusters
  
  # Engine configuration
  engine               = "redis"
  engine_version       = var.engine_version
  
  # Network and security
  subnet_group_name    = aws_elasticache_subnet_group.main.name
  security_group_ids   = [aws_security_group.redis.id]
  
  # Backup and maintenance
  snapshot_retention_limit = var.snapshot_retention_limit
  snapshot_window         = var.snapshot_window
  maintenance_window      = var.maintenance_window
  
  # Security
  at_rest_encryption_enabled = var.at_rest_encryption_enabled
  transit_encryption_enabled = var.transit_encryption_enabled
  auth_token                = var.auth_token
  
  # Multi-AZ
  automatic_failover_enabled = var.automatic_failover_enabled
  multi_az_enabled          = var.multi_az_enabled
  
  # Notifications
  notification_topic_arn = var.notification_topic_arn
  
  # Auto minor version upgrade
  auto_minor_version_upgrade = var.auto_minor_version_upgrade

  # Final snapshot
  final_snapshot_identifier = var.environment == "prod" ? "${var.project_name}-${var.environment}-redis-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}" : null

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-redis"
  })

  depends_on = [
    aws_elasticache_subnet_group.main,
    aws_elasticache_parameter_group.main
  ]
}

# ElastiCache Replication Group (Redis Cluster Mode Enabled)
resource "aws_elasticache_replication_group" "cluster" {
  count = var.cluster_mode_enabled ? 1 : 0

  replication_group_id       = "${var.project_name}-${var.environment}-redis-cluster"
  description                = "Redis cluster for ${var.project_name} ${var.environment}"
  
  # Node configuration
  node_type            = var.node_type
  port                 = 6379
  parameter_group_name = aws_elasticache_parameter_group.main.name

  # Cluster configuration
  num_node_groups         = var.num_node_groups
  replicas_per_node_group = var.replicas_per_node_group
  
  # Engine configuration
  engine               = "redis"
  engine_version       = var.engine_version
  
  # Network and security
  subnet_group_name    = aws_elasticache_subnet_group.main.name
  security_group_ids   = [aws_security_group.redis.id]
  
  # Backup and maintenance
  snapshot_retention_limit = var.snapshot_retention_limit
  snapshot_window         = var.snapshot_window
  maintenance_window      = var.maintenance_window
  
  # Security
  at_rest_encryption_enabled = var.at_rest_encryption_enabled
  transit_encryption_enabled = var.transit_encryption_enabled
  auth_token                = var.auth_token
  
  # Multi-AZ
  automatic_failover_enabled = var.automatic_failover_enabled
  multi_az_enabled          = var.multi_az_enabled
  
  # Notifications
  notification_topic_arn = var.notification_topic_arn
  
  # Auto minor version upgrade
  auto_minor_version_upgrade = var.auto_minor_version_upgrade

  # Final snapshot
  final_snapshot_identifier = var.environment == "prod" ? "${var.project_name}-${var.environment}-redis-cluster-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}" : null

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-redis-cluster"
  })

  depends_on = [
    aws_elasticache_subnet_group.main,
    aws_elasticache_parameter_group.main
  ]
}

# CloudWatch Log Group for Redis logs (if needed)
resource "aws_cloudwatch_log_group" "redis" {
  count             = var.enable_logging ? 1 : 0
  name              = "/aws/elasticache/redis/${var.project_name}-${var.environment}"
  retention_in_days = var.log_retention_in_days

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-redis-logs"
  })
}
