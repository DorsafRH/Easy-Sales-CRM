package com.crm.modules.marketing.service;

import com.crm.modules.marketing.dto.request.LeadAutomationRequestDTO;
import com.crm.modules.marketing.dto.response.PageConnecteeDTO;
import com.crm.modules.vente.dto.LeadResponse;

import java.util.List;

/**
 * Orchestration des appels automatisés (n8n → backend) du module marketing :
 * qualification Messenger / commentaires. Résout le propriétaire (multi-tenant)
 * à partir de la page Meta, puis délègue la persistance au module vente.
 *
 * @author Riahi Dorsaf
 */
public interface IMarketingAutomationService {

    /**
     * Crée un lead qualifié issu du chatbot Messenger (ou d'un commentaire).
     * Le propriétaire est résolu via {@code pageId}.
     */
    LeadResponse creerLeadDepuisMessenger(LeadAutomationRequestDTO request);

    /**
     * Liste les pages Facebook connectées (id, jeton, propriétaire) pour la collecte
     * d'engagement par l'orchestrateur n8n.
     */
    List<PageConnecteeDTO> listerPagesConnectees();
}
