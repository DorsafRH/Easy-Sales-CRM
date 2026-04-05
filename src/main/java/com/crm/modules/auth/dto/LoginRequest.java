package com.crm.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de connexion partagé par tous les rôles de l'application.
 *
 * <p>Utilisé aussi bien par le propriétaire d'entreprise (application mobile)
 * que par le super-administrateur (backoffice Angular).
 * Le rôle est déterminé côté serveur depuis la base de données.</p>
 *
 * @author Riahi Dorsaf
 */
@Data
public class LoginRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String motDePasse;
}
