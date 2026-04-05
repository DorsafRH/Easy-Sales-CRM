package com.crm.modules.entreprise.controller;

import com.crm.modules.entreprise.dto.EntrepriseCompteResponse;
import com.crm.modules.entreprise.dto.ValiderEntrepriseRequest;
import com.crm.modules.entreprise.service.EntrepriseService;
import com.crm.shared.enums.StatutCompte;
import com.crm.shared.response.ApiResponse;
import com.crm.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur d'administration pour la gestion des comptes entreprises.
 *
 * <p>Réservé au ROLE_SUPER_ADMIN. Base path : {@code /api/admin/entreprises}</p>
 *
 * @author Riahi Dorsaf
 */
@Tag(
        name = "Administration — Entreprises",
        description = "Endpoints réservés au super-administrateur : validation, consultation et suppression"
)
@RestController
@RequestMapping("/admin/entreprises")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminEntrepriseController {

    private final EntrepriseService entrepriseService;

    /**
     * Liste les comptes en attente de validation.
     *
     * @param page numéro de page (défaut 0)
     * @param size taille de page (défaut 10)
     * @return la page des comptes EN_ATTENTE
     */
    @Operation(summary = "Lister les comptes en attente",
            description = "Retourne les comptes avec statut EN_ATTENTE, triés par date décroissante.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Liste récupérée",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @GetMapping("/en-attente")
    public ResponseEntity<ApiResponse<PageResponse<EntrepriseCompteResponse>>> listerEnAttente(
            @Parameter(description = "Numéro de page", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                entrepriseService.listerComptesEnAttente(page, size),
                "Comptes en attente récupérés avec succès."));
    }

    /**
     * Liste toutes les entreprises avec filtres optionnels.
     *
     * @param statut  filtre par statut (optionnel)
     * @param keyword recherche par nom ou matricule (optionnel)
     * @param page    numéro de page
     * @param size    taille de page
     * @return la page filtrée
     */
    @Operation(summary = "Lister toutes les entreprises (avec filtres)",
            description = "Filtrage dynamique par statut et/ou mot-clé via JPA Specification.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Liste récupérée",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EntrepriseCompteResponse>>> listerTous(
            @Parameter(description = "Statut (ACTIVE, EN_ATTENTE, REFUSE, SUSPENDU)")
            @RequestParam(required = false) StatutCompte statut,
            @Parameter(description = "Nom ou matricule fiscale", example = "Tech")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Numéro de page", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                entrepriseService.listerEntreprises(statut, keyword, page, size),
                "Entreprises récupérées avec succès."));
    }

    /**
     * Retourne les détails d'une entreprise.
     *
     * @param id l'identifiant du compte
     * @return les informations détaillées
     */
    @Operation(summary = "Détails d'une entreprise",
            description = "Retourne les informations complètes du compte et de son propriétaire.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Détails récupérés",
                    content = @Content(schema = @Schema(implementation = EntrepriseCompteResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Introuvable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EntrepriseCompteResponse>> consulterDetails(
            @Parameter(description = "ID du compte entreprise", required = true, example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                entrepriseService.consulterDetails(id),
                "Détails récupérés avec succès."));
    }

    /**
     * Valide ou refuse un compte en attente.
     *
     * @param id          l'identifiant du compte
     * @param request     décision + motif de refus éventuel
     * @param userDetails l'admin connecté
     * @return le compte mis à jour
     */
    @Operation(summary = "Valider ou refuser un compte",
            description = "valider=true → ACTIVE, valider=false → REFUSE. Motif obligatoire en cas de refus.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Décision enregistrée",
                    content = @Content(schema = @Schema(implementation = EntrepriseCompteResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides ou compte déjà traité"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Introuvable")
    })
    @PostMapping("/{id}/decision")
    public ResponseEntity<ApiResponse<EntrepriseCompteResponse>> traiterDemande(
            @Parameter(description = "ID du compte entreprise", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ValiderEntrepriseRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        EntrepriseCompteResponse response =
                entrepriseService.traiterDemande(id, request, userDetails.getUsername());
        String message = Boolean.TRUE.equals(request.getValider())
                ? "Compte validé. Email de confirmation envoyé."
                : "Compte refusé. Email de notification envoyé.";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * Supprime logiquement un compte entreprise.
     *
     * @param id          l'identifiant du compte
     * @param userDetails l'admin connecté
     * @return confirmation de suppression
     */
    @Operation(summary = "Suppression logique d'une entreprise",
            description = "Soft delete : isDeleted=true, l'entreprise disparaît des listings.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Supprimé avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Introuvable")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimerLogiquement(
            @Parameter(description = "ID du compte entreprise", required = true, example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        entrepriseService.supprimerLogiquement(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Entreprise supprimée avec succès."));
    }
}
