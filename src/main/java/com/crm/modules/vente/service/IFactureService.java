package com.crm.modules.vente.service;

import com.crm.modules.vente.dto.FactureResponse;
import com.crm.shared.enums.StatutFacture;

import java.util.List;

/**
 * @author Riahi Dorsaf
 */
public interface IFactureService {

    List<FactureResponse> lister(Long proprietaireId, StatutFacture statut);

    FactureResponse obtenir(Long id, Long proprietaireId);

    FactureResponse changerStatut(Long id, StatutFacture statut, Long proprietaireId);
}