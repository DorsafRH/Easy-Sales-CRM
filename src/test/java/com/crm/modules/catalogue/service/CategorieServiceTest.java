package com.crm.modules.catalogue.service;

import com.crm.modules.catalogue.dto.CategorieRequest;
import com.crm.modules.catalogue.dto.CategorieResponse;
import com.crm.modules.catalogue.entity.Categorie;
import com.crm.modules.catalogue.mapper.CategorieMapper;
import com.crm.modules.catalogue.repository.CategorieRepository;
import com.crm.modules.catalogue.repository.ProduitRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutProduit;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link CategorieService}.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class CategorieServiceTest {

    @Mock private CategorieRepository categorieRepository;
    @Mock private ProduitRepository   produitRepository;
    @Mock private CategorieMapper     categorieMapper;

    @InjectMocks
    private CategorieService categorieService;

    private ProprietaireEntreprise proprietaire;
    private Categorie              categorie;
    private CategorieResponse      categorieResponse;
    private CategorieRequest       request;

    @BeforeEach
    void setUp() {
        proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);

        categorie = Categorie.builder()
                .id(10L).nom("Informatique")
                .description("Matériel informatique")
                .dateCreation(LocalDateTime.now())
                .proprietaire(proprietaire)
                .build();

        categorieResponse = new CategorieResponse();
        categorieResponse.setId(10L);
        categorieResponse.setNom("Informatique");

        request = new CategorieRequest();
        request.setNom("Informatique");
        request.setDescription("Matériel informatique");
    }

    // ── listerCategories ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("listerCategories")
    class ListerCategories {

        @Test
        @DisplayName("Sans keyword — retourne toutes les catégories enrichies")
        void sansKeyword_retourneListe() {
            when(categorieRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(categorie));
            when(categorieMapper.toResponse(categorie)).thenReturn(categorieResponse);
            when(produitRepository.countByCategorieIdAndStatut(10L, StatutProduit.ACTIF))
                    .thenReturn(3);

            List<CategorieResponse> result = categorieService.listerCategories(1L, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNbProduits()).isEqualTo(3);
        }

        @Test
        @DisplayName("Avec keyword — délègue à la specification (liste filtrée)")
        void avecKeyword_retourneListeFiltree() {
            when(categorieRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(categorie));
            when(categorieMapper.toResponse(categorie)).thenReturn(categorieResponse);
            when(produitRepository.countByCategorieIdAndStatut(10L, StatutProduit.ACTIF))
                    .thenReturn(0);

            List<CategorieResponse> result = categorieService.listerCategories(1L, "info");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Aucune catégorie — retourne liste vide")
        void aucuneCategorie_retourneListeVide() {
            when(categorieRepository.findAll(any(Specification.class))).thenReturn(List.of());

            assertThat(categorieService.listerCategories(1L, null)).isEmpty();
        }
    }

    // ── obtenirCategorie ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenirCategorie")
    class ObtenirCategorie {

        @Test
        @DisplayName("Catégorie trouvée — retourne la réponse enrichie")
        void trouvee_retourneReponse() {
            when(categorieRepository.findByIdAndProprietaireId(10L, 1L))
                    .thenReturn(Optional.of(categorie));
            when(categorieMapper.toResponse(categorie)).thenReturn(categorieResponse);
            when(produitRepository.countByCategorieIdAndStatut(10L, StatutProduit.ACTIF))
                    .thenReturn(2);

            CategorieResponse result = categorieService.obtenirCategorie(10L, 1L);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getNbProduits()).isEqualTo(2);
        }

        @Test
        @DisplayName("Catégorie introuvable — lève ResourceNotFoundException")
        void introuvable_leveException() {
            when(categorieRepository.findByIdAndProprietaireId(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categorieService.obtenirCategorie(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Multi-tenant — un propriétaire ne voit pas les catégories d'un autre")
        void multiTenant_accesDenied() {
            when(categorieRepository.findByIdAndProprietaireId(10L, 2L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categorieService.obtenirCategorie(10L, 2L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── creerCategorie ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("creerCategorie")
    class CreerCategorie {

        @Test
        @DisplayName("Nom unique — catégorie créée avec nbProduits = 0")
        void nomUnique_succes() {
            when(categorieRepository.existsByNomIgnoreCaseAndProprietaireId("Informatique", 1L))
                    .thenReturn(false);
            when(categorieRepository.save(any(Categorie.class))).thenReturn(categorie);
            when(categorieMapper.toResponse(categorie)).thenReturn(categorieResponse);
            when(produitRepository.countByCategorieIdAndStatut(10L, StatutProduit.ACTIF))
                    .thenReturn(0);

            CategorieResponse result = categorieService.creerCategorie(request, proprietaire);

            assertThat(result.getNom()).isEqualTo("Informatique");
            assertThat(result.getNbProduits()).isZero();
            verify(categorieRepository).save(any(Categorie.class));
        }

        @Test
        @DisplayName("Nom en double — lève BusinessException, aucun save")
        void nomEnDouble_leveException() {
            when(categorieRepository.existsByNomIgnoreCaseAndProprietaireId("Informatique", 1L))
                    .thenReturn(true);

            assertThatThrownBy(() -> categorieService.creerCategorie(request, proprietaire))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("nom existe déjà");

            verify(categorieRepository, never()).save(any());
        }

        @Test
        @DisplayName("Le propriétaire est bien associé à la catégorie créée")
        void proprietaireAssocieCorrectement() {
            when(categorieRepository.existsByNomIgnoreCaseAndProprietaireId(any(), eq(1L)))
                    .thenReturn(false);
            when(categorieRepository.save(any(Categorie.class))).thenReturn(categorie);
            when(categorieMapper.toResponse(any())).thenReturn(categorieResponse);
            when(produitRepository.countByCategorieIdAndStatut(any(), any())).thenReturn(0);

            categorieService.creerCategorie(request, proprietaire);

            verify(categorieRepository).save(argThat(c ->
                    c.getProprietaire().getId().equals(1L)));
        }
    }

    // ── modifierCategorie ────────────────────────────────────────────────────

    @Nested
    @DisplayName("modifierCategorie")
    class ModifierCategorie {

        @Test
        @DisplayName("Nouveau nom unique — catégorie modifiée")
        void nouveauNomUnique_succes() {
            CategorieRequest req = new CategorieRequest();
            req.setNom("Bureautique");
            req.setDescription("Matériel de bureau");

            when(categorieRepository.findByIdAndProprietaireId(10L, 1L))
                    .thenReturn(Optional.of(categorie));
            when(categorieRepository.existsByNomIgnoreCaseAndProprietaireId("Bureautique", 1L))
                    .thenReturn(false);
            when(categorieRepository.save(categorie)).thenReturn(categorie);
            when(categorieMapper.toResponse(categorie)).thenReturn(categorieResponse);
            when(produitRepository.countByCategorieIdAndStatut(10L, StatutProduit.ACTIF))
                    .thenReturn(0);

            categorieService.modifierCategorie(10L, req, 1L);

            assertThat(categorie.getNom()).isEqualTo("Bureautique");
            verify(categorieRepository).save(categorie);
        }

        @Test
        @DisplayName("Même nom (pas de changement) — aucune vérification de doublon")
        void memeNom_pasDeVerificationDoublon() {
            CategorieRequest req = new CategorieRequest();
            req.setNom("Informatique");
            req.setDescription("Nouvelle description");

            when(categorieRepository.findByIdAndProprietaireId(10L, 1L))
                    .thenReturn(Optional.of(categorie));
            when(categorieRepository.save(categorie)).thenReturn(categorie);
            when(categorieMapper.toResponse(categorie)).thenReturn(categorieResponse);
            when(produitRepository.countByCategorieIdAndStatut(10L, StatutProduit.ACTIF))
                    .thenReturn(0);

            categorieService.modifierCategorie(10L, req, 1L);

            verify(categorieRepository, never())
                    .existsByNomIgnoreCaseAndProprietaireId(any(), any());
        }

        @Test
        @DisplayName("Nouveau nom en double — lève BusinessException")
        void nouveauNomEnDouble_leveException() {
            CategorieRequest req = new CategorieRequest();
            req.setNom("Bureautique");

            when(categorieRepository.findByIdAndProprietaireId(10L, 1L))
                    .thenReturn(Optional.of(categorie));
            when(categorieRepository.existsByNomIgnoreCaseAndProprietaireId("Bureautique", 1L))
                    .thenReturn(true);

            assertThatThrownBy(() -> categorieService.modifierCategorie(10L, req, 1L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Catégorie introuvable — lève ResourceNotFoundException")
        void introuvable_leveException() {
            when(categorieRepository.findByIdAndProprietaireId(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categorieService.modifierCategorie(99L, request, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── supprimerCategorie ───────────────────────────────────────────────────

    @Nested
    @DisplayName("supprimerCategorie")
    class SupprimerCategorie {

        @Test
        @DisplayName("Catégorie vide — suppression réussie")
        void categorieVide_succes() {
            when(categorieRepository.findByIdAndProprietaireId(10L, 1L))
                    .thenReturn(Optional.of(categorie));
            when(produitRepository.countByCategorieIdAndStatut(10L, StatutProduit.ACTIF))
                    .thenReturn(0);

            categorieService.supprimerCategorie(10L, 1L);

            verify(categorieRepository).delete(categorie);
        }

        @Test
        @DisplayName("Catégorie avec produits actifs — lève BusinessException, aucun delete")
        void avecProduitsActifs_leveException() {
            when(categorieRepository.findByIdAndProprietaireId(10L, 1L))
                    .thenReturn(Optional.of(categorie));
            when(produitRepository.countByCategorieIdAndStatut(10L, StatutProduit.ACTIF))
                    .thenReturn(5);

            assertThatThrownBy(() -> categorieService.supprimerCategorie(10L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("produit(s) actif(s)");

            verify(categorieRepository, never()).delete(any(Categorie.class));
        }

        @Test
        @DisplayName("Catégorie introuvable — lève ResourceNotFoundException")
        void introuvable_leveException() {
            when(categorieRepository.findByIdAndProprietaireId(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categorieService.supprimerCategorie(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}