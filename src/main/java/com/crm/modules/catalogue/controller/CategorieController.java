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
 * @author Riahi Dorsaf
 */
@Tag(name = "Catalogue — Catégories")
@RestController
@RequestMapping("/catalogue/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class CategorieController {

    private final ICategorieService categorieService;

    @Operation(summary = "Lister les catégories",
            description = "Paramètre optionnel : keyword (nom, description)")
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

    @Operation(summary = "Supprimer une catégorie")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        categorieService.supprimerCategorie(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Catégorie supprimée."));
    }
}