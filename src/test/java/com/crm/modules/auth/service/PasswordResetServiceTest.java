package com.crm.modules.auth.service;

import com.crm.modules.auth.dto.MotDePasseOublieRequest;
import com.crm.modules.auth.dto.ReinitialisationMotDePasseRequest;
import com.crm.modules.auth.entity.PasswordResetToken;
import com.crm.modules.auth.repository.PasswordResetTokenRepository;
import com.crm.modules.utilisateur.entity.Utilisateur;
import com.crm.modules.utilisateur.repository.UtilisateurRepository;
import com.crm.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link PasswordResetService} — génération et validation des codes de
 * réinitialisation (email inconnu, token invalide, token expiré).
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private PasswordResetToken token(boolean valideDansLeTemps) {
        return PasswordResetToken.builder()
                .token("123456")
                .utilisateur(mock(Utilisateur.class))
                .dateExpiration(valideDansLeTemps
                        ? LocalDateTime.now().plusMinutes(10)
                        : LocalDateTime.now().minusMinutes(10))
                .utilise(false)
                .build();
    }

    @Test
    @DisplayName("demande pour un email inconnu → BusinessException")
    void demander_emailInconnu_leve() {
        MotDePasseOublieRequest req = new MotDePasseOublieRequest();
        req.setEmail("inconnu@ex.com");
        when(utilisateurRepository.findByEmail("inconnu@ex.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.demanderReinitialisation(req))
                .isInstanceOf(BusinessException.class);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("réinitialisation avec token inexistant → BusinessException")
    void reinitialiser_tokenInexistant_leve() {
        ReinitialisationMotDePasseRequest req = new ReinitialisationMotDePasseRequest();
        req.setToken("000000");
        req.setNouveauMotDePasse("nouveau");
        when(tokenRepository.findByToken("000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.reinitialiserMotDePasse(req))
                .isInstanceOf(BusinessException.class);
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    @DisplayName("réinitialisation avec token expiré → BusinessException")
    void reinitialiser_tokenExpire_leve() {
        ReinitialisationMotDePasseRequest req = new ReinitialisationMotDePasseRequest();
        req.setToken("123456");
        req.setNouveauMotDePasse("nouveau");
        when(tokenRepository.findByToken("123456")).thenReturn(Optional.of(token(false)));

        assertThatThrownBy(() -> passwordResetService.reinitialiserMotDePasse(req))
                .isInstanceOf(BusinessException.class);
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    @DisplayName("vérification d'un code valide → aucune exception")
    void verifierCode_valide_ok() {
        when(tokenRepository.findByToken("123456")).thenReturn(Optional.of(token(true)));

        assertThatCode(() -> passwordResetService.verifierCode("123456")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("vérification d'un code expiré → BusinessException")
    void verifierCode_expire_leve() {
        when(tokenRepository.findByToken("123456")).thenReturn(Optional.of(token(false)));

        assertThatThrownBy(() -> passwordResetService.verifierCode("123456"))
                .isInstanceOf(BusinessException.class);
    }
}
