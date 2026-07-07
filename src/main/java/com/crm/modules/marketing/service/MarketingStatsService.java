package com.crm.modules.marketing.service;

import com.crm.modules.marketing.dto.request.ReactionsSeedRequestDTO;
import com.crm.modules.marketing.dto.response.MarketingOverviewResponseDTO;
import com.crm.modules.marketing.dto.response.MarketingOverviewResponseDTO.BesoinRecentDTO;
import com.crm.modules.marketing.dto.response.MarketingOverviewResponseDTO.LeadParMoisDTO;
import com.crm.modules.marketing.dto.response.MarketingOverviewResponseDTO.ReactionsAggregatDTO;
import com.crm.modules.marketing.dto.response.MarketingOverviewResponseDTO.RepartitionSourceDTO;
import com.crm.modules.marketing.dto.response.StatistiquesPublicationDTO;
import com.crm.modules.marketing.dto.response.StatistiquesPublicationDTO.PointStatistiqueDTO;
import com.crm.modules.marketing.dto.response.TopPostReactionsDTO;
import com.crm.modules.marketing.entity.DiffusionPublication;
import com.crm.modules.marketing.entity.PublicationMarketing;
import com.crm.modules.marketing.entity.ReactionPublication;
import com.crm.modules.marketing.repository.CompteSocialConnecteRepository;
import com.crm.modules.marketing.repository.PublicationMarketingRepository;
import com.crm.modules.marketing.repository.ReactionPublicationRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.vente.entity.Lead;
import com.crm.modules.vente.repository.LeadRepository;
import com.crm.shared.enums.SourceLead;
import com.crm.shared.enums.StatutDiffusion;
import com.crm.shared.enums.StatutLead;
import com.crm.shared.enums.StatutPublication;
import com.crm.shared.enums.TypeReseau;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MarketingStatsService implements IMarketingStatsService {

    /** Canaux d'acquisition considérés comme « marketing » (réseaux sociaux). */
    private static final Set<SourceLead> SOURCES_MARKETING =
            EnumSet.of(SourceLead.MESSENGER, SourceLead.COMMENTAIRE, SourceLead.FACEBOOK);

    /** Statuts considérés comme « qualifiés ou au-delà ». */
    private static final Set<StatutLead> STATUTS_QUALIFIES = EnumSet.of(
            StatutLead.QUALIFIE, StatutLead.PROPOSITION, StatutLead.NEGOCIATION, StatutLead.CONVERTI);

    private static final DateTimeFormatter FORMAT_MOIS = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<String> TYPES_REACTION =
            List.of("like", "love", "wow", "sad", "angry", "haha");

    private final LeadRepository leadRepository;
    private final PublicationMarketingRepository publicationRepository;
    private final ReactionPublicationRepository reactionRepository;
    private final CompteSocialConnecteRepository compteSocialRepository;
    private final FacebookInsightsService facebookInsightsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────
    //  VUE D'ENSEMBLE
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public MarketingOverviewResponseDTO getOverview(Long proprietaireId) {
        List<Lead> leads = leadRepository
                .findByProprietaireIdAndSourceInOrderByDateCreationDesc(proprietaireId, SOURCES_MARKETING);

        long total = leads.size();
        long qualifies = leads.stream().filter(l -> STATUTS_QUALIFIES.contains(l.getStatut())).count();
        long convertis = leads.stream().filter(l -> l.getStatut() == StatutLead.CONVERTI).count();
        int scoreMoyen = (int) Math.round(leads.stream()
                .filter(l -> l.getScore() != null).mapToInt(Lead::getScore).average().orElse(0));
        double tauxConversion = total == 0 ? 0
                : Math.round((convertis * 1000.0) / total) / 10.0;

        return MarketingOverviewResponseDTO.builder()
                .leadsMarketing(total)
                .leadsQualifies(qualifies)
                .scoreMoyen(scoreMoyen)
                .tauxConversion(tauxConversion)
                .repartitionSource(repartitionParSource(leads))
                .leadsParMois(leadsParMois(leads))
                .derniersBesoins(derniersBesoins(leads))
                .publicationsPubliees(publicationRepository
                        .countByProprietaireIdAndStatut(proprietaireId, StatutPublication.PUBLIEE))
                .publicationsProgrammees(publicationRepository
                        .countByProprietaireIdAndStatut(proprietaireId, StatutPublication.PROGRAMMEE))
                .publicationsBrouillons(publicationRepository
                        .countByProprietaireIdAndStatut(proprietaireId, StatutPublication.BROUILLON))
                .reactions(reactionsAgregees(proprietaireId))
                .build();
    }

    // ─────────────────────────────────────────────────────────
    //  TOP PUBLICATIONS
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public List<TopPostReactionsDTO> getTopPosts(Long proprietaireId) {
        return reactionRepository.findTop5ByProprietaireIdOrderByLikesDesc(proprietaireId).stream()
                .map(r -> TopPostReactionsDTO.builder()
                        .postId(r.getPostId())
                        .titre(r.getTitre())
                        .likes(valeur(r.getLikes()))
                        .comments(valeur(r.getComments()))
                        .shares(valeur(r.getShares()))
                        .reactionsBreakdown(lireBreakdown(r.getReactionsBreakdown()))
                        .build())
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    //  STATISTIQUES D'UNE PUBLICATION
    // ─────────────────────────────────────────────────────────

    /**
     * Statistiques Facebook d'une publication PUBLIEE du propriétaire.
     *
     * <p>Les totaux proviennent de l'API Graph (insights + engagement). En cas
     * d'indisponibilité (token expiré, hors-ligne), repli sur l'engagement déjà
     * collecté en base ({@link ReactionPublication}).</p>
     *
     * <p>L'API Graph n'expose pas d'historique jour-par-jour au niveau du post
     * (métriques lifetime uniquement) : la courbe journalière est donc une
     * répartition déterministe des vues totales sur les jours écoulés depuis
     * la publication (fenêtre de {@value #JOURS_COURBE} jours maximum,
     * pondération croissante, somme exacte égale aux vues).</p>
     */
    @Transactional(readOnly = true)
    @Override
    public StatistiquesPublicationDTO getStatistiquesPublication(Long publicationId,
                                                                 Long proprietaireId) {
        PublicationMarketing publication = publicationRepository
                .findByIdAndProprietaireId(publicationId, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Publication introuvable : " + publicationId));

        if (publication.getStatut() != StatutPublication.PUBLIEE) {
            throw new BusinessException(
                    "Les statistiques ne sont disponibles que pour une publication publiée.");
        }

        DiffusionPublication diffusion = diffusionFacebookPubliee(publication);
        StatistiquesPublicationDTO stats = facebookInsightsService
                .recupererInsights(diffusion.getIdPublicationExt(),
                        diffusion.getCompteSocial().getAccessToken())
                .map(i -> StatistiquesPublicationDTO.builder()
                        .vues(i.vues())
                        .vuesUniques(i.vuesUniques())
                        .reactions(i.reactions())
                        .commentaires(i.commentaires())
                        .partages(i.partages())
                        .build())
                .orElseGet(() -> statsDeRepli(proprietaireId, diffusion.getIdPublicationExt()));

        LocalDateTime datePublication = publication.getDatePublication() != null
                ? publication.getDatePublication()
                : diffusion.getDateDiffusion();
        stats.setDatePublication(datePublication);
        stats.setCourbeVuesJournalieres(
                courbeVuesJournalieres(datePublication, stats.getVues()));
        return stats;
    }

    // ─────────────────────────────────────────────────────────
    //  COLLECTE / SEED DE L'ENGAGEMENT
    // ─────────────────────────────────────────────────────────

    @Override
    public void upsertReactions(ReactionsSeedRequestDTO req) {
        ProprietaireEntreprise proprietaire = resoudreProprietaire(req.getPageId());
        if (req.getItems() == null) return;

        for (ReactionsSeedRequestDTO.ReactionItemDTO item : req.getItems()) {
            ReactionPublication reaction = reactionRepository
                    .findByProprietaireIdAndPostId(proprietaire.getId(), item.getPostId())
                    .orElseGet(() -> ReactionPublication.builder()
                            .postId(item.getPostId())
                            .proprietaire(proprietaire)
                            .build());

            reaction.setTitre(item.getTitre());
            reaction.setLikes(valeur(item.getLikes()));
            reaction.setComments(valeur(item.getComments()));
            reaction.setShares(valeur(item.getShares()));
            reaction.setReactionsBreakdown(ecrireBreakdown(item.getReactionsBreakdown()));
            reaction.setCollectedAt(LocalDateTime.now());
            reactionRepository.save(reaction);
        }
        log.info("[MARKETING-STATS] Engagement mis à jour — pageId={} items={}",
                req.getPageId(), req.getItems().size());
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS — STATISTIQUES D'UNE PUBLICATION
    // ─────────────────────────────────────────────────────────

    /** Fenêtre maximale de la courbe des vues journalières (en jours). */
    private static final int JOURS_COURBE = 7;

    /** Pondération croissante appliquée à la répartition des vues. */
    private static final int[] POIDS_COURBE = {1, 2, 2, 3, 4, 5, 7};

    private static final DateTimeFormatter FORMAT_JOUR = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Diffusion Facebook publiée de la publication (porteuse du postId et du token). */
    private DiffusionPublication diffusionFacebookPubliee(PublicationMarketing publication) {
        return publication.getDiffusions().stream()
                .filter(d -> d.getCompteSocial().getTypeReseau() == TypeReseau.FACEBOOK)
                .filter(d -> d.getStatutDiffusion() == StatutDiffusion.PUBLIEE)
                .filter(d -> d.getIdPublicationExt() != null)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Aucune diffusion Facebook publiée pour cette publication."));
    }

    /** Repli local : engagement déjà collecté en base, vues inconnues (0). */
    private StatistiquesPublicationDTO statsDeRepli(Long proprietaireId, String postId) {
        log.info("[MARKETING-STATS] API Graph indisponible — repli local pour le post {}", postId);
        return reactionRepository.findByProprietaireIdAndPostId(proprietaireId, postId)
                .map(r -> StatistiquesPublicationDTO.builder()
                        .vues(0)
                        .vuesUniques(0)
                        .reactions(valeur(r.getLikes()))
                        .commentaires(valeur(r.getComments()))
                        .partages(valeur(r.getShares()))
                        .build())
                .orElseGet(() -> StatistiquesPublicationDTO.builder().build());
    }

    /**
     * Répartit les vues totales sur les jours écoulés depuis la publication
     * (fenêtre glissante de {@value #JOURS_COURBE} jours, pondération croissante,
     * somme exacte égale à {@code vues}).
     */
    private List<PointStatistiqueDTO> courbeVuesJournalieres(LocalDateTime datePublication,
                                                             long vues) {
        LocalDate debut = datePublication != null
                ? datePublication.toLocalDate() : LocalDate.now();
        LocalDate fin = LocalDate.now();
        int jours = (int) Math.min(ChronoUnit.DAYS.between(debut, fin) + 1, JOURS_COURBE);
        jours = Math.max(jours, 1);
        LocalDate premierJour = fin.minusDays(jours - 1L);

        int[] poids = new int[jours];
        int totalPoids = 0;
        for (int i = 0; i < jours; i++) {
            poids[i] = POIDS_COURBE[POIDS_COURBE.length - jours + i];
            totalPoids += poids[i];
        }

        List<PointStatistiqueDTO> courbe = new ArrayList<>(jours);
        long cumul = 0;
        for (int i = 0; i < jours; i++) {
            long valeurJour = i == jours - 1
                    ? vues - cumul
                    : (vues * poids[i]) / totalPoids;
            cumul += valeurJour;
            courbe.add(PointStatistiqueDTO.builder()
                    .date(premierJour.plusDays(i).format(FORMAT_JOUR))
                    .valeur(valeurJour)
                    .build());
        }
        return courbe;
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────

    private List<RepartitionSourceDTO> repartitionParSource(List<Lead> leads) {
        Map<SourceLead, Long> parSource = new LinkedHashMap<>();
        for (Lead l : leads) {
            parSource.merge(l.getSource(), 1L, Long::sum);
        }
        List<RepartitionSourceDTO> resultat = new ArrayList<>();
        parSource.forEach((source, count) -> resultat.add(
                RepartitionSourceDTO.builder().source(source.name()).count(count).build()));
        return resultat;
    }

    /** Six derniers mois (du plus ancien au plus récent), comptage des leads par mois. */
    private List<LeadParMoisDTO> leadsParMois(List<Lead> leads) {
        Map<String, Long> compteur = new LinkedHashMap<>();
        YearMonth courant = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            compteur.put(courant.minusMonths(i).format(FORMAT_MOIS), 0L);
        }
        for (Lead l : leads) {
            if (l.getDateCreation() == null) continue;
            String mois = YearMonth.from(l.getDateCreation()).format(FORMAT_MOIS);
            if (compteur.containsKey(mois)) {
                compteur.merge(mois, 1L, Long::sum);
            }
        }
        List<LeadParMoisDTO> resultat = new ArrayList<>();
        compteur.forEach((mois, count) -> resultat.add(
                LeadParMoisDTO.builder().mois(mois).count(count).build()));
        return resultat;
    }

    /** Cinq derniers leads ayant un besoin renseigné. */
    private List<BesoinRecentDTO> derniersBesoins(List<Lead> leads) {
        return leads.stream()
                .filter(l -> l.getDescriptionBesoin() != null && !l.getDescriptionBesoin().isBlank())
                .limit(5)
                .map(l -> BesoinRecentDTO.builder()
                        .nom(l.getNom())
                        .besoin(l.getDescriptionBesoin())
                        .score(l.getScore())
                        .dateRelative(dateRelative(l.getDateCreation()))
                        .build())
                .toList();
    }

    private ReactionsAggregatDTO reactionsAgregees(Long proprietaireId) {
        List<ReactionPublication> reactions = reactionRepository.findByProprietaireId(proprietaireId);
        int totalLikes = 0, totalComments = 0, totalShares = 0;
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        TYPES_REACTION.forEach(t -> breakdown.put(t, 0));

        for (ReactionPublication r : reactions) {
            totalLikes += valeur(r.getLikes());
            totalComments += valeur(r.getComments());
            totalShares += valeur(r.getShares());
            lireBreakdown(r.getReactionsBreakdown())
                    .forEach((type, n) -> breakdown.merge(type, n, Integer::sum));
        }
        return ReactionsAggregatDTO.builder()
                .totalLikes(totalLikes)
                .totalComments(totalComments)
                .totalShares(totalShares)
                .breakdown(breakdown)
                .build();
    }

    private ProprietaireEntreprise resoudreProprietaire(String pageId) {
        return compteSocialRepository
                .findFirstByIdentifiantExterneAndTypeReseau(pageId, TypeReseau.FACEBOOK)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune page Facebook connectée pour l'identifiant : " + pageId))
                .getProprietaire();
    }

    private int valeur(Integer v) {
        return v == null ? 0 : v;
    }

    private String ecrireBreakdown(Map<String, Integer> breakdown) {
        if (breakdown == null || breakdown.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(breakdown);
        } catch (Exception e) {
            log.warn("[MARKETING-STATS] Sérialisation breakdown impossible : {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Integer> lireBreakdown(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String dateRelative(LocalDateTime date) {
        if (date == null) return "";
        Duration d = Duration.between(date, LocalDateTime.now());
        long minutes = d.toMinutes();
        if (minutes < 1) return "à l'instant";
        if (minutes < 60) return "il y a " + minutes + " min";
        long h = d.toHours();
        if (h < 24) return "il y a " + h + "h";
        long j = d.toDays();
        if (j < 30) return "il y a " + j + "j";
        return "il y a " + (j / 30) + " mois";
    }
}
