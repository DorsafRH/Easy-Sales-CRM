package com.crm.modules.auth.service;

import com.crm.modules.auth.dto.MotDePasseOublieRequest;
import com.crm.modules.auth.dto.ReinitialisationMotDePasseRequest;
import com.crm.modules.auth.entity.PasswordResetToken;
import com.crm.modules.auth.repository.PasswordResetTokenRepository;
import com.crm.modules.utilisateur.entity.Utilisateur;
import com.crm.modules.utilisateur.repository.UtilisateurRepository;
import com.crm.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service de réinitialisation de mot de passe.
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService implements IPasswordResetService {

    private static final int EXPIRATION_MINUTES = 15;

    private final UtilisateurRepository           utilisateurRepository;
    private final PasswordResetTokenRepository    tokenRepository;
    private final EmailService                    emailService;
    private final PasswordEncoder                 passwordEncoder;

    @Transactional
    public void demanderReinitialisation(MotDePasseOublieRequest request) {
        Optional<Utilisateur> optUtilisateur =
                utilisateurRepository.findByEmail(request.getEmail());

        if (optUtilisateur.isEmpty()) {
            // Ne pas révéler l'existence du compte (anti-enumération)
            log.info("[PASSWORD_RESET] Demande pour email inconnu : {}", request.getEmail());
            return;
        }

        Utilisateur utilisateur = optUtilisateur.get();

        // Invalider les anciens tokens
        tokenRepository.invaliderTokensExistants(utilisateur.getId());

        // Créer un nouveau token
        String tokenValeur = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(tokenValeur)
                .utilisateur(utilisateur)
                .dateExpiration(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                .utilise(false)
                .build();

        tokenRepository.save(resetToken);

        log.info("[PASSWORD_RESET] Token généré pour : {}", utilisateur.getEmail());

        emailService.envoyerEmailReinitialisationMotDePasse(
                utilisateur.getEmail(),
                utilisateur.getPrenom(),
                tokenValeur
        );
    }

    @Transactional
    public void reinitialiserMotDePasse(ReinitialisationMotDePasseRequest request) {
        PasswordResetToken resetToken = tokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new BusinessException("Token de réinitialisation invalide."));

        if (!resetToken.estValide()) {
            log.warn("[PASSWORD_RESET] Token invalide ou expiré utilisé");
            throw new BusinessException(
                    "Ce lien de réinitialisation est expiré ou déjà utilisé.");
        }

        Utilisateur utilisateur = resetToken.getUtilisateur();
        utilisateur.setMotDePasseHash(
                passwordEncoder.encode(request.getNouveauMotDePasse()));

        utilisateurRepository.save(utilisateur);

        resetToken.setUtilise(true);
        tokenRepository.save(resetToken);

        log.info("[PASSWORD_RESET] Mot de passe réinitialisé pour : {}",
                utilisateur.getEmail());
    }
}