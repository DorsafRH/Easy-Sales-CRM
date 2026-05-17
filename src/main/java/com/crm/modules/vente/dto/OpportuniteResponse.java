package com.crm.modules.vente.dto;

import com.crm.shared.enums.StatutOpportunite;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class OpportuniteResponse {
    private Long id;
    private String titre;
    private String description;
    private BigDecimal montantEstime;
    private Integer probabilite;
    private StatutOpportunite statut;
    private String dateCloturePrevue;
    private String raisonPerte;
    private Long clientId;
    private String clientNom;
    private Long leadId;
    private String leadNom;
    private String dateCreation;
    private String dateModification;
    private String dateRelative;
}