package com.crm.modules.client.controller;

import com.crm.modules.client.dto.ClientRequest;
import com.crm.modules.client.dto.ClientResponse;
import com.crm.modules.client.service.IClientService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.response.ApiResponse;
import com.crm.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller — Clients CRM.
 * Base path : /api/clients
 *
 * @author Riahi Dorsaf
 */
@Tag(name = "Clients", description = "Gestion des clients (individuel et entreprise)")
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class ClientController {

    private final IClientService clientService;

    @Operation(summary = "Lister les clients avec filtres et pagination")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ClientResponse>>> lister(
            @RequestParam(required = false) String typeClient,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                clientService.lister(proprietaire.getId(), typeClient, keyword, page, size),
                "Clients récupérés."));
    }

    @Operation(summary = "Obtenir le détail d'un client")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> obtenir(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                clientService.obtenir(id, proprietaire.getId()),
                "Client récupéré."));
    }

    @Operation(summary = "Créer un nouveau client")
    @PostMapping
    public ResponseEntity<ApiResponse<ClientResponse>> creer(
            @Valid @RequestBody ClientRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                clientService.creer(request, proprietaire),
                "Client créé avec succès."));
    }

    @Operation(summary = "Modifier un client existant")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                clientService.modifier(id, request, proprietaire.getId()),
                "Client modifié."));
    }

    @Operation(summary = "Supprimer un client (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        clientService.supprimer(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Client supprimé."));
    }

    @Operation(summary = "Détecter des clients depuis une photo via OCR Groq Vision")
    @PostMapping(value = "/import/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ClientRequest>>> importerDepuisPhoto(
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {

        return ResponseEntity.ok(ApiResponse.success(
                clientService.extraireClientsDepuisPhoto(image),
                "Clients détectés."));
    }
}