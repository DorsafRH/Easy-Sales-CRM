package com.crm.modules.agenda.controller;

import com.crm.modules.agenda.dto.ReunionRequest;
import com.crm.modules.agenda.dto.ReunionResponse;
import com.crm.modules.agenda.service.IReunionService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller — Agenda / Réunions client.
 * Base path : {@code /api/reunions}
 *
 * <p>Toutes les opérations sont restreintes au rôle
 * {@code ROLE_PROPRIETAIRE} et cloisonnées par {@code proprietaireId}
 * extrait automatiquement du token JWT via
 * {@code @AuthenticationPrincipal ProprietaireEntreprise}.</p>
 *
 * @author Riahi Dorsaf
 */
@Tag(
        name        = "Agenda",
        description = "Gestion des réunions client — planification, modification et suivi"
)
@RestController
@RequestMapping("/reunions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class ReunionController {

    private final IReunionService reunionService;

    // ─────────────────────────────────────────────────────────────────────────
    //  LISTE
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Lister toutes les réunions",
            description = "Retourne toutes les réunions du propriétaire connecté, "
                    + "triées par date croissante."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReunionResponse>>> lister(
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                reunionService.lister(proprietaire.getId()),
                "Réunions récupérées."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LISTE PAR SEMAINE
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Lister les réunions d'une semaine",
            description = "Retourne les réunions entre deux dates. "
                    + "Utilisé pour l'écran Agenda hebdomadaire."
    )
    @GetMapping("/semaine")
    public ResponseEntity<ApiResponse<List<ReunionResponse>>> listerSemaine(
            @Parameter(description = "Date de début (format : YYYY-MM-DD)", example = "2026-05-04")
            @RequestParam String debut,
            @Parameter(description = "Date de fin (format : YYYY-MM-DD)",   example = "2026-05-10")
            @RequestParam String fin,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                reunionService.listerSemaine(proprietaire.getId(), debut, fin),
                "Réunions de la semaine récupérées."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LISTE PAR CLIENT
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Lister les réunions d'un client",
            description = "Retourne toutes les réunions associées à un client spécifique. "
                    + "Utilisé pour l'onglet Réunions de la fiche client."
    )
    @GetMapping("/client/{clientId}")
    public ResponseEntity<ApiResponse<List<ReunionResponse>>> listerParClient(
            @Parameter(description = "Identifiant du client", required = true)
            @PathVariable Long clientId,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                reunionService.listerParClient(clientId, proprietaire.getId()),
                "Réunions du client récupérées."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DÉTAIL
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Obtenir le détail d'une réunion",
            description = "Retourne le détail complet d'une réunion par son identifiant."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReunionResponse>> obtenir(
            @Parameter(description = "Identifiant de la réunion", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                reunionService.obtenir(id, proprietaire.getId()),
                "Réunion récupérée."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CRÉATION
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Créer une réunion",
            description = "Planifie une nouvelle réunion avec un client. "
                    + "Le contact est optionnel. "
                    + "Les rappelsMinutes définissent les délais de notification "
                    + "(ex: [30, 1440] = 30 min avant + 1 jour avant)."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ReunionResponse>> creer(
            @Valid @RequestBody ReunionRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        reunionService.creer(request, proprietaire.getId()),
                        "Réunion planifiée avec succès."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MODIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Modifier une réunion",
            description = "Modifie une réunion existante. "
                    + "Seules les réunions au statut PLANIFIEE peuvent être modifiées."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReunionResponse>> modifier(
            @Parameter(description = "Identifiant de la réunion", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ReunionRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                reunionService.modifier(id, request, proprietaire.getId()),
                "Réunion mise à jour."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TERMINER
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Marquer une réunion comme terminée",
            description = "Passe le statut de la réunion à TERMINEE. "
                    + "Impossible si déjà TERMINEE ou ANNULEE."
    )
    @PatchMapping("/{id}/terminer")
    public ResponseEntity<ApiResponse<Void>> terminer(
            @Parameter(description = "Identifiant de la réunion", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        reunionService.terminer(id, proprietaire.getId());
        return ResponseEntity.ok(
                ApiResponse.success(null, "Réunion marquée comme terminée."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ANNULER
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Annuler une réunion",
            description = "Passe le statut de la réunion à ANNULEE. "
                    + "Impossible si déjà ANNULEE ou TERMINEE."
    )
    @PatchMapping("/{id}/annuler")
    public ResponseEntity<ApiResponse<Void>> annuler(
            @Parameter(description = "Identifiant de la réunion", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        reunionService.annuler(id, proprietaire.getId());
        return ResponseEntity.ok(
                ApiResponse.success(null, "Réunion annulée."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUPPRESSION
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Supprimer une réunion",
            description = "Supprime définitivement une réunion (suppression physique). "
                    + "Cette action est irréversible."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(
            @Parameter(description = "Identifiant de la réunion", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        reunionService.supprimer(id, proprietaire.getId());
        return ResponseEntity.ok(
                ApiResponse.success(null, "Réunion supprimée."));
    }
}