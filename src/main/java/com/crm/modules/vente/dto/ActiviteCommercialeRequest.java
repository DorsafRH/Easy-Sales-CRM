package com.crm.modules.vente.dto;

import com.crm.shared.enums.ResultatActivite;
import com.crm.shared.enums.TypeActiviteCommerciale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Riahi Dorsaf
 */
@Data
public class ActiviteCommercialeRequest {

    @NotNull(message = "Le type est obligatoire")
    private TypeActiviteCommerciale type;

    @NotBlank(message = "Le sujet est obligatoire")
    private String sujet;

    private String notes;
    private ResultatActivite resultat;
    private Integer dureeMinutes;
    private LocalDateTime dateActivite;

    /** L'activité porte sur un Lead OU une Opportunité */
    private Long leadId;
    private Long opportuniteId;
}