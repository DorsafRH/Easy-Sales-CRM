package com.crm.modules.client.service;

import com.crm.modules.client.dto.ClientRequest;
import com.crm.modules.client.dto.ClientResponse;
import com.crm.modules.client.entity.Client;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.crm.modules.client.entity.ClientEntreprise;
import com.crm.modules.client.entity.ClientIndividuel;
import com.crm.modules.client.mapper.ClientMapper;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.contact.repository.ContactRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import com.crm.shared.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link ClientService}.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientServiceTest {

    @Mock private ClientRepository  clientRepository;
    @Mock private ContactRepository contactRepository;
    @Mock private ClientMapper      clientMapper;

    @InjectMocks
    private ClientService clientService;

    private ProprietaireEntreprise proprietaire;
    private ClientIndividuel       individuel;
    private ClientEntreprise       entreprise;
    private ClientResponse         responseIndividuel;
    private ClientResponse         responseEntreprise;

    @BeforeEach
    void setUp() {
        proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);

        individuel = new ClientIndividuel();
        individuel.setId(10L);
        individuel.setNom("Ben Ali");
        individuel.setPrenom("Karim");
        individuel.setEmail("karim@test.com");
        individuel.setNomAffichage("Karim Ben Ali");
        individuel.setStatut("ACTIF");
        individuel.setDateCreation(LocalDateTime.now());
        individuel.setProprietaire(proprietaire);

        entreprise = new ClientEntreprise();
        entreprise.setId(20L);
        entreprise.setRaisonSociale("TechCorp SARL");
        entreprise.setEmail("contact@techcorp.com");
        entreprise.setNomAffichage("TechCorp SARL");
        entreprise.setStatut("ACTIF");
        entreprise.setDateCreation(LocalDateTime.now());
        entreprise.setProprietaire(proprietaire);

        responseIndividuel = new ClientResponse();
        responseIndividuel.setId(10L);
        responseIndividuel.setTypeClient("INDIVIDUEL");
        responseIndividuel.setNomAffichage("Karim Ben Ali");

        responseEntreprise = new ClientResponse();
        responseEntreprise.setId(20L);
        responseEntreprise.setTypeClient("ENTREPRISE");
        responseEntreprise.setNomAffichage("TechCorp SARL");
    }

    // ── lister ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("lister")
    class Lister {

        @Test
        @DisplayName("Sans filtres — retourne tous les clients paginés")
        void sansFiltres_retourneListe() {
            when(clientRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(individuel)));
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseIndividuel);
            when(contactRepository.countByClientId(10L)).thenReturn(2);

            PageResponse<ClientResponse> result =
                    clientService.lister(1L, null, null, 0, 20);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getNomAffichage()).isEqualTo("Karim Ben Ali");
            assertThat(result.getContent().get(0).getNbContacts()).isEqualTo(2);
        }

        @Test
        @DisplayName("Avec filtre type INDIVIDUEL — délègue à la specification")
        void avecFiltreType_delegueSpecification() {
            when(clientRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(individuel)));
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseIndividuel);
            when(contactRepository.countByClientId(10L)).thenReturn(0);

            PageResponse<ClientResponse> result =
                    clientService.lister(1L, "INDIVIDUEL", null, 0, 20);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Avec keyword — délègue à la specification")
        void avecKeyword_delegueSpecification() {
            when(clientRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            PageResponse<ClientResponse> result =
                    clientService.lister(1L, null, "xyz", 0, 20);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("chiffreAffaires toujours à ZERO en Sprint 2")
        void chiffreAffaires_zero_sprint2() {
            when(clientRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(individuel)));
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseIndividuel);
            when(contactRepository.countByClientId(10L)).thenReturn(0);

            PageResponse<ClientResponse> result =
                    clientService.lister(1L, null, null, 0, 20);

            assertThat(result.getContent().get(0).getChiffreAffaires())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ── obtenir ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenir")
    class Obtenir {

        @Test
        @DisplayName("Client individuel trouvé — retourne la réponse enrichie")
        void individuel_retourneReponse() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(10L, 1L))
                    .thenReturn(Optional.of(individuel));
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseIndividuel);
            when(contactRepository.countByClientId(10L)).thenReturn(3);

            ClientResponse result = clientService.obtenir(10L, 1L);

            assertThat(result.getNomAffichage()).isEqualTo("Karim Ben Ali");
            assertThat(result.getNbContacts()).isEqualTo(3);
        }

        @Test
        @DisplayName("Client entreprise trouvé — retourne la réponse enrichie")
        void entreprise_retourneReponse() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(20L, 1L))
                    .thenReturn(Optional.of(entreprise));
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseEntreprise);
            when(contactRepository.countByClientId(20L)).thenReturn(1);

            ClientResponse result = clientService.obtenir(20L, 1L);

            assertThat(result.getTypeClient()).isEqualTo("ENTREPRISE");
        }

        @Test
        @DisplayName("Client introuvable — lève ResourceNotFoundException")
        void introuvable_leveException() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.obtenir(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Multi-tenant — un propriétaire ne voit pas les clients d'un autre")
        void multiTenant_accesDenied() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(10L, 2L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.obtenir(10L, 2L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── creer INDIVIDUEL ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("creer — INDIVIDUEL")
    class CreerIndividuel {

        private ClientRequest req;

        @BeforeEach
        void setUp() {
            req = new ClientRequest();
            req.setTypeClient("INDIVIDUEL");
            req.setNom("Ben Ali");
            req.setPrenom("Karim");
            req.setEmail("karim@test.com");
        }

        @Test
        @DisplayName("Données valides — client individuel créé")
        void donneesValides_succes() {
            when(clientRepository.existsByEmailAndProprietaireIdAndIsDeletedFalse(
                    "karim@test.com", 1L)).thenReturn(false);
            when(clientRepository.save(any(ClientIndividuel.class))).thenReturn(individuel);
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseIndividuel);
            when(contactRepository.countByClientId(10L)).thenReturn(0);

            ClientResponse result = clientService.creer(req, proprietaire);

            assertThat(result).isNotNull();
            verify(clientRepository).save(argThat(c -> c instanceof ClientIndividuel));
        }

        @Test
        @DisplayName("Sans email — création sans validation email")
        void sansEmail_succes() {
            req.setEmail(null);

            when(clientRepository.save(any(ClientIndividuel.class))).thenReturn(individuel);
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseIndividuel);
            when(contactRepository.countByClientId(10L)).thenReturn(0);

            assertThatNoException().isThrownBy(() -> clientService.creer(req, proprietaire));
        }

        @Test
        @DisplayName("Sans nom — lève BusinessException")
        void sansNom_leveException() {
            req.setNom(null);

            assertThatThrownBy(() -> clientService.creer(req, proprietaire))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("nom est obligatoire");

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nom vide — lève BusinessException")
        void nomVide_leveException() {
            req.setNom("   ");

            assertThatThrownBy(() -> clientService.creer(req, proprietaire))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("nom est obligatoire");
        }

        @Test
        @DisplayName("Sans prénom — lève BusinessException")
        void sansPrenom_leveException() {
            req.setPrenom(null);

            assertThatThrownBy(() -> clientService.creer(req, proprietaire))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("prénom est obligatoire");
        }

        @Test
        @DisplayName("Email en double — lève BusinessException, aucun save")
        void emailEnDouble_leveException() {
            when(clientRepository.existsByEmailAndProprietaireIdAndIsDeletedFalse(
                    "karim@test.com", 1L)).thenReturn(true);

            assertThatThrownBy(() -> clientService.creer(req, proprietaire))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("email existe déjà");

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("nomAffichage calculé : prénom + nom")
        void nomAffichage_calculeCorrectement() {
            when(clientRepository.existsByEmailAndProprietaireIdAndIsDeletedFalse(any(), any()))
                    .thenReturn(false);
            when(clientRepository.save(any(ClientIndividuel.class))).thenReturn(individuel);
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseIndividuel);
            when(contactRepository.countByClientId(any())).thenReturn(0);

            clientService.creer(req, proprietaire);

            verify(clientRepository).save(argThat(c -> {
                ClientIndividuel ci = (ClientIndividuel) c;
                return "Karim Ben Ali".equals(ci.getNomAffichage());
            }));
        }
    }

    // ── creer ENTREPRISE ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("creer — ENTREPRISE")
    class CreerEntreprise {

        private ClientRequest req;

        @BeforeEach
        void setUp() {
            req = new ClientRequest();
            req.setTypeClient("ENTREPRISE");
            req.setRaisonSociale("TechCorp SARL");
            req.setEmail("contact@techcorp.com");
        }

        @Test
        @DisplayName("Données valides — client entreprise créé")
        void donneesValides_succes() {
            when(clientRepository.existsByEmailAndProprietaireIdAndIsDeletedFalse(
                    "contact@techcorp.com", 1L)).thenReturn(false);
            when(clientRepository.save(any(ClientEntreprise.class))).thenReturn(entreprise);
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseEntreprise);
            when(contactRepository.countByClientId(20L)).thenReturn(0);

            ClientResponse result = clientService.creer(req, proprietaire);

            assertThat(result.getTypeClient()).isEqualTo("ENTREPRISE");
            verify(clientRepository).save(argThat(c -> c instanceof ClientEntreprise));
        }

        @Test
        @DisplayName("Sans raison sociale — lève BusinessException")
        void sansRaisonSociale_leveException() {
            req.setRaisonSociale(null);

            assertThatThrownBy(() -> clientService.creer(req, proprietaire))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("raison sociale est obligatoire");
        }

        @Test
        @DisplayName("Raison sociale vide — lève BusinessException")
        void raisonSocialeVide_leveException() {
            req.setRaisonSociale("  ");

            assertThatThrownBy(() -> clientService.creer(req, proprietaire))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("raison sociale est obligatoire");
        }

        @Test
        @DisplayName("nomAffichage = raison sociale")
        void nomAffichage_egaleRaisonSociale() {
            when(clientRepository.existsByEmailAndProprietaireIdAndIsDeletedFalse(any(), any()))
                    .thenReturn(false);
            when(clientRepository.save(any(ClientEntreprise.class))).thenReturn(entreprise);
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseEntreprise);
            when(contactRepository.countByClientId(any())).thenReturn(0);

            clientService.creer(req, proprietaire);

            verify(clientRepository).save(argThat(c -> {
                ClientEntreprise ce = (ClientEntreprise) c;
                return "TechCorp SARL".equals(ce.getNomAffichage());
            }));
        }
    }

    // ── creer — cas généraux ─────────────────────────────────────────────────

    @Nested
    @DisplayName("creer — cas généraux")
    class CreerCasGeneraux {

        @Test
        @DisplayName("Type invalide — lève BusinessException")
        void typeInvalide_leveException() {
            ClientRequest req = new ClientRequest();
            req.setTypeClient("INCONNU");

            assertThatThrownBy(() -> clientService.creer(req, proprietaire))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Type client invalide");
        }

        @Test
        @DisplayName("Email null — aucune vérification de doublon email")
        void emailNull_aucuneVerification() {
            ClientRequest req = new ClientRequest();
            req.setTypeClient("INDIVIDUEL");
            req.setNom("Ben Ali");
            req.setPrenom("Karim");
            req.setEmail(null);

            when(clientRepository.save(any(ClientIndividuel.class))).thenReturn(individuel);
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseIndividuel);
            when(contactRepository.countByClientId(any())).thenReturn(0);

            clientService.creer(req, proprietaire);

            verify(clientRepository, never())
                    .existsByEmailAndProprietaireIdAndIsDeletedFalse(any(), any());
        }
    }

    // ── modifier ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("modifier")
    class Modifier {

        @Test
        @DisplayName("Modification individuel — données mises à jour")
        void individuel_donneesMAJ() {
            ClientRequest req = new ClientRequest();
            req.setTypeClient("INDIVIDUEL");
            req.setNom("Trabelsi");
            req.setPrenom("Sami");
            req.setEmail("sami@test.com");

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(10L, 1L))
                    .thenReturn(Optional.of(individuel));
            when(clientRepository.existsByEmailAndProprietaireIdAndIsDeletedFalse(
                    "sami@test.com", 1L)).thenReturn(false);
            when(clientRepository.save(individuel)).thenReturn(individuel);
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseIndividuel);
            when(contactRepository.countByClientId(10L)).thenReturn(0);

            clientService.modifier(10L, req, 1L);

            assertThat(individuel.getNom()).isEqualTo("Trabelsi");
            assertThat(individuel.getPrenom()).isEqualTo("Sami");
        }

        @Test
        @DisplayName("Même email — aucune vérification de doublon")
        void memeEmail_pasDeVerificationDoublon() {
            ClientRequest req = new ClientRequest();
            req.setTypeClient("INDIVIDUEL");
            req.setNom("Ben Ali");
            req.setPrenom("Karim");
            req.setEmail("karim@test.com");

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(10L, 1L))
                    .thenReturn(Optional.of(individuel));
            when(clientRepository.save(individuel)).thenReturn(individuel);
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseIndividuel);
            when(contactRepository.countByClientId(10L)).thenReturn(0);

            clientService.modifier(10L, req, 1L);

            verify(clientRepository, never())
                    .existsByEmailAndProprietaireIdAndIsDeletedFalse(any(), any());
        }

        @Test
        @DisplayName("Nouvel email en double — lève BusinessException")
        void nouvelEmailEnDouble_leveException() {
            ClientRequest req = new ClientRequest();
            req.setTypeClient("INDIVIDUEL");
            req.setNom("Ben Ali");
            req.setPrenom("Karim");
            req.setEmail("autre@test.com");

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(10L, 1L))
                    .thenReturn(Optional.of(individuel));
            when(clientRepository.existsByEmailAndProprietaireIdAndIsDeletedFalse(
                    "autre@test.com", 1L)).thenReturn(true);

            assertThatThrownBy(() -> clientService.modifier(10L, req, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("email existe déjà");
        }

        @Test
        @DisplayName("Client introuvable — lève ResourceNotFoundException")
        void introuvable_leveException() {
            ClientRequest req = new ClientRequest();
            req.setTypeClient("INDIVIDUEL");
            req.setNom("X");
            req.setPrenom("Y");

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.modifier(99L, req, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("nomAffichage recalculé après modification")
        void nomAffichage_recalcule() {
            ClientRequest req = new ClientRequest();
            req.setTypeClient("INDIVIDUEL");
            req.setNom("Mansour");
            req.setPrenom("Leila");
            req.setEmail("karim@test.com");

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(10L, 1L))
                    .thenReturn(Optional.of(individuel));
            when(clientRepository.save(individuel)).thenReturn(individuel);
            when(clientMapper.toResponse(any(Client.class))).thenReturn(responseIndividuel);
            when(contactRepository.countByClientId(10L)).thenReturn(0);

            clientService.modifier(10L, req, 1L);

            assertThat(individuel.getNomAffichage()).isEqualTo("Leila Mansour");
        }
    }

    // ── supprimer ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("supprimer")
    class Supprimer {

        @Test
        @DisplayName("Suppression logique — isDeleted true et statut INACTIF")
        void softDelete_isDeletedEtStatut() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(10L, 1L))
                    .thenReturn(Optional.of(individuel));
            when(clientRepository.save(individuel)).thenReturn(individuel);

            clientService.supprimer(10L, 1L);

            assertThat(individuel.isDeleted()).isTrue();
            assertThat(individuel.getStatut()).isEqualTo("INACTIF");
            assertThat(individuel.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Client introuvable — lève ResourceNotFoundException")
        void introuvable_leveException() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.supprimer(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Après suppression logique — le client n'est plus visible")
        void apresSuppressionLogique_nonVisible() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(10L, 1L))
                    .thenReturn(Optional.of(individuel));
            when(clientRepository.save(individuel)).thenReturn(individuel);

            clientService.supprimer(10L, 1L);

            verify(clientRepository).save(argThat(c ->
                    c.isDeleted() && "INACTIF".equals(c.getStatut())));
        }
    }
}