package com.crm.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO pour la vérification du code de réinitialisation.
 *
 * @author Riahi Dorsaf
 */
@Data
public class VerifierCodeRequest {

    @NotBlank(message = "Le code est obligatoire")
    @Pattern(regexp = "\\d{6}", message = "Le code doit contenir 6 chiffres")
    private String token;
}