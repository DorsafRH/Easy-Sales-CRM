package com.crm.modules.vente.service;

import com.crm.modules.client.entity.Client;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.contact.repository.ContactRepository;
import com.crm.modules.reporting.service.IActiviteService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.modules.vente.dto.LeadQualifieRequest;
import com.crm.modules.vente.dto.LeadRequest;
import com.crm.modules.vente.dto.LeadResponse;
import com.crm.modules.vente.dto.OpportuniteResponse;
import com.crm.modules.vente.entity.Lead;
import com.crm.modules.vente.entity.Opportunite;
import com.crm.modules.vente.mapper.LeadMapper;
import com.crm.modules.vente.mapper.OpportuniteMapper;
import com.crm.modules.vente.repository.LeadRepository;
import com.crm.modules.vente.repository.OpportuniteRepository;
import com.crm.shared.enums.SourceLead;
import com.crm.shared.enums.StatutLead;
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
 * Tests unitaires de {@link LeadService} — création, qualification automatique (borne du
 * score), changement de statut, conversion en opportunité et multi-tenant.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock private LeadRepository leadRepository;
    @Mock private OpportuniteRepository opportuniteRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ContactRepository contactRepository;
    @Mock private ProprietaireRepository proprietaireRepository;
    @Mock private LeadMapper leadMapper;
    @Mock private OpportuniteMapper opportuniteMapper;
    @Mock private IActiviteService activiteService;

    @InjectMocks
    private LeadService leadService;

    private ProprietaireEntreprise proprietaire;

    @BeforeEach
    void setUp() {
        proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);
    }

    private Lead lead(StatutLead statut) {
        return Lead.builder()
                .id(50L)
                .nom("Ali Ben")
                .statut(statut)
                .proprietaire(proprietaire)
                .build();
    }

    @Nested
    @DisplayName("creer")
    class Creer {

        @Test
        @DisplayName("statut NOUVEAU + score serveur calculé + activité tracée")
        void creer_statutEtScore() {
            LeadRequest req = new LeadRequest();
            req.setNom("Ali");
            req.setEmail("ali@ex.com");        // +20
            req.setTelephone("22334455");      // +20
            req.setDescriptionBesoin("site");  // +30
            req.setEntreprise("ACME");         // +15
            req.setPoste("CEO");               // +15
            req.setSource(SourceLead.SITE_WEB);

            when(proprietaireRepository.findById(1L)).thenReturn(Optional.of(proprietaire));
            when(leadRepository.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));
            when(leadMapper.toResponse(any(Lead.class))).thenReturn(mock(LeadResponse.class));

            leadService.creer(req, 1L);

            ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
            verify(leadRepository).save(captor.capture());
            assertThat(captor.getValue().getStatut()).isEqualTo(StatutLead.NOUVEAU);
            assertThat(captor.getValue().getScore()).isEqualTo(100);
            verify(activiteService).enregistrer(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("creerQualifie")
    class CreerQualifie {

        @Test
        @DisplayName("statut QUALIFIE + score borné à 100 quand le LLM renvoie > 100")
        void creerQualifie_borneScoreHaut() {
            LeadQualifieRequest req = LeadQualifieRequest.builder()
                    .nom("Dorsaf").source(SourceLead.MESSENGER).score(150).resume("devis").build();

            when(proprietaireRepository.findById(1L)).thenReturn(Optional.of(proprietaire));
            when(leadRepository.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));
            when(leadMapper.toResponse(any(Lead.class))).thenReturn(mock(LeadResponse.class));

            leadService.creerQualifie(req, 1L);

            ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
            verify(leadRepository).save(captor.capture());
            assertThat(captor.getValue().getStatut()).isEqualTo(StatutLead.QUALIFIE);
            assertThat(captor.getValue().getScore()).isEqualTo(100);
        }

        @Test
        @DisplayName("score null → 0")
        void creerQualifie_scoreNull() {
            LeadQualifieRequest req = LeadQualifieRequest.builder()
                    .nom("X").source(SourceLead.COMMENTAIRE).score(null).resume("r").build();

            when(proprietaireRepository.findById(1L)).thenReturn(Optional.of(proprietaire));
            when(leadRepository.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));
            when(leadMapper.toResponse(any(Lead.class))).thenReturn(mock(LeadResponse.class));

            leadService.creerQualifie(req, 1L);

            ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
            verify(leadRepository).save(captor.capture());
            assertThat(captor.getValue().getScore()).isZero();
        }
    }

    @Nested
    @DisplayName("changerStatut")
    class ChangerStatut {

        @Test
        @DisplayName("PERDU enregistre la raison de perte")
        void perdu_enregistreRaison() {
            when(leadRepository.findByIdAndProprietaireId(50L, 1L)).thenReturn(Optional.of(lead(StatutLead.NOUVEAU)));
            when(leadRepository.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));
            when(leadMapper.toResponse(any(Lead.class))).thenReturn(mock(LeadResponse.class));

            leadService.changerStatut(50L, StatutLead.PERDU, "Trop cher", 1L);

            ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
            verify(leadRepository).save(captor.capture());
            assertThat(captor.getValue().getStatut()).isEqualTo(StatutLead.PERDU);
            assertThat(captor.getValue().getRaisonPerte()).isEqualTo("Trop cher");
        }

        @Test
        @DisplayName("un lead converti ne peut plus changer de statut")
        void converti_leve() {
            when(leadRepository.findByIdAndProprietaireId(50L, 1L)).thenReturn(Optional.of(lead(StatutLead.CONVERTI)));

            assertThatThrownBy(() -> leadService.changerStatut(50L, StatutLead.PERDU, null, 1L))
                    .isInstanceOf(BusinessException.class);
            verify(leadRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("convertirEnOpportunite")
    class Convertir {

        @Test
        @DisplayName("lead déjà converti → BusinessException")
        void dejaConverti_leve() {
            when(leadRepository.findByIdAndProprietaireId(50L, 1L)).thenReturn(Optional.of(lead(StatutLead.CONVERTI)));
            when(proprietaireRepository.findById(1L)).thenReturn(Optional.of(proprietaire));

            assertThatThrownBy(() ->
                    leadService.convertirEnOpportunite(50L, 5L, false, "Titre", 1L))
                    .isInstanceOf(BusinessException.class);
            verify(opportuniteRepository, never()).save(any());
        }

        @Test
        @DisplayName("avec client existant → crée l'opportunité et passe le lead à CONVERTI")
        void clientExistant_convertit() {
            Lead lead = lead(StatutLead.QUALIFIE);
            when(leadRepository.findByIdAndProprietaireId(50L, 1L)).thenReturn(Optional.of(lead));
            when(proprietaireRepository.findById(1L)).thenReturn(Optional.of(proprietaire));
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(mock(Client.class)));
            when(opportuniteRepository.save(any(Opportunite.class))).thenAnswer(i -> i.getArgument(0));
            when(leadRepository.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));
            OpportuniteResponse oppResp = mock(OpportuniteResponse.class);
            when(opportuniteMapper.toResponse(any(Opportunite.class))).thenReturn(oppResp);

            OpportuniteResponse resultat =
                    leadService.convertirEnOpportunite(50L, 5L, false, "Grand projet", 1L);

            assertThat(resultat).isSameAs(oppResp);
            assertThat(lead.getStatut()).isEqualTo(StatutLead.CONVERTI);
            verify(opportuniteRepository).save(any(Opportunite.class));
        }

        @Test
        @DisplayName("sans client et sans création auto → BusinessException")
        void sansClient_leve() {
            when(leadRepository.findByIdAndProprietaireId(50L, 1L)).thenReturn(Optional.of(lead(StatutLead.QUALIFIE)));
            when(proprietaireRepository.findById(1L)).thenReturn(Optional.of(proprietaire));

            assertThatThrownBy(() ->
                    leadService.convertirEnOpportunite(50L, null, false, "T", 1L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("obtenir / supprimer")
    class ObtenirSupprimer {

        @Test
        @DisplayName("obtenir : lead introuvable → ResourceNotFoundException")
        void obtenir_introuvable() {
            when(leadRepository.findByIdAndProprietaireId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> leadService.obtenir(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("supprimer : supprime le lead du propriétaire")
        void supprimer_ok() {
            when(leadRepository.findByIdAndProprietaireId(50L, 1L)).thenReturn(Optional.of(lead(StatutLead.NOUVEAU)));

            leadService.supprimer(50L, 1L);

            verify(leadRepository).deleteById(50L);
        }
    }
}
