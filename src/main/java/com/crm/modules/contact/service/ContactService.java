package com.crm.modules.contact.service;

import com.crm.modules.client.entity.Client;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.contact.dto.ContactRequest;
import com.crm.modules.contact.dto.ContactResponse;
import com.crm.modules.contact.entity.Contact;
import com.crm.modules.contact.mapper.ContactMapper;
import com.crm.modules.contact.repository.ContactRepository;
import com.crm.modules.contact.specification.ContactSpecification;
import com.crm.modules.reporting.service.IActiviteService;
import com.crm.shared.enums.TypeActivite;
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
    private final ClientRepository clientRepository;
    private final ContactMapper contactMapper;

    /**
     * Injection via l'interface IActiviteService — pas l'implémentation concrète.
     * Bonne pratique SOLID D : dépendre d'une abstraction, pas d'une implémentation.
     */
    private final IActiviteService activiteService;

    // ─────────────────────────────────────────────────────────────────────────
    //  LISTE
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    //  DÉTAIL
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public ContactResponse obtenir(Long contactId, Long clientId, Long proprietaireId) {
        verifierAccesClient(clientId, proprietaireId);
        return contactMapper.toResponse(charger(contactId, clientId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CRÉATION
    // ─────────────────────────────────────────────────────────────────────────

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

        // titre = label de l'action | description = nom du contact
        // entiteParentId = clientId → navigation directe vers ContactDetail
        activiteService.enregistrer(
                TypeActivite.CONTACT_AJOUTE,
                "Nouveau contact ajouté",
                contact.getNom() + " " + contact.getPrenom(),
                contact.getId(), "CONTACT", clientId,
                client.getProprietaire());

        return contactMapper.toResponse(contact);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MODIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ContactResponse modifier(Long contactId, Long clientId,
                                    ContactRequest req, Long proprietaireId) {
        Client client = verifierAccesClient(clientId, proprietaireId);
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

        activiteService.enregistrer(
                TypeActivite.CONTACT_MODIFIE,
                "Contact mis à jour",
                contact.getNom() + " " + contact.getPrenom(),
                contact.getId(), "CONTACT", clientId,
                client.getProprietaire());

        return contactMapper.toResponse(contact);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUPPRESSION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void supprimer(Long contactId, Long clientId, Long proprietaireId) {
        Client client = verifierAccesClient(clientId, proprietaireId);
        Contact contact = charger(contactId, clientId);

        if (contact.isPrincipal()) {
            throw new BusinessException(
                    "Impossible de supprimer le contact principal. "
                            + "Définissez-en un autre d'abord.");
        }

        String nomContact = contact.getNom() + " " + contact.getPrenom();
        contactRepository.deleteById(contact.getId());
        log.info("[CONTACT] Supprimé — id={}", contactId);

        activiteService.enregistrer(
                TypeActivite.CONTACT_SUPPRIME,
                "Contact supprimé",
                nomContact,
                contactId, "CONTACT", clientId,
                client.getProprietaire());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS
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