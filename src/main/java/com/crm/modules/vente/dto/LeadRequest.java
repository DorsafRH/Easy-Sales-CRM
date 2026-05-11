package com.crm.modules.vente.dto;

import com.crm.shared.enums.SourceLead;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author Riahi Dorsaf
 */
@Data
public class LeadRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @Email(message = "Format d'email invalide")
    private String email;

    private String telephone;
    private String entreprise;
    private String poste;

    @NotNull(message = "La source est obligatoire")
    private SourceLead source;

    private String descriptionBesoin;

    /** Lier à un client CRM existant (optionnel) */
    private Long clientId;
}