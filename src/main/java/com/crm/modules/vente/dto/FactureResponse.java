package com.crm.modules.vente.dto;

import com.crm.shared.enums.StatutFacture;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class FactureResponse {
    private Long id;
    private String numero;
    private StatutFacture statut;
    private BigDecimal montantHt;
    private BigDecimal montantTva;
    private BigDecimal montantTtc;
    private String dateEcheance;
    private String dateEmission;
    private String dateLivraison;
    private String datePaiement;
    private String notes;
    private Long clientId;
    private String clientNom;
    private String devisNumero;
    private String proprietaireNom;
    private List<LigneFactureResponse> lignes;
    private String dateCreation;
    private String dateRelative;
}