package com.crm.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de réponse des KPIs du tableau de bord.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class ReportingKpisResponse {

    /**
     * Nombre de clients actifs sur la période sélectionnée.
     */
    private Long nbClients;

    /**
     * Nombre d'opportunités — Sprint 3.
     */
    private Long nbOpportunites;

    /**
     * Chiffre d'affaires — Sprint 3.
     */
    private BigDecimal chiffreAffaires;

    /**
     * Nombre de devis — Sprint 3.
     */
    private Long nbDevis;

    /**
     * Points du graphique sparkline — Sprint 3.
     */
    private List<Integer> sparkline;

    /**
     * 10 dernières activités pour la section "Activité récente".
     */
    private List<ActiviteRecenteItem> activiteRecente;

    /**
     * Élément d'activité récente affiché dans le Dashboard.
     */
    @Data
    @Builder
    public static class ActiviteRecenteItem {

        /**
         * ID de l'entité concernée (clientId, contactId, produitId…).
         */
        private Long id;

        /**
         * Type de l'entité pour la navigation mobile.
         * Valeurs : "CLIENT", "CONTACT", "PRODUIT".
         */
        private String type;

        /**
         * Type précis de l'activité pour l'icône mobile.
         * Ex : "CLIENT_CREE", "CONTACT_AJOUTE", "PRODUIT_ARCHIVE".
         */
        private String typeActivite;

        /**
         * Label de l'action — titre principal de l'item.
         * Ex : "Nouveau client ajouté", "Produit archivé".
         */
        private String titre;

        /**
         * Nom de l'entité concernée — sous-titre.
         * Ex : "Ahmed Ben Ali", "Logiciel CRM Pro".
         */
        private String soustitre;

        /**
         * Date relative affichée (ex : "il y a 5 min").
         */
        private String dateRelative;

        /**
         * ID de l'entité parente — null si absent.
         * Pour les contacts : clientId pour la navigation vers ContactDetail.
         */
        private Long entiteParentId;
    }
}