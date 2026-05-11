package com.crm.modules.vente.entity;

import com.crm.modules.catalogue.entity.Produit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ligne d'un devis : produit × quantité × remise.
 *
 * @author Riahi Dorsaf
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lignes_devis")
public class LigneDevis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devis_id", nullable = false)
    private Devis devis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    /** Désignation libre (peut différer du nom produit) */
    @Column(name = "designation", nullable = false, length = 200)
    private String designation;

    @Column(name = "quantite", nullable = false)
    @Builder.Default
    private Integer quantite = 1;

    @Column(name = "prix_unitaire_ht", nullable = false, precision = 12, scale = 3)
    private BigDecimal prixUnitaireHt;

    @Column(name = "taux_tva", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tauxTva = BigDecimal.ZERO;

    /** Remise en % (0-100) */
    @Column(name = "remise", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal remise = BigDecimal.ZERO;

    @Column(name = "montant_ht", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal montantHt = BigDecimal.ZERO;

    @Column(name = "montant_tva", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal montantTva = BigDecimal.ZERO;

    @Column(name = "montant_ttc", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal montantTtc = BigDecimal.ZERO;

    // ── Helper ────────────────────────────────────────────────
    public void calculer() {
        // montantHt = prixUnitaireHt × quantite × (1 - remise/100)
        BigDecimal coeff = BigDecimal.ONE.subtract(
                remise.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        );
        this.montantHt = prixUnitaireHt
                .multiply(BigDecimal.valueOf(quantite))
                .multiply(coeff)
                .setScale(3, RoundingMode.HALF_UP);

        this.montantTva = montantHt
                .multiply(tauxTva.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                .setScale(3, RoundingMode.HALF_UP);

        this.montantTtc = montantHt.add(montantTva)
                .setScale(3, RoundingMode.HALF_UP);
    }
}