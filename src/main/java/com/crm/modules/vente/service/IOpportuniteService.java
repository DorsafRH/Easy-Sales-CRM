package com.crm.modules.vente.service;

import com.crm.modules.vente.dto.DevisResponse;
import com.crm.modules.vente.dto.OpportuniteRequest;
import com.crm.modules.vente.dto.OpportuniteResponse;
import com.crm.shared.enums.StatutOpportunite;

import java.util.List;
import java.util.Map;

/**
 * @author Riahi Dorsaf
 */
public interface IOpportuniteService {

    List<OpportuniteResponse> lister(Long proprietaireId, StatutOpportunite statut, String keyword);

    /** Retourne les opportunités groupées par statut — pour le Kanban */
    Map<StatutOpportunite, List<OpportuniteResponse>> listerParStatut(Long proprietaireId);

    OpportuniteResponse obtenir(Long id, Long proprietaireId);

    OpportuniteResponse creer(OpportuniteRequest request, Long proprietaireId);

    OpportuniteResponse modifier(Long id, OpportuniteRequest request, Long proprietaireId);

    OpportuniteResponse changerStatut(Long id, StatutOpportunite nouveauStatut,
                                      String raisonPerte, Long proprietaireId);

    /** Génère un devis brouillon depuis l'opportunité */
    DevisResponse genererDevis(Long opportuniteId, Long proprietaireId);

    void supprimer(Long id, Long proprietaireId);
}