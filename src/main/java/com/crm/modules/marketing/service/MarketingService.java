package com.crm.modules.marketing.service;

import com.crm.modules.catalogue.dto.ProduitResponse;
import com.crm.modules.catalogue.service.ICategorieService;
import com.crm.modules.catalogue.service.IProduitService;
import com.crm.modules.marketing.dto.request.AmeliorerContenuRequestDTO;
import com.crm.modules.marketing.dto.request.GenererContenuRequestDTO;
import com.crm.modules.marketing.dto.request.GenererPublicationRequestDTO;
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
import com.crm.modules.marketing.repository.PublicationMarketingRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.shared.enums.StatutDiffusion;
import com.crm.shared.enums.StatutProduit;
import com.crm.shared.enums.StatutPublication;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@Transactional
public class MarketingService implements IMarketingService {

    private final PublicationMarketingRepository publicationRepository;
    private final CompteSocialConnecteRepository compteRepository;
    private final ProprietaireRepository proprietaireRepository;
    private final PublicationMapper publicationMapper;
    private final CompteSocialMapper compteSocialMapper;
    private final GroqService groqService;
    private final FacebookPublicationService facebookPublicationService;
    private final IProduitService produitService;
    private final ICategorieService categorieService;

    public MarketingService(PublicationMarketingRepository publicationRepository,
                            CompteSocialConnecteRepository compteRepository,
                            ProprietaireRepository proprietaireRepository,
                            PublicationMapper publicationMapper,
                            CompteSocialMapper compteSocialMapper,
                            GroqService groqService,
                            FacebookPublicationService facebookPublicationService,
                            IProduitService produitService,
                            ICategorieService categorieService) {
        this.publicationRepository = publicationRepository;
        this.compteRepository = compteRepository;
        this.proprietaireRepository = proprietaireRepository;
        this.publicationMapper = publicationMapper;
        this.compteSocialMapper = compteSocialMapper;
        this.groqService = groqService;
        this.facebookPublicationService = facebookPublicationService;
        this.produitService = produitService;
        this.categorieService = categorieService;
    }

    @Override
    public GenererContenuResponseDTO genererContenu(GenererContenuRequestDTO request) {
        return groqService.genererContenu(request);
    }

    @Override
    public GenererContenuResponseDTO genererPublication(GenererPublicationRequestDTO req,
                                                        Long proprietaireId) {
        GenererContenuRequestDTO delegate = new GenererContenuRequestDTO();
        delegate.setSujet(construireContexte(req, proprietaireId));
        delegate.setTypeContenu("post_facebook");
        delegate.setTonalite(valeurOuDefaut(req.getTonalite(), "professionnel"));
        delegate.setLangue(valeurOuDefaut(req.getLangue(), "fr"));
        log.info("[MARKETING-IA] Génération publication — portée={} proprietaire={}",
                req.getPortee(), proprietaireId);
        return groqService.genererContenu(delegate);
    }

    @Override
    public GenererContenuResponseDTO ameliorerContenu(AmeliorerContenuRequestDTO req) {
        return groqService.ameliorer(req.getTexte(), req.getConsigne(), req.getTonalite());
    }

    /**
     * Construit le contexte (« sujet ») transmis au LLM à partir de la portée : le backend
     * récupère les vraies données produit et calcule les prix promo ; le LLM ne recalcule rien.
     */
    private String construireContexte(GenererPublicationRequestDTO req, Long proprietaireId) {
        int remise = req.getRemise() != null ? req.getRemise() : 0;
        StringBuilder sb = new StringBuilder();

        switch (req.getPortee()) {
            case PRODUITS -> {
                sb.append("Rédige un post Facebook pour promouvoir ces produits/services :\n");
                List<Long> ids = req.getProduitIds() != null ? req.getProduitIds() : List.of();
                for (Long id : ids) {
                    ProduitResponse p = produitService.obtenirProduit(id, proprietaireId);
                    ligneProduit(sb, p, remise);
                }
            }
            case CATEGORIE -> {
                String categorie = categorieService
                        .obtenirCategorie(req.getCategorieId(), proprietaireId).getNom();
                sb.append("Rédige un post Facebook pour mettre en avant la catégorie ")
                        .append(categorie).append(".\n");
                List<ProduitResponse> produits = produitService.listerProduits(
                        proprietaireId, null, StatutProduit.ACTIF, req.getCategorieId(), null);
                if (!produits.isEmpty()) {
                    sb.append("Produits/services de cette catégorie :\n");
                    produits.stream().limit(5).forEach(p -> ligneProduit(sb, p, remise));
                }
                if (remise > 0) {
                    sb.append("Une remise de -").append(remise).append("% s'applique.\n");
                }
            }
            case BOUTIQUE -> {
                sb.append("Rédige un post Facebook pour promouvoir l'ensemble de la boutique.\n");
                if (remise > 0) {
                    sb.append("Mentionne une promotion de -").append(remise)
                            .append("% sur toute la boutique.\n");
                }
            }
            case LIBRE -> sb.append("Rédige un post Facebook marketing engageant.\n");
        }

        if (req.getConsigne() != null && !req.getConsigne().isBlank()) {
            sb.append("Ce que le commerçant veut mettre en avant : ")
                    .append(req.getConsigne().trim()).append('\n');
        }
        sb.append("Utilise exactement les prix indiqués ci-dessus ; n'invente aucun prix ni produit non listé.");
        return sb.toString();
    }

    /** Ajoute une ligne « - Nom : prix TND (prix promo -X% = ... TND) ». */
    private void ligneProduit(StringBuilder sb, ProduitResponse p, int remise) {
        sb.append("- ").append(p.getNom());
        BigDecimal prix = p.getPrixTTC() != null ? p.getPrixTTC() : p.getPrixHT();
        if (prix != null) {
            sb.append(" : ").append(fmtPrix(prix)).append(" TND");
            if (remise > 0) {
                BigDecimal promo = prix
                        .multiply(BigDecimal.valueOf(100L - remise))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                sb.append(" (prix promo -").append(remise).append("% = ")
                        .append(fmtPrix(promo)).append(" TND)");
            }
        }
        sb.append('\n');
    }

    private String fmtPrix(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private String valeurOuDefaut(String valeur, String defaut) {
        return valeur != null && !valeur.isBlank() ? valeur : defaut;
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
        pub.setDateProgrammation(req.getDateProgrammation());
        // Recalcule le statut selon la programmation : ajouter une date programme la publication,
        // la retirer la repasse en brouillon (uniquement depuis un état non publié).
        pub.setStatut(pub.getDateProgrammation() != null
                ? StatutPublication.PROGRAMMEE : StatutPublication.BROUILLON);
        if (req.getComptesSociauxIds() != null) {
            pub.getDiffusions().clear();
            attacherDiffusions(pub, req.getComptesSociauxIds(), proprietaireId);
        }
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
        diffuserPublication(pub);
        log.info("[MARKETING] Publication {} traitée — statut={}", id, pub.getStatut());
        return publicationMapper.toResponse(pub);
    }

    @Override
    public void publierPublicationsProgrammees() {
        List<PublicationMarketing> dues = publicationRepository
                .findByStatutAndDateProgrammationLessThanEqual(
                        StatutPublication.PROGRAMMEE, LocalDateTime.now());
        if (dues.isEmpty()) {
            return;
        }
        log.info("[MARKETING] {} publication(s) programmée(s) à diffuser", dues.size());
        for (PublicationMarketing pub : dues) {
            try {
                diffuserPublication(pub);
                log.info("[MARKETING] Publication programmée {} traitée — statut={}",
                        pub.getId(), pub.getStatut());
            } catch (Exception e) {
                log.error("[MARKETING] Échec diffusion programmée {} : {}",
                        pub.getId(), e.getMessage());
            }
        }
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

    /**
     * Diffuse une publication : publie chaque diffusion directement sur son
     * réseau via l'API Graph, puis recalcule le statut global de la publication.
     */
    private void diffuserPublication(PublicationMarketing pub) {
        pub.setStatut(StatutPublication.EN_COURS);
        pub.getDiffusions().forEach(d -> d.setStatutDiffusion(StatutDiffusion.EN_COURS));
        pub.getDiffusions().forEach(facebookPublicationService::publierDiffusion);
        recalculerStatutPublication(pub);
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
        // Modifiable tant que non publiée : brouillon ou programmée (pas encore diffusée).
        if (pub.getStatut() != StatutPublication.BROUILLON
                && pub.getStatut() != StatutPublication.PROGRAMMEE) {
            throw new BusinessException(
                    "Seules les publications en brouillon ou programmées peuvent être modifiées.");
        }
    }

    private void verifierPubliable(PublicationMarketing pub) {
        // ECHEC inclus → permet de réessayer après un échec transitoire (ex. panne Meta).
        if (pub.getStatut() != StatutPublication.BROUILLON
                && pub.getStatut() != StatutPublication.PROGRAMMEE
                && pub.getStatut() != StatutPublication.ECHEC) {
            throw new BusinessException("Cette publication ne peut pas être publiée.");
        }
        if (pub.getDiffusions().isEmpty()) {
            throw new BusinessException("Aucun réseau sélectionné pour la publication.");
        }
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
