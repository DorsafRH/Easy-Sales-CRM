package com.crm.modules.catalogue.controller;

import com.crm.modules.catalogue.dto.CategorieRequest;
import com.crm.modules.catalogue.dto.CategorieResponse;
import com.crm.modules.catalogue.service.ICategorieService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
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
 * Contrôleur REST pour la gestion des catégories du catalogue.
 * Base path : {@code /api/catalogue/categories}
 *
 * <p><b>Bonne pratique REST :</b> chaque endpoint a une responsabilité unique.
 * Les actions de pré-traitement avant suppression sont des endpoints PATCH
 * séparés, pas des paramètres {@code ?action=} sur le DELETE.</p>
 *
 * @author Riahi Dorsaf
 */
@Tag(name = "Catalogue — Catégories")
@RestController
@RequestMapping("/catalogue/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class CategorieController {

    private final ICategorieService categorieService;

    // ─────────────────────────────────────────────────────────
    //  LECTURE
    // ─────────────────────────────────────────────────────────

    @Operation(
            summary     = "Lister les catégories",
            description = "Paramètre optionnel : keyword (nom, description)"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategorieResponse>>> lister(
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                categorieService.listerCategories(proprietaire.getId(), keyword),
                "Catégories récupérées."));
    }

    @Operation(summary = "Détail d'une catégorie")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategorieResponse>> obtenir(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                categorieService.obtenirCategorie(id, proprietaire.getId()),
                "Catégorie récupérée."));
    }

    // ─────────────────────────────────────────────────────────
    //  ÉCRITURE
    // ─────────────────────────────────────────────────────────

    @Operation(summary = "Créer une catégorie")
    @PostMapping
    public ResponseEntity<ApiResponse<CategorieResponse>> creer(
            @Valid @RequestBody CategorieRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                categorieService.creerCategorie(request, proprietaire),
                "Catégorie créée."));
    }

    @Operation(summary = "Modifier une catégorie")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategorieResponse>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody CategorieRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                categorieService.modifierCategorie(id, request, proprietaire.getId()),
                "Catégorie modifiée."));
    }

    // ─────────────────────────────────────────────────────────
    //  PRÉ-TRAITEMENT AVANT SUPPRESSION
    // ─────────────────────────────────────────────────────────

    /**
     * Désactive tous les produits actifs d'une catégorie (statut → INACTIF).
     *
     * <p>À appeler avant {@code DELETE /{id}} quand la catégorie contient
     * des produits actifs et que l'utilisateur choisit de les désactiver.</p>
     *
     * <p><b>Pourquoi PATCH ?</b> PATCH = modification partielle d'une ressource.
     * On modifie partiellement les produits (uniquement leur statut).
     * PUT serait incorrect car on ne remplace pas la ressource entière.</p>
     */
    @Operation(
            summary     = "Désactiver les produits d'une catégorie",
            description = "Passe tous les produits ACTIF de la catégorie au statut INACTIF. "
                    + "À appeler avant la suppression de la catégorie."
    )
    @PatchMapping("/{id}/desactiver-produits")
    public ResponseEntity<ApiResponse<Void>> desactiverProduits(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        categorieService.desactiverProduitsCategorie(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success(
                "Produits de la catégorie désactivés."));
    }

    /**
     * Retire la catégorie de tous ses produits (categorieId → null).
     *
     * <p>À appeler avant {@code DELETE /{id}} quand la catégorie contient
     * des produits actifs et que l'utilisateur choisit de les conserver actifs
     * sans catégorie.</p>
     */
    @Operation(
            summary     = "Retirer la catégorie des produits",
            description = "Détache tous les produits de la catégorie (categorieId → null). "
                    + "Les produits restent actifs. À appeler avant la suppression."
    )
    @PatchMapping("/{id}/retirer-categorie")
    public ResponseEntity<ApiResponse<Void>> retirerCategorie(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        categorieService.retirerCategorieProduits(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success(
                "Catégorie retirée des produits."));
    }

    // ─────────────────────────────────────────────────────────
    //  SUPPRESSION
    // ─────────────────────────────────────────────────────────

    /**
     * Supprime une catégorie sans produits actifs.
     *
     * <p>Retourne 400 si la catégorie contient encore des produits actifs.
     * Dans ce cas, appeler d'abord {@code PATCH /{id}/desactiver-produits}
     * ou {@code PATCH /{id}/retirer-categorie}.</p>
     */
    @Operation(
            summary     = "Supprimer une catégorie",
            description = "Supprime la catégorie si elle ne contient plus de produits actifs. "
                    + "Retourne 400 sinon."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        categorieService.supprimerCategorie(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Catégorie supprimée."));
    }
}