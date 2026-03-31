package com.crm.modules.auth.service;

import com.crm.config.jwt.JwtService;
import com.crm.modules.auth.dto.AuthResponse;
import com.crm.modules.auth.dto.LoginRequest;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.entity.SuperAdmin;
import com.crm.modules.utilisateur.entity.Utilisateur;
import com.crm.modules.utilisateur.repository.UtilisateurRepository;
import com.crm.shared.enums.StatutCompte;
import com.crm.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service d'authentification.
 * Gère la connexion pour ProprietaireEntreprise (mobile) et SuperAdmin (Angular).
 * DEV-XX : Connexion ProprietaireEntreprise
 * DEV-XX : Connexion SuperAdmin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Authentification Spring Security
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getMotDePasse()
                    )
            );
        } catch (AuthenticationException e) {
            throw new BusinessException("Email ou mot de passe incorrect.");
        }

        // 2. Chargement de l'utilisateur
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));

        // 3. Vérifications spécifiques au rôle
        if (utilisateur instanceof ProprietaireEntreprise proprietaire) {
            verifierAccesProprietaire(proprietaire);
        }

        // 4. Mise à jour de la dernière connexion
        utilisateur.setDerniereConnexion(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        // 5. Génération des tokens JWT
        String accessToken  = jwtService.generateToken(utilisateur);
        String refreshToken = jwtService.generateRefreshToken(utilisateur);

        // 6. Construction de la réponse
        return buildAuthResponse(utilisateur, accessToken, refreshToken);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void verifierAccesProprietaire(ProprietaireEntreprise proprietaire) {
        if (proprietaire.getEntrepriseCompte() == null) {
            throw new BusinessException("Aucun compte entreprise associé à cet utilisateur.");
        }
        StatutCompte statut = proprietaire.getEntrepriseCompte().getStatutCompte();
        switch (statut) {
            case EN_ATTENTE ->
                throw new BusinessException(
                    "Votre compte est en attente de validation par un administrateur.");
            case REFUSE ->
                throw new BusinessException(
                    "Votre compte a été refusé. Motif : " +
                    proprietaire.getEntrepriseCompte().getMotifRefus());
            case SUSPENDU ->
                throw new BusinessException("Votre compte a été suspendu. Veuillez contacter le support.");
            case ACTIVE -> { /* OK */ }
        }
    }

    private AuthResponse buildAuthResponse(Utilisateur utilisateur, String accessToken, String refreshToken) {
        AuthResponse.AuthResponseBuilder builder = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(utilisateur.getId())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole());

        // Enrichissement pour le ProprietaireEntreprise
        if (utilisateur instanceof ProprietaireEntreprise proprietaire
                && proprietaire.getEntrepriseCompte() != null) {
            builder
                .entrepriseId(proprietaire.getEntrepriseCompte().getId())
                .nomEntreprise(proprietaire.getEntrepriseCompte().getNomEntreprise())
                .statutCompte(proprietaire.getEntrepriseCompte().getStatutCompte().name());
        }

        return builder.build();
    }
}
