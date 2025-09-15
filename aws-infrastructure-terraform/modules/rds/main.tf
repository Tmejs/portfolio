# DB Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-db-subnet-group"
  subnet_ids = var.database_subnet_ids

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-db-subnet-group"
  })
}

# DB Parameter Group
resource "aws_db_parameter_group" "main" {
  family = var.db_family
  name   = "${var.project_name}-${var.environment}-db-params"

  dynamic "parameter" {
    for_each = var.db_parameters
    content {
      name  = parameter.value.name
      value = parameter.value.value
    }
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-db-params"
  })
}

# Security Group for RDS
resource "aws_security_group" "rds" {
  name_prefix = "${var.project_name}-${var.environment}-rds-"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
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
    Name = "${var.project_name}-${var.environment}-rds-sg"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# Random password for RDS
resource "random_password" "db_password" {
  count   = var.manage_master_user_password ? 0 : 1
  length  = 32
  special = true
}

# AWS Secrets Manager secret for database credentials
resource "aws_secretsmanager_secret" "db_credentials" {
  count                   = var.manage_master_user_password ? 0 : 1
  name                    = "${var.project_name}-${var.environment}-db-credentials"
  description             = "Database credentials for ${var.project_name} ${var.environment}"
  recovery_window_in_days = var.environment == "prod" ? 30 : 0

  tags = merge(var.tags, {
    Name        = "${var.project_name}-${var.environment}-db-credentials"
    Environment = var.environment
  })
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  count     = var.manage_master_user_password ? 0 : 1
  secret_id = aws_secretsmanager_secret.db_credentials[0].id
  secret_string = jsonencode({
    username = var.db_username
    password = random_password.db_password[0].result
    engine   = "postgres"
    host     = aws_db_instance.main.address
    port     = aws_db_instance.main.port
    dbname   = var.db_name
  })
}

# RDS Instance
resource "aws_db_instance" "main" {
  identifier = "${var.project_name}-${var.environment}-db"

  # Engine configuration
  engine         = "postgres"
  engine_version = var.db_engine_version
  instance_class = var.db_instance_class

  # Database configuration
  db_name  = var.db_name
  username = var.db_username
  password = var.manage_master_user_password ? null : random_password.db_password[0].result

  # Storage configuration
  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_max_allocated_storage
  storage_type          = var.db_storage_type
  storage_encrypted     = true
  kms_key_id           = var.kms_key_id

  # Network and security
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  parameter_group_name   = aws_db_parameter_group.main.name

  # Backup configuration
  backup_retention_period = var.backup_retention_period
  backup_window          = var.backup_window
  maintenance_window     = var.maintenance_window
  delete_automated_backups = var.environment != "prod"

  # High availability
  multi_az               = var.multi_az
  publicly_accessible    = false
  copy_tags_to_snapshot  = true

  # Security and management
  deletion_protection = var.environment == "prod"
  skip_final_snapshot = var.environment != "prod"
  final_snapshot_identifier = var.environment == "prod" ? "${var.project_name}-${var.environment}-db-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}" : null

  # AWS managed master user password (RDS manages the secret)
  manage_master_user_password = var.manage_master_user_password

  # Monitoring
  monitoring_interval = var.monitoring_interval
  monitoring_role_arn = var.monitoring_interval > 0 ? aws_iam_role.rds_enhanced_monitoring[0].arn : null

  # Performance Insights
  performance_insights_enabled = var.performance_insights_enabled
  performance_insights_retention_period = var.performance_insights_enabled ? var.performance_insights_retention_period : null

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-db"
  })

  depends_on = [
    aws_db_subnet_group.main,
    aws_db_parameter_group.main
  ]
}

# Enhanced Monitoring IAM Role
resource "aws_iam_role" "rds_enhanced_monitoring" {
  count = var.monitoring_interval > 0 ? 1 : 0
  name  = "${var.project_name}-${var.environment}-rds-monitoring-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "monitoring.rds.amazonaws.com"
        }
      }
    ]
  })

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "rds_enhanced_monitoring" {
  count      = var.monitoring_interval > 0 ? 1 : 0
  role       = aws_iam_role.rds_enhanced_monitoring[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

# Read replica (optional, for prod environment)
resource "aws_db_instance" "read_replica" {
  count = var.create_read_replica ? 1 : 0

  identifier             = "${var.project_name}-${var.environment}-db-read-replica"
  replicate_source_db    = aws_db_instance.main.identifier
  instance_class         = var.read_replica_instance_class
  publicly_accessible    = false
  auto_minor_version_upgrade = true

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-db-read-replica"
  })
}
