package com.crm.modules.marketing.service;

import com.crm.modules.marketing.dto.request.LeadAutomationRequestDTO;
import com.crm.modules.marketing.dto.response.PageConnecteeDTO;
import com.crm.modules.marketing.entity.CompteSocialConnecte;
import com.crm.modules.marketing.repository.CompteSocialConnecteRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.vente.dto.LeadQualifieRequest;
import com.crm.modules.vente.dto.LeadResponse;
import com.crm.modules.vente.service.ILeadService;
import com.crm.shared.enums.TypeReseau;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MarketingAutomationService implements IMarketingAutomationService {

    private final CompteSocialConnecteRepository compteSocialRepository;
    private final ILeadService leadService;

    @Override
    public LeadResponse creerLeadDepuisMessenger(LeadAutomationRequestDTO req) {
        ProprietaireEntreprise proprietaire = resoudreProprietaire(req.getPageId());

        LeadQualifieRequest qualifie = LeadQualifieRequest.builder()
                .nom(req.getNom())
                .email(req.getEmail())
                .telephone(req.getTelephone())
                .source(req.getSource())
                .score(req.getScore())
                .resume(req.getResume())
                .build();

        log.info("[MARKETING-AUTO] Lead Messenger reçu — pageId={} psid={} source={}",
                req.getPageId(), req.getPsid(), req.getSource());

        return leadService.creerQualifie(qualifie, proprietaire.getId());
    }

    @Transactional(readOnly = true)
    @Override
    public List<PageConnecteeDTO> listerPagesConnectees() {
        return compteSocialRepository.findByTypeReseau(TypeReseau.FACEBOOK).stream()
                .map(c -> PageConnecteeDTO.builder()
                        .pageId(c.getIdentifiantExterne())
                        .accessToken(c.getAccessToken())
                        .proprietaireId(c.getProprietaire().getId())
                        .build())
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS — RÉSOLUTION MULTI-TENANT (pageId → owner)
    // ─────────────────────────────────────────────────────────

    /**
     * Résout le propriétaire à partir de l'identifiant de page Meta.
     * Si la page n'est pas connectée, on rejette (jamais de lead orphelin).
     */
    private ProprietaireEntreprise resoudreProprietaire(String pageId) {
        CompteSocialConnecte compte = compteSocialRepository
                .findFirstByIdentifiantExterneAndTypeReseau(pageId, TypeReseau.FACEBOOK)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune page Facebook connectée pour l'identifiant : " + pageId));
        return compte.getProprietaire();
    }
}
