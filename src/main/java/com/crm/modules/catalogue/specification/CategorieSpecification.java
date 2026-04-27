package com.crm.modules.catalogue.specification;

import com.crm.modules.catalogue.entity.Categorie;
import org.springframework.data.jpa.domain.Specification;

/**
 * Spécifications JPA pour les requêtes dynamiques sur {@link Categorie}.
 *
 * @author Riahi Dorsaf
 */
public class CatalogueSpecification {

    private CatalogueSpecification() {}

    /** Filtre les catégories appartenant à un propriétaire donné. */
    public static Specification<Categorie> duProprietaire(Long proprietaireId) {
        return (root, query, cb) ->
                cb.equal(root.get("proprietaire").get("id"), proprietaireId);
    }

    /**
     * Recherche insensible à la casse sur {@code nom} et {@code description}.
     * Ignorée si {@code keyword} est {@code null} ou vide.
     */
    public static Specification<Categorie> recherche(String keyword) {
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