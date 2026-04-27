package com.crm.modules.contact.repository;

import com.crm.modules.contact.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA pour l'entité {@link Contact}.
 *
 * <p>{@link JpaSpecificationExecutor} activé pour la recherche dynamique
 * via {@link com.crm.modules.contact.specification.ContactSpecification}.</p>
 *
 * <p>La réinitialisation du contact principal est effectuée en Java (load + save)
 * via {@link #findByClientIdAndIsPrincipalTrue(Long)}, sans mise à jour en masse.</p>
 *
 * @author Riahi Dorsaf
 */
@Repository
public interface ContactRepository
        extends JpaRepository<Contact, Long>,
        JpaSpecificationExecutor<Contact> {

    Optional<Contact> findByIdAndClientId(Long id, Long clientId);

    int countByClientId(Long clientId);

    /**
     * Retourne le contact actuellement principal d'un client.
     * Utilisé avant la définition d'un nouveau contact principal pour
     * décocher l'ancien sans mise à jour en masse.
     */
    Optional<Contact> findByClientIdAndIsPrincipalTrue(Long clientId);
}