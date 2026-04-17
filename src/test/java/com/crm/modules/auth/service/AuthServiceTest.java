package com.crm.modules.auth.service;

import com.crm.config.jwt.JwtService;
import com.crm.modules.auth.dto.AuthResponse;
import com.crm.modules.auth.dto.LoginRequest;
import com.crm.modules.entreprise.entity.EntrepriseCompte;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.entity.SuperAdmin;
import com.crm.modules.utilisateur.repository.UtilisateurRepository;
import com.crm.shared.enums.RoleUtilisateur;
import com.crm.shared.enums.StatutCompte;
import com.crm.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AuthService.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private SuperAdmin superAdmin;
    private ProprietaireEntreprise proprietaire;
    private EntrepriseCompte entrepriseActive;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setMotDePasse("password123");

        superAdmin = new SuperAdmin();
        superAdmin.setEmail("admin@crm.com");
        superAdmin.setNom("Admin");
        superAdmin.setPrenom("Super");
        superAdmin.setRole(RoleUtilisateur.ROLE_SUPER_ADMIN);
        superAdmin.setMotDePasseHash("hashedPwd");

        entrepriseActive = new EntrepriseCompte();
        entrepriseActive.setId(1L);
        entrepriseActive.setNomEntreprise("Tech Corp");
        entrepriseActive.setStatutCompte(StatutCompte.ACTIVE);

        proprietaire = new ProprietaireEntreprise();
        proprietaire.setEmail("owner@corp.com");
        proprietaire.setNom("Dupont");
        proprietaire.setPrenom("Jean");
        proprietaire.setRole(RoleUtilisateur.ROLE_PROPRIETAIRE);
        proprietaire.setMotDePasseHash("hashedPwd");
        proprietaire.setEntrepriseCompte(entrepriseActive);
    }

    @Test
    @DisplayName("Connexion SuperAdmin réussie — retourne un token JWT")
    void login_superAdmin_succes() {
        loginRequest.setEmail("admin@crm.com");
        when(utilisateurRepository.findByEmail("admin@crm.com"))
                .thenReturn(Optional.of(superAdmin));
        when(jwtService.generateToken(superAdmin)).thenReturn("access_token");
        when(jwtService.generateRefreshToken(superAdmin)).thenReturn("refresh_token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRole()).isEqualTo(RoleUtilisateur.ROLE_SUPER_ADMIN);
        assertThat(response.getEntrepriseId()).isNull();
        verify(utilisateurRepository).save(superAdmin);
    }

    @Test
    @DisplayName("Connexion ProprietaireEntreprise avec compte ACTIVE — succès")
    void login_proprietaire_compteActive_succes() {
        loginRequest.setEmail("owner@corp.com");
        when(utilisateurRepository.findByEmail("owner@corp.com"))
                .thenReturn(Optional.of(proprietaire));
        when(jwtService.generateToken(proprietaire)).thenReturn("access_token");
        when(jwtService.generateRefreshToken(proprietaire)).thenReturn("refresh_token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getEntrepriseId()).isEqualTo(1L);
        assertThat(response.getNomEntreprise()).isEqualTo("Tech Corp");
        assertThat(response.getStatutCompte()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Identifiants incorrects — lève BusinessException")
    void login_identifiantsIncorrects_leveException() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");
    }

    @Test
    @DisplayName("Compte ProprietaireEntreprise EN_ATTENTE — connexion refusée")
    void login_compteEnAttente_connexionRefusee() {
        entrepriseActive.setStatutCompte(StatutCompte.EN_ATTENTE);
        loginRequest.setEmail("owner@corp.com");
        when(utilisateurRepository.findByEmail("owner@corp.com"))
                .thenReturn(Optional.of(proprietaire));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("en attente de validation");
    }

    @Test
    @DisplayName("Compte ProprietaireEntreprise REFUSE — connexion refusée avec motif")
    void login_compteRefuse_connexionRefusee() {
        entrepriseActive.setStatutCompte(StatutCompte.REFUSE);
        entrepriseActive.setMotifRefus("Documents incomplets");
        loginRequest.setEmail("owner@corp.com");
        when(utilisateurRepository.findByEmail("owner@corp.com"))
                .thenReturn(Optional.of(proprietaire));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("refusé");
    }

    @Test
    @DisplayName("Compte ProprietaireEntreprise SUSPENDU — connexion refusée")
    void login_compteSuspendu_connexionRefusee() {
        entrepriseActive.setStatutCompte(StatutCompte.SUSPENDU);
        loginRequest.setEmail("owner@corp.com");
        when(utilisateurRepository.findByEmail("owner@corp.com"))
                .thenReturn(Optional.of(proprietaire));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("suspendu");
    }

    @Test
    @DisplayName("ProprietaireEntreprise sans compte entreprise — lève BusinessException")
    void login_sanCompteEntreprise_leveException() {
        proprietaire.setEntrepriseCompte(null);
        loginRequest.setEmail("owner@corp.com");
        when(utilisateurRepository.findByEmail("owner@corp.com"))
                .thenReturn(Optional.of(proprietaire));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Aucun compte entreprise");
    }
}