# ============================================================
#  Sorties — affichées après `terraform apply`
# ============================================================

output "acr_login_server" {
  description = "Adresse du registre (pour docker push)"
  value       = azurerm_container_registry.acr.login_server
}

output "acr_admin_username" {
  description = "Login ACR (pour docker login)"
  value       = azurerm_container_registry.acr.admin_username
}

output "acr_admin_password" {
  description = "Mot de passe ACR (pour docker login)"
  value       = azurerm_container_registry.acr.admin_password
  sensitive   = true
}

output "backend_url" {
  description = "URL publique HTTPS du backend (à mettre dans le mobile, l'Angular et le dashboard Meta)"
  value       = "https://${azurerm_container_app.backend.ingress[0].fqdn}/api"
}

output "postgres_fqdn" {
  description = "Nom d'hôte du serveur PostgreSQL"
  value       = azurerm_postgresql_flexible_server.pg.fqdn
}

output "static_web_app_default_host" {
  description = "URL publique du front Angular (Static Web App)"
  value       = azurerm_static_web_app.frontend.default_host_name
}

output "static_web_app_api_token" {
  description = "Token de déploiement (à mettre dans le secret GitHub AZURE_STATIC_WEB_APPS_API_TOKEN du repo front)"
  value       = azurerm_static_web_app.frontend.api_key
  sensitive   = true
}
