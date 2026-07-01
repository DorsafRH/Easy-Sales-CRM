package com.crm.shared.enums;

/**
 * Portée d'une publication générée par IA : détermine quelles données du catalogue
 * le backend récupère pour construire le prompt (l'utilisateur ne ressaisit jamais
 * les données produit).
 *
 * @author Riahi Dorsaf
 */
public enum PorteePublication {
    /** Un ou plusieurs produits précis (par identifiant). */
    PRODUITS,
    /** Une catégorie entière. */
    CATEGORIE,
    /** Toute la boutique (post générique + remise éventuelle). */
    BOUTIQUE,
    /** Rien de précis : post piloté uniquement par la consigne libre. */
    LIBRE
}
