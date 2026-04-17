package com.crm.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO pour la réinitialisation du mot de passe avec le token reçu par email.
 *
 * @author Riahi Dorsaf
 */
@Data
public class ReinitialisationMotDePasseRequest {

    @NotBlank(message = "Le token est obligatoire")
    private String token;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String nouveauMotDePasse;
}