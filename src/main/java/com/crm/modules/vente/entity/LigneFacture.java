package com.crm.modules.vente.entity;

import com.crm.modules.catalogue.entity.Produit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Ligne d'une facture — copie fidèle de LigneDevis au moment de la conversion.
 *
 * @author Riahi Dorsaf
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lignes_facture")
public class LigneFacture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @Column(name = "designation", nullable = false, length = 200)
    private String designation;

    @Column(name = "quantite", nullable = false)
    private Integer quantite;

    @Column(name = "prix_unitaire_ht", nullable = false, precision = 12, scale = 3)
    private BigDecimal prixUnitaireHt;

    @Column(name = "taux_tva", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tauxTva = BigDecimal.ZERO;

    @Column(name = "remise", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal remise = BigDecimal.ZERO;

    @Column(name = "montant_ht", precision = 12, scale = 3)
    private BigDecimal montantHt;

    @Column(name = "montant_tva", precision = 12, scale = 3)
    private BigDecimal montantTva;

    @Column(name = "montant_ttc", precision = 12, scale = 3)
    private BigDecimal montantTtc;
}