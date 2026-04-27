package com.crm.modules.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO création / modification d'un contact.
 *
 * @author Riahi Dorsaf
 */
@Data
public class ContactRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String prenom;

    @Email(message = "Format d'email invalide")
    private String email;

    private String telephone;
    private String poste;
    private Boolean isPrincipal;
}