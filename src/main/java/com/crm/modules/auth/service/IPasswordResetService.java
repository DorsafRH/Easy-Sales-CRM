package com.crm.modules.auth.service;

import com.crm.modules.auth.dto.MotDePasseOublieRequest;
import com.crm.modules.auth.dto.ReinitialisationMotDePasseRequest;

/**
 * Contrat du service de réinitialisation de mot de passe.
 *
 * @author Riahi Dorsaf
 */
public interface IPasswordResetService {

    /**
     * Génère un token et envoie un email de réinitialisation.
     * Ne révèle pas si l'email existe ou non (sécurité).
     *
     * @param request l'email de l'utilisateur
     */
    void demanderReinitialisation(MotDePasseOublieRequest request);

    /**
     * Réinitialise le mot de passe avec le token fourni.
     *
     * @param request le token + le nouveau mot de passe
     * @throws com.crm.shared.exception.BusinessException si le token est invalide ou expiré
     */
    void reinitialiserMotDePasse(ReinitialisationMotDePasseRequest request);
    /**
     * Vérifie que le code à 6 chiffres est valide.
     * Appelé avant l'écran de nouveau mot de passe.
     *
     * @param token le code à 6 chiffres
     * @throws com.crm.shared.exception.BusinessException si le code est invalide ou expiré
     */
    void verifierCode(String token);

}