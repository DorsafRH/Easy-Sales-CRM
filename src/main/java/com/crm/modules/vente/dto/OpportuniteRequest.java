package com.crm.modules.vente.dto;

import com.crm.shared.enums.StatutOpportunite;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Riahi Dorsaf
 */
@Data
public class OpportuniteRequest {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String description;

    private BigDecimal montantEstime;

    private Integer probabilite;

    private StatutOpportunite statut;

    private LocalDate dateCloturePrevue;

    private String raisonPerte;

    @NotNull(message = "Le client est obligatoire")
    private Long clientId;

    /** Lead source optionnel */
    private Long leadId;
}