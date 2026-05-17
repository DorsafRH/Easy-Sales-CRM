package com.crm.shared.enums;

/**
 * Types de participants à une réunion CRM.
 *
 * <ul>
 *   <li>{@link #CLIENT}   — le client principal de la réunion</li>
 *   <li>{@link #CONTACT}  — un contact du client</li>
 *   <li>{@link #EXTERNE}  — collaborateur, partenaire ou invité externe</li>
 * </ul>
 *
 * @author Riahi Dorsaf
 */
public enum TypeParticipant {

    /**
     * Client principal de la réunion.
     */
    CLIENT,

    /**
     * Contact rattaché au client.
     */
    CONTACT,

    /**
     * Participant externe (collaborateur, partenaire…).
     */
    EXTERNE,
}