package com.crm.shared.enums;

/**
 * Types d'activité enregistrés dans le journal d'activité CRM.
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

    // ── Sprint 3 — Leads ─────────────────────────────────────
    LEAD_CREE,
    LEAD_MODIFIE,
    LEAD_QUALIFIE,
    LEAD_CONVERTI,
    LEAD_PERDU,

    // ── Sprint 3 — Opportunités ───────────────────────────────
    OPPORTUNITE_CREEE,
    OPPORTUNITE_MODIFIEE,
    OPPORTUNITE_STATUT_CHANGE,
    OPPORTUNITE_GAGNEE,
    OPPORTUNITE_PERDUE,

    // ── Sprint 3 — Devis ──────────────────────────────────────
    DEVIS_CREE,
    DEVIS_ENVOYE,
    DEVIS_ACCEPTE,
    DEVIS_REFUSE,

    // ── Sprint 3 — Factures ───────────────────────────────────
    FACTURE_CREEE,
    FACTURE_EMISE,
    FACTURE_LIVREE,
    FACTURE_PAYEE,
    FACTURE_ANNULEE,

    // ── Sprint 4 ──────────────────────────────────────────────
    PUBLICATION_CREEE,
}