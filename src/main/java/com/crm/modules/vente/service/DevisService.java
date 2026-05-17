package com.crm.modules.vente.service;

import com.crm.modules.catalogue.repository.ProduitRepository;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.reporting.service.IActiviteService;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.modules.vente.dto.DevisRequest;
import com.crm.modules.vente.dto.DevisResponse;
import com.crm.modules.vente.dto.FactureResponse;
import com.crm.modules.vente.entity.Devis;
import com.crm.modules.vente.entity.Facture;
import com.crm.modules.vente.entity.LigneDevis;
import com.crm.modules.vente.entity.LigneFacture;
import com.crm.modules.vente.mapper.DevisMapper;
import com.crm.modules.vente.mapper.FactureMapper;
import com.crm.modules.vente.repository.DevisRepository;
import com.crm.modules.vente.repository.FactureRepository;
import com.crm.modules.vente.repository.OpportuniteRepository;
import com.crm.shared.enums.StatutDevis;
import com.crm.shared.enums.StatutFacture;
import com.crm.shared.enums.TypeActivite;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DevisService implements IDevisService {

    private final DevisRepository devisRepository;
    private final FactureRepository factureRepository;
    private final ClientRepository clientRepository;
    private final OpportuniteRepository opportuniteRepository;
    private final ProduitRepository produitRepository;
    private final ProprietaireRepository proprietaireRepository;
    private final DevisMapper devisMapper;
    private final FactureMapper factureMapper;
    private final IActiviteService activiteService;

    @Transactional(readOnly = true)
    @Override
    public List<DevisResponse> lister(Long proprietaireId, StatutDevis statut) {
        List<Devis> list = devisRepository.findByProprietaireIdOrderByDateCreationDesc(proprietaireId);
        return list.stream()
                .filter(d -> statut == null || d.getStatut() == statut)
                .map(this::enrichir)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public DevisResponse obtenir(Long id, Long proprietaireId) {
        return enrichir(charger(id, proprietaireId));
    }

    @Override
    public DevisResponse creer(DevisRequest req, Long proprietaireId) {
        var proprietaire = proprietaireRepository.findById(proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire introuvable"));
        var client = clientRepository.findByIdAndProprietaireIdAndIsDeletedFalse(req.getClientId(), proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));

        String numero = genererNumero(proprietaireId, "DV");

        Devis devis = Devis.builder()
                .numero(numero)
                .statut(StatutDevis.BROUILLON)
                .client(client)
                .notes(req.getNotes())
                .validiteJours(req.getValiditeJours() != null ? req.getValiditeJours() : 30)
                .proprietaire(proprietaire)
                .build();

        if (req.getOpportuniteId() != null) {
            devis.setOpportunite(opportuniteRepository.findByIdAndProprietaireId(req.getOpportuniteId(), proprietaireId)
                    .orElse(null));
        }

        // ── Lignes ─────────────────────────────────────────────
        List<LigneDevis> lignes = construireLignes(req, devis, proprietaireId);
        devis.setLignes(lignes);
        devis.recalculerTotaux();

        devis = devisRepository.save(devis);
        log.info("[DEVIS] Créé — numero={}", numero);

        activiteService.enregistrer(
                TypeActivite.DEVIS_CREE, "Devis créé", numero,
                devis.getId(), "DEVIS", null, proprietaire);

        return enrichir(devis);
    }

    @Override
    public DevisResponse modifier(Long id, DevisRequest req, Long proprietaireId) {
        Devis devis = charger(id, proprietaireId);

        if (devis.getStatut() != StatutDevis.BROUILLON) {
            throw new BusinessException("Seuls les devis en brouillon peuvent être modifiés.");
        }

        devis.setNotes(req.getNotes());
        if (req.getValiditeJours() != null) devis.setValiditeJours(req.getValiditeJours());

        devis.getLignes().clear();
        List<LigneDevis> lignes = construireLignes(req, devis, proprietaireId);
        devis.getLignes().addAll(lignes);
        devis.recalculerTotaux();

        devis = devisRepository.save(devis);
        return enrichir(devis);
    }

    @Override
    public DevisResponse changerStatut(Long id, StatutDevis statut, Long proprietaireId) {
        Devis devis = charger(id, proprietaireId);
        validerTransitionDevis(devis.getStatut(), statut);

        devis.setStatut(statut);
        devis = devisRepository.save(devis);

        TypeActivite type = switch (statut) {
            case ENVOYE -> TypeActivite.DEVIS_ENVOYE;
            case ACCEPTE -> TypeActivite.DEVIS_ACCEPTE;
            case REFUSE -> TypeActivite.DEVIS_REFUSE;
            default -> TypeActivite.DEVIS_CREE;
        };

        activiteService.enregistrer(
                type, "Devis " + statut.name().toLowerCase(), devis.getNumero(),
                devis.getId(), "DEVIS", null, devis.getProprietaire());

        return enrichir(devis);
    }

    @Override
    public FactureResponse convertirEnFacture(Long id, Long proprietaireId) {
        Devis devis = charger(id, proprietaireId);

        if (devis.getStatut() != StatutDevis.ACCEPTE) {
            throw new BusinessException("Seuls les devis acceptés peuvent être convertis en facture.");
        }

        String numeroFacture = genererNumero(proprietaireId, "FA");

        Facture facture = Facture.builder()
                .numero(numeroFacture)
                .statut(StatutFacture.BROUILLON)
                .client(devis.getClient())
                .devisOrigine(devis)
                .montantHt(devis.getMontantHt())
                .montantTva(devis.getMontantTva())
                .montantTtc(devis.getMontantTtc())
                .notes(devis.getNotes())
                .dateEcheance(LocalDate.now().plusDays(30))
                .proprietaire(devis.getProprietaire())
                .build();

        // ── Copie des lignes ─────────────────────────────────
        List<LigneFacture> lignesFacture = devis.getLignes().stream()
                .map(l -> LigneFacture.builder()
                        .facture(facture)
                        .produit(l.getProduit())
                        .designation(l.getDesignation())
                        .quantite(l.getQuantite())
                        .prixUnitaireHt(l.getPrixUnitaireHt())
                        .tauxTva(l.getTauxTva())
                        .remise(l.getRemise())
                        .montantHt(l.getMontantHt())
                        .montantTva(l.getMontantTva())
                        .montantTtc(l.getMontantTtc())
                        .build())
                .toList();

        facture.setLignes(new ArrayList<>(lignesFacture));
        Facture saved = factureRepository.save(facture);

        activiteService.enregistrer(
                TypeActivite.FACTURE_CREEE, "Facture créée", numeroFacture,
                saved.getId(), "FACTURE", null, devis.getProprietaire());

        log.info("[DEVIS] Converti en facture — devisId={} factureId={}", id, saved.getId());
        return enrichirFacture(saved);
    }

    @Override
    public void supprimer(Long id, Long proprietaireId) {
        Devis devis = charger(id, proprietaireId);
        if (devis.getStatut() != StatutDevis.BROUILLON) {
            throw new BusinessException("Seuls les devis en brouillon peuvent être supprimés.");
        }
        devisRepository.deleteById(devis.getId());
        log.info("[DEVIS] Supprimé — id={}", id);
    }

    // ─────────────────────────────────────────────────────────

    private Devis charger(Long id, Long proprietaireId) {
        return devisRepository.findByIdAndProprietaireId(id, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Devis introuvable"));
    }

    private List<LigneDevis> construireLignes(DevisRequest req, Devis devis, Long proprietaireId) {
        return req.getLignes().stream().map(lr -> {
            var produit = produitRepository.findByIdAndProprietaireId(lr.getProduitId(), proprietaireId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + lr.getProduitId()));

            LigneDevis ligne = LigneDevis.builder()
                    .devis(devis)
                    .produit(produit)
                    .designation(lr.getDesignation() != null ? lr.getDesignation() : produit.getNom())
                    .quantite(lr.getQuantite())
                    .prixUnitaireHt(lr.getPrixUnitaireHt() != null ? lr.getPrixUnitaireHt() : produit.getPrixHT())
                    .tauxTva(lr.getTauxTva() != null ? lr.getTauxTva()
                            : (produit.getTauxTVA() != null ? produit.getTauxTVA() : BigDecimal.ZERO))
                    .remise(lr.getRemise() != null ? lr.getRemise() : BigDecimal.ZERO)
                    .build();

            ligne.calculer();
            return ligne;
        }).toList();
    }

    private void validerTransitionDevis(StatutDevis actuel, StatutDevis nouveau) {
        boolean valide = switch (actuel) {
            case BROUILLON -> nouveau == StatutDevis.ENVOYE;
            case ENVOYE ->
                    nouveau == StatutDevis.ACCEPTE || nouveau == StatutDevis.REFUSE || nouveau == StatutDevis.EXPIRE;
            default -> false;
        };
        if (!valide) throw new BusinessException(
                "Transition invalide : " + actuel + " → " + nouveau);
    }

    private String genererNumero(Long proprietaireId, String prefixe) {
        String annee = String.valueOf(Year.now().getValue());
        String pref = prefixe + "-" + annee + "-";

        if ("DV".equals(prefixe)) {
            return devisRepository
                    .findTopByProprietaireIdAndNumeroStartingWithOrderByNumeroDesc(proprietaireId, pref)
                    .map(d -> String.format("%s-%s-%04d", prefixe, annee,
                            Integer.parseInt(d.getNumero().substring(d.getNumero().length() - 4)) + 1))
                    .orElse(String.format("%s-%s-0001", prefixe, annee));
        } else {
            return factureRepository
                    .findTopByProprietaireIdAndNumeroStartingWithOrderByNumeroDesc(proprietaireId, pref)
                    .map(f -> String.format("%s-%s-%04d", prefixe, annee,
                            Integer.parseInt(f.getNumero().substring(f.getNumero().length() - 4)) + 1))
                    .orElse(String.format("%s-%s-0001", prefixe, annee));
        }
    }

    private DevisResponse enrichir(Devis d) {
        DevisResponse r = devisMapper.toResponse(d);
        r.setLignes(d.getLignes().stream().map(devisMapper::toLigneResponse).toList());
        r.setDateRelative(dateRelative(d.getDateCreation()));
        return r;
    }

    private FactureResponse enrichirFacture(Facture f) {
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