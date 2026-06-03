package com.crm.modules.marketing.service.impl;

import com.crm.modules.marketing.dto.request.GenererContenuRequestDTO;
import com.crm.modules.marketing.dto.request.N8NCallbackDTO;
import com.crm.modules.marketing.dto.request.PublicationRequestDTO;
import com.crm.modules.marketing.dto.response.CompteSocialResponseDTO;
import com.crm.modules.marketing.dto.response.GenererContenuResponseDTO;
import com.crm.modules.marketing.dto.response.PublicationResponseDTO;
import com.crm.modules.marketing.entity.CompteSocialConnecte;
import com.crm.modules.marketing.entity.DiffusionPublication;
import com.crm.modules.marketing.entity.PublicationMarketing;
import com.crm.modules.marketing.mapper.CompteSocialMapper;
import com.crm.modules.marketing.mapper.PublicationMapper;
import com.crm.modules.marketing.repository.CompteSocialConnecteRepository;
import com.crm.modules.marketing.repository.DiffusionPublicationRepository;
import com.crm.modules.marketing.repository.PublicationMarketingRepository;
import com.crm.modules.marketing.service.GroqService;
import com.crm.modules.marketing.service.IMarketingService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.shared.enums.StatutDiffusion;
import com.crm.shared.enums.StatutPublication;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@Transactional
public class MarketingServiceImpl implements IMarketingService {

    private final RestClient restClient = RestClient.create();

    private final PublicationMarketingRepository publicationRepository;
    private final DiffusionPublicationRepository diffusionRepository;
    private final CompteSocialConnecteRepository compteRepository;
    private final ProprietaireRepository proprietaireRepository;
    private final PublicationMapper publicationMapper;
    private final CompteSocialMapper compteSocialMapper;
    private final GroqService groqService;
    private final String n8nWebhookUrl;
    private final String n8nCallbackSecret;

    public MarketingServiceImpl(PublicationMarketingRepository publicationRepository,
                                DiffusionPublicationRepository diffusionRepository,
                                CompteSocialConnecteRepository compteRepository,
                                ProprietaireRepository proprietaireRepository,
                                PublicationMapper publicationMapper,
                                CompteSocialMapper compteSocialMapper,
                                GroqService groqService,
                                @Value("${n8n.webhook.url}") String n8nWebhookUrl,
                                @Value("${n8n.webhook.callback-secret}") String n8nCallbackSecret) {
        this.publicationRepository = publicationRepository;
        this.diffusionRepository = diffusionRepository;
        this.compteRepository = compteRepository;
        this.proprietaireRepository = proprietaireRepository;
        this.publicationMapper = publicationMapper;
        this.compteSocialMapper = compteSocialMapper;
        this.groqService = groqService;
        this.n8nWebhookUrl = n8nWebhookUrl;
        this.n8nCallbackSecret = n8nCallbackSecret;
    }

    @Override
    public GenererContenuResponseDTO genererContenu(GenererContenuRequestDTO request) {
        return groqService.genererContenu(request);
    }

    @Override
    public PublicationResponseDTO creerPublication(PublicationRequestDTO req, Long proprietaireId) {
        ProprietaireEntreprise proprietaire = chargerProprietaire(proprietaireId);
        PublicationMarketing pub = construirePublication(req, proprietaire);
        attacherDiffusions(pub, req.getComptesSociauxIds(), proprietaireId);
        pub = publicationRepository.save(pub);
        log.info("[MARKETING] Publication créée — id={}", pub.getId());
        return publicationMapper.toResponse(pub);
    }

    @Transactional(readOnly = true)
    @Override
    public List<PublicationResponseDTO> listerPublications(Long proprietaireId) {
        return publicationRepository.findByProprietaireIdOrderByDateCreationDesc(proprietaireId)
                .stream().map(publicationMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PublicationResponseDTO obtenirPublication(Long id, Long proprietaireId) {
        return publicationMapper.toResponse(chargerPublication(id, proprietaireId));
    }

    @Override
    public PublicationResponseDTO modifierPublication(Long id, PublicationRequestDTO req,
                                                      Long proprietaireId) {
        PublicationMarketing pub = chargerPublication(id, proprietaireId);
        verifierModifiable(pub);
        publicationMapper.updateFromRequest(req, pub);
        pub = publicationRepository.save(pub);
        log.info("[MARKETING] Publication modifiée — id={}", id);
        return publicationMapper.toResponse(pub);
    }

    @Override
    public void supprimerPublication(Long id, Long proprietaireId) {
        PublicationMarketing pub = chargerPublication(id, proprietaireId);
        publicationRepository.deleteById(pub.getId());
        log.info("[MARKETING] Publication supprimée — id={}", id);
    }

    @Override
    public PublicationResponseDTO publier(Long id, Long proprietaireId) {
        PublicationMarketing pub = chargerPublication(id, proprietaireId);
        verifierPubliable(pub);
        pub.getDiffusions().forEach(d -> d.setStatutDiffusion(StatutDiffusion.EN_COURS));
        pub.setStatut(StatutPublication.EN_COURS);
        pub = publicationRepository.save(pub);
        envoyerAuWebhookN8N(pub);
        log.info("[MARKETING] Publication envoyée à N8N — id={}", id);
        return publicationMapper.toResponse(pub);
    }

    @Override
    public PublicationResponseDTO annuler(Long id, Long proprietaireId) {
        PublicationMarketing pub = chargerPublication(id, proprietaireId);
        pub.setStatut(StatutPublication.ANNULEE);
        pub.getDiffusions().forEach(d -> d.setStatutDiffusion(StatutDiffusion.ANNULEE));
        pub = publicationRepository.save(pub);
        log.info("[MARKETING] Publication annulée — id={}", id);
        return publicationMapper.toResponse(pub);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CompteSocialResponseDTO> listerReseaux(Long proprietaireId) {
        return compteRepository.findByProprietaireIdOrderByDateConnexionDesc(proprietaireId)
                .stream().map(compteSocialMapper::toResponse).toList();
    }

    @Override
    public void deconnecterCompte(Long id, Long proprietaireId) {
        CompteSocialConnecte compte = chargerCompte(id, proprietaireId);
        compteRepository.deleteById(compte.getId());
        log.info("[MARKETING] Compte social déconnecté — id={}", id);
    }

    @Override
    public void traiterCallbackN8N(N8NCallbackDTO callback) {
        DiffusionPublication diffusion = diffusionRepository
                .findByPublicationIdAndCompteSocialId(
                        callback.getPublicationId(), callback.getCompteSocialId())
                .orElseThrow(() -> new ResourceNotFoundException("Diffusion introuvable"));
        appliquerResultatDiffusion(diffusion, callback);
        diffusionRepository.save(diffusion);
        recalculerStatutPublication(diffusion.getPublication());
    }

    private PublicationMarketing construirePublication(PublicationRequestDTO req,
                                                      ProprietaireEntreprise proprietaire) {
        return PublicationMarketing.builder()
                .titre(req.getTitre())
                .texte(req.getTexte())
                .mediaUrl(req.getMediaUrl())
                .dateProgrammation(req.getDateProgrammation())
                .statut(determinerStatutInitial(req))
                .proprietaire(proprietaire)
                .build();
    }

    private StatutPublication determinerStatutInitial(PublicationRequestDTO req) {
        return req.getDateProgrammation() != null
                ? StatutPublication.PROGRAMMEE : StatutPublication.BROUILLON;
    }

    private void attacherDiffusions(PublicationMarketing pub, List<Long> compteIds,
                                    Long proprietaireId) {
        if (compteIds == null || compteIds.isEmpty()) {
            return;
        }
        for (Long compteId : compteIds) {
            CompteSocialConnecte compte = chargerCompte(compteId, proprietaireId);
            pub.getDiffusions().add(DiffusionPublication.builder()
                    .statutDiffusion(StatutDiffusion.EN_ATTENTE)
                    .publication(pub)
                    .compteSocial(compte)
                    .build());
        }
    }

    private void verifierModifiable(PublicationMarketing pub) {
        if (pub.getStatut() != StatutPublication.BROUILLON) {
            throw new BusinessException("Seuls les brouillons peuvent être modifiés.");
        }
    }

    private void verifierPubliable(PublicationMarketing pub) {
        if (pub.getStatut() != StatutPublication.BROUILLON
                && pub.getStatut() != StatutPublication.PROGRAMMEE) {
            throw new BusinessException("Cette publication ne peut pas être publiée.");
        }
        if (pub.getDiffusions().isEmpty()) {
            throw new BusinessException("Aucun réseau sélectionné pour la publication.");
        }
    }

    private void envoyerAuWebhookN8N(PublicationMarketing pub) {
        try {
            restClient.post().uri(n8nWebhookUrl)
                    .header("X-Callback-Secret", n8nCallbackSecret)
                    .body(construirePayloadN8N(pub))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("[MARKETING] Webhook N8N injoignable (publication {}): {}",
                    pub.getId(), e.getMessage());
        }
    }

    private Map<String, Object> construirePayloadN8N(PublicationMarketing pub) {
        return Map.of(
                "publicationId", pub.getId(),
                "titre", pub.getTitre(),
                "texte", pub.getTexte() != null ? pub.getTexte() : "",
                "mediaUrl", pub.getMediaUrl() != null ? pub.getMediaUrl() : "",
                "callbackSecret", n8nCallbackSecret,
                "cibles", construireCibles(pub));
    }

    private List<Map<String, Object>> construireCibles(PublicationMarketing pub) {
        List<Map<String, Object>> cibles = new ArrayList<>();
        for (DiffusionPublication diffusion : pub.getDiffusions()) {
            CompteSocialConnecte compte = diffusion.getCompteSocial();
            cibles.add(Map.of(
                    "compteSocialId", compte.getId(),
                    "typeReseau", compte.getTypeReseau().name(),
                    "identifiantExterne", compte.getIdentifiantExterne(),
                    "accessToken", compte.getAccessToken()));
        }
        return cibles;
    }

    private void appliquerResultatDiffusion(DiffusionPublication diffusion, N8NCallbackDTO cb) {
        boolean succes = "PUBLIEE".equalsIgnoreCase(cb.getStatut());
        diffusion.setStatutDiffusion(succes ? StatutDiffusion.PUBLIEE : StatutDiffusion.ECHEC);
        diffusion.setIdPublicationExt(cb.getIdPublicationExt());
        diffusion.setUrlPublication(cb.getUrlPublication());
        diffusion.setMessageErreur(cb.getMessageErreur());
        diffusion.setDateDiffusion(LocalDateTime.now());
    }

    private void recalculerStatutPublication(PublicationMarketing pub) {
        List<DiffusionPublication> diffusions = pub.getDiffusions();
        boolean enCours = diffusions.stream().anyMatch(d ->
                d.getStatutDiffusion() == StatutDiffusion.EN_COURS
                        || d.getStatutDiffusion() == StatutDiffusion.EN_ATTENTE);
        if (enCours) {
            return;
        }
        boolean auMoinsUnePubliee = diffusions.stream()
                .anyMatch(d -> d.getStatutDiffusion() == StatutDiffusion.PUBLIEE);
        pub.setStatut(auMoinsUnePubliee ? StatutPublication.PUBLIEE : StatutPublication.ECHEC);
        if (auMoinsUnePubliee) {
            pub.setDatePublication(LocalDateTime.now());
        }
        publicationRepository.save(pub);
    }

    private PublicationMarketing chargerPublication(Long id, Long proprietaireId) {
        return publicationRepository.findByIdAndProprietaireId(id, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Publication introuvable"));
    }

    private CompteSocialConnecte chargerCompte(Long id, Long proprietaireId) {
        return compteRepository.findByIdAndProprietaireId(id, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte social introuvable"));
    }

    private ProprietaireEntreprise chargerProprietaire(Long id) {
        return proprietaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire introuvable"));
    }
}
