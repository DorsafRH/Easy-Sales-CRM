package com.crm.modules.catalogue.entity;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutProduit;
import com.crm.shared.enums.TypeProduit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Produit ou service du catalogue CRM.
 * Code auto-généré côté service : PRD-YYYY-NNNN.
 *
 * @author Riahi Dorsaf
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "produits")
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_produit", unique = true)
    private String codeProduit;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypeProduit type;

    @Column(name = "prix_ht", nullable = false, precision = 12, scale = 3)
    private BigDecimal prixHT;

    /** Taux de TVA en %. Null si exonéré. */
    @Column(name = "taux_tva", precision = 5, scale = 2)
    private BigDecimal tauxTVA;

    @Column(name = "unite", length = 50)
    private String unite;

    /** Null pour les services. Valorisé uniquement pour STOCKABLE. */
    @Column(name = "stock_disponible")
    private Integer stockDisponible;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutProduit statut = StatutProduit.ACTIF;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private ProprietaireEntreprise proprietaire;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}