package com.crm.modules.marketing.controller;

import com.crm.modules.marketing.dto.request.AmeliorerContenuRequestDTO;
import com.crm.modules.marketing.dto.request.GenererContenuRequestDTO;
import com.crm.modules.marketing.dto.request.GenererPublicationRequestDTO;
import com.crm.modules.marketing.dto.request.PublicationRequestDTO;
import com.crm.modules.marketing.dto.response.CompteSocialResponseDTO;
import com.crm.modules.marketing.dto.response.GenererContenuResponseDTO;
import com.crm.modules.marketing.dto.response.PublicationResponseDTO;
import com.crm.modules.marketing.dto.response.StatistiquesPublicationDTO;
import com.crm.modules.marketing.service.IMarketingService;
import com.crm.modules.marketing.service.IMarketingStatsService;
import com.crm.modules.marketing.service.MetaOAuthService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller — Module Marketing IA.
 * Base path : /api/marketing
 *
 * @author Riahi Dorsaf
 */
@Tag(name = "Marketing IA",
        description = "Génération de contenu IA, publications et réseaux sociaux")
@RestController
@RequestMapping("/marketing")
@RequiredArgsConstructor
public class MarketingController {

    private final IMarketingService marketingService;
    private final IMarketingStatsService marketingStatsService;
    private final MetaOAuthService metaOAuthService;

    @Operation(summary = "Générer du contenu via IA",
            description = "Pattern Generator → Critic via l'API Groq")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Contenu généré avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Requête invalide")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @PostMapping("/generer")
    public ResponseEntity<ApiResponse<GenererContenuResponseDTO>> generer(
            @Valid @RequestBody GenererContenuRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.genererContenu(request), "Contenu généré."));
    }

    @Operation(summary = "Générer une publication pilotée par le catalogue",
            description = "Selon la portée (produits/catégorie/boutique/libre), le backend récupère "
                    + "les données produit, calcule les prix promo et fait rédiger le post par l'IA")
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @PostMapping("/generer-publication")
    public ResponseEntity<ApiResponse<GenererContenuResponseDTO>> genererPublication(
            @Valid @RequestBody GenererPublicationRequestDTO request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.genererPublication(request, proprietaire.getId()),
                "Contenu généré."));
    }

    @Operation(summary = "Améliorer un texte avec l'IA",
            description = "Raffine un texte déjà rédigé ; peut être relancé autant de fois que voulu")
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @PostMapping("/ameliorer")
    public ResponseEntity<ApiResponse<GenererContenuResponseDTO>> ameliorer(
            @Valid @RequestBody AmeliorerContenuRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.ameliorerContenu(request), "Contenu amélioré."));
    }

    @Operation(summary = "Créer une publication",
            description = "Crée une publication (brouillon ou programmée)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Publication créée"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Requête invalide")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @PostMapping("/publications")
    public ResponseEntity<ApiResponse<PublicationResponseDTO>> creerPublication(
            @Valid @RequestBody PublicationRequestDTO request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                marketingService.creerPublication(request, proprietaire.getId()),
                "Publication créée."));
    }

    @Operation(summary = "Lister les publications",
            description = "Liste les publications du propriétaire courant")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Liste récupérée")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @GetMapping("/publications")
    public ResponseEntity<ApiResponse<List<PublicationResponseDTO>>> listerPublications(
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.listerPublications(proprietaire.getId()),
                "Publications récupérées."));
    }

    @Operation(summary = "Détail d'une publication")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Publication trouvée"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Publication introuvable")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @GetMapping("/publications/{id}")
    public ResponseEntity<ApiResponse<PublicationResponseDTO>> obtenirPublication(
            @Parameter(description = "Identifiant de la publication") @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.obtenirPublication(id, proprietaire.getId()),
                "Publication récupérée."));
    }

    @Operation(summary = "Statistiques Facebook d'une publication",
            description = "Vues, engagement et courbe des vues journalières "
                    + "d'une publication PUBLIEE (via l'API Graph)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Statistiques récupérées"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Publication non publiée"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Publication introuvable")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @GetMapping("/publications/{id}/statistiques")
    public ResponseEntity<ApiResponse<StatistiquesPublicationDTO>> statistiquesPublication(
            @Parameter(description = "Identifiant de la publication") @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingStatsService.getStatistiquesPublication(id, proprietaire.getId()),
                "Statistiques récupérées."));
    }

    @Operation(summary = "Modifier une publication",
            description = "Modification possible uniquement à l'état BROUILLON")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Publication modifiée"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Publication non modifiable"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Publication introuvable")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @PutMapping("/publications/{id}")
    public ResponseEntity<ApiResponse<PublicationResponseDTO>> modifierPublication(
            @Parameter(description = "Identifiant de la publication") @PathVariable Long id,
            @Valid @RequestBody PublicationRequestDTO request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.modifierPublication(id, request, proprietaire.getId()),
                "Publication modifiée."));
    }

    @Operation(summary = "Supprimer une publication")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Publication supprimée"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Publication introuvable")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @DeleteMapping("/publications/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimerPublication(
            @Parameter(description = "Identifiant de la publication") @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        marketingService.supprimerPublication(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Publication supprimée."));
    }

    @Operation(summary = "Publier une publication",
            description = "Publie immédiatement la publication sur ses réseaux via l'API Graph")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Publication diffusée"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Publication non publiable"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Publication introuvable")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @PostMapping("/publications/{id}/publier")
    public ResponseEntity<ApiResponse<PublicationResponseDTO>> publier(
            @Parameter(description = "Identifiant de la publication") @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.publier(id, proprietaire.getId()),
                "Publication diffusée."));
    }

    @Operation(summary = "Annuler une publication")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Publication annulée"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Publication introuvable")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @PatchMapping("/publications/{id}/annuler")
    public ResponseEntity<ApiResponse<PublicationResponseDTO>> annuler(
            @Parameter(description = "Identifiant de la publication") @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.annuler(id, proprietaire.getId()),
                "Publication annulée."));
    }

    @Operation(summary = "Lister les comptes sociaux connectés")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Comptes récupérés")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @GetMapping("/reseaux")
    public ResponseEntity<ApiResponse<List<CompteSocialResponseDTO>>> listerReseaux(
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                marketingService.listerReseaux(proprietaire.getId()),
                "Comptes sociaux récupérés."));
    }

    @Operation(summary = "Déconnecter un compte social")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Compte déconnecté"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Compte introuvable")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @DeleteMapping("/reseaux/{id}")
    public ResponseEntity<ApiResponse<Void>> deconnecterCompte(
            @Parameter(description = "Identifiant du compte social") @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        marketingService.deconnecterCompte(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Compte social déconnecté."));
    }

    @Operation(summary = "Générer l'URL OAuth Meta",
            description = "Retourne l'URL d'autorisation Facebook/Instagram à ouvrir côté mobile")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "URL générée")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @GetMapping("/oauth/facebook")
    public ResponseEntity<ApiResponse<String>> genererUrlOAuth(
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                metaOAuthService.construireUrlOAuth(proprietaire.getId()),
                "URL OAuth générée."));
    }

    @Operation(summary = "Callback OAuth Meta",
            description = "Endpoint public appelé par Meta après autorisation de l'utilisateur")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Compte connecté"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Code ou state invalide")
    })
    @GetMapping(value = "/oauth/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callbackOAuth(
            @Parameter(description = "Code d'autorisation Meta") @RequestParam String code,
            @Parameter(description = "State anti-CSRF généré à l'initiation") @RequestParam String state) {
        String resultat = metaOAuthService.traiterCallback(code, state);
        return ResponseEntity.ok("<html><body><h2>" + resultat
                + "</h2><p>Vous pouvez fermer cette fenêtre et revenir à l'application.</p></body></html>");
    }
}
