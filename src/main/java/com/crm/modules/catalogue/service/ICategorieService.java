package com.crm.modules.catalogue.service;

import com.crm.modules.catalogue.dto.CategorieRequest;
import com.crm.modules.catalogue.dto.CategorieResponse;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;

import java.util.List;

/**
 * Contrat du service de gestion des catégories du catalogue.
 *
 * @author Riahi Dorsaf
 * @see CategorieService
 */
public interface ICategorieService {

    /**
     * @param proprietaireId identifiant du propriétaire connecté
     * @param keyword        recherche optionnelle sur nom et description
     */
    List<CategorieResponse> listerCategories(Long proprietaireId, String keyword);

    CategorieResponse obtenirCategorie(Long id, Long proprietaireId);

    CategorieResponse creerCategorie(CategorieRequest request,
                                     ProprietaireEntreprise proprietaire);

    CategorieResponse modifierCategorie(Long id, CategorieRequest request,
                                        Long proprietaireId);

    void supprimerCategorie(Long id, Long proprietaireId);
}