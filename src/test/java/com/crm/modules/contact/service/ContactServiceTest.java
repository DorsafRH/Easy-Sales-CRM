package com.crm.modules.contact.service;

import com.crm.modules.client.entity.ClientEntreprise;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.contact.dto.ContactRequest;
import com.crm.modules.contact.dto.ContactResponse;
import com.crm.modules.contact.entity.Contact;
import com.crm.modules.contact.mapper.ContactMapper;
import com.crm.modules.contact.repository.ContactRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link ContactService}.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock private ContactRepository contactRepository;
    @Mock private ClientRepository  clientRepository;
    @Mock private ContactMapper     contactMapper;

    @InjectMocks
    private ContactService contactService;

    private ProprietaireEntreprise proprietaire;
    private ClientEntreprise       client;
    private Contact                contact;
    private Contact                contactPrincipal;
    private ContactResponse        contactResponse;

    @BeforeEach
    void setUp() {
        proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);

        client = new ClientEntreprise();
        client.setId(5L);
        client.setRaisonSociale("TechCorp SARL");
        client.setNomAffichage("TechCorp SARL");
        client.setProprietaire(proprietaire);

        contact = Contact.builder()
                .id(100L).nom("Trabelsi").prenom("Sami")
                .email("sami@techcorp.com").poste("DG")
                .isPrincipal(false).client(client)
                .dateCreation(LocalDateTime.now()).build();

        contactPrincipal = Contact.builder()
                .id(99L).nom("Mansour").prenom("Leila")
                .isPrincipal(true).client(client)
                .dateCreation(LocalDateTime.now()).build();

        contactResponse = new ContactResponse();
        contactResponse.setId(100L);
        contactResponse.setNom("Trabelsi");
        contactResponse.setPrenom("Sami");
        contactResponse.setClientId(5L);
        contactResponse.setClientNomAffichage("TechCorp SARL");
    }

    // ── lister ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("lister")
    class Lister {

        @Test
        @DisplayName("Sans keyword — retourne tous les contacts du client")
        void sansKeyword_retourneListe() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(contactPrincipal, contact));
            when(contactMapper.toResponse(any())).thenReturn(contactResponse);

            List<ContactResponse> result = contactService.lister(5L, 1L, null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Avec keyword — délègue à la specification")
        void avecKeyword_delegueSpecification() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(contact));
            when(contactMapper.toResponse(contact)).thenReturn(contactResponse);

            List<ContactResponse> result = contactService.lister(5L, 1L, "sami");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Client introuvable — lève ResourceNotFoundException")
        void clientIntrouvable_leveException() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactService.lister(99L, 1L, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Multi-tenant — propriétaire ne voit pas les contacts d'un autre client")
        void multiTenant_accesDenied() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 2L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactService.lister(5L, 2L, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Aucun contact — retourne liste vide")
        void aucunContact_retourneListeVide() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findAll(any(Specification.class))).thenReturn(List.of());

            assertThat(contactService.lister(5L, 1L, null)).isEmpty();
        }
    }

    // ── obtenir ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenir")
    class Obtenir {

        @Test
        @DisplayName("Contact trouvé — retourne la réponse mappée")
        void trouve_retourneReponse() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findByIdAndClientId(100L, 5L))
                    .thenReturn(Optional.of(contact));
            when(contactMapper.toResponse(contact)).thenReturn(contactResponse);

            ContactResponse result = contactService.obtenir(100L, 5L, 1L);

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getClientNomAffichage()).isEqualTo("TechCorp SARL");
        }

        @Test
        @DisplayName("Contact introuvable — lève ResourceNotFoundException")
        void contactIntrouvable_leveException() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findByIdAndClientId(999L, 5L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactService.obtenir(999L, 5L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Client introuvable — lève ResourceNotFoundException")
        void clientIntrouvable_leveException() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactService.obtenir(100L, 99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── creer ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("creer")
    class Creer {

        @Test
        @DisplayName("Contact non principal — sauvegardé sans toucher au principal existant")
        void nonPrincipal_sansModifierPrincipalExistant() {
            ContactRequest req = new ContactRequest();
            req.setNom("Trabelsi"); req.setPrenom("Sami");
            req.setEmail("sami@techcorp.com");
            req.setIsPrincipal(false);

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.save(any(Contact.class))).thenReturn(contact);
            when(contactMapper.toResponse(contact)).thenReturn(contactResponse);

            contactService.creer(5L, req, 1L);

            verify(contactRepository, never()).findByClientIdAndIsPrincipalTrue(any());
            verify(contactRepository).save(any(Contact.class));
        }

        @Test
        @DisplayName("Contact principal — l'ancien principal est décroché")
        void principal_retirerAncienPrincipal() {
            ContactRequest req = new ContactRequest();
            req.setNom("Trabelsi"); req.setPrenom("Sami");
            req.setIsPrincipal(true);

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findByClientIdAndIsPrincipalTrue(5L))
                    .thenReturn(Optional.of(contactPrincipal));
            when(contactRepository.save(any(Contact.class))).thenReturn(contact);
            when(contactMapper.toResponse(contact)).thenReturn(contactResponse);

            contactService.creer(5L, req, 1L);

            assertThat(contactPrincipal.isPrincipal()).isFalse();
            verify(contactRepository, times(2)).save(any(Contact.class));
        }

        @Test
        @DisplayName("Contact principal — aucun ancien principal — aucune erreur")
        void principal_sansAncienPrincipal_succes() {
            ContactRequest req = new ContactRequest();
            req.setNom("Trabelsi"); req.setPrenom("Sami");
            req.setIsPrincipal(true);

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findByClientIdAndIsPrincipalTrue(5L))
                    .thenReturn(Optional.empty());
            when(contactRepository.save(any(Contact.class))).thenReturn(contact);
            when(contactMapper.toResponse(contact)).thenReturn(contactResponse);

            assertThatNoException().isThrownBy(
                    () -> contactService.creer(5L, req, 1L));
        }

        @Test
        @DisplayName("isPrincipal null traité comme false")
        void isPrincipalNull_traitesCommeFalse() {
            ContactRequest req = new ContactRequest();
            req.setNom("Trabelsi"); req.setPrenom("Sami");
            req.setIsPrincipal(null);

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.save(any(Contact.class))).thenReturn(contact);
            when(contactMapper.toResponse(contact)).thenReturn(contactResponse);

            contactService.creer(5L, req, 1L);

            verify(contactRepository).save(argThat(c -> !c.isPrincipal()));
        }

        @Test
        @DisplayName("Client introuvable — lève ResourceNotFoundException")
        void clientIntrouvable_leveException() {
            ContactRequest req = new ContactRequest();
            req.setNom("X"); req.setIsPrincipal(false);

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactService.creer(99L, req, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── modifier ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("modifier")
    class Modifier {

        @Test
        @DisplayName("Modification des champs — données mises à jour")
        void champs_misesAJour() {
            ContactRequest req = new ContactRequest();
            req.setNom("Trabelsi"); req.setPrenom("Sami");
            req.setPoste("PDG"); req.setEmail("new@techcorp.com");
            req.setIsPrincipal(false);

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findByIdAndClientId(100L, 5L))
                    .thenReturn(Optional.of(contact));
            when(contactRepository.save(contact)).thenReturn(contact);
            when(contactMapper.toResponse(contact)).thenReturn(contactResponse);

            contactService.modifier(100L, 5L, req, 1L);

            assertThat(contact.getPoste()).isEqualTo("PDG");
            assertThat(contact.getEmail()).isEqualTo("new@techcorp.com");
            verify(contactRepository).save(contact);
        }

        @Test
        @DisplayName("Passage en principal — retire l'ancien principal")
        void passageEnPrincipal_retirerAncien() {
            ContactRequest req = new ContactRequest();
            req.setNom("Trabelsi"); req.setPrenom("Sami");
            req.setIsPrincipal(true);

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findByIdAndClientId(100L, 5L))
                    .thenReturn(Optional.of(contact));
            when(contactRepository.findByClientIdAndIsPrincipalTrue(5L))
                    .thenReturn(Optional.of(contactPrincipal));
            when(contactRepository.save(any())).thenReturn(contact);
            when(contactMapper.toResponse(any())).thenReturn(contactResponse);

            contactService.modifier(100L, 5L, req, 1L);

            assertThat(contactPrincipal.isPrincipal()).isFalse();
            assertThat(contact.isPrincipal()).isTrue();
        }

        @Test
        @DisplayName("Déjà principal — aucune réinitialisation de l'ancien")
        void dejasPrincipal_aucuneReinitialisation() {
            contact.setPrincipal(true);

            ContactRequest req = new ContactRequest();
            req.setNom("Trabelsi"); req.setPrenom("Sami");
            req.setIsPrincipal(true);

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findByIdAndClientId(100L, 5L))
                    .thenReturn(Optional.of(contact));
            when(contactRepository.save(contact)).thenReturn(contact);
            when(contactMapper.toResponse(contact)).thenReturn(contactResponse);

            contactService.modifier(100L, 5L, req, 1L);

            verify(contactRepository, never()).findByClientIdAndIsPrincipalTrue(any());
        }

        @Test
        @DisplayName("isPrincipal null — le flag actuel conservé")
        void isPrincipalNull_flagConserve() {
            ContactRequest req = new ContactRequest();
            req.setNom("Trabelsi"); req.setPrenom("Sami");
            req.setIsPrincipal(null);

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findByIdAndClientId(100L, 5L))
                    .thenReturn(Optional.of(contact));
            when(contactRepository.save(contact)).thenReturn(contact);
            when(contactMapper.toResponse(contact)).thenReturn(contactResponse);

            contactService.modifier(100L, 5L, req, 1L);

            assertThat(contact.isPrincipal()).isFalse();
        }

        @Test
        @DisplayName("Contact introuvable — lève ResourceNotFoundException")
        void contactIntrouvable_leveException() {
            ContactRequest req = new ContactRequest();
            req.setNom("X"); req.setIsPrincipal(false);

            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findByIdAndClientId(999L, 5L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactService.modifier(999L, 5L, req, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── supprimer ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("supprimer")
    class Supprimer {

        @Test
        @DisplayName("Contact non principal — supprimé via deleteById")
        void nonPrincipal_supprime() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findByIdAndClientId(100L, 5L))
                    .thenReturn(Optional.of(contact));

            contactService.supprimer(100L, 5L, 1L);

            verify(contactRepository).deleteById(100L);
        }

        @Test
        @DisplayName("Contact principal — lève BusinessException, aucun delete")
        void principal_leveException() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findByIdAndClientId(99L, 5L))
                    .thenReturn(Optional.of(contactPrincipal));

            assertThatThrownBy(() -> contactService.supprimer(99L, 5L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("contact principal");

            verify(contactRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Contact introuvable — lève ResourceNotFoundException")
        void contactIntrouvable_leveException() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(5L, 1L))
                    .thenReturn(Optional.of(client));
            when(contactRepository.findByIdAndClientId(999L, 5L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactService.supprimer(999L, 5L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Client introuvable — lève ResourceNotFoundException")
        void clientIntrouvable_leveException() {
            when(clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactService.supprimer(100L, 99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}