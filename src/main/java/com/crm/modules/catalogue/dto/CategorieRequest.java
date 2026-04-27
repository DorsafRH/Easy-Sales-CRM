package com.crm.modules.catalogue.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO création / modification d'une catégorie.
 *
 * @author Riahi Dorsaf
 */
@Data
public class CategorieRequest {

    @NotBlank(message = "Le nom de la catégorie est obligatoire")
    private String nom;

    private String description;
}