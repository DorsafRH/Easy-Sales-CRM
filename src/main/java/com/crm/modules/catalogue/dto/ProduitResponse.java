package com.crm.modules.catalogue.dto;

import com.crm.shared.enums.StatutProduit;
import com.crm.shared.enums.TypeProduit;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un produit.
 * prixTTC calculé via expression MapStruct.
 * enAlerte : true si stockDisponible <= stockMinimum (calculé côté service).
 *
 * @author Riahi Dorsaf
 */
@Data
public class ProduitResponse {

    private Long          id;
    private String        codeProduit;
    private String        nom;
    private String        description;
    private TypeProduit   type;
    private BigDecimal    prixHT;
    private BigDecimal    tauxTVA;
    private BigDecimal    prixTTC;
    private String        unite;
    private Integer       stockDisponible;

    /**
     * Seuil d'alerte stock.
     * Null pour les services.
     */
    private Integer       stockMinimum;

    /**
     * true si le produit STOCKABLE est en alerte de stock
     * (stockDisponible <= stockMinimum ou stockDisponible == 0).
     * Calculé côté service, pas mappé par MapStruct.
     */
    private boolean       enAlerte;

    private StatutProduit statut;
    private Long          categorieId;
    private String        categorieNom;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}