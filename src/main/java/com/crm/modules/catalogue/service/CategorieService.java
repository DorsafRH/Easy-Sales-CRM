package com.crm.modules.catalogue.service;

import com.crm.modules.catalogue.dto.CategorieRequest;
import com.crm.modules.catalogue.dto.CategorieResponse;
import com.crm.modules.catalogue.entity.Categorie;
import com.crm.modules.catalogue.mapper.CatalogueMapper;
import com.crm.modules.catalogue.repository.CategorieRepository;
import com.crm.modules.catalogue.repository.ProduitRepository;
import com.crm.modules.catalogue.specification.CatalogueSpecification;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutProduit;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class CatalogueService implements ICatalogueService {

    private final CategorieRepository categorieRepository;
    private final ProduitRepository   produitRepository;
    private final CatalogueMapper     catalogueMapper;

    @Transactional(readOnly = true)
    @Override
    public List<CategorieResponse> listerCategories(Long proprietaireId, String keyword) {
        Specification<Categorie> spec =
                CatalogueSpecification.duProprietaire(proprietaireId)
                        .and(CatalogueSpecification.recherche(keyword));

        return categorieRepository.findAll(spec)
                .stream()
                .map(c -> enrichir(catalogueMapper.toResponse(c), c.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public CategorieResponse obtenirCategorie(Long id, Long proprietaireId) {
        Categorie cat = charger(id, proprietaireId);
        return enrichir(catalogueMapper.toResponse(cat), id);
    }

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
        log.info("[CATALOGUE] Catégorie créée — id={}", cat.getId());
        return enrichir(catalogueMapper.toResponse(cat), cat.getId());
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
        return enrichir(catalogueMapper.toResponse(categorieRepository.save(cat)), id);
    }

    @Override
    public void supprimerCategorie(Long id, Long proprietaireId) {
        Categorie cat = charger(id, proprietaireId);
        if (produitRepository.countByCategorieIdAndStatut(id, StatutProduit.ACTIF) > 0) {
            throw new BusinessException(
                    "Impossible de supprimer une catégorie contenant des produits actifs");
        }
        categorieRepository.delete(cat);
        log.info("[CATALOGUE] Catégorie supprimée — id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────────────────────

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