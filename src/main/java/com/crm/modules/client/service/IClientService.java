package com.crm.modules.client.service;

import com.crm.modules.client.dto.ClientRequest;
import com.crm.modules.client.dto.ClientResponse;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.response.PageResponse;

/**
 * Contrat du service de gestion des clients.
 *
 * @author Riahi Dorsaf
 */
public interface IClientService {

    PageResponse<ClientResponse> lister(Long proprietaireId, String typeClient,
                                        String keyword, int page, int size);

    ClientResponse obtenir(Long id, Long proprietaireId);

    ClientResponse creer(ClientRequest request, ProprietaireEntreprise proprietaire);

    ClientResponse modifier(Long id, ClientRequest request, Long proprietaireId);

    void supprimer(Long id, Long proprietaireId);
}