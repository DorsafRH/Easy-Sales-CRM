package com.crm.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de réponse pour les KPIs du module reporting.
 *
 * Sprint 2 : nbClients réel — autres KPIs à 0 (câblés en Sprint 3).
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class ReportingKpisResponse {

    /** Nombre total de clients actifs — réel Sprint 2. */
    private long nbClients;

    /** Nombre d'opportunités en cours — 0 Sprint 2, réel Sprint 3. */
    private long nbOpportunites;

    /** Chiffre d'affaires du mois — 0.0 Sprint 2, réel Sprint 3. */
    private BigDecimal chiffreAffaires;

    /** Nombre de devis en attente — 0 Sprint 2, réel Sprint 3. */
    private long nbDevis;

    /**
     * Données sparkline (7 derniers jours).
     * Sprint 2 : [0, 0, 0, 0, 0, 0, 0].
     */
    private List<Integer> sparkline;

    /**
     * Activité récente : 5 derniers clients créés.
     */
    private List<ActiviteRecenteItem> activiteRecente;

    @Data
    @Builder
    public static class ActiviteRecenteItem {
        private Long   id;
        private String type;         // "CLIENT"
        private String titre;        // nomAffichage du client
        private String soustitre;    // "Individuel" | "Entreprise"
        private String dateRelative; // "il y a 2 h"
    }
}