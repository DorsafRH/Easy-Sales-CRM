package com.crm.modules.contact.service;

import com.crm.modules.client.entity.Client;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.contact.dto.ContactRequest;
import com.crm.modules.contact.dto.ContactResponse;
import com.crm.modules.contact.entity.Contact;
import com.crm.modules.contact.mapper.ContactMapper;
import com.crm.modules.contact.repository.ContactRepository;
import com.crm.modules.contact.specification.ContactSpecification;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implémentation du service de gestion des contacts.
 *
 * <p>Règle métier : un seul contact principal par client.
 * La réinitialisation du contact principal précédent est effectuée
 * par chargement + sauvegarde de l'entité, sans mise à jour en masse.</p>
 *
 * <p>La suppression utilise {@code deleteById} pour éviter l'ambiguïté
 * entre {@code CrudRepository.delete(T)} et
 * {@code JpaSpecificationExecutor.delete(Specification)} en Spring Data JPA 3.x.</p>
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContactService implements IContactService {

    private final ContactRepository contactRepository;
    private final ClientRepository  clientRepository;
    private final ContactMapper     contactMapper;

    @Transactional(readOnly = true)
    @Override
    public List<ContactResponse> lister(Long clientId, Long proprietaireId, String keyword) {
        verifierAccesClient(clientId, proprietaireId);

        Specification<Contact> spec = ContactSpecification.duClient(clientId)
                .and(ContactSpecification.recherche(keyword));

        return contactRepository.findAll(spec)
                .stream()
                .map(contactMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ContactResponse obtenir(Long contactId, Long clientId, Long proprietaireId) {
        verifierAccesClient(clientId, proprietaireId);
        return contactMapper.toResponse(charger(contactId, clientId));
    }

    @Override
    public ContactResponse creer(Long clientId, ContactRequest req, Long proprietaireId) {
        Client client = verifierAccesClient(clientId, proprietaireId);

        if (Boolean.TRUE.equals(req.getIsPrincipal())) {
            retirerPrincipalActuel(clientId);
        }

        Contact contact = Contact.builder()
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .email(req.getEmail())
                .telephone(req.getTelephone())
                .poste(req.getPoste())
                .isPrincipal(Boolean.TRUE.equals(req.getIsPrincipal()))
                .client(client)
                .build();

        contact = contactRepository.save(contact);
        log.info("[CONTACT] Créé — id={} clientId={}", contact.getId(), clientId);
        return contactMapper.toResponse(contact);
    }

    @Override
    public ContactResponse modifier(Long contactId, Long clientId,
                                    ContactRequest req, Long proprietaireId) {
        verifierAccesClient(clientId, proprietaireId);
        Contact contact = charger(contactId, clientId);

        if (Boolean.TRUE.equals(req.getIsPrincipal()) && !contact.isPrincipal()) {
            retirerPrincipalActuel(clientId);
        }

        contact.setNom(req.getNom());
        contact.setPrenom(req.getPrenom());
        contact.setEmail(req.getEmail());
        contact.setTelephone(req.getTelephone());
        contact.setPoste(req.getPoste());
        if (req.getIsPrincipal() != null) {
            contact.setPrincipal(req.getIsPrincipal());
        }

        contact = contactRepository.save(contact);
        log.info("[CONTACT] Modifié — id={}", contactId);
        return contactMapper.toResponse(contact);
    }

    @Override
    public void supprimer(Long contactId, Long clientId, Long proprietaireId) {
        verifierAccesClient(clientId, proprietaireId);
        Contact contact = charger(contactId, clientId);
        if (contact.isPrincipal()) {
            throw new BusinessException(
                    "Impossible de supprimer le contact principal. "
                            + "Définissez-en un autre d'abord.");
        }
        contactRepository.deleteById(contact.getId());
        log.info("[CONTACT] Supprimé — id={}", contactId);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Client verifierAccesClient(Long clientId, Long proprietaireId) {
        return clientRepository
                .findByIdAndProprietaireIdAndIsDeletedFalse(clientId, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));
    }

    private Contact charger(Long contactId, Long clientId) {
        return contactRepository.findByIdAndClientId(contactId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact introuvable"));
    }

    private void retirerPrincipalActuel(Long clientId) {
        contactRepository.findByClientIdAndIsPrincipalTrue(clientId)
                .ifPresent(ancien -> {
                    ancien.setPrincipal(false);
                    contactRepository.save(ancien);
                });
    }
}