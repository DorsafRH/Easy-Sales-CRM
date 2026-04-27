package com.crm.modules.catalogue.specification;

import com.crm.modules.catalogue.entity.Produit;
import com.crm.shared.enums.StatutProduit;
import com.crm.shared.enums.TypeProduit;
import org.springframework.data.jpa.domain.Specification;

/**
 * Spécifications JPA pour les requêtes dynamiques sur {@link Produit}.
 *
 * @author Riahi Dorsaf
 */
public class ProduitSpecification {

    private ProduitSpecification() {}

    /** Filtre les produits appartenant à un propriétaire donné. */
    public static Specification<Produit> duProprietaire(Long proprietaireId) {
        return (root, query, cb) ->
                cb.equal(root.get("proprietaire").get("id"), proprietaireId);
    }

    /**
     * Filtre par type ({@code SERVICE} / {@code STOCKABLE}).
     * Ignoré si {@code type} est {@code null}.
     */
    public static Specification<Produit> avecType(TypeProduit type) {
        return (root, query, cb) ->
                type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    /**
     * Filtre par statut ({@code ACTIF} / {@code INACTIF} / {@code ARCHIVE}).
     * Ignoré si {@code statut} est {@code null}.
     */
    public static Specification<Produit> avecStatut(StatutProduit statut) {
        return (root, query, cb) ->
                statut == null ? cb.conjunction() : cb.equal(root.get("statut"), statut);
    }

    /**
     * Filtre par catégorie.
     * Ignoré si {@code categorieId} est {@code null}.
     */
    public static Specification<Produit> avecCategorie(Long categorieId) {
        return (root, query, cb) ->
                categorieId == null
                        ? cb.conjunction()
                        : cb.equal(root.get("categorie").get("id"), categorieId);
    }

    /**
     * Recherche insensible à la casse sur {@code nom} et {@code description}.
     * Ignorée si {@code keyword} est {@code null} ou vide.
     */
    public static Specification<Produit> recherche(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("nom")),         pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}