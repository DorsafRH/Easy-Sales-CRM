package com.crm.modules.catalogue.service;

import com.crm.modules.catalogue.dto.ProduitRequest;
import com.crm.modules.catalogue.dto.ProduitResponse;
import com.crm.modules.catalogue.entity.Produit;
import com.crm.modules.catalogue.mapper.ProduitMapper;
import com.crm.modules.catalogue.repository.CategorieRepository;
import com.crm.modules.catalogue.repository.ProduitRepository;
import com.crm.modules.catalogue.specification.ProduitSpecification;
import com.crm.modules.reporting.service.IActiviteService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutProduit;
import com.crm.shared.enums.TypeActivite;
import com.crm.shared.enums.TypeProduit;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

/**
 * Implémentation du service de gestion des produits du catalogue.
 *
 * <p>Génération du code produit : {@code findTop...OrderByCodeProduitDesc}
 * retourne le dernier code de l'année courante ; le numéro de séquence
 * est extrait et incrémenté en Java.</p>
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProduitService implements IProduitService {

    private final ProduitRepository   produitRepository;
    private final CategorieRepository categorieRepository;
    private final ProduitMapper       produitMapper;

    /**
     * Injection via l'interface IActiviteService — pas l'implémentation concrète.
     * Bonne pratique SOLID D : dépendre d'une abstraction, pas d'une implémentation.
     */
    private final IActiviteService    activiteService;

    // ─────────────────────────────────────────────────────────────────────────
    //  LECTURE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public List<ProduitResponse> listerProduits(Long proprietaireId,
                                                TypeProduit type,
                                                StatutProduit statut,
                                                Long categorieId,
                                                String keyword) {
        Specification<Produit> spec =
                ProduitSpecification.duProprietaire(proprietaireId)
                        .and(ProduitSpecification.avecType(type))
                        .and(ProduitSpecification.avecStatut(statut))
                        .and(ProduitSpecification.avecCategorie(categorieId))
                        .and(ProduitSpecification.recherche(keyword));

        return produitRepository.findAll(spec)
                .stream()
                .map(produitMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ProduitResponse obtenirProduit(Long id, Long proprietaireId) {
        return produitMapper.toResponse(charger(id, proprietaireId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ÉCRITURE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ProduitResponse creerProduit(ProduitRequest req,
                                        ProprietaireEntreprise proprietaire) {
        if (produitRepository.existsByNomIgnoreCaseAndProprietaireId(
                req.getNom(), proprietaire.getId())) {
            throw new BusinessException("Un produit avec ce nom existe déjà");
        }

        Produit produit = new Produit();
        produit.setProprietaire(proprietaire);
        produit.setCodeProduit(genererCode(proprietaire.getId()));
        appliquer(req, produit, proprietaire.getId());
        if (produit.getStatut() == null) produit.setStatut(StatutProduit.ACTIF);
        produit = produitRepository.save(produit);
        log.info("[PRODUIT] Créé — code={}", produit.getCodeProduit());

        // titre = label de l'action | description = nom du produit
        activiteService.enregistrer(
                TypeActivite.PRODUIT_CREE,
                "Nouveau produit ajouté",
                produit.getNom(),
                produit.getId(), "PRODUIT", null, proprietaire);

        return produitMapper.toResponse(produit);
    }

    @Override
    public ProduitResponse modifierProduit(Long id, ProduitRequest req,
                                           Long proprietaireId) {
        Produit produit = charger(id, proprietaireId);
        if (!produit.getNom().equalsIgnoreCase(req.getNom())
                && produitRepository.existsByNomIgnoreCaseAndProprietaireId(
                req.getNom(), proprietaireId)) {
            throw new BusinessException("Un produit avec ce nom existe déjà");
        }

        appliquer(req, produit, proprietaireId);
        produit.setDateModification(LocalDateTime.now());
        produit = produitRepository.save(produit);

        activiteService.enregistrer(
                TypeActivite.PRODUIT_MODIFIE,
                "Produit mis à jour",
                produit.getNom(),
                produit.getId(), "PRODUIT", null, produit.getProprietaire());

        return produitMapper.toResponse(produit);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ARCHIVAGE / DÉSARCHIVAGE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void archiverProduit(Long id, Long proprietaireId) {
        Produit produit = charger(id, proprietaireId);
        if (StatutProduit.ARCHIVE.equals(produit.getStatut())) {
            throw new BusinessException("Ce produit est déjà archivé");
        }
        produit.setStatut(StatutProduit.ARCHIVE);
        produit.setDateModification(LocalDateTime.now());
        produitRepository.save(produit);
        log.info("[PRODUIT] Archivé — id={}", id);

        activiteService.enregistrer(
                TypeActivite.PRODUIT_ARCHIVE,
                "Produit archivé",
                produit.getNom(),
                id, "PRODUIT", null, produit.getProprietaire());
    }

    @Override
    public void desarchiverProduit(Long id, Long proprietaireId) {
        Produit produit = charger(id, proprietaireId);
        if (!StatutProduit.ARCHIVE.equals(produit.getStatut())) {
            throw new BusinessException("Ce produit n'est pas archivé");
        }
        produit.setStatut(StatutProduit.INACTIF);
        produit.setDateModification(LocalDateTime.now());
        produitRepository.save(produit);
        log.info("[PRODUIT] Désarchivé → INACTIF — id={}", id);

        activiteService.enregistrer(
                TypeActivite.PRODUIT_DESARCHIVE,
                "Produit désarchivé",
                produit.getNom(),
                id, "PRODUIT", null, produit.getProprietaire());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TOGGLE ACTIF / INACTIF
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void activerProduit(Long id, Long proprietaireId) {
        Produit produit = charger(id, proprietaireId);
        if (StatutProduit.ARCHIVE.equals(produit.getStatut())) {
            throw new BusinessException(
                    "Impossible d'activer un produit archivé. Désarchivez-le d'abord.");
        }
        if (StatutProduit.ACTIF.equals(produit.getStatut())) {
            throw new BusinessException("Ce produit est déjà actif");
        }
        produit.setStatut(StatutProduit.ACTIF);
        produit.setDateModification(LocalDateTime.now());
        produitRepository.save(produit);
        log.info("[PRODUIT] Activé — id={}", id);

        activiteService.enregistrer(
                TypeActivite.PRODUIT_ACTIVE,
                "Produit activé",
                produit.getNom(),
                id, "PRODUIT", null, produit.getProprietaire());
    }

    @Override
    public void desactiverProduit(Long id, Long proprietaireId) {
        Produit produit = charger(id, proprietaireId);
        if (StatutProduit.ARCHIVE.equals(produit.getStatut())) {
            throw new BusinessException(
                    "Impossible de désactiver un produit archivé.");
        }
        if (StatutProduit.INACTIF.equals(produit.getStatut())) {
            throw new BusinessException("Ce produit est déjà inactif");
        }
        produit.setStatut(StatutProduit.INACTIF);
        produit.setDateModification(LocalDateTime.now());
        produitRepository.save(produit);
        log.info("[PRODUIT] Désactivé — id={}", id);

        activiteService.enregistrer(
                TypeActivite.PRODUIT_DESACTIVE,
                "Produit désactivé",
                produit.getNom(),
                id, "PRODUIT", null, produit.getProprietaire());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────────────────────

    private Produit charger(Long id, Long proprietaireId) {
        return produitRepository.findByIdAndProprietaireId(id, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));
    }

    /**
     * Génère un code séquentiel au format {@code PRD-YYYY-NNNN}.
     */
    private String genererCode(Long proprietaireId) {
        String annee   = String.valueOf(Year.now().getValue());
        String prefixe = "PRD-" + annee + "-";

        int seq = produitRepository
                .findTopByProprietaireIdAndCodeProduitStartingWithOrderByCodeProduitDesc(
                        proprietaireId, prefixe)
                .map(p -> {
                    String code = p.getCodeProduit();
                    return Integer.parseInt(code.substring(code.length() - 4)) + 1;
                })
                .orElse(1);

        return String.format("PRD-%s-%04d", annee, seq);
    }

    private void appliquer(ProduitRequest req, Produit produit, Long proprietaireId) {
        produit.setNom(req.getNom());
        produit.setDescription(req.getDescription());
        produit.setType(req.getType());
        produit.setPrixHT(req.getPrixHT());
        produit.setTauxTVA(req.getTauxTVA());
        produit.setUnite(req.getUnite());
        if (req.getStatut() != null) produit.setStatut(req.getStatut());

        boolean estStockable = req.getType() != null
                && "STOCKABLE".equals(req.getType().name());
        produit.setStockDisponible(estStockable
                ? (req.getStockDisponible() != null ? req.getStockDisponible() : 0)
                : null);

        if (req.getCategorieId() != null) {
            produit.setCategorie(
                    categorieRepository.findByIdAndProprietaireId(
                                    req.getCategorieId(), proprietaireId)
                            .orElseThrow(() -> new BusinessException("Catégorie introuvable")));
        } else {
            produit.setCategorie(null);
        }
    }
}