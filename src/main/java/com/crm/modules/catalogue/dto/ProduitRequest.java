package com.crm.modules.catalogue.dto;

import com.crm.shared.enums.StatutProduit;
import com.crm.shared.enums.TypeProduit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO création / modification d'un produit.
 * stockMinimum : seuil d'alerte, applicable aux produits STOCKABLE uniquement.
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

    /** Quantité physique en stock. Null pour SERVICE. */
    @Min(value = 0, message = "Le stock ne peut pas être négatif")
    private Integer stockDisponible;

    /**
     * Seuil d'alerte en dessous duquel une alerte est déclenchée.
     * Null pour SERVICE.
     */
    @Min(value = 0, message = "Le stock minimum ne peut pas être négatif")
    private Integer stockMinimum;

    private Long categorieId;

    /** Défaut ACTIF si null à la création. */
    private StatutProduit statut;
}