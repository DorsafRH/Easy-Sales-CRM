package com.crm.shared.enums;

/**
 * Types d'activité enregistrés dans le journal d'activité CRM.
 * Chaque valeur correspond à une action métier traçable.
 *
 * @author Riahi Dorsaf
 */
public enum TypeActivite {

    // ── Clients ──────────────────────────────────────────────
    CLIENT_CREE,
    CLIENT_MODIFIE,
    CLIENT_SUPPRIME,

    // ── Contacts ─────────────────────────────────────────────
    CONTACT_AJOUTE,
    CONTACT_MODIFIE,
    CONTACT_SUPPRIME,

    // ── Catalogue ────────────────────────────────────────────
    PRODUIT_CREE,
    PRODUIT_MODIFIE,
    PRODUIT_ARCHIVE,
    PRODUIT_DESARCHIVE,
    PRODUIT_ACTIVE,
    PRODUIT_DESACTIVE,

    // ── Sprints futurs ───────────────────────────────────────
    OPPORTUNITE_CREEE,    // Sprint 3
    DEVIS_CREE,           // Sprint 3
    PUBLICATION_CREEE,    // Sprint 4
}