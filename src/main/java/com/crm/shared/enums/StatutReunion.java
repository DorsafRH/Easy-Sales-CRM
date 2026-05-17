package com.crm.shared.enums;

/**
 * Statuts possibles d'une réunion client.
 *
 * <ul>
 *   <li>{@link #PLANIFIEE}  — réunion créée, pas encore tenue</li>
 *   <li>{@link #TERMINEE}   — réunion tenue avec succès</li>
 *   <li>{@link #ANNULEE}    — réunion annulée avant sa tenue</li>
 * </ul>
 *
 * @author Riahi Dorsaf
 */
public enum StatutReunion {

    /**
     * Réunion planifiée — en attente.
     */
    PLANIFIEE,

    /**
     * Réunion tenue avec succès.
     */
    TERMINEE,

    /**
     * Réunion annulée avant sa tenue.
     */
    ANNULEE,
}