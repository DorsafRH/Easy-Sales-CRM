package com.crm.modules.entreprise.specification;

import com.crm.modules.entreprise.entity.EntrepriseCompte;
import com.crm.shared.enums.StatutCompte;
import org.springframework.data.jpa.domain.Specification;

/**
 * Classe utilitaire de spécifications JPA pour l'entité {@link EntrepriseCompte}.
 *
 * <p>Chaque méthode statique retourne une {@link Specification} composable
 * permettant de construire des requêtes dynamiques sans JPQL ni méthodes dérivées.</p>
 *
 * @author Riahi Dorsaf
 */
public class EntrepriseCompteSpecification {

    /** Constructeur privé : classe utilitaire non instanciable. */
    private EntrepriseCompteSpecification() {}

    /**
     * Filtre les entreprises non supprimées logiquement.
     *
     * @return spécification vérifiant {@code isDeleted = false}
     */
    public static Specification<EntrepriseCompte> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    /**
     * Filtre par statut de compte. Si {@code statut} est {@code null}, aucun filtre n'est appliqué.
     *
     * @param statut le statut à filtrer, ou {@code null} pour ignorer ce critère
     * @return spécification sur {@code statutCompte}
     */
    public static Specification<EntrepriseCompte> hasStatut(StatutCompte statut) {
        return (root, query, cb) ->
                statut == null ? cb.conjunction() : cb.equal(root.get("statutCompte"), statut);
    }

    /**
     * Recherche par mot-clé dans le nom de l'entreprise ou la matricule fiscale (insensible à la casse).
     * Si {@code keyword} est {@code null} ou vide, aucun filtre n'est appliqué.
     *
     * @param keyword le mot-clé à rechercher
     * @return spécification de recherche textuelle
     */
    public static Specification<EntrepriseCompte> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("nomEntreprise")), pattern),
                    cb.like(cb.lower(root.get("matriculeFiscale")), pattern)
            );
        };
    }

    /**
     * Filtre par identifiant unique de l'entreprise.
     *
     * @param id l'identifiant de l'entité
     * @return spécification sur {@code id}
     */
    public static Specification<EntrepriseCompte> hasId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    /**
     * Filtre par matricule fiscale exacte.
     *
     * @param matriculeFiscale la matricule fiscale à rechercher
     * @return spécification sur {@code matriculeFiscale}
     */
    public static Specification<EntrepriseCompte> hasMatriculeFiscale(String matriculeFiscale) {
        return (root, query, cb) -> cb.equal(root.get("matriculeFiscale"), matriculeFiscale);
    }
}
