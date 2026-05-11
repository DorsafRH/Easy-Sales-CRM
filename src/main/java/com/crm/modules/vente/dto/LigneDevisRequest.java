package com.crm.modules.vente.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Riahi Dorsaf
 */
@Data
public class LigneDevisRequest {

    @NotNull(message = "Le produit est obligatoire")
    private Long produitId;

    /** Désignation libre — pré-remplie depuis le nom produit */
    private String designation;

    @NotNull
    @Min(value = 1, message = "La quantité minimale est 1")
    private Integer quantite;

    /** Si null, utilise le prix HT du produit catalogue */
    private BigDecimal prixUnitaireHt;

    /** Taux TVA en % — si null, utilise celui du produit */
    private BigDecimal tauxTva;

    /** Remise en % (0-100) — défaut 0 */
    private BigDecimal remise;
}