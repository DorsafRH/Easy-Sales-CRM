package com.crm.modules.marketing.entity;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Engagement public d'une publication (réactions, commentaires, partages) collecté
 * depuis la plateforme ou injecté pour la démonstration. Sert le dashboard marketing.
 * Le détail des réactions par emoji est stocké en JSON dans {@code reactionsBreakdown}.
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
        name = "reactions_publication",
        indexes = {
                @Index(name = "idx_reaction_proprietaire", columnList = "proprietaire_id"),
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reaction_proprietaire_post",
                        columnNames = {"proprietaire_id", "post_id"}),
        }
)
public class ReactionPublication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant du post sur la plateforme. */
    @Column(name = "post_id", nullable = false, length = 200)
    private String postId;

    /** Libellé lisible de la publication (nom/sujet), affiché sur le dashboard. */
    @Column(name = "titre", length = 200)
    private String titre;

    @Column(name = "likes")
    @Builder.Default
    private Integer likes = 0;

    @Column(name = "comments")
    @Builder.Default
    private Integer comments = 0;

    @Column(name = "shares")
    @Builder.Default
    private Integer shares = 0;

    /** Détail des réactions par type (JSON) : like, love, wow, sad, angry, haha. */
    @Column(name = "reactions_breakdown", columnDefinition = "TEXT")
    private String reactionsBreakdown;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private ProprietaireEntreprise proprietaire;
}
