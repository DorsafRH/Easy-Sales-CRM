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
 *
 * @author Riahi Dorsaf
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
        log.info("[AUTH] Tentative de connexion pour : {}", request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getMotDePasse()
                    )
            );
        } catch (AuthenticationException e) {
            log.warn("[AUTH] Échec de connexion pour : {} — identifiants incorrects", request.getEmail());
            throw new BusinessException("Email ou mot de passe incorrect.");
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));

        if (utilisateur instanceof ProprietaireEntreprise proprietaire) {
            verifierAccesProprietaire(proprietaire);
        }

        utilisateur.setDerniereConnexion(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        String accessToken  = jwtService.generateToken(utilisateur);
        String refreshToken = jwtService.generateRefreshToken(utilisateur);

        log.info("[AUTH] Connexion réussie — utilisateur : {} | rôle : {}",
                utilisateur.getEmail(), utilisateur.getRole());

        return buildAuthResponse(utilisateur, accessToken, refreshToken);
    }

    private void verifierAccesProprietaire(ProprietaireEntreprise proprietaire) {
        if (proprietaire.getEntrepriseCompte() == null) {
            log.warn("[AUTH] Propriétaire {} sans compte entreprise associé", proprietaire.getEmail());
            throw new BusinessException("Aucun compte entreprise associé à cet utilisateur.");
        }
        StatutCompte statut = proprietaire.getEntrepriseCompte().getStatutCompte();
        switch (statut) {
            case EN_ATTENTE -> {
                log.info("[AUTH] Connexion refusée — compte en attente : {}", proprietaire.getEmail());
                throw new BusinessException(
                        "Votre compte est en attente de validation par un administrateur.");
            }
            case REFUSE -> {
                log.info("[AUTH] Connexion refusée — compte refusé : {}", proprietaire.getEmail());
                throw new BusinessException(
                        "Votre compte a été refusé. Motif : " +
                                proprietaire.getEntrepriseCompte().getMotifRefus());
            }
            case SUSPENDU -> {
                log.warn("[AUTH] Connexion refusée — compte suspendu : {}", proprietaire.getEmail());
                throw new BusinessException("Votre compte a été suspendu. Veuillez contacter le support.");
            }
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