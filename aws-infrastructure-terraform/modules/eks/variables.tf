variable "cluster_name" {
  description = "Name of the EKS cluster"
  type        = string
}

variable "kubernetes_version" {
  description = "Kubernetes version for the EKS cluster"
  type        = string
  default     = "1.28"
}

variable "vpc_id" {
  description = "VPC ID where EKS cluster will be created"
  type        = string
}

variable "private_subnet_ids" {
  description = "List of private subnet IDs"
  type        = list(string)
}

variable "public_subnet_ids" {
  description = "List of public subnet IDs"
  type        = list(string)
}

variable "tags" {
  description = "A map of tags to add to all resources"
  type        = map(string)
  default     = {}
}

# Node Group Configuration
variable "instance_types" {
  description = "List of instance types for the EKS node group"
  type        = list(string)
  default     = ["t3.medium"]
}

variable "capacity_type" {
  description = "Type of capacity associated with the EKS Node Group (ON_DEMAND or SPOT)"
  type        = string
  default     = "ON_DEMAND"
}

variable "scaling_config" {
  description = "Scaling configuration for the EKS node group"
  type = object({
    desired_size = number
    max_size     = number
    min_size     = number
  })
  default = {
    desired_size = 2
    max_size     = 4
    min_size     = 1
  }
}

variable "disk_size" {
  description = "Disk size in GiB for worker nodes"
  type        = number
  default     = 20
}

variable "ami_type" {
  description = "Type of Amazon Machine Image (AMI) associated with the EKS Node Group"
  type        = string
  default     = "AL2_x86_64"
}

# Networking Configuration
variable "endpoint_private_access" {
  description = "Enable private API server endpoint"
  type        = bool
  default     = true
}

variable "endpoint_public_access" {
  description = "Enable public API server endpoint"
  type        = bool
  default     = true
}

variable "public_access_cidrs" {
  description = "List of CIDR blocks that can access the public API server endpoint"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# Logging Configuration
variable "cluster_log_types" {
  description = "List of control plane logging to enable"
  type        = list(string)
  default     = ["api", "audit", "authenticator", "controllerManager", "scheduler"]
}

variable "log_retention_in_days" {
  description = "Number of days to retain log events"
  type        = number
  default     = 7
}

# Service Account Configuration
variable "enable_irsa" {
  description = "Enable IAM Roles for Service Accounts"
  type        = bool
  default     = true
}

variable "oidc_root_ca_thumbprint" {
  description = "Thumbprint of Root CA for EKS OIDC, defaults to AWS default"
  type        = string
  default     = "9e99a48a9960b14926bb7f3b02e22da2b0ab7280"
}

# Node Group IAM Configuration
variable "node_group_role_policies" {
  description = "Additional IAM policy ARNs to attach to the node group role"
  type        = list(string)
  default     = []
}

# Addon Configuration
variable "addons" {
  description = "Map of EKS addons to install"
  type = map(object({
    addon_version            = optional(string)
    resolve_conflicts        = optional(string, "OVERWRITE")
    service_account_role_arn = optional(string)
  }))
  default = {
    vpc-cni = {
      addon_version     = null
      resolve_conflicts = "OVERWRITE"
    }
    coredns = {
      addon_version     = null
      resolve_conflicts = "OVERWRITE"
    }
    kube-proxy = {
      addon_version     = null
      resolve_conflicts = "OVERWRITE"
    }
  }
}

# Security
variable "cluster_security_group_additional_rules" {
  description = "Additional security group rules to add to the cluster security group"
  type        = any
  default     = {}
}

# Launch Template
variable "use_custom_launch_template" {
  description = "Use custom launch template for worker nodes"
  type        = bool
  default     = false
}

variable "user_data" {
  description = "User data script for worker nodes"
  type        = string
  default     = ""
}
