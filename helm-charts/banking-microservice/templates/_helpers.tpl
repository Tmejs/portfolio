{{/*
Expand the name of the chart.
*/}}
{{- define "banking-microservice.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "banking-microservice.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "banking-microservice.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "banking-microservice.labels" -}}
helm.sh/chart: {{ include "banking-microservice.chart" . }}
{{ include "banking-microservice.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "banking-microservice.selectorLabels" -}}
app.kubernetes.io/name: {{ include "banking-microservice.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "banking-microservice.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "banking-microservice.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Environment-specific values merge helper
*/}}
{{- define "banking-microservice.environmentValues" -}}
{{- $environment := .Values.app.profile -}}
{{- $envValues := index .Values.environments $environment -}}
{{- if $envValues -}}
{{- toYaml $envValues -}}
{{- end -}}
{{- end }}

{{/*
Get environment-specific replica count
*/}}
{{- define "banking-microservice.replicaCount" -}}
{{- $environment := .Values.app.profile -}}
{{- $envValues := index .Values.environments $environment -}}
{{- if and $envValues $envValues.replicaCount -}}
{{- $envValues.replicaCount -}}
{{- else -}}
{{- .Values.replicaCount -}}
{{- end -}}
{{- end }}

{{/*
Get environment-specific resources
*/}}
{{- define "banking-microservice.resources" -}}
{{- $environment := .Values.app.profile -}}
{{- $envValues := index .Values.environments $environment -}}
{{- if and $envValues $envValues.resources -}}
{{- toYaml $envValues.resources -}}
{{- else -}}
{{- toYaml .Values.resources -}}
{{- end -}}
{{- end }}

{{/*
Get environment-specific autoscaling
*/}}
{{- define "banking-microservice.autoscaling" -}}
{{- $environment := .Values.app.profile -}}
{{- $envValues := index .Values.environments $environment -}}
{{- if and $envValues $envValues.autoscaling -}}
{{- toYaml $envValues.autoscaling -}}
{{- else -}}
{{- toYaml .Values.autoscaling -}}
{{- end -}}
{{- end }}
