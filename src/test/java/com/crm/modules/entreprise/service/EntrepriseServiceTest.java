package com.crm.modules.entreprise.service;

import com.crm.modules.auth.service.EmailService;
import com.crm.modules.entreprise.dto.*;
import com.crm.modules.entreprise.entity.EntrepriseCompte;
import com.crm.modules.entreprise.repository.EntrepriseCompteRepository;
import com.crm.modules.notification.service.NotificationService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.entity.SuperAdmin;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.modules.utilisateur.repository.SuperAdminRepository;
import com.crm.modules.utilisateur.repository.UtilisateurRepository;
import com.crm.shared.enums.RoleUtilisateur;
import com.crm.shared.enums.StatutCompte;
import com.crm.shared.enums.TailleEntreprise;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour EntrepriseService.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class EntrepriseServiceTest {

    @Mock private EntrepriseCompteRepository entrepriseRepository;
    @Mock private ProprietaireRepository     proprietaireRepository;
    @Mock private SuperAdminRepository       superAdminRepository;
    @Mock private UtilisateurRepository      utilisateurRepository;
    @Mock private EntrepriseMapper           mapper;
    @Mock private PasswordEncoder            passwordEncoder;
    @Mock private EmailService               emailService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private EntrepriseService entrepriseService;

    private InscriptionEntrepriseRequest inscriptionRequest;
    private EntrepriseCompte entreprise;
    private ProprietaireEntreprise proprietaire;
    private SuperAdmin superAdmin;

    @BeforeEach
    void setUp() {
        inscriptionRequest = new InscriptionEntrepriseRequest();
        inscriptionRequest.setNom("Martin");
        inscriptionRequest.setPrenom("Paul");
        inscriptionRequest.setEmail("paul.martin@corp.com");
        inscriptionRequest.setMotDePasse("SecurePass123");
        inscriptionRequest.setTelephone("0612345678");
        inscriptionRequest.setNomEntreprise("TechCorp");
        inscriptionRequest.setMatriculeFiscale("MAT001");
        inscriptionRequest.setTailleEntreprise(TailleEntreprise.PME);

        entreprise = new EntrepriseCompte();
        entreprise.setId(1L);
        entreprise.setNomEntreprise("TechCorp");
        entreprise.setMatriculeFiscale("MAT001");
        entreprise.setStatutCompte(StatutCompte.EN_ATTENTE);

        proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);
        proprietaire.setEmail("paul.martin@corp.com");
        proprietaire.setNom("Martin");
        proprietaire.setPrenom("Paul");
        proprietaire.setEntrepriseCompte(entreprise);

        superAdmin = new SuperAdmin();
        superAdmin.setId(99L);
        superAdmin.setEmail("admin@crm.com");
    }

    // ── Tests inscription ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Inscription entreprise — succès — retourne le compte créé")
    void inscrire_succes_retourneCompteCreé() {
        when(proprietaireRepository.existsByEmailAndCompteNonSupprime(anyString())).thenReturn(false);
        when(entrepriseRepository.existsByMatriculeFiscaleAndIsDeletedFalse(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPwd");
        when(proprietaireRepository.save(any())).thenReturn(proprietaire);

        // ← AJOUT
        doNothing().when(notificationService).creerNotification(
                anyString(), anyString(), any(), anyString()
        );

        EntrepriseCompteResponse expected = EntrepriseCompteResponse.builder()
                .id(1L).nomEntreprise("TechCorp").build();
        when(mapper.toResponseWithProprietaire(any(), any())).thenReturn(expected);

        EntrepriseCompteResponse result = entrepriseService.inscrireEntreprise(inscriptionRequest);

        assertThat(result.getNomEntreprise()).isEqualTo("TechCorp");
        verify(emailService).envoyerEmailBienvenue(
                eq("paul.martin@corp.com"), anyString(), eq("TechCorp"));
    }

    @Test
    @DisplayName("Inscription — email déjà utilisé — lève BusinessException")
    void inscrire_emailDejaUtilise_leveException() {
        when(proprietaireRepository.existsByEmailAndCompteNonSupprime("paul.martin@corp.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> entrepriseService.inscrireEntreprise(inscriptionRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("Inscription — matricule déjà utilisée — lève BusinessException")
    void inscrire_matriculeDejaUtilisee_leveException() {
        when(proprietaireRepository.existsByEmailAndCompteNonSupprime(anyString())).thenReturn(false);
        when(entrepriseRepository.existsByMatriculeFiscaleAndIsDeletedFalse("MAT001"))
                .thenReturn(true);

        assertThatThrownBy(() -> entrepriseService.inscrireEntreprise(inscriptionRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("matricule");
    }

    // ── Tests traiterDemande ──────────────────────────────────────────────────

    @Test
    @DisplayName("Valider une entreprise EN_ATTENTE — statut devient ACTIVE")
    void traiterDemande_valider_statutActive() {
        ValiderEntrepriseRequest request = new ValiderEntrepriseRequest();
        request.setValider(true);

        when(entrepriseRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(entreprise));
        when(superAdminRepository.findByEmail("admin@crm.com"))
                .thenReturn(Optional.of(superAdmin));
        when(proprietaireRepository.findByEntrepriseCompteId(1L))
                .thenReturn(Optional.of(proprietaire));
        when(entrepriseRepository.save(any())).thenReturn(entreprise);
        when(mapper.toResponseWithProprietaire(any(), any()))
                .thenReturn(EntrepriseCompteResponse.builder().id(1L).build());

        entrepriseService.traiterDemande(1L, request, "admin@crm.com");

        assertThat(entreprise.getStatutCompte()).isEqualTo(StatutCompte.ACTIVE);
        verify(emailService).envoyerEmailValidation(anyString(), eq("TechCorp"));
    }

    @Test
    @DisplayName("Refuser une entreprise — motif obligatoire absent — lève BusinessException")
    void traiterDemande_refuserSansMotif_leveException() {
        ValiderEntrepriseRequest request = new ValiderEntrepriseRequest();
        request.setValider(false);
        request.setMotifRefus("   ");

        when(entrepriseRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(entreprise));
        when(superAdminRepository.findByEmail("admin@crm.com"))
                .thenReturn(Optional.of(superAdmin));
        when(proprietaireRepository.findByEntrepriseCompteId(1L))
                .thenReturn(Optional.of(proprietaire));

        assertThatThrownBy(() -> entrepriseService.traiterDemande(1L, request, "admin@crm.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motif");
    }

    @Test
    @DisplayName("Traiter un compte déjà validé — lève BusinessException")
    void traiterDemande_compteDejaTraite_leveException() {
        entreprise.setStatutCompte(StatutCompte.ACTIVE);
        ValiderEntrepriseRequest request = new ValiderEntrepriseRequest();
        request.setValider(true);

        when(entrepriseRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(entreprise));

        assertThatThrownBy(() -> entrepriseService.traiterDemande(1L, request, "admin@crm.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà été traité");
    }

    @Test
    @DisplayName("Consulter détails d'une entreprise inexistante — lève ResourceNotFoundException")
    void consulterDetails_inexistant_leveException() {
        when(entrepriseRepository.findByIdAndIsDeletedFalse(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> entrepriseService.consulterDetails(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Suppression logique — isDeleted devient true")
    void supprimerLogiquement_succes() {
        when(entrepriseRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(entreprise));
        when(entrepriseRepository.save(any())).thenReturn(entreprise);

        entrepriseService.supprimerLogiquement(1L, "admin@crm.com");

        assertThat(entreprise.isDeleted()).isTrue();
        assertThat(entreprise.getDeletedAt()).isNotNull();
    }
}