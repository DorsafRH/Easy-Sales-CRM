# ============================================================
#  Variables d'entrée — valeurs fournies dans terraform.tfvars
# ============================================================

# ---- Identité Azure ----
variable "subscription_id" {
  description = "ID de l'abonnement Azure (Azure for Students)"
  type        = string
}

variable "tenant_id" {
  description = "ID du tenant Azure"
  type        = string
}

# ---- Nommage / localisation ----
# ⚠️ Azure for Students : régions autorisées = France Central, Poland Central,
#    Germany West Central, Sweden Central, UAE North (West Europe/East US => 403).
#    Postgres managé CONFIRMÉ en France Central (interdit en Germany West Central : "OfferRestricted").
#    On abandonne App Service (quota B1 = 0) pour Container Apps (serverless, sans quota VM)
#    => France Central redevient viable pour TOUT.
variable "location" {
  description = "Région Azure (France Central : Postgres + ACR + Container Apps OK)"
  type        = string
  default     = "France Central"
}

variable "resource_group_name" {
  description = "Nom du groupe de ressources"
  type        = string
  default     = "easysales-rg"
}

variable "acr_name" {
  description = "Nom du registre de conteneurs (GLOBALEMENT unique, alphanumérique, 5-50 car.)"
  type        = string
  # ex : "easysalesacr2026" — à personnaliser dans terraform.tfvars
}

variable "app_service_name" {
  description = "Nom de l'App Service backend (fait partie de l'URL .azurewebsites.net, unique)"
  type        = string
  default     = "easysales-backend"
}

variable "frontend_app_name" {
  description = "Nom de la Container App front admin Angular (aussi le nom de l'image dans l'ACR)"
  type        = string
  default     = "easysales-admin-web"
}

variable "frontend_image_tag" {
  description = "Tag de l'image front à déployer (ex: latest ou le SHA du commit)"
  type        = string
  default     = "latest"
}

variable "backend_image_tag" {
  description = "Tag de l'image backend à déployer (ex: latest ou le SHA du commit)"
  type        = string
  default     = "latest"
}

# ---- Base de données PostgreSQL managée ----
variable "pg_server_name" {
  description = "Nom du serveur PostgreSQL Flexible (unique)"
  type        = string
  default     = "easysales-pg"
}

variable "pg_database_name" {
  description = "Nom de la base applicative"
  type        = string
  default     = "crm_db"
}

variable "pg_admin_login" {
  description = "Login administrateur PostgreSQL"
  type        = string
  default     = "pgadmin"
}

variable "pg_admin_password" {
  description = "Mot de passe administrateur PostgreSQL"
  type        = string
  sensitive   = true
}

# ---- Secrets applicatifs (injectés dans l'App Service) ----
variable "jwt_secret" {
  type      = string
  sensitive = true
}

variable "mail_username" {
  type = string
}

variable "mail_password" {
  type      = string
  sensitive = true
}

variable "admin_email" {
  type = string
}

variable "admin_password" {
  type      = string
  sensitive = true
}

variable "admin_nom" {
  type    = string
  default = "Admin"
}

variable "admin_prenom" {
  type    = string
  default = "Super"
}

variable "groq_api_key" {
  type      = string
  sensitive = true
}

variable "meta_app_id" {
  type    = string
  default = ""
}

variable "meta_app_secret" {
  type      = string
  sensitive = true
  default   = ""
}

variable "meta_redirect_uri" {
  description = "URL de callback OAuth Meta (sera l'URL Azure une fois déployée)"
  type        = string
  default     = ""
}

variable "reporting_callback_secret" {
  type      = string
  sensitive = true
}

variable "messenger_callback_secret" {
  description = "Secret du callback Messenger (module non utilisé en prod : mettre une valeur aléatoire)"
  type        = string
  sensitive   = true
}
