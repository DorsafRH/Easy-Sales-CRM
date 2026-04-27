package com.crm.modules.catalogue.dto;

import com.crm.shared.enums.StatutProduit;
import com.crm.shared.enums.TypeProduit;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un produit.
 * prixTTC calculé via expression MapStruct.
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
    private StatutProduit statut;

    private Long   categorieId;
    private String categorieNom;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}