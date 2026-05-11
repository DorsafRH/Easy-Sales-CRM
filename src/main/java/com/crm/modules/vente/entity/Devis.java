package com.crm.modules.vente.entity;

import com.crm.modules.client.entity.Client;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutDevis;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Devis commercial — composé de 1..* LigneDevis.
 * Numéro auto-généré : DV-YYYY-NNNN
 *
 * @author Riahi Dorsaf
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "devis",
        indexes = {
                @Index(name = "idx_devis_proprietaire", columnList = "proprietaire_id"),
                @Index(name = "idx_devis_client",       columnList = "client_id"),
                @Index(name = "idx_devis_statut",       columnList = "statut"),
        }
)
public class Devis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 20)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutDevis statut = StatutDevis.BROUILLON;

    @Column(name = "montant_ht", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal montantHt = BigDecimal.ZERO;

    @Column(name = "montant_tva", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal montantTva = BigDecimal.ZERO;

    @Column(name = "montant_ttc", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal montantTtc = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "validite_jours")
    @Builder.Default
    private Integer validiteJours = 30;

    // ── Lignes ────────────────────────────────────────────────
    @OneToMany(
            mappedBy    = "devis",
            cascade     = CascadeType.ALL,
            orphanRemoval = true,
            fetch       = FetchType.LAZY
    )
    @Builder.Default
    private List<LigneDevis> lignes = new ArrayList<>();

    // ── Relations ─────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunite_id")
    private Opportunite opportunite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private ProprietaireEntreprise proprietaire;

    // ── Dates ─────────────────────────────────────────────────
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() { this.dateCreation = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { this.dateModification = LocalDateTime.now(); }

    // ── Helper ────────────────────────────────────────────────
    public void recalculerTotaux() {
        BigDecimal ht  = BigDecimal.ZERO;
        BigDecimal tva = BigDecimal.ZERO;
        for (LigneDevis l : lignes) {
            ht  = ht.add(l.getMontantHt());
            tva = tva.add(l.getMontantTva());
        }
        this.montantHt  = ht;
        this.montantTva = tva;
        this.montantTtc = ht.add(tva);
    }
}