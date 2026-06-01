package com.crm.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO représentant le chiffre d'affaires d'un mois donné.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class CaMensuelDto {

    /** Numéro du mois (1 = janvier … 12 = décembre). */
    private int mois;

    /** Année du mois. */
    private int annee;

    /** Somme des montants TTC des factures PAYÉE pour ce mois. */
    private BigDecimal montant;

    /** Étiquette courte affichée sur l'axe X du graphique (ex. "Jan", "Fév"). */
    private String label;
}
