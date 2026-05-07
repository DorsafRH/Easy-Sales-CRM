package com.crm.modules.agenda.dto;

import com.crm.shared.enums.StatutReunion;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO de réponse représentant une réunion client.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class ReunionResponse {

    private Long          id;
    private String        titre;

    /** Date et heure ISO : "2026-05-10T14:00:00". */
    private String        dateHeure;
    private int           dureeMinutes;
    private String        lieu;
    private String        notes;
    private StatutReunion statut;

    /** Indique si la réunion se tient en ligne. */
    private boolean       enLigne;

    /**
     * Lien de la réunion (Jitsi, Google Meet, Teams…).
     * Null si réunion en présentiel.
     */
    private String        lienReunion;

    // ─────────────────────────────────────────────────────────
    //  Client
    // ─────────────────────────────────────────────────────────

    private Long          clientId;
    private String        clientNom;

    // ─────────────────────────────────────────────────────────
    //  Participants
    // ─────────────────────────────────────────────────────────

    /**
     * Liste des participants (client, contacts, externes).
     * Enrichie côté service avec les noms résolus.
     */
    private List<ReunionParticipantDto> participants;

    // ─────────────────────────────────────────────────────────
    //  Rappels
    // ─────────────────────────────────────────────────────────

    /** Rappels en minutes avant la réunion. */
    private List<Integer> rappelsMinutes;

    // ─────────────────────────────────────────────────────────
    //  Dates
    // ─────────────────────────────────────────────────────────

    /** Date relative (ex: "dans 2 h", "demain", "il y a 1 j"). */
    private String        dateRelative;

    /** Date de création ISO. */
    private String        dateCreation;
}