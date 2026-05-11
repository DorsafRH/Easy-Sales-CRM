package com.crm.modules.vente.service;

import com.crm.modules.vente.dto.LeadRequest;
import com.crm.modules.vente.dto.LeadResponse;
import com.crm.modules.vente.dto.OpportuniteResponse;
import com.crm.shared.enums.StatutLead;

import java.util.List;

/**
 * @author Riahi Dorsaf
 */
public interface ILeadService {

    List<LeadResponse> lister(Long proprietaireId, StatutLead statut, String keyword);

    LeadResponse obtenir(Long id, Long proprietaireId);

    LeadResponse creer(LeadRequest request, Long proprietaireId);

    LeadResponse modifier(Long id, LeadRequest request, Long proprietaireId);

    LeadResponse changerStatut(Long id, StatutLead nouveauStatut,
                               String raisonPerte, Long proprietaireId);

    /**
     * Convertit un lead en opportunité.
     * Option A : lier à un client existant (clientId fourni)
     * Option B : créer un nouveau client depuis les infos du lead
     */
    OpportuniteResponse convertirEnOpportunite(Long leadId,
                                               Long clientExistantId,
                                               boolean creerNouveauClient,
                                               String titreOpportunite,
                                               Long proprietaireId);

    void supprimer(Long id, Long proprietaireId);
}