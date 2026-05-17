package com.crm.modules.catalogue.mapper;

import com.crm.modules.catalogue.dto.ProduitResponse;
import com.crm.modules.catalogue.entity.Produit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

/**
 * MapStruct mapper : {@link Produit} → {@link ProduitResponse}.
 *
 * <p>{@code prixTTC} est calculé via {@link #calculerPrixTTC(Produit)}.
 * {@code categorieId} et {@code categorieNom} sont mappés depuis la relation
 * {@code categorie}.
 * {@code stockMinimum} et {@code enAlerte} sont ignorés ici —
 * ils sont valorisés manuellement dans {@code ProduitService.toResponse()}.</p>
 *
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring")
public interface ProduitMapper {

    @Mapping(target = "categorieId",  source = "categorie.id")
    @Mapping(target = "categorieNom", source = "categorie.nom")
    @Mapping(target = "prixTTC",      expression = "java(calculerPrixTTC(produit))")
    @Mapping(target = "stockMinimum", ignore = true)
    @Mapping(target = "enAlerte",     ignore = true)
    ProduitResponse toResponse(Produit produit);

    /**
     * Calcule le prix TTC à partir du prix HT et du taux de TVA.
     * Retourne {@code null} si l'un des deux champs est absent.
     */
    default BigDecimal calculerPrixTTC(Produit produit) {
        if (produit.getPrixHT() == null || produit.getTauxTVA() == null) return null;
        return produit.getPrixHT().multiply(
                BigDecimal.ONE.add(
                        produit.getTauxTVA().divide(BigDecimal.valueOf(100))));
    }
}