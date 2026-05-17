package com.crm.modules.auth.service;

import com.crm.modules.auth.dto.AuthResponse;
import com.crm.modules.auth.dto.LoginRequest;

/**
 * Contrat du service d'authentification.
 *
 * <p>Définit l'opération de connexion partagée par tous les rôles de l'application
 * (propriétaire d'entreprise et super-administrateur).</p>
 *
 * @author Riahi Dorsaf
 * @see AuthService
 */
public interface IAuthService {

    /**
     * Authentifie un utilisateur et retourne les tokens JWT ainsi que ses informations.
     *
     * @param request les credentials de connexion (e-mail et mot de passe)
     * @return la réponse contenant les tokens JWT et les données de l'utilisateur
     * @throws com.crm.shared.exception.BusinessException si les credentials sont invalides
     *                                                    ou si le compte n'est pas autorisé à se connecter
     */
    AuthResponse login(LoginRequest request);
}
