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

    /**
     * Archive un produit (statut → ARCHIVE).
     * Un produit archivé n'apparaît plus dans les listes actives.
     * Il peut être restauré via {@link #desarchiverProduit}.
     *
     * @param id             identifiant du produit
     * @param proprietaireId identifiant du propriétaire connecté
     */
    void archiverProduit(Long id, Long proprietaireId);

    /**
     * Désarchive un produit (statut ARCHIVE → INACTIF).
     * Le produit redevient visible dans la liste filtrée par INACTIF.
     * L'utilisateur valide intentionnellement la remise en service
     * en l'activant depuis la liste INACTIF.
     *
     * <p><b>Pourquoi INACTIF et pas ACTIF ?</b>
     * Un produit archivé peut avoir des données obsolètes. Le passer
     * à INACTIF impose une étape de contrôle avant réactivation.</p>
     *
     * @param id             identifiant du produit
     * @param proprietaireId identifiant du propriétaire connecté
     */
    void desarchiverProduit(Long id, Long proprietaireId);

    /**
     * Active un produit (statut INACTIF → ACTIF).
     * Le produit apparaît dans les listes actives du catalogue.
     * Lève une {@code BusinessException} si le produit est ARCHIVE.
     *
     * @param id             identifiant du produit
     * @param proprietaireId identifiant du propriétaire connecté
     */
    void activerProduit(Long id, Long proprietaireId);

    /**
     * Désactive un produit (statut ACTIF → INACTIF).
     * Le produit disparaît des listes actives mais reste consultable.
     * Lève une {@code BusinessException} si le produit est ARCHIVE.
     *
     * @param id             identifiant du produit
     * @param proprietaireId identifiant du propriétaire connecté
     */
    void desactiverProduit(Long id, Long proprietaireId);
}