package com.crm.modules.catalogue.service;

import com.crm.modules.catalogue.dto.ProduitRequest;
import com.crm.modules.catalogue.dto.ProduitResponse;
import com.crm.modules.catalogue.entity.Categorie;
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
 * Service de gestion des produits du catalogue.
 * Chaque méthode publique délègue à des helpers privés courts.
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
    private final IActiviteService    activiteService;

    // ─────────────────────────────────────────────────────────
    //  LECTURE
    // ─────────────────────────────────────────────────────────

    /**
     * Liste les produits selon les filtres fournis.
     * Chaque critère null est ignoré par la spécification.
     */
    @Transactional(readOnly = true)
    @Override
    public List<ProduitResponse> listerProduits(Long proprietaireId,
                                                TypeProduit type,
                                                StatutProduit statut,
                                                Long categorieId,
                                                String keyword) {
        Specification<Produit> spec = construireSpec(
                proprietaireId, type, statut, categorieId, keyword);
        return produitRepository.findAll(spec)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retourne le détail d'un produit par son identifiant.
     */
    @Transactional(readOnly = true)
    @Override
    public ProduitResponse obtenirProduit(Long id, Long proprietaireId) {
        return toResponse(charger(id, proprietaireId));
    }

    // ─────────────────────────────────────────────────────────
    //  ÉCRITURE
    // ─────────────────────────────────────────────────────────

    /**
     * Crée un nouveau produit dans le catalogue.
     * Vérifie l'unicité du nom avant la création.
     */
    @Override
    public ProduitResponse creerProduit(ProduitRequest req,
                                        ProprietaireEntreprise proprietaire) {
        verifierUniciteNom(req.getNom(), null, proprietaire.getId());
        Produit produit = initialiserProduit(req, proprietaire);
        produit = produitRepository.save(produit);
        log.info("[PRODUIT] Créé — code={}", produit.getCodeProduit());
        enregistrerActivite(TypeActivite.PRODUIT_CREE,
                "Nouveau produit ajouté", produit, proprietaire);
        return toResponse(produit);
    }

    /**
     * Modifie un produit existant.
     * Vérifie l'unicité du nom si celui-ci a changé.
     */
    @Override
    public ProduitResponse modifierProduit(Long id, ProduitRequest req,
                                           Long proprietaireId) {
        Produit produit = charger(id, proprietaireId);
        verifierUniciteNom(req.getNom(), produit.getNom(), proprietaireId);
        appliquer(req, produit, proprietaireId);
        produit.setDateModification(LocalDateTime.now());
        produit = produitRepository.save(produit);
        enregistrerActivite(TypeActivite.PRODUIT_MODIFIE,
                "Produit mis à jour", produit, produit.getProprietaire());
        return toResponse(produit);
    }

    // ─────────────────────────────────────────────────────────
    //  ARCHIVAGE
    // ─────────────────────────────────────────────────────────

    /**
     * Archive un produit (ACTIF/INACTIF → ARCHIVE).
     */
    @Override
    public void archiverProduit(Long id, Long proprietaireId) {
        Produit produit = charger(id, proprietaireId);
        if (StatutProduit.ARCHIVE.equals(produit.getStatut())) {
            throw new BusinessException("Ce produit est déjà archivé");
        }
        changerStatut(produit, StatutProduit.ARCHIVE);
        enregistrerActivite(TypeActivite.PRODUIT_ARCHIVE,
                "Produit archivé", produit, produit.getProprietaire());
        log.info("[PRODUIT] Archivé — id={}", id);
    }

    /**
     * Désarchive un produit (ARCHIVE → INACTIF).
     */
    @Override
    public void desarchiverProduit(Long id, Long proprietaireId) {
        Produit produit = charger(id, proprietaireId);
        if (!StatutProduit.ARCHIVE.equals(produit.getStatut())) {
            throw new BusinessException("Ce produit n'est pas archivé");
        }
        changerStatut(produit, StatutProduit.INACTIF);
        enregistrerActivite(TypeActivite.PRODUIT_DESARCHIVE,
                "Produit désarchivé", produit, produit.getProprietaire());
        log.info("[PRODUIT] Désarchivé → INACTIF — id={}", id);
    }

    // ─────────────────────────────────────────────────────────
    //  TOGGLE ACTIF / INACTIF
    // ─────────────────────────────────────────────────────────

    /**
     * Active un produit INACTIF.
     * Lève une exception si le produit est archivé.
     */
    @Override
    public void activerProduit(Long id, Long proprietaireId) {
        Produit produit = charger(id, proprietaireId);
        verifierNonArchive(produit, "activer");
        if (StatutProduit.ACTIF.equals(produit.getStatut())) {
            throw new BusinessException("Ce produit est déjà actif");
        }
        changerStatut(produit, StatutProduit.ACTIF);
        enregistrerActivite(TypeActivite.PRODUIT_ACTIVE,
                "Produit activé", produit, produit.getProprietaire());
        log.info("[PRODUIT] Activé — id={}", id);
    }

    /**
     * Désactive un produit ACTIF.
     * Lève une exception si le produit est archivé.
     */
    @Override
    public void desactiverProduit(Long id, Long proprietaireId) {
        Produit produit = charger(id, proprietaireId);
        verifierNonArchive(produit, "désactiver");
        if (StatutProduit.INACTIF.equals(produit.getStatut())) {
            throw new BusinessException("Ce produit est déjà inactif");
        }
        changerStatut(produit, StatutProduit.INACTIF);
        enregistrerActivite(TypeActivite.PRODUIT_DESACTIVE,
                "Produit désactivé", produit, produit.getProprietaire());
        log.info("[PRODUIT] Désactivé — id={}", id);
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS — CHARGEMENT
    // ─────────────────────────────────────────────────────────

    /**
     * Charge un produit par son identifiant et son propriétaire.
     * Lève une exception si introuvable.
     */
    private Produit charger(Long id, Long proprietaireId) {
        return produitRepository.findByIdAndProprietaireId(id, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));
    }

    /**
     * Construit la spécification JPA pour le filtrage.
     */
    private Specification<Produit> construireSpec(Long proprietaireId,
                                                  TypeProduit type,
                                                  StatutProduit statut,
                                                  Long categorieId,
                                                  String keyword) {
        return ProduitSpecification.duProprietaire(proprietaireId)
                .and(ProduitSpecification.avecType(type))
                .and(ProduitSpecification.avecStatut(statut))
                .and(ProduitSpecification.avecCategorie(categorieId))
                .and(ProduitSpecification.recherche(keyword));
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS — CRÉATION / MODIFICATION
    // ─────────────────────────────────────────────────────────

    /**
     * Initialise une nouvelle entité Produit depuis la requête.
     */
    private Produit initialiserProduit(ProduitRequest req,
                                       ProprietaireEntreprise proprietaire) {
        Produit produit = new Produit();
        produit.setProprietaire(proprietaire);
        produit.setCodeProduit(genererCode(proprietaire.getId()));
        appliquer(req, produit, proprietaire.getId());
        if (produit.getStatut() == null) produit.setStatut(StatutProduit.ACTIF);
        return produit;
    }

    /**
     * Applique les champs de la requête sur l'entité Produit.
     * Gère la logique SERVICE vs STOCKABLE pour le stock.
     */
    private void appliquer(ProduitRequest req, Produit produit, Long proprietaireId) {
        produit.setNom(req.getNom());
        produit.setDescription(req.getDescription());
        produit.setType(req.getType());
        produit.setPrixHT(req.getPrixHT());
        produit.setTauxTVA(req.getTauxTVA());
        produit.setUnite(req.getUnite());
        if (req.getStatut() != null) produit.setStatut(req.getStatut());
        appliquerStock(req, produit);
        appliquerCategorie(req, produit, proprietaireId);
    }

    /**
     * Applique les champs stock selon le type du produit.
     * SERVICE → stockDisponible et stockMinimum sont null.
     * STOCKABLE → valorisés depuis la requête.
     */
    private void appliquerStock(ProduitRequest req, Produit produit) {
        boolean estStockable = TypeProduit.STOCKABLE.equals(req.getType());
        produit.setStockDisponible(estStockable
                ? (req.getStockDisponible() != null ? req.getStockDisponible() : 0)
                : null);
        produit.setStockMinimum(estStockable ? req.getStockMinimum() : null);
    }

    /**
     * Applique la catégorie si fournie, sinon null.
     */
    private void appliquerCategorie(ProduitRequest req, Produit produit,
                                    Long proprietaireId) {
        if (req.getCategorieId() == null) {
            produit.setCategorie(null);
            return;
        }
        Categorie categorie = categorieRepository
                .findByIdAndProprietaireId(req.getCategorieId(), proprietaireId)
                .orElseThrow(() -> new BusinessException("Catégorie introuvable"));
        produit.setCategorie(categorie);
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS — STATUT
    // ─────────────────────────────────────────────────────────

    /**
     * Change le statut du produit et sauvegarde immédiatement.
     */
    private void changerStatut(Produit produit, StatutProduit nouveauStatut) {
        produit.setStatut(nouveauStatut);
        produit.setDateModification(LocalDateTime.now());
        produitRepository.save(produit);
    }

    /**
     * Vérifie que le produit n'est pas archivé avant une action.
     * Lève une BusinessException si archivé.
     */
    private void verifierNonArchive(Produit produit, String action) {
        if (StatutProduit.ARCHIVE.equals(produit.getStatut())) {
            throw new BusinessException(
                    "Impossible de " + action + " un produit archivé.");
        }
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS — VALIDATION
    // ─────────────────────────────────────────────────────────

    /**
     * Vérifie que le nom n'est pas déjà utilisé par un autre produit.
     * nomActuel null → création (tout doublon est refusé).
     * nomActuel non null → modification (autorisé si même nom).
     */
    private void verifierUniciteNom(String nomNouv, String nomActuel,
                                    Long proprietaireId) {
        if (nomNouv.equalsIgnoreCase(nomActuel)) return;
        if (produitRepository.existsByNomIgnoreCaseAndProprietaireId(
                nomNouv, proprietaireId)) {
            throw new BusinessException("Un produit avec ce nom existe déjà");
        }
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS — MAPPING
    // ─────────────────────────────────────────────────────────

    /**
     * Mappe un Produit vers son DTO de réponse.
     * Calcule le champ enAlerte côté service (non géré par MapStruct).
     */
    private ProduitResponse toResponse(Produit produit) {
        ProduitResponse response = produitMapper.toResponse(produit);
        response.setStockMinimum(produit.getStockMinimum());
        response.setEnAlerte(calculerAlerte(produit));
        return response;
    }

    /**
     * Retourne true si le produit STOCKABLE est en alerte de stock.
     * Un produit SERVICE ne déclenche jamais d'alerte.
     */
    private boolean calculerAlerte(Produit produit) {
        if (!TypeProduit.STOCKABLE.equals(produit.getType())) return false;
        if (produit.getStockDisponible() == null) return false;
        if (produit.getStockDisponible() == 0) return true;
        if (produit.getStockMinimum() == null) return false;
        return produit.getStockDisponible() <= produit.getStockMinimum();
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS — ACTIVITÉ
    // ─────────────────────────────────────────────────────────

    /**
     * Enregistre une activité dans le journal de bord.
     */
    private void enregistrerActivite(TypeActivite type, String titre,
                                     Produit produit,
                                     ProprietaireEntreprise proprietaire) {
        activiteService.enregistrer(
                type, titre, produit.getNom(),
                produit.getId(), "PRODUIT", null, proprietaire);
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS — GÉNÉRATION CODE
    // ─────────────────────────────────────────────────────────

    /**
     * Génère un code séquentiel au format PRD-YYYY-NNNN.
     * Utilise le dernier code de l'année courante pour incrémenter.
     */
    private String genererCode(Long proprietaireId) {
        String annee   = String.valueOf(Year.now().getValue());
        String prefixe = "PRD-" + annee + "-";
        int seq = produitRepository
                .findTopByProprietaireIdAndCodeProduitStartingWithOrderByCodeProduitDesc(
                        proprietaireId, prefixe)
                .map(p -> extraireSequence(p.getCodeProduit()) + 1)
                .orElse(1);
        return String.format("PRD-%s-%04d", annee, seq);
    }

    /**
     * Extrait le numéro de séquence depuis un code produit.
     * Exemple : "PRD-2026-0042" → 42.
     */
    private int extraireSequence(String code) {
        return Integer.parseInt(code.substring(code.length() - 4));
    }
}