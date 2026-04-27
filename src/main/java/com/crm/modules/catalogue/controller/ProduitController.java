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
 * @author Riahi Dorsaf
 */
@Tag(name = "Catalogue — Produits")
@RestController
@RequestMapping("/catalogue/produits")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class ProduitController {

    private final IProduitService produitService;

    @Operation(summary = "Lister les produits",
            description = "Filtres optionnels : type, statut, categorieId, keyword")
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

    @Operation(summary = "Archiver un produit")
    @PatchMapping("/{id}/archiver")
    public ResponseEntity<ApiResponse<Void>> archiver(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        produitService.archiverProduit(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Produit archivé."));
    }
}