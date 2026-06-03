package com.crm.modules.marketing.entity;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutPublication;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Publication marketing créée par un propriétaire.
 * Peut être générée par IA, programmée puis diffusée sur plusieurs réseaux.
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
        name = "publications_marketing",
        indexes = {
                @Index(name = "idx_publication_proprietaire", columnList = "proprietaire_id"),
                @Index(name = "idx_publication_statut", columnList = "statut"),
        }
)
public class PublicationMarketing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Column(name = "texte", columnDefinition = "TEXT")
    private String texte;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    @Builder.Default
    private StatutPublication statut = StatutPublication.BROUILLON;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_programmation")
    private LocalDateTime dateProgrammation;

    @Column(name = "date_publication")
    private LocalDateTime datePublication;

    @Column(name = "contenu_genere_ia", columnDefinition = "TEXT")
    private String contenuGenereIa;

    @Column(name = "message_erreur", columnDefinition = "TEXT")
    private String messageErreur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private ProprietaireEntreprise proprietaire;

    @OneToMany(
            mappedBy = "publication",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<DiffusionPublication> diffusions = new ArrayList<>();
}
