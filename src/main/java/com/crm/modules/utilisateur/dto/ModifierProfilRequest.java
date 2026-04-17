package com.crm.modules.utilisateur.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour la modification des données personnelles du propriétaire.
 *
 * @author Riahi Dorsaf
 */
@Data
public class ModifierProfilRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le téléphone est obligatoire")
    private String telephone;
}