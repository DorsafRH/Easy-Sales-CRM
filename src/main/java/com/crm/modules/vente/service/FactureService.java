package com.crm.modules.vente.service;

import com.crm.modules.catalogue.entity.Produit;
import com.crm.modules.reporting.service.IActiviteService;
import com.crm.modules.vente.dto.FactureResponse;
import com.crm.modules.vente.entity.Facture;
import com.crm.modules.vente.entity.LigneFacture;
import com.crm.modules.vente.entity.Opportunite;
import com.crm.modules.vente.mapper.FactureMapper;
import com.crm.modules.vente.repository.FactureRepository;
import com.crm.modules.vente.repository.OpportuniteRepository;
import com.crm.shared.enums.StatutFacture;
import com.crm.shared.enums.StatutOpportunite;
import com.crm.shared.enums.TypeActivite;
import com.crm.shared.enums.TypeProduit;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
/**
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FactureService implements IFactureService {

    private final FactureRepository factureRepository;
    private final FactureMapper factureMapper;
    private final IActiviteService activiteService;
    private final OpportuniteRepository opportuniteRepository;

    @Transactional(readOnly = true)
    @Override
    public List<FactureResponse> lister(Long proprietaireId, StatutFacture statut) {
        return factureRepository.findByProprietaireIdOrderByDateCreationDesc(proprietaireId)
                .stream()
                .filter(f -> statut == null || f.getStatut() == statut)
                .map(this::enrichir)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public FactureResponse obtenir(Long id, Long proprietaireId) {
        return enrichir(charger(id, proprietaireId));
    }

    @Override
    public FactureResponse changerStatut(Long id, StatutFacture statut, Long proprietaireId) {
        Facture facture = charger(id, proprietaireId);
        StatutFacture statutPrecedent = facture.getStatut();
        validerTransition(statutPrecedent, statut);

        facture.setStatut(statut);

        switch (statut) {
            case EMISE   -> facture.setDateEmission(LocalDateTime.now());
            case PAYEE   -> facture.setDatePaiement(LocalDateTime.now());
            case LIVREE  -> {
                facture.setDateLivraison(LocalDateTime.now());
                appliquerMouvementStock(facture, -1); // décrémente le stock
            }
            case ANNULEE -> {
                // Restaure le stock si la facture était déjà livrée
                if (statutPrecedent == StatutFacture.LIVREE) {
                    appliquerMouvementStock(facture, +1);
                }
            }
            default -> { /* BROUILLON, EN_RETARD : pas d'effet secondaire */ }
        }

        facture = factureRepository.save(facture);

        if (statut == StatutFacture.PAYEE) {
            try {
                mettreAJourOpportuniteApresPaiement(facture);
            } catch (Exception e) {
                log.warn("[OPPORTUNITE] Mise a jour statut ignoree : {}", e.getMessage());
            }
        }

        TypeActivite type = switch (statut) {
            case EMISE   -> TypeActivite.FACTURE_EMISE;
            case LIVREE  -> TypeActivite.FACTURE_LIVREE;
            case PAYEE   -> TypeActivite.FACTURE_PAYEE;
            case ANNULEE -> TypeActivite.FACTURE_ANNULEE;
            default      -> TypeActivite.FACTURE_CREEE;
        };

        activiteService.enregistrer(
                type, "Facture " + statut.name().toLowerCase(), facture.getNumero(),
                facture.getId(), "FACTURE", null, facture.getProprietaire());

        return enrichir(facture);
    }

    // ── Opportunité → GAGNEE après paiement ───────────────────────────────
    private void mettreAJourOpportuniteApresPaiement(Facture facture) {
        if (facture.getDevisOrigine() == null) return;
        Opportunite opportunite = facture.getDevisOrigine().getOpportunite();
        if (opportunite == null) return;
        if (opportunite.getStatut() == StatutOpportunite.GAGNEE
                || opportunite.getStatut() == StatutOpportunite.PERDUE) return;

        opportunite.setStatut(StatutOpportunite.GAGNEE);
        opportuniteRepository.save(opportunite);

        activiteService.enregistrer(
                TypeActivite.OPPORTUNITE_GAGNEE,
                "Opportunite gagnee apres paiement",
                opportunite.getTitre(),
                opportunite.getId(),
                "OPPORTUNITE",
                null,
                opportunite.getProprietaire());
    }

    // ── Mouvement de stock ─────────────────────────────────────────────────
    // signe = -1 : livraison (décrémente). signe = +1 : annulation (restaure).
    // CRM ≠ WMS → stock négatif autorisé, juste loggué.
    private void appliquerMouvementStock(Facture facture, int signe) {
        for (LigneFacture ligne : facture.getLignes()) {
            Produit produit = ligne.getProduit();
            if (produit == null || produit.getType() != TypeProduit.STOCKABLE) continue;

            int avant = produit.getStockDisponible() != null ? produit.getStockDisponible() : 0;
            int apres = avant + (signe * ligne.getQuantite());
            produit.setStockDisponible(apres);

            if (apres < 0) {
                log.warn("[STOCK] Produit id={} \"{}\" : stock négatif après mouvement ({} → {})",
                        produit.getId(), produit.getNom(), avant, apres);
            } else {
                log.info("[STOCK] Produit id={} \"{}\" : {} → {}", produit.getId(), produit.getNom(), avant, apres);
            }
        }
    }

    private Facture charger(Long id, Long proprietaireId) {
        return factureRepository.findByIdAndProprietaireId(id, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
    }

    private void validerTransition(StatutFacture actuel, StatutFacture nouveau) {
        boolean ok = switch (actuel) {
            case BROUILLON -> nouveau == StatutFacture.EMISE;
            case EMISE     -> nouveau == StatutFacture.LIVREE
                    || nouveau == StatutFacture.PAYEE
                    || nouveau == StatutFacture.ANNULEE
                    || nouveau == StatutFacture.EN_RETARD;
            case LIVREE    -> nouveau == StatutFacture.PAYEE
                    || nouveau == StatutFacture.ANNULEE;
            case EN_RETARD -> nouveau == StatutFacture.PAYEE
                    || nouveau == StatutFacture.ANNULEE;
            default -> false;
        };
        if (!ok) throw new BusinessException("Transition invalide : " + actuel + " → " + nouveau);
    }

    private FactureResponse enrichir(Facture f) {
        FactureResponse r = factureMapper.toResponse(f);
        r.setLignes(f.getLignes().stream().map(factureMapper::toLigneResponse).toList());
        r.setDateRelative(dateRelative(f.getDateCreation()));
        return r;
    }

    private String dateRelative(LocalDateTime date) {
        if (date == null) return "";
        Duration d = Duration.between(date, LocalDateTime.now());
        long min = d.toMinutes();
        if (min < 1) return "à l'instant";
        if (min < 60) return "il y a " + min + " min";
        long h = d.toHours();
        if (h < 24) return "il y a " + h + "h";
        return "il y a " + d.toDays() + "j";
    }
}