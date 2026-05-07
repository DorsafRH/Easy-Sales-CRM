package com.crm.modules.catalogue.controller;

import com.crm.modules.catalogue.dto.ProduitRequest;
import com.crm.modules.catalogue.dto.ProduitResponse;
import com.crm.modules.catalogue.service.IProduitService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutProduit;
import com.crm.shared.enums.TypeProduit;
import com.crm.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
 * Contrôleur REST pour la gestion des produits du catalogue.
 * Base path : {@code /api/catalogue/produits}
 *
 * <p><b>Bonne pratique REST :</b> chaque changement de statut est un endpoint
 * PATCH dédié avec une responsabilité unique. Pas de paramètre {@code ?statut=}
 * sur un endpoint générique.</p>
 *
 * @author Riahi Dorsaf
 */
@Tag(name = "Catalogue — Produits")
@RestController
@RequestMapping("/catalogue/produits")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class ProduitController {

    private final IProduitService produitService;

    // ─────────────────────────────────────────────────────────
    //  LECTURE — inchangé
    // ─────────────────────────────────────────────────────────

    @Operation(
            summary     = "Lister les produits",
            description = "Filtres optionnels : type, statut, categorieId, keyword. "
                    + "Passer statut=ARCHIVE pour consulter les produits archivés."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProduitResponse>>> lister(
            @RequestParam(required = false) TypeProduit   type,
            @RequestParam(required = false) StatutProduit statut,
            @RequestParam(required = false) Long          categorieId,
            @RequestParam(required = false) String        keyword,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                produitService.listerProduits(
                        proprietaire.getId(), type, statut, categorieId, keyword),
                "Produits récupérés."));
    }

    @Operation(summary = "Détail d'un produit")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProduitResponse>> obtenir(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                produitService.obtenirProduit(id, proprietaire.getId()),
                "Produit récupéré."));
    }

    // ─────────────────────────────────────────────────────────
    //  ÉCRITURE — inchangé
    // ─────────────────────────────────────────────────────────

    @Operation(summary = "Créer un produit")
    @PostMapping
    public ResponseEntity<ApiResponse<ProduitResponse>> creer(
            @Valid @RequestBody ProduitRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                produitService.creerProduit(request, proprietaire),
                "Produit créé."));
    }

    @Operation(summary = "Modifier un produit")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProduitResponse>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody ProduitRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                produitService.modifierProduit(id, request, proprietaire.getId()),
                "Produit modifié."));
    }

    // ─────────────────────────────────────────────────────────
    //  GESTION DU STATUT — endpoints PATCH séparés (bonne pratique)
    // ─────────────────────────────────────────────────────────

    /**
     * Archive un produit (statut → ARCHIVE).
     * Le produit disparaît des listes actives.
     * Consultable via {@code GET /produits?statut=ARCHIVE}.
     */
    @Operation(
            summary     = "Archiver un produit",
            description = "Passe le produit au statut ARCHIVE. "
                    + "Consultable via GET /produits?statut=ARCHIVE."
    )
    @PatchMapping("/{id}/archiver")
    public ResponseEntity<ApiResponse<Void>> archiver(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        produitService.archiverProduit(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Produit archivé."));
    }

    /**
     * Désarchive un produit (statut ARCHIVE → INACTIF).
     * Le produit redevient visible dans la liste INACTIF.
     * L'utilisateur peut ensuite l'activer via {@code PATCH /{id}/activer}.
     */
    @Operation(
            summary     = "Désarchiver un produit",
            description = "Passe le produit archivé au statut INACTIF. "
                    + "Activer ensuite via PATCH /{id}/activer."
    )
    @PatchMapping("/{id}/desarchiver")
    public ResponseEntity<ApiResponse<Void>> desarchiver(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        produitService.desarchiverProduit(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Produit désarchivé."));
    }

    /**
     * Active un produit (statut INACTIF → ACTIF).
     * Le produit apparaît dans les listes actives du catalogue.
     * Retourne 400 si le produit est archivé.
     */
    @Operation(
            summary     = "Activer un produit",
            description = "Passe le produit INACTIF au statut ACTIF. "
                    + "Retourne 400 si le produit est archivé."
    )
    @PatchMapping("/{id}/activer")
    public ResponseEntity<ApiResponse<Void>> activer(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        produitService.activerProduit(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Produit activé."));
    }

    /**
     * Désactive un produit (statut ACTIF → INACTIF).
     * Le produit disparaît des listes actives mais reste consultable.
     * Retourne 400 si le produit est archivé.
     */
    @Operation(
            summary     = "Désactiver un produit",
            description = "Passe le produit ACTIF au statut INACTIF. "
                    + "Retourne 400 si le produit est archivé."
    )
    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<ApiResponse<Void>> desactiver(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        produitService.desactiverProduit(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Produit désactivé."));
    }
}