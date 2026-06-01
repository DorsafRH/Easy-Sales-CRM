package com.crm.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de réponse pour les statistiques de vente avancées du dashboard.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class StatsVentesResponse {

    /** Nombre de leads actifs (hors CONVERTI et PERDU). */
    private long nbLeadsActifs;

    /** Taux de conversion lead → client (CONVERTI / total, en %). */
    private double tauxConversionLeads;

    /** Somme des montantEstime des opportunités actives (hors GAGNEE/PERDUE). */
    private BigDecimal valeurPipeline;

    /** Taux de conversion opportunités (GAGNEE / total, en %). */
    private double tauxConversionOpportunites;

    /** Taux d'acceptation des devis (ACCEPTE / non-BROUILLON, en %). */
    private double tauxAcceptationDevis;

    /** Panier moyen = CA total PAYEE / nombre de factures PAYEE. */
    private BigDecimal panierMoyen;

    /** Chiffre d'affaires cumulé sur l'année civile en cours (factures PAYEE). */
    private BigDecimal caAnnuel;

    /** Répartition du nombre d'opportunités par statut. */
    private List<StatutOpportuniteCount> repartitionOpportunites;

    /** Top 3 des opportunités actives classées par montantEstime décroissant. */
    private List<OpportuniteResume> top3Opportunites;

    /** Nombre d'opportunités pour un statut donné. */
    @Data
    @Builder
    public static class StatutOpportuniteCount {
        private String statut;
        private long   count;
    }

    /** Résumé d'une opportunité pour le widget Top 3. */
    @Data
    @Builder
    public static class OpportuniteResume {
        private Long       id;
        private String     titre;
        private String     clientNom;
        private BigDecimal montantEstime;
        private String     statut;
    }
}
