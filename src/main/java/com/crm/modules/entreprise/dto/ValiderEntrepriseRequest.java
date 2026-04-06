package com.crm.modules.entreprise.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO pour la décision du SuperAdmin sur un compte entreprise.
 * Valider ou refuser un compte entreprise
 */
@Data
public class ValiderEntrepriseRequest {

    @NotNull(message = "La décision est obligatoire")
    private Boolean valider; // true = valider, false = refuser

    // Obligatoire uniquement si valider = false
    private String motifRefus;
}
