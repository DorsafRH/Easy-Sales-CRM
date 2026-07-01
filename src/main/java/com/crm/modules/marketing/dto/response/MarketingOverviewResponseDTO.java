package com.crm.modules.marketing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Vue d'ensemble du dashboard marketing : indicateurs de leads issus des réseaux
 * sociaux, état des publications et engagement agrégé (réactions). Construit à partir
 * des données du CRM, complété par l'engagement public collecté/injecté.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class MarketingOverviewResponseDTO {

    // ── Leads marketing ──────────────────────────────────────────
    private long leadsMarketing;
    private long leadsQualifies;
    private int scoreMoyen;
    private double tauxConversion;
    private List<RepartitionSourceDTO> repartitionSource;
    private List<LeadParMoisDTO> leadsParMois;
    private List<BesoinRecentDTO> derniersBesoins;

    // ── Publications ─────────────────────────────────────────────
    private long publicationsPubliees;
    private long publicationsProgrammees;
    private long publicationsBrouillons;

    // ── Engagement (réactions agrégées) ──────────────────────────
    private ReactionsAggregatDTO reactions;

    @Data
    @Builder
    public static class RepartitionSourceDTO {
        private String source;
        private long count;
    }

    @Data
    @Builder
    public static class LeadParMoisDTO {
        /** Format AAAA-MM. */
        private String mois;
        private long count;
    }

    @Data
    @Builder
    public static class BesoinRecentDTO {
        private String nom;
        private String besoin;
        private Integer score;
        private String dateRelative;
    }

    @Data
    @Builder
    public static class ReactionsAggregatDTO {
        private int totalLikes;
        private int totalComments;
        private int totalShares;
        /** Détail par emoji : like, love, wow, sad, angry, haha. */
        private Map<String, Integer> breakdown;
    }
}
