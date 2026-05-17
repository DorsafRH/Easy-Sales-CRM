package com.crm.modules.vente.dto;

import com.crm.shared.enums.SourceLead;
import com.crm.shared.enums.StatutLead;
import lombok.Builder;
import lombok.Data;

/**
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class LeadResponse {
    private Long id;
    private String nom;
    private String email;
    private String telephone;
    private String entreprise;
    private String poste;
    private SourceLead source;
    private String descriptionBesoin;
    private StatutLead statut;
    private Integer score;
    private String raisonPerte;
    private Long clientId;
    private String clientNom;
    private String dateCreation;
    private String dateModification;
    private String dateRelative;
}