package com.crm.modules.vente.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class LigneDevisResponse {
    private Long       id;
    private Long       produitId;
    private String     produitNom;
    private String     designation;
    private Integer    quantite;
    private BigDecimal prixUnitaireHt;
    private BigDecimal tauxTva;
    private BigDecimal remise;
    private BigDecimal montantHt;
    private BigDecimal montantTva;
    private BigDecimal montantTtc;
}