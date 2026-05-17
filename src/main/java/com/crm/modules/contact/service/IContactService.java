package com.crm.modules.contact.service;

import com.crm.modules.contact.dto.ContactRequest;
import com.crm.modules.contact.dto.ContactResponse;

import java.util.List;

/**
 * Contrat du service de gestion des contacts.
 *
 * @author Riahi Dorsaf
 * @see ContactService
 */
public interface IContactService {

    /**
     * @param clientId       identifiant du client propriétaire des contacts
     * @param proprietaireId identifiant du propriétaire connecté (contrôle d'accès)
     * @param keyword        recherche optionnelle sur nom, prénom, email, poste
     */
    List<ContactResponse> lister(Long clientId, Long proprietaireId, String keyword);

    ContactResponse obtenir(Long contactId, Long clientId, Long proprietaireId);

    ContactResponse creer(Long clientId, ContactRequest request, Long proprietaireId);

    ContactResponse modifier(Long contactId, Long clientId,
                             ContactRequest request, Long proprietaireId);

    void supprimer(Long contactId, Long clientId, Long proprietaireId);
}