package com.crm.modules.vente.dto;

import com.crm.shared.enums.SourceLead;
import lombok.Builder;
import lombok.Data;

/**
 * Données d'un lead déjà qualifié (créé automatiquement, ex. chatbot Messenger).
 * DTO interne au module vente : la résolution du propriétaire et l'orchestration
 * externe se font en amont (module marketing) — vente ne dépend d'aucun autre module.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class LeadQualifieRequest {

    private String nom;
    private String email;
    private String telephone;
    private SourceLead source;
    private Integer score;
    /** Résumé du besoin → descriptionBesoin du lead. */
    private String resume;
}
