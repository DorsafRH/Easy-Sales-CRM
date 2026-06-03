package com.crm.modules.marketing.entity;

import com.crm.shared.enums.StatutDiffusion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Diffusion d'une publication sur un compte social donné.
 * Une publication possède une diffusion par réseau ciblé.
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
        name = "diffusions_publication",
        indexes = {
                @Index(name = "idx_diffusion_publication", columnList = "publication_id"),
                @Index(name = "idx_diffusion_compte", columnList = "compte_social_id"),
        }
)
public class DiffusionPublication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_diffusion", nullable = false, length = 30)
    private StatutDiffusion statutDiffusion;

    @Column(name = "date_diffusion")
    private LocalDateTime dateDiffusion;

    @Column(name = "message_erreur", columnDefinition = "TEXT")
    private String messageErreur;

    @Column(name = "id_publication_ext", length = 200)
    private String idPublicationExt;

    @Column(name = "url_publication", length = 500)
    private String urlPublication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id", nullable = false)
    private PublicationMarketing publication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_social_id", nullable = false)
    private CompteSocialConnecte compteSocial;
}
