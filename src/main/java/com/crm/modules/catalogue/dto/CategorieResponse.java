package com.crm.modules.catalogue.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour une catégorie.
 * nbProduits valorisé par le service après mapping MapStruct.
 *
 * @author Riahi Dorsaf
 */
@Data
public class CategorieResponse {

    private Long id;
    private String nom;
    private String description;
    private LocalDateTime dateCreation;

    private int nbProduits;
}