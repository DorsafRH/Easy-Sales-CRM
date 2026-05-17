package com.crm.modules.vente.dto;

import com.crm.shared.enums.StatutDevis;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class DevisResponse {
    private Long id;
    private String numero;
    private StatutDevis statut;
    private BigDecimal montantHt;
    private BigDecimal montantTva;
    private BigDecimal montantTtc;
    private String notes;
    private Integer validiteJours;
    private Long clientId;
    private String clientNom;
    private Long opportuniteId;
    private String opportuniteTitre;
    private List<LigneDevisResponse> lignes;
    private String dateCreation;
    private String dateModification;
    private String dateRelative;
}