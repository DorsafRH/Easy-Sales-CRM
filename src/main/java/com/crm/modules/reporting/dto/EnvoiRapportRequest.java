package com.crm.modules.reporting.dto;

import com.crm.shared.enums.PeriodeRapport;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Corps de la requête de diffusion d'un rapport commercial par email.
 *
 * <p>Envoyé par l'orchestration n8n après génération du rapport : n8n y ajoute la
 * synthèse rédigée par l'IA ({@code syntheseIa}). Le backend reste la source de vérité
 * des chiffres : il <b>régénère</b> le rapport à partir de {@code proprietaireId} +
 * {@code periode}, injecte la synthèse IA, puis envoie l'email.</p>
 *
 * @author Riahi Dorsaf
 */
@Data
public class EnvoiRapportRequest {

    /** Propriétaire destinataire du rapport. */
    @NotNull
    private Long proprietaireId;

    /** Période complète écoulée : SEMAINE | MOIS | ANNEE. */
    @NotNull
    private PeriodeRapport periode;

    /** Synthèse rédigée par l'IA (Groq), optionnelle — ajoutée par n8n. */
    private String syntheseIa;
}
