package com.crm.modules.client.service;

import com.crm.modules.client.dto.ClientRequest;
import com.crm.modules.client.dto.ClientResponse;
import com.crm.modules.client.entity.Client;
import com.crm.modules.client.entity.ClientEntreprise;
import com.crm.modules.client.entity.ClientIndividuel;
import com.crm.modules.client.mapper.ClientMapper;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.client.specification.ClientSpecification;
import com.crm.modules.contact.repository.ContactRepository;
import com.crm.modules.reporting.service.IActiviteService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.TypeActivite;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import com.crm.shared.response.PageResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service métier — Clients.
 * Multi-tenant : chaque opération est cloisonnée par proprietaireId.
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClientService implements IClientService {

    private static final String VISION_MODEL = "llama-3.2-11b-vision-preview";
    private static final String PROMPT_OCR = """
            Analyse cette image. Elle contient une liste ou un tableau de clients (imprimé ou manuscrit).
            Extrais toutes les personnes ou entreprises visibles et retourne UNIQUEMENT un tableau JSON :
            [{"typeClient":"INDIVIDUEL","nom":"...","prenom":"...","telephone":"...","email":"...","ville":"..."},
             {"typeClient":"ENTREPRISE","raisonSociale":"...","telephone":"...","email":"...","ville":"..."}]
            Règles : typeClient = "INDIVIDUEL" pour personne physique, "ENTREPRISE" pour société.
            Pour INDIVIDUEL : nom et prenom obligatoires. Pour ENTREPRISE : raisonSociale obligatoire.
            Omets les champs absents. Retourne UNIQUEMENT le tableau JSON, sans texte avant/après.
            Si image illisible ou vide : retourne [].
            """;

    private final ClientRepository clientRepository;
    private final ContactRepository contactRepository;
    private final ClientMapper clientMapper;

    /**
     * Injection via l'interface IActiviteService — pas l'implémentation concrète.
     * Bonne pratique SOLID D : dépendre d'une abstraction, pas d'une implémentation.
     */
    private final IActiviteService activiteService;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    // ─────────────────────────────────────────────────────────────────────────
    //  LISTE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ClientResponse> lister(Long proprietaireId, String typeClient,
                                               String keyword, int page, int size) {
        Specification<Client> spec = ClientSpecification.duProprietaire(proprietaireId)
                .and(ClientSpecification.avecType(typeClient))
                .and(ClientSpecification.recherche(keyword));

        Page<Client> pageResult = clientRepository.findAll(
                spec, PageRequest.of(page, size, Sort.by("dateCreation").descending()));

        return PageResponse.from(pageResult.map(c -> enrichir(clientMapper.toResponse(c), c)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DÉTAIL
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public ClientResponse obtenir(Long id, Long proprietaireId) {
        Client client = charger(id, proprietaireId);
        return enrichir(clientMapper.toResponse(client), client);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CRÉATION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ClientResponse creer(ClientRequest req, ProprietaireEntreprise proprietaire) {
        validerEmail(req.getEmail(), null, proprietaire.getId());
        Client client = construire(req, proprietaire);
        client.recalculerNomAffichage();
        client = clientRepository.save(client);
        log.info("[CLIENT] Créé — type={} id={} proprietaire={}",
                req.getTypeClient(), client.getId(), proprietaire.getId());

        // titre = label de l'action | description = nom de l'entité
        activiteService.enregistrer(
                TypeActivite.CLIENT_CREE,
                "Nouveau client ajouté",
                client.getNomAffichage(),
                client.getId(), "CLIENT", null, proprietaire);

        return enrichir(clientMapper.toResponse(client), client);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MODIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ClientResponse modifier(Long id, ClientRequest req, Long proprietaireId) {
        Client client = charger(id, proprietaireId);
        validerEmail(req.getEmail(), client.getEmail(), proprietaireId);
        appliquer(req, client);
        client.recalculerNomAffichage();
        client = clientRepository.save(client);
        log.info("[CLIENT] Modifié — id={}", id);

        activiteService.enregistrer(
                TypeActivite.CLIENT_MODIFIE,
                "Client mis à jour",
                client.getNomAffichage(),
                client.getId(), "CLIENT", null, client.getProprietaire());

        return enrichir(clientMapper.toResponse(client), client);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUPPRESSION (soft delete)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void supprimer(Long id, Long proprietaireId) {
        Client client = charger(id, proprietaireId);
        String nomAffichage = client.getNomAffichage();
        ProprietaireEntreprise proprietaire = client.getProprietaire();
        client.supprimerLogiquement();
        clientRepository.save(client);
        log.info("[CLIENT] Supprimé logiquement — id={}", id);

        activiteService.enregistrer(
                TypeActivite.CLIENT_SUPPRIME,
                "Client supprimé",
                nomAffichage,
                id, "CLIENT", null, proprietaire);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  IMPORT PHOTO OCR
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ClientRequest> extraireClientsDepuisPhoto(MultipartFile image) {
        try {
            String base64 = Base64.getEncoder().encodeToString(image.getBytes());
            String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";
            String dataUrl = "data:" + mimeType + ";base64," + base64;

            Map<String, Object> body = Map.of(
                    "model", VISION_MODEL,
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "text", "text", PROMPT_OCR),
                                    Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                            ))),
                    "max_tokens", 2000,
                    "temperature", 0.1);

            RestClient restClient = RestClient.builder().baseUrl(groqApiUrl).build();
            Map<?, ?> response = restClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return parseClientsDepuisReponse(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CLIENT OCR] Erreur : {}", e.getMessage());
            throw new BusinessException("Erreur lors de l'analyse de l'image : " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<ClientRequest> parseClientsDepuisReponse(Map<?, ?> response) {
        try {
            Object choices = response != null ? response.get("choices") : null;
            if (!(choices instanceof List<?> liste) || liste.isEmpty()) return Collections.emptyList();

            Map<String, Object> message = (Map<String, Object>) ((Map<?, ?>) liste.get(0)).get("message");
            if (message == null) return Collections.emptyList();

            String json = String.valueOf(message.get("content")).trim();
            // Extraire uniquement le tableau JSON (le LLM peut ajouter du texte autour)
            int debut = json.indexOf('[');
            int fin = json.lastIndexOf(']');
            if (debut == -1 || fin == -1 || fin <= debut) return Collections.emptyList();
            json = json.substring(debut, fin + 1);

            return new ObjectMapper().readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[CLIENT OCR] Parsing JSON échoué : {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────────────────────

    private Client charger(Long id, Long proprietaireId) {
        return clientRepository
                .findByIdAndProprietaireIdAndIsDeletedFalse(id, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));
    }

    private void validerEmail(String email, String emailActuel, Long proprietaireId) {
        if (email == null || email.isBlank()) return;
        if (email.equalsIgnoreCase(emailActuel)) return;
        if (clientRepository.existsByEmailAndProprietaireIdAndIsDeletedFalse(
                email, proprietaireId)) {
            throw new BusinessException("Un client avec cet email existe déjà");
        }
    }

    /**
     * Enrichit le ClientResponse avec les données calculées non mappées par MapStruct.
     * nbContacts : réel — chiffreAffaires : 0.0 Sprint 2, réel Sprint 3.
     */
    private ClientResponse enrichir(ClientResponse response, Client client) {
        response.setNbContacts(contactRepository.countByClientId(client.getId()));
        response.setChiffreAffaires(BigDecimal.ZERO);
        return response;
    }

    private Client construire(ClientRequest req, ProprietaireEntreprise proprietaire) {
        if ("INDIVIDUEL".equals(req.getTypeClient())) {
            if (req.getNom() == null || req.getNom().isBlank()) {
                throw new BusinessException(
                        "Le nom est obligatoire pour un client individuel");
            }
            if (req.getPrenom() == null || req.getPrenom().isBlank()) {
                throw new BusinessException(
                        "Le prénom est obligatoire pour un client individuel");
            }
            ClientIndividuel c = new ClientIndividuel();
            c.setNom(req.getNom());
            c.setPrenom(req.getPrenom());
            appliquerCommun(req, c, proprietaire);
            return c;
        }
        if ("ENTREPRISE".equals(req.getTypeClient())) {
            if (req.getRaisonSociale() == null || req.getRaisonSociale().isBlank()) {
                throw new BusinessException(
                        "La raison sociale est obligatoire pour un client entreprise");
            }
            ClientEntreprise c = new ClientEntreprise();
            c.setRaisonSociale(req.getRaisonSociale());
            appliquerCommun(req, c, proprietaire);
            return c;
        }
        throw new BusinessException("Type client invalide : " + req.getTypeClient());
    }

    private void appliquer(ClientRequest req, Client client) {
        if (client instanceof ClientIndividuel c) {
            if (req.getNom() != null) c.setNom(req.getNom());
            if (req.getPrenom() != null) c.setPrenom(req.getPrenom());
        } else if (client instanceof ClientEntreprise c) {
            if (req.getRaisonSociale() != null) c.setRaisonSociale(req.getRaisonSociale());
        }
        client.setEmail(req.getEmail());
        client.setTelephone(req.getTelephone());
        client.setAdresse(req.getAdresse());
        client.setVille(req.getVille());
        client.setPays(req.getPays());
    }

    private void appliquerCommun(ClientRequest req, Client client,
                                 ProprietaireEntreprise proprietaire) {
        client.setEmail(req.getEmail());
        client.setTelephone(req.getTelephone());
        client.setAdresse(req.getAdresse());
        client.setVille(req.getVille());
        client.setPays(req.getPays());
        client.setProprietaire(proprietaire);
    }
}