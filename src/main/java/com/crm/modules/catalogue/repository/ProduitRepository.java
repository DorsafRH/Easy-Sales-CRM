package com.crm.modules.catalogue.repository;

import com.crm.modules.catalogue.entity.Produit;
import com.crm.shared.enums.StatutProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA pour l'entité {@link Produit}.
 *
 * <p>{@link JpaSpecificationExecutor} activé pour le filtrage dynamique
 * via {@link com.crm.modules.catalogue.specification.ProduitSpecification}.</p>
 *
 * <p>Génération du code séquentiel : {@code findTop...OrderByCodeProduitDesc}
 * retourne le dernier produit de l'année ; le service extrait le numéro en Java.</p>
 *
 * @author Riahi Dorsaf
 */
@Repository
public interface ProduitRepository
        extends JpaRepository<Produit, Long>,
        JpaSpecificationExecutor<Produit> {

    Optional<Produit> findByIdAndProprietaireId(Long id, Long proprietaireId);

    boolean existsByNomIgnoreCaseAndProprietaireId(String nom, Long proprietaireId);

    /**
     * Retourne le produit dont le code est lexicographiquement le plus élevé
     * parmi ceux dont le code commence par {@code prefixe} (ex. {@code "PRD-2026-"}).
     * Fiable grâce au zero-padding sur 4 chiffres.
     */
    Optional<Produit> findTopByProprietaireIdAndCodeProduitStartingWithOrderByCodeProduitDesc(
            Long proprietaireId, String prefixe);

    /**
     * Compte les produits d'une catégorie selon leur statut.
     * Utilisé pour enrichir {@code CategorieResponse#nbProduits}
     * et pour valider la suppression d'une catégorie.
     */
    int countByCategorieIdAndStatut(Long categorieId, StatutProduit statut);

    /** Compte les produits d'un propriétaire selon leur statut. */
    long countByProprietaireIdAndStatut(Long proprietaireId, StatutProduit statut);
}