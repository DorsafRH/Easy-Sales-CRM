package com.crm.modules.catalogue.dto;

import com.crm.shared.enums.StatutProduit;
import com.crm.shared.enums.TypeProduit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO création / modification d'un produit.
 *
 * @author Riahi Dorsaf
 */
@Data
public class ProduitRequest {

    @NotBlank(message = "Le nom du produit est obligatoire")
    private String nom;

    private String description;

    @NotNull(message = "Le type est obligatoire (SERVICE ou STOCKABLE)")
    private TypeProduit type;

    @NotNull(message = "Le prix HT est obligatoire")
    @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif")
    private BigDecimal prixHT;

    private BigDecimal tauxTVA;
    private String     unite;
    private Integer    stockDisponible;
    private Long       categorieId;

    /** Défaut ACTIF si null à la création. */
    private StatutProduit statut;
}