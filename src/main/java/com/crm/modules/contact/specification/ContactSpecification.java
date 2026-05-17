package com.crm.modules.contact.specification;

import com.crm.modules.contact.entity.Contact;
import org.springframework.data.jpa.domain.Specification;

/**
 * Spécifications JPA pour les requêtes dynamiques sur {@link Contact}.
 *
 * @author Riahi Dorsaf
 */
public class ContactSpecification {

    private ContactSpecification() {
    }

    /**
     * Filtre les contacts appartenant à un client donné.
     */
    public static Specification<Contact> duClient(Long clientId) {
        return (root, query, cb) ->
                cb.equal(root.get("client").get("id"), clientId);
    }

    /**
     * Recherche insensible à la casse sur {@code nom}, {@code prenom},
     * {@code email} et {@code poste}.
     * Ignorée si {@code keyword} est {@code null} ou vide.
     */
    public static Specification<Contact> recherche(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("nom")), pattern),
                    cb.like(cb.lower(root.get("prenom")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("poste")), pattern)
            );
        };
    }
}