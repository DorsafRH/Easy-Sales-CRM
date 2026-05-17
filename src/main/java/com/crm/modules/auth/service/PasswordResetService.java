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

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void demanderReinitialisation(MotDePasseOublieRequest request) {
        Optional<Utilisateur> optUtilisateur =
                utilisateurRepository.findByEmail(request.getEmail());

        // ← CHANGEMENT : on révèle si l'email existe
        if (optUtilisateur.isEmpty()) {
            log.info("[PASSWORD_RESET] Demande pour email inconnu : {}", request.getEmail());
            throw new BusinessException(
                    "Aucun compte n'est associé à cette adresse email.");
        }

        Utilisateur utilisateur = optUtilisateur.get();

        // Invalider les anciens tokens
        tokenRepository.invaliderTokensExistants(utilisateur.getId());

        // ← CHANGEMENT : code à 6 chiffres au lieu d'UUID
        String code = String.format("%06d", new java.util.Random().nextInt(999999));

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(code)
                .utilisateur(utilisateur)
                .dateExpiration(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                .utilise(false)
                .build();

        tokenRepository.save(resetToken);

        log.info("[PASSWORD_RESET] Code généré pour : {}", utilisateur.getEmail());

        emailService.envoyerEmailReinitialisationMotDePasse(
                utilisateur.getEmail(),
                utilisateur.getPrenom(),
                code
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

    @Transactional(readOnly = true)
    public void verifierCode(String token) {
        PasswordResetToken resetToken = tokenRepository
                .findByToken(token)
                .orElseThrow(() -> new BusinessException(
                        "Code invalide."));

        if (!resetToken.estValide()) {
            throw new BusinessException(
                    "Ce code est expiré ou déjà utilisé.");
        }

        log.info("[PASSWORD_RESET] Code vérifié avec succès");
    }
}