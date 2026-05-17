package com.crm.modules.agenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de création et modification d'une réunion.
 *
 * @author Riahi Dorsaf
 */
@Data
public class ReunionRequest {

    // ─────────────────────────────────────────────────────────
    //  Champs obligatoires
    // ─────────────────────────────────────────────────────────

    /**
     * Titre de la réunion.
     */
    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
    private String titre;

    /**
     * Date et heure de début.
     */
    @NotNull(message = "La date et l'heure sont obligatoires")
    private LocalDateTime dateHeure;

    /**
     * Durée en minutes.
     * Entre 15 min et 480 min (8 heures).
     */
    @NotNull(message = "La durée est obligatoire")
    @Min(value = 15, message = "La durée minimale est de 15 minutes")
    @Max(value = 480, message = "La durée maximale est de 8 heures")
    private Integer dureeMinutes;

    /**
     * Identifiant du client principal.
     */
    @NotNull(message = "Le client est obligatoire")
    private Long clientId;

    // ─────────────────────────────────────────────────────────
    //  Champs optionnels
    // ─────────────────────────────────────────────────────────

    /**
     * Lieu physique de la réunion.
     */
    @Size(max = 200)
    private String lieu;

    /**
     * Notes libres.
     */
    private String notes;

    /**
     * Indique si la réunion se tient en ligne.
     * Si {@code true} et {@link #lienReunion} est vide,
     * un lien Jitsi Meet est généré automatiquement.
     */
    private Boolean enLigne = false;

    /**
     * Lien de la réunion en ligne — générique.
     * Peut être un lien Jitsi Meet, Google Meet, Teams, etc.
     * Si vide et {@link #enLigne} = true, généré automatiquement.
     */
    @Size(max = 500)
    private String lienReunion;

    /**
     * Liste des participants à la réunion.
     * Peut contenir le client, ses contacts et des invités externes.
     */
    @Valid
    private List<ReunionParticipantDto> participants = new ArrayList<>();

    /**
     * Liste des rappels en minutes avant la réunion.
     * Ex : [30, 1440] = 30 min avant + 1 jour avant.
     */
    private List<Integer> rappelsMinutes = new ArrayList<>();

    /**
     * Indique si une invitation email doit être envoyée aux participants.
     * L'invitation contient le lien de réunion + un fichier .ics (calendrier).
     */
    private Boolean envoyerInvitation = false;
}