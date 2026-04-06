package com.crm.modules.entreprise.specification;

import com.crm.modules.entreprise.entity.EntrepriseCompte;
import com.crm.shared.enums.StatutCompte;
import org.springframework.data.jpa.domain.Specification;

/**
 * Spécifications JPA pour les requêtes dynamiques sur EntrepriseCompte.
 *
 * @author Riahi Dorsaf
 */
public class EntrepriseCompteSpecification {

    private EntrepriseCompteSpecification() {}

    /** Filtre les entreprises non supprimées. */
    public static Specification<EntrepriseCompte> nonSupprime() {
        return (root, query, cb) ->
                cb.isFalse(root.get("isDeleted"));
    }

    /** Filtre par statut — ignoré si statut est null. */
    public static Specification<EntrepriseCompte> avecStatut(StatutCompte statut) {
        return (root, query, cb) ->
                statut == null
                        ? cb.conjunction()
                        : cb.equal(root.get("statutCompte"), statut);
    }

    /** Recherche par nom ou matricule fiscale (insensible à la casse). */
    public static Specification<EntrepriseCompte> recherche(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("nomEntreprise")),    pattern),
                    cb.like(cb.lower(root.get("matriculeFiscale")), pattern)
            );
        };
    }
}