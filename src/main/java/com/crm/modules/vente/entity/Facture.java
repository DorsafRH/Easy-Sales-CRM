package com.crm.modules.vente.entity;

import com.crm.modules.client.entity.Client;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutFacture;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Facture générée depuis un Devis accepté.
 * Numéro auto-généré : FA-YYYY-NNNN
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
        name = "factures",
        indexes = {
                @Index(name = "idx_facture_proprietaire", columnList = "proprietaire_id"),
                @Index(name = "idx_facture_client",       columnList = "client_id"),
        }
)
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 20)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutFacture statut = StatutFacture.BROUILLON;

    @Column(name = "montant_ht",  precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal montantHt  = BigDecimal.ZERO;

    @Column(name = "montant_tva", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal montantTva = BigDecimal.ZERO;

    @Column(name = "montant_ttc", precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal montantTtc = BigDecimal.ZERO;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ── Lignes ────────────────────────────────────────────────
    @OneToMany(
            mappedBy    = "facture",
            cascade     = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<LigneFacture> lignes = new ArrayList<>();

    // ── Relations ─────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devis_id")
    private Devis devisOrigine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private ProprietaireEntreprise proprietaire;

    // ── Dates ─────────────────────────────────────────────────
    @Column(name = "date_emission")
    private LocalDateTime dateEmission;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() { this.dateCreation = LocalDateTime.now(); }
}