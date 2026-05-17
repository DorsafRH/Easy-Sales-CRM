package com.crm.modules.vente.service;

import com.crm.modules.vente.dto.DevisRequest;
import com.crm.modules.vente.dto.DevisResponse;
import com.crm.modules.vente.dto.FactureResponse;
import com.crm.shared.enums.StatutDevis;

import java.util.List;

/**
 * @author Riahi Dorsaf
 */
public interface IDevisService {

    List<DevisResponse> lister(Long proprietaireId, StatutDevis statut);

    DevisResponse obtenir(Long id, Long proprietaireId);

    DevisResponse creer(DevisRequest request, Long proprietaireId);

    DevisResponse modifier(Long id, DevisRequest request, Long proprietaireId);

    DevisResponse changerStatut(Long id, StatutDevis statut, Long proprietaireId);

    /**
     * Convertit un devis ACCEPTE en facture
     */
    FactureResponse convertirEnFacture(Long id, Long proprietaireId);

    void supprimer(Long id, Long proprietaireId);
}