package com.crm.modules.catalogue.service;

import com.crm.modules.catalogue.dto.CategorieRequest;
import com.crm.modules.catalogue.dto.CategorieResponse;
import com.crm.modules.catalogue.entity.Categorie;
import com.crm.modules.catalogue.entity.Produit;
import com.crm.modules.catalogue.mapper.CategorieMapper;
import com.crm.modules.catalogue.repository.CategorieRepository;
import com.crm.modules.catalogue.repository.ProduitRepository;
import com.crm.modules.catalogue.specification.CategorieSpecification;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutProduit;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implémentation du service de gestion des catégories du catalogue.
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategorieService implements ICategorieService {

    private final CategorieRepository categorieRepository;
    private final ProduitRepository   produitRepository;
    private final CategorieMapper     categorieMapper;

    // ─────────────────────────────────────────────────────────
    //  LECTURE
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public List<CategorieResponse> listerCategories(Long proprietaireId, String keyword) {
        Specification<Categorie> spec =
                CategorieSpecification.duProprietaire(proprietaireId)
                        .and(CategorieSpecification.recherche(keyword));

        return categorieRepository.findAll(spec)
                .stream()
                .map(c -> enrichir(categorieMapper.toResponse(c), c.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public CategorieResponse obtenirCategorie(Long id, Long proprietaireId) {
        Categorie cat = charger(id, proprietaireId);
        return enrichir(categorieMapper.toResponse(cat), id);
    }

    // ─────────────────────────────────────────────────────────
    //  ÉCRITURE
    // ─────────────────────────────────────────────────────────

    @Override
    public CategorieResponse creerCategorie(CategorieRequest req,
                                            ProprietaireEntreprise proprietaire) {
        if (categorieRepository.existsByNomIgnoreCaseAndProprietaireId(
                req.getNom(), proprietaire.getId())) {
            throw new BusinessException("Une catégorie avec ce nom existe déjà");
        }
        Categorie cat = Categorie.builder()
                .nom(req.getNom())
                .description(req.getDescription())
                .proprietaire(proprietaire)
                .build();
        cat = categorieRepository.save(cat);
        log.info("[CATEGORIE] Créée — id={}", cat.getId());
        return enrichir(categorieMapper.toResponse(cat), cat.getId());
    }

    @Override
    public CategorieResponse modifierCategorie(Long id, CategorieRequest req,
                                               Long proprietaireId) {
        Categorie cat = charger(id, proprietaireId);
        if (!cat.getNom().equalsIgnoreCase(req.getNom())
                && categorieRepository.existsByNomIgnoreCaseAndProprietaireId(
                req.getNom(), proprietaireId)) {
            throw new BusinessException("Une catégorie avec ce nom existe déjà");
        }
        cat.setNom(req.getNom());
        cat.setDescription(req.getDescription());
        return enrichir(categorieMapper.toResponse(categorieRepository.save(cat)), id);
    }

    // ─────────────────────────────────────────────────────────
    //  SUPPRESSION
    // ─────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * Lève {@link BusinessException} si la catégorie contient des produits actifs.
     * Le client doit d'abord appeler {@link #desactiverProduitsCategorie} ou
     * {@link #retirerCategorieProduits} avant de supprimer.
     */
    @Override
    public void supprimerCategorie(Long id, Long proprietaireId) {
        Categorie cat = charger(id, proprietaireId);
        long nbActifs = produitRepository.countByCategorieIdAndStatut(
                id, StatutProduit.ACTIF);
        if (nbActifs > 0) {
            throw new BusinessException(
                    "Cette catégorie contient " + nbActifs + " produit(s) actif(s). "
                            + "Désactivez-les ou retirez leur catégorie avant de supprimer.");
        }
        categorieRepository.delete(cat);
        log.info("[CATEGORIE] Supprimée — id={}", id);
    }

    /**
     * {@inheritDoc}
     *
     * Passe tous les produits ACTIF de la catégorie au statut INACTIF
     * ET retire leur référence à la catégorie (categorieId → null).
     *
     * POURQUOI retirer aussi la référence ?
     * La contrainte de clé étrangère FK en base de données interdit de
     * supprimer une catégorie tant que des produits y font référence,
     * même si ces produits sont INACTIF. Il faut donc couper la FK
     * en plus de changer le statut.
     */
    @Override
    public void desactiverProduitsCategorie(Long categorieId, Long proprietaireId) {
        // Vérifie que la catégorie appartient bien au propriétaire (sécurité multi-tenant)
        charger(categorieId, proprietaireId);

        List<Produit> produits = produitRepository
                .findByCategorieIdAndProprietaireId(categorieId, proprietaireId);

        produits.forEach(p -> {
            // Désactive le produit s'il est ACTIF
            if (StatutProduit.ACTIF.equals(p.getStatut())) {
                p.setStatut(StatutProduit.INACTIF);
            }
            // Retire la référence à la catégorie pour libérer la contrainte FK
            p.setCategorie(null);
            p.setDateModification(LocalDateTime.now());
        });

        if (!produits.isEmpty()) {
            produitRepository.saveAll(produits);
        }

        log.info("[CATEGORIE] {} produit(s) désactivé(s) et détachés — categorieId={}",
                produits.size(), categorieId);
    }

    /**
     * {@inheritDoc}
     *
     * Retire la catégorie (categorieId → null) de tous les produits.
     * Les produits restent dans leur statut actuel (ACTIF, INACTIF ou ARCHIVE).
     */
    @Override
    public void retirerCategorieProduits(Long categorieId, Long proprietaireId) {
        // Vérifie que la catégorie appartient bien au propriétaire (sécurité multi-tenant)
        charger(categorieId, proprietaireId);

        List<Produit> produits = produitRepository
                .findByCategorieIdAndProprietaireId(categorieId, proprietaireId);

        produits.forEach(p -> {
            p.setCategorie(null);
            p.setDateModification(LocalDateTime.now());
        });

        if (!produits.isEmpty()) {
            produitRepository.saveAll(produits);
        }

        log.info("[CATEGORIE] Catégorie retirée de {} produit(s) — categorieId={}",
                produits.size(), categorieId);
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────

    private Categorie charger(Long id, Long proprietaireId) {
        return categorieRepository.findByIdAndProprietaireId(id, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
    }

    private CategorieResponse enrichir(CategorieResponse response, Long categorieId) {
        response.setNbProduits(
                produitRepository.countByCategorieIdAndStatut(
                        categorieId, StatutProduit.ACTIF));
        return response;
    }
}