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

output "frontend_url" {
  description = "URL publique HTTPS du front admin Angular"
  value       = "https://${azurerm_container_app.frontend.ingress[0].fqdn}"
}
