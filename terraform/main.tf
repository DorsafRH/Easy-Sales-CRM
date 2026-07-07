# ============================================================
#  Infrastructure Azure — PFE Easy Sales
#  Resource Group + ACR + PostgreSQL managé + Container Apps (backend serverless)
#  ⚠️ Container Apps : pas de quota VM (contourne le quota App Service B1 = 0),
#     HTTPS automatique, facturation à l'usage. Postgres B1ms ~12$/mois + ACR ~5$/mois.
#     Pense à `terraform destroy` quand tu ne t'en sers pas pour préserver le crédit.
# ============================================================

# ---------- Groupe de ressources (le "dossier" qui contient tout) ----------
resource "azurerm_resource_group" "rg" {
  name     = var.resource_group_name
  location = var.location
}

# ---------- Azure Container Registry (stocke l'image Docker du backend) ----------
resource "azurerm_container_registry" "acr" {
  name                = var.acr_name
  resource_group_name = azurerm_resource_group.rg.name
  location            = azurerm_resource_group.rg.location
  sku                 = "Basic"
  admin_enabled       = true # permet le docker login user/password (simple pour un PFE)
}

# ---------- PostgreSQL Flexible Server (base de données managée) ----------
resource "azurerm_postgresql_flexible_server" "pg" {
  name                          = var.pg_server_name
  resource_group_name           = azurerm_resource_group.rg.name
  location                      = azurerm_resource_group.rg.location
  version                       = "16"
  administrator_login           = var.pg_admin_login
  administrator_password        = var.pg_admin_password
  sku_name                      = "B_Standard_B1ms" # Burstable, le moins cher
  storage_mb                    = 32768             # 32 Go (minimum)
  public_network_access_enabled = true
  zone                          = "1"
}

resource "azurerm_postgresql_flexible_server_database" "db" {
  name      = var.pg_database_name
  server_id = azurerm_postgresql_flexible_server.pg.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

# Autorise les services Azure (dont la Container App) à joindre la base.
# 0.0.0.0/0.0.0.0 = règle spéciale "Allow Azure services".
resource "azurerm_postgresql_flexible_server_firewall_rule" "allow_azure" {
  name             = "allow-azure-services"
  server_id        = azurerm_postgresql_flexible_server.pg.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}

# ---------- Log Analytics (requis par l'environnement Container Apps) ----------
resource "azurerm_log_analytics_workspace" "law" {
  name                = "easysales-logs"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
}

# ---------- Environnement Container Apps ----------
resource "azurerm_container_app_environment" "env" {
  name                       = "easysales-env"
  location                   = azurerm_resource_group.rg.location
  resource_group_name        = azurerm_resource_group.rg.name
  log_analytics_workspace_id = azurerm_log_analytics_workspace.law.id
}

# ---------- Container App (backend serverless, HTTPS automatique) ----------
resource "azurerm_container_app" "backend" {
  name                         = var.app_service_name
  container_app_environment_id = azurerm_container_app_environment.env.id
  resource_group_name          = azurerm_resource_group.rg.name
  revision_mode                = "Single"

  # Identifiants ACR (le mot de passe passe par un secret de la Container App)
  registry {
    server               = azurerm_container_registry.acr.login_server
    username             = azurerm_container_registry.acr.admin_username
    password_secret_name = "acr-password"
  }
  secret {
    name  = "acr-password"
    value = azurerm_container_registry.acr.admin_password
  }

  # Accès HTTPS public ; le trafic arrive sur le port 8080 du conteneur
  ingress {
    external_enabled = true
    target_port      = 8080
    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  template {
    min_replicas = 1 # 1 instance active en continu (évite les démarrages à froid lents de Spring)
    max_replicas = 1

    container {
      name   = "backend"
      image  = "${azurerm_container_registry.acr.login_server}/${var.app_service_name}:${var.backend_image_tag}"
      cpu    = 0.5
      memory = "1Gi"

      # Variables d'environnement = les ${VAR} attendus par application.yml
      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "azure"
      }
      env {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://${azurerm_postgresql_flexible_server.pg.fqdn}:5432/${var.pg_database_name}?sslmode=require"
      }
      env {
        name  = "DB_USERNAME"
        value = var.pg_admin_login
      }
      env {
        name  = "DB_PASSWORD"
        value = var.pg_admin_password
      }
      env {
        name  = "MAIL_USERNAME"
        value = var.mail_username
      }
      env {
        name  = "MAIL_PASSWORD"
        value = var.mail_password
      }
      env {
        name  = "JWT_SECRET"
        value = var.jwt_secret
      }
      env {
        name  = "ADMIN_EMAIL"
        value = var.admin_email
      }
      env {
        name  = "ADMIN_PASSWORD"
        value = var.admin_password
      }
      env {
        name  = "ADMIN_NOM"
        value = var.admin_nom
      }
      env {
        name  = "ADMIN_PRENOM"
        value = var.admin_prenom
      }
      env {
        name  = "GROQ_API_KEY"
        value = var.groq_api_key
      }
      env {
        name  = "META_APP_ID"
        value = var.meta_app_id
      }
      env {
        name  = "META_APP_SECRET"
        value = var.meta_app_secret
      }
      env {
        name  = "META_REDIRECT_URI"
        value = var.meta_redirect_uri
      }
      env {
        name  = "REPORTING_CALLBACK_SECRET"
        value = var.reporting_callback_secret
      }
      # Exigé au démarrage par MarketingCallbackGuard (Sprint 4), même si le module
      # Messenger n'est PAS utilisé en prod (App Review Meta) -> valeur aléatoire dédiée.
      env {
        name  = "MESSENGER_CALLBACK_SECRET"
        value = var.messenger_callback_secret
      }
      env {
        name  = "REPORTING_SCHEDULER_ENABLED"
        value = "true"
      }
    }
  }
}

# ---------- Container App front admin Angular (nginx) ----------
# ⚠️ Static Web App impossible : la policy de l'abonnement étudiant n'autorise aucune
#    des régions SWA (403 RequestDisallowedByAzure). On sert donc l'Angular compilé
#    via une image nginx dans le MÊME environnement Container Apps que le backend.
# ⚠️ Œuf-poule : l'image doit exister dans l'ACR AVANT le premier apply
#    (le pipeline du repo front la pousse) — même contrainte que le backend.
resource "azurerm_container_app" "frontend" {
  name                         = var.frontend_app_name
  container_app_environment_id = azurerm_container_app_environment.env.id
  resource_group_name          = azurerm_resource_group.rg.name
  revision_mode                = "Single"

  registry {
    server               = azurerm_container_registry.acr.login_server
    username             = azurerm_container_registry.acr.admin_username
    password_secret_name = "acr-password"
  }
  secret {
    name  = "acr-password"
    value = azurerm_container_registry.acr.admin_password
  }

  # HTTPS public ; nginx écoute sur le port 80 du conteneur
  ingress {
    external_enabled = true
    target_port      = 80
    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  template {
    # Fichiers statiques : nginx démarre en <1s -> scale-to-zero sans douleur (0 coût au repos)
    min_replicas = 0
    max_replicas = 1

    container {
      name   = "frontend"
      image  = "${azurerm_container_registry.acr.login_server}/${var.frontend_app_name}:${var.frontend_image_tag}"
      cpu    = 0.25
      memory = "0.5Gi"
    }
  }
}
