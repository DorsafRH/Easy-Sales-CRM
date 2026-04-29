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
     * Liste les catégories du propriétaire avec filtre optionnel.
     *
     * @param proprietaireId identifiant du propriétaire connecté
     * @param keyword        recherche optionnelle sur nom et description
     */
    List<CategorieResponse> listerCategories(Long proprietaireId, String keyword);

    CategorieResponse obtenirCategorie(Long id, Long proprietaireId);

    CategorieResponse creerCategorie(CategorieRequest request,
                                     ProprietaireEntreprise proprietaire);

    CategorieResponse modifierCategorie(Long id, CategorieRequest request,
                                        Long proprietaireId);

    /**
     * Supprime une catégorie sans produits actifs.
     * Lève une {@code BusinessException} si la catégorie contient des produits actifs.
     *
     * @param id             identifiant de la catégorie
     * @param proprietaireId identifiant du propriétaire connecté
     */
    void supprimerCategorie(Long id, Long proprietaireId);

    /**
     * Désactive tous les produits actifs d'une catégorie (statut → INACTIF).
     * Opération préalable à la suppression de la catégorie.
     *
     * <p><b>Bonne pratique REST :</b> endpoint séparé avec responsabilité unique —
     * une seule action par endpoint, pas de paramètre {@code ?action=} qui mélange
     * plusieurs comportements dans le même endpoint.</p>
     *
     * @param categorieId    identifiant de la catégorie
     * @param proprietaireId identifiant du propriétaire connecté
     */
    void desactiverProduitsCategorie(Long categorieId, Long proprietaireId);

    /**
     * Retire la catégorie de tous ses produits (categorieId → null).
     * Les produits restent actifs mais sans catégorie.
     * Opération préalable à la suppression de la catégorie.
     *
     * @param categorieId    identifiant de la catégorie
     * @param proprietaireId identifiant du propriétaire connecté
     */
    void retirerCategorieProduits(Long categorieId, Long proprietaireId);
}