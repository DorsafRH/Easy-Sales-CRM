package com.crm.modules.vente.service;

import com.crm.modules.client.entity.Client;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.reporting.service.IActiviteService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.modules.vente.dto.DevisResponse;
import com.crm.modules.vente.dto.OpportuniteRequest;
import com.crm.modules.vente.dto.OpportuniteResponse;
import com.crm.modules.vente.entity.Devis;
import com.crm.modules.vente.entity.Opportunite;
import com.crm.modules.vente.mapper.DevisMapper;
import com.crm.modules.vente.mapper.OpportuniteMapper;
import com.crm.modules.vente.repository.DevisRepository;
import com.crm.modules.vente.repository.FactureRepository;
import com.crm.modules.vente.repository.LeadRepository;
import com.crm.modules.vente.repository.OpportuniteRepository;
import com.crm.shared.enums.StatutDevis;
import com.crm.shared.enums.StatutOpportunite;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link OpportuniteService} — règles quote-to-cash : on ne gagne pas
 * une opportunité sans devis, un devis ne se crée qu'en NÉGOCIATION, pas de doublon de devis.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class OpportuniteServiceTest {

    @Mock private OpportuniteRepository opportuniteRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private DevisRepository devisRepository;
    @Mock private FactureRepository factureRepository;
    @Mock private ProprietaireRepository proprietaireRepository;
    @Mock private OpportuniteMapper opportuniteMapper;
    @Mock private DevisMapper devisMapper;
    @Mock private IActiviteService activiteService;

    @InjectMocks
    private OpportuniteService opportuniteService;

    private ProprietaireEntreprise proprietaire;

    @BeforeEach
    void setUp() {
        proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);
    }

    private Opportunite opp(StatutOpportunite statut) {
        return Opportunite.builder()
                .id(30L)
                .titre("Projet X")
                .statut(statut)
                .client(mock(Client.class))
                .proprietaire(proprietaire)
                .build();
    }

    @Nested
    @DisplayName("creer")
    class Creer {

        @Test
        @DisplayName("statut par défaut PROSPECTION quand non fourni")
        void creer_statutParDefaut() {
            OpportuniteRequest req = new OpportuniteRequest();
            req.setTitre("Nouveau projet");
            req.setClientId(5L);

            when(proprietaireRepository.findById(1L)).thenReturn(Optional.of(proprietaire));
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(mock(Client.class)));
            when(opportuniteRepository.save(any(Opportunite.class))).thenAnswer(i -> i.getArgument(0));
            when(opportuniteMapper.toResponse(any(Opportunite.class)))
                    .thenReturn(mock(OpportuniteResponse.class));

            opportuniteService.creer(req, 1L);

            ArgumentCaptor<Opportunite> captor = ArgumentCaptor.forClass(Opportunite.class);
            verify(opportuniteRepository).save(captor.capture());
            assertThat(captor.getValue().getStatut()).isEqualTo(StatutOpportunite.PROSPECTION);
        }

        @Test
        @DisplayName("client introuvable → ResourceNotFoundException")
        void creer_clientIntrouvable() {
            OpportuniteRequest req = new OpportuniteRequest();
            req.setClientId(99L);
            when(proprietaireRepository.findById(1L)).thenReturn(Optional.of(proprietaire));
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> opportuniteService.creer(req, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("changerStatut")
    class ChangerStatut {

        @Test
        @DisplayName("GAGNEE sans devis actif → BusinessException")
        void gagnee_sansDevis_leve() {
            when(opportuniteRepository.findByIdAndProprietaireId(30L, 1L))
                    .thenReturn(Optional.of(opp(StatutOpportunite.NEGOCIATION)));
            when(devisRepository.existsByOpportuniteIdAndStatutNotIn(eq(30L), anyList()))
                    .thenReturn(false);

            assertThatThrownBy(() ->
                    opportuniteService.changerStatut(30L, StatutOpportunite.GAGNEE, null, 1L))
                    .isInstanceOf(BusinessException.class);
            verify(opportuniteRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("genererDevis")
    class GenererDevis {

        @Test
        @DisplayName("hors phase NÉGOCIATION → BusinessException")
        void horsNegociation_leve() {
            when(opportuniteRepository.findByIdAndProprietaireId(30L, 1L))
                    .thenReturn(Optional.of(opp(StatutOpportunite.PROSPECTION)));

            assertThatThrownBy(() -> opportuniteService.genererDevis(30L, 1L))
                    .isInstanceOf(BusinessException.class);
            verify(devisRepository, never()).save(any());
        }

        @Test
        @DisplayName("devis déjà existant → BusinessException")
        void devisExistant_leve() {
            when(opportuniteRepository.findByIdAndProprietaireId(30L, 1L))
                    .thenReturn(Optional.of(opp(StatutOpportunite.NEGOCIATION)));
            when(devisRepository.existsByOpportuniteIdAndStatutNotIn(eq(30L), anyList()))
                    .thenReturn(true);

            assertThatThrownBy(() -> opportuniteService.genererDevis(30L, 1L))
                    .isInstanceOf(BusinessException.class);
            verify(devisRepository, never()).save(any());
        }

        @Test
        @DisplayName("nominal : crée un devis BROUILLON numéroté DV-…")
        void nominal_creeDevisBrouillon() {
            when(opportuniteRepository.findByIdAndProprietaireId(30L, 1L))
                    .thenReturn(Optional.of(opp(StatutOpportunite.NEGOCIATION)));
            when(devisRepository.existsByOpportuniteIdAndStatutNotIn(eq(30L), anyList()))
                    .thenReturn(false);
            when(devisRepository.findTopByProprietaireIdAndNumeroStartingWithOrderByNumeroDesc(eq(1L), anyString()))
                    .thenReturn(Optional.empty());
            when(devisRepository.save(any(Devis.class))).thenAnswer(i -> i.getArgument(0));
            when(devisMapper.toResponse(any(Devis.class))).thenReturn(mock(DevisResponse.class));

            opportuniteService.genererDevis(30L, 1L);

            ArgumentCaptor<Devis> captor = ArgumentCaptor.forClass(Devis.class);
            verify(devisRepository).save(captor.capture());
            assertThat(captor.getValue().getStatut()).isEqualTo(StatutDevis.BROUILLON);
            assertThat(captor.getValue().getNumero()).startsWith("DV-");
        }
    }

    @Test
    @DisplayName("obtenir : opportunité introuvable → ResourceNotFoundException")
    void obtenir_introuvable() {
        when(opportuniteRepository.findByIdAndProprietaireId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> opportuniteService.obtenir(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
