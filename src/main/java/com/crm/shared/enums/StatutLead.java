package com.crm.shared.enums;

/**
 * Statuts du cycle de vie d'un lead (prospect).
 * Correspond aux 6 colonnes du pipeline Kanban mobile.
 *
 * @author Riahi Dorsaf
 */
public enum StatutLead {
    NOUVEAU,
    CONTACTE,
    QUALIFIE,
    PROPOSITION,
    NEGOCIATION,
    CONVERTI,
    PERDU
}