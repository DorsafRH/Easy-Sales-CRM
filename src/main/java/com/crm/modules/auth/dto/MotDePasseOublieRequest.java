package com.crm.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour la demande de réinitialisation de mot de passe.
 *
 * @author Riahi Dorsaf
 */
@Data
public class MotDePasseOublieRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;
}