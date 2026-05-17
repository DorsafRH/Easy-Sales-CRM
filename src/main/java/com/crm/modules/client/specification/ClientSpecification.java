package com.crm.modules.client.specification;

import com.crm.modules.client.entity.Client;
import org.springframework.data.jpa.domain.Specification;

/**
 * Spécifications JPA pour les requêtes dynamiques sur Client.
 * Utilisées uniquement pour le filtrage multi-critères (liste avec pagination).
 *
 * @author Riahi Dorsaf
 */
public class ClientSpecification {

    private ClientSpecification() {
    }

    /**
     * Filtre les clients non supprimés d'un propriétaire.
     */
    public static Specification<Client> duProprietaire(Long proprietaireId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("proprietaire").get("id"), proprietaireId),
                cb.isFalse(root.get("isDeleted"))
        );
    }

    /**
     * Filtre par discriminateur de type (INDIVIDUEL / ENTREPRISE).
     * Ignoré si typeClient est null ou vide.
     */
    public static Specification<Client> avecType(String typeClient) {
        return (root, query, cb) -> {
            if (typeClient == null || typeClient.isBlank()) return cb.conjunction();
            return cb.equal(root.type().as(String.class), typeClient.toUpperCase());
        };
    }

    /**
     * Recherche insensible à la casse sur nomAffichage et email.
     * Ignorée si keyword est null ou vide.
     */
    public static Specification<Client> recherche(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("nomAffichage")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            );
        };
    }
}