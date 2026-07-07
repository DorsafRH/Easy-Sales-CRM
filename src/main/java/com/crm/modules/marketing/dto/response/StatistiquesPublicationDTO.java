package com.crm.modules.marketing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Statistiques Facebook d'une publication PUBLIEE : portée, engagement
 * et courbe des vues journalières (fiche publication mobile).
 *
 * @author Riahi Dorsaf
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesPublicationDTO {

    /** Impressions totales du post (post_impressions). */
    private long vues;

    /** Personnes uniques atteintes (post_impressions_unique). */
    private long vuesUniques;

    /** Nombre total de réactions (like, love, wow...). */
    private long reactions;

    /** Nombre de commentaires. */
    private long commentaires;

    /** Nombre de partages. */
    private long partages;

    /** Date de publication effective. */
    private LocalDateTime datePublication;

    /** Vues jour par jour depuis la publication. */
    private List<PointStatistiqueDTO> courbeVuesJournalieres;

    // ─────────────────────────────────────────────────────────

    /** Point d'une série temporelle journalière (date ISO + valeur). */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointStatistiqueDTO {

        /** Date du point au format yyyy-MM-dd. */
        private String date;

        /** Valeur mesurée ce jour-là. */
        private long valeur;
    }
}
