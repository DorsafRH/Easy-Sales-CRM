# ============================================================
#  Terraform — fournisseurs requis
# ============================================================
terraform {
  required_version = ">= 1.5.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 4.1.0"
    }
  }
}

provider "azurerm" {
  subscription_id = var.subscription_id # obligatoire en azurerm v4
  tenant_id       = var.tenant_id
  features {}
}
