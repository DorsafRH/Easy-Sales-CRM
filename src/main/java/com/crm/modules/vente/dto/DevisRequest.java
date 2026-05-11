package com.crm.modules.vente.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Data
public class DevisRequest {

    @NotNull(message = "Le client est obligatoire")
    private Long clientId;

    private Long opportuniteId;

    private String notes;

    private Integer validiteJours;

    @Valid
    @NotEmpty(message = "Le devis doit contenir au moins une ligne")
    private List<LigneDevisRequest> lignes;
}