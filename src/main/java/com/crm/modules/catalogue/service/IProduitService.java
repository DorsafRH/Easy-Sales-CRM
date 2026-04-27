package com.crm.modules.catalogue.service;

import com.crm.modules.catalogue.dto.ProduitRequest;
import com.crm.modules.catalogue.dto.ProduitResponse;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutProduit;
import com.crm.shared.enums.TypeProduit;

import java.util.List;

/**
 * Contrat du service de gestion des produits du catalogue.
 *
 * @author Riahi Dorsaf
 * @see ProduitService
 */
public interface IProduitService {

    /**
     * @param proprietaireId identifiant du propriétaire connecté
     * @param type           filtre optionnel par type ({@code SERVICE} / {@code STOCKABLE})
     * @param statut         filtre optionnel par statut
     * @param categorieId    filtre optionnel par catégorie
     * @param keyword        recherche optionnelle sur nom et description
     */
    List<ProduitResponse> listerProduits(Long proprietaireId,
                                         TypeProduit type,
                                         StatutProduit statut,
                                         Long categorieId,
                                         String keyword);

    ProduitResponse obtenirProduit(Long id, Long proprietaireId);

    ProduitResponse creerProduit(ProduitRequest request, ProprietaireEntreprise proprietaire);

    ProduitResponse modifierProduit(Long id, ProduitRequest request, Long proprietaireId);

    void archiverProduit(Long id, Long proprietaireId);
}