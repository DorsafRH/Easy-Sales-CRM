package com.crm.modules.marketing.service;

import com.crm.modules.marketing.dto.request.ReactionsSeedRequestDTO;
import com.crm.modules.marketing.dto.response.MarketingOverviewResponseDTO;
import com.crm.modules.marketing.dto.response.TopPostReactionsDTO;
import com.crm.modules.marketing.entity.CompteSocialConnecte;
import com.crm.modules.marketing.entity.ReactionPublication;
import com.crm.modules.marketing.repository.CompteSocialConnecteRepository;
import com.crm.modules.marketing.repository.PublicationMarketingRepository;
import com.crm.modules.marketing.repository.ReactionPublicationRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.vente.entity.Lead;
import com.crm.modules.vente.repository.LeadRepository;
import com.crm.shared.enums.SourceLead;
import com.crm.shared.enums.StatutLead;
import com.crm.shared.enums.StatutPublication;
import com.crm.shared.enums.TypeReseau;
import com.crm.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link MarketingStatsService} — agrégations du dashboard et upsert
 * de l'engagement (réactions) avec résolution multi-tenant par pageId.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class MarketingStatsServiceTest {

    @Mock private LeadRepository leadRepository;
    @Mock private PublicationMarketingRepository publicationRepository;
    @Mock private ReactionPublicationRepository reactionRepository;
    @Mock private CompteSocialConnecteRepository compteSocialRepository;

    @InjectMocks
    private MarketingStatsService statsService;

    private Lead lead(SourceLead source, StatutLead statut, int score) {
        return Lead.builder()
                .nom("Prospect")
                .source(source)
                .statut(statut)
                .score(score)
                .dateCreation(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getOverview : calcule total, qualifiés, score moyen et taux de conversion")
    void getOverview_agregeLesLeads() {
        List<Lead> leads = List.of(
                lead(SourceLead.MESSENGER, StatutLead.QUALIFIE, 80),
                lead(SourceLead.MESSENGER, StatutLead.CONVERTI, 60),
                lead(SourceLead.COMMENTAIRE, StatutLead.NOUVEAU, 40));
        when(leadRepository.findByProprietaireIdAndSourceInOrderByDateCreationDesc(eq(7L), any()))
                .thenReturn(leads);
        when(publicationRepository.countByProprietaireIdAndStatut(eq(7L), any(StatutPublication.class)))
                .thenReturn(0L);
        when(reactionRepository.findByProprietaireId(7L)).thenReturn(List.of());

        MarketingOverviewResponseDTO dto = statsService.getOverview(7L);

        assertThat(dto.getLeadsMarketing()).isEqualTo(3);
        assertThat(dto.getLeadsQualifies()).isEqualTo(2);         // QUALIFIE + CONVERTI
        assertThat(dto.getScoreMoyen()).isEqualTo(60);            // (80+60+40)/3
        assertThat(dto.getTauxConversion()).isCloseTo(33.3, within(0.1)); // 1 converti / 3
    }

    @Test
    @DisplayName("getTopPosts : mappe le titre et les compteurs")
    void getTopPosts_mappeTitre() {
        ReactionPublication r = ReactionPublication.builder()
                .postId("post_1")
                .titre("Soldes d'été")
                .likes(42)
                .comments(5)
                .shares(3)
                .build();
        when(reactionRepository.findTop5ByProprietaireIdOrderByLikesDesc(7L))
                .thenReturn(List.of(r));

        List<TopPostReactionsDTO> top = statsService.getTopPosts(7L);

        assertThat(top).hasSize(1);
        assertThat(top.get(0).getTitre()).isEqualTo("Soldes d'été");
        assertThat(top.get(0).getLikes()).isEqualTo(42);
    }

    @Test
    @DisplayName("upsertReactions : résout le propriétaire par pageId et enregistre avec le titre")
    void upsertReactions_resoutOwnerEtEnregistre() {
        ProprietaireEntreprise owner = new ProprietaireEntreprise();
        owner.setId(7L);
        CompteSocialConnecte compte = CompteSocialConnecte.builder()
                .identifiantExterne("PAGE_123")
                .typeReseau(TypeReseau.FACEBOOK)
                .proprietaire(owner)
                .build();
        when(compteSocialRepository
                .findFirstByIdentifiantExterneAndTypeReseau("PAGE_123", TypeReseau.FACEBOOK))
                .thenReturn(Optional.of(compte));
        when(reactionRepository.findByProprietaireIdAndPostId(7L, "post_1"))
                .thenReturn(Optional.empty());

        ReactionsSeedRequestDTO.ReactionItemDTO item = new ReactionsSeedRequestDTO.ReactionItemDTO();
        item.setPostId("post_1");
        item.setTitre("Nouvelle collection");
        item.setLikes(10);
        ReactionsSeedRequestDTO req = new ReactionsSeedRequestDTO();
        req.setPageId("PAGE_123");
        req.setItems(List.of(item));

        statsService.upsertReactions(req);

        ArgumentCaptor<ReactionPublication> captor = ArgumentCaptor.forClass(ReactionPublication.class);
        verify(reactionRepository).save(captor.capture());
        assertThat(captor.getValue().getPostId()).isEqualTo("post_1");
        assertThat(captor.getValue().getTitre()).isEqualTo("Nouvelle collection");
        assertThat(captor.getValue().getProprietaire()).isSameAs(owner);
    }

    @Test
    @DisplayName("upsertReactions : rejette si la page n'est pas connectée")
    void upsertReactions_pageInconnue_leve() {
        when(compteSocialRepository
                .findFirstByIdentifiantExterneAndTypeReseau(anyString(), eq(TypeReseau.FACEBOOK)))
                .thenReturn(Optional.empty());

        ReactionsSeedRequestDTO req = new ReactionsSeedRequestDTO();
        req.setPageId("INCONNU");
        req.setItems(List.of());

        assertThatThrownBy(() -> statsService.upsertReactions(req))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(reactionRepository, never()).save(any());
    }
}
