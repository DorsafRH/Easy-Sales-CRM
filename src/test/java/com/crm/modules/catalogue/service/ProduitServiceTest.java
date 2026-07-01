package com.crm.modules.catalogue.service;

import com.crm.modules.catalogue.dto.ProduitRequest;
import com.crm.modules.catalogue.dto.ProduitResponse;
import com.crm.modules.catalogue.entity.Produit;
import com.crm.modules.catalogue.mapper.ProduitMapper;
import com.crm.modules.catalogue.repository.CategorieRepository;
import com.crm.modules.catalogue.repository.ProduitRepository;
import com.crm.modules.reporting.service.IActiviteService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutProduit;
import com.crm.shared.enums.TypeProduit;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link ProduitService}.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class ProduitServiceTest {

    @Mock private ProduitRepository   produitRepository;
    @Mock private CategorieRepository categorieRepository;
    @Mock private ProduitMapper       produitMapper;
    @Mock private IActiviteService    activiteService;

    @InjectMocks
    private ProduitService produitService;

    private ProprietaireEntreprise proprietaire;
    private Produit                produit;
    private ProduitResponse        produitResponse;
    private ProduitRequest         request;

    @BeforeEach
    void setUp() {
        proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);

        produit = new Produit();
        produit.setId(20L);
        produit.setCodeProduit("PRD-2026-0001");
        produit.setNom("Laptop Pro");
        produit.setType(TypeProduit.STOCKABLE);
        produit.setPrixHT(new BigDecimal("1200.000"));
        produit.setStatut(StatutProduit.ACTIF);
        produit.setProprietaire(proprietaire);

        produitResponse = new ProduitResponse();
        produitResponse.setId(20L);
        produitResponse.setCodeProduit("PRD-2026-0001");
        produitResponse.setNom("Laptop Pro");
        produitResponse.setStatut(StatutProduit.ACTIF);

        request = new ProduitRequest();
        request.setNom("Laptop Pro");
        request.setType(TypeProduit.STOCKABLE);
        request.setPrixHT(new BigDecimal("1200.000"));
        request.setStockDisponible(10);
    }

    // ── listerProduits ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("listerProduits")
    class ListerProduits {

        @Test
        @DisplayName("Sans filtres — retourne tous les produits du propriétaire")
        void sansFiltres_retourneListe() {
            when(produitRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(produit));
            when(produitMapper.toResponse(produit)).thenReturn(produitResponse);

            List<ProduitResponse> result = produitService.listerProduits(
                    1L, null, null, null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Avec tous les filtres — délègue à la specification")
        void avecTousFiltres_delegueSpecification() {
            when(produitRepository.findAll(any(Specification.class))).thenReturn(List.of());

            List<ProduitResponse> result = produitService.listerProduits(
                    1L, TypeProduit.SERVICE, StatutProduit.ACTIF, 5L, "consul");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Aucun produit — retourne liste vide")
        void aucunProduit_retourneListeVide() {
            when(produitRepository.findAll(any(Specification.class))).thenReturn(List.of());

            assertThat(produitService.listerProduits(1L, null, null, null, null)).isEmpty();
        }
    }

    // ── obtenirProduit ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenirProduit")
    class ObtenirProduit {

        @Test
        @DisplayName("Produit trouvé — retourne la réponse mappée")
        void trouve_retourneReponse() {
            when(produitRepository.findByIdAndProprietaireId(20L, 1L))
                    .thenReturn(Optional.of(produit));
            when(produitMapper.toResponse(produit)).thenReturn(produitResponse);

            ProduitResponse result = produitService.obtenirProduit(20L, 1L);

            assertThat(result.getCodeProduit()).isEqualTo("PRD-2026-0001");
        }

        @Test
        @DisplayName("Produit introuvable — lève ResourceNotFoundException")
        void introuvable_leveException() {
            when(produitRepository.findByIdAndProprietaireId(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> produitService.obtenirProduit(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── creerProduit ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("creerProduit")
    class CreerProduit {

        @Test
        @DisplayName("STOCKABLE — stock valorisé et code généré")
        void stockable_stockValoriseEtCodeGenere() {
            when(produitRepository.existsByNomIgnoreCaseAndProprietaireId("Laptop Pro", 1L))
                    .thenReturn(false);
            when(produitRepository
                    .findTopByProprietaireIdAndCodeProduitStartingWithOrderByCodeProduitDesc(
                            eq(1L), anyString()))
                    .thenReturn(Optional.empty());
            when(produitRepository.save(any(Produit.class))).thenReturn(produit);
            when(produitMapper.toResponse(produit)).thenReturn(produitResponse);

            produitService.creerProduit(request, proprietaire);

            verify(produitRepository).save(argThat(p ->
                    p.getStockDisponible() != null
                            && p.getStockDisponible() == 10
                            && p.getCodeProduit() != null));
        }

        @Test
        @DisplayName("STOCKABLE sans stock renseigné — stock défaut à 0")
        void stockable_sansStock_defautZero() {
            request.setStockDisponible(null);

            when(produitRepository.existsByNomIgnoreCaseAndProprietaireId(any(), any()))
                    .thenReturn(false);
            when(produitRepository
                    .findTopByProprietaireIdAndCodeProduitStartingWithOrderByCodeProduitDesc(
                            any(), any()))
                    .thenReturn(Optional.empty());
            when(produitRepository.save(any(Produit.class))).thenReturn(produit);
            when(produitMapper.toResponse(any())).thenReturn(produitResponse);

            produitService.creerProduit(request, proprietaire);

            verify(produitRepository).save(argThat(p -> p.getStockDisponible() == 0));
        }

        @Test
        @DisplayName("SERVICE — stockDisponible doit être null")
        void service_stockNull() {
            request.setNom("Consultation");
            request.setType(TypeProduit.SERVICE);
            request.setStockDisponible(5);

            Produit produitEntite = new Produit();
            produitEntite.setId(21L);
            produitEntite.setStatut(StatutProduit.ACTIF);
            produitEntite.setProprietaire(proprietaire);

            when(produitRepository.existsByNomIgnoreCaseAndProprietaireId("Consultation", 1L))
                    .thenReturn(false);
            when(produitRepository
                    .findTopByProprietaireIdAndCodeProduitStartingWithOrderByCodeProduitDesc(
                            any(), any()))
                    .thenReturn(Optional.empty());
            when(produitRepository.save(any(Produit.class))).thenReturn(produitEntite);
            when(produitMapper.toResponse(any())).thenReturn(new ProduitResponse());

            produitService.creerProduit(request, proprietaire);

            verify(produitRepository).save(argThat(p -> p.getStockDisponible() == null));
        }

        @Test
        @DisplayName("Premier produit de l'année — code se termine par 0001")
        void premierDeAnnee_code0001() {
            when(produitRepository.existsByNomIgnoreCaseAndProprietaireId(any(), any()))
                    .thenReturn(false);
            when(produitRepository
                    .findTopByProprietaireIdAndCodeProduitStartingWithOrderByCodeProduitDesc(
                            eq(1L), anyString()))
                    .thenReturn(Optional.empty());
            when(produitRepository.save(any(Produit.class))).thenReturn(produit);
            when(produitMapper.toResponse(any())).thenReturn(produitResponse);

            produitService.creerProduit(request, proprietaire);

            verify(produitRepository).save(argThat(p ->
                    p.getCodeProduit() != null && p.getCodeProduit().endsWith("0001")));
        }

        @Test
        @DisplayName("Code séquentiel — incrémente depuis le dernier code existant")
        void codeSequentiel_incremente() {
            Produit dernierProduit = new Produit();
            dernierProduit.setCodeProduit("PRD-2026-0005");

            when(produitRepository.existsByNomIgnoreCaseAndProprietaireId(any(), any()))
                    .thenReturn(false);
            when(produitRepository
                    .findTopByProprietaireIdAndCodeProduitStartingWithOrderByCodeProduitDesc(
                            eq(1L), anyString()))
                    .thenReturn(Optional.of(dernierProduit));
            when(produitRepository.save(any(Produit.class))).thenReturn(produit);
            when(produitMapper.toResponse(any())).thenReturn(produitResponse);

            produitService.creerProduit(request, proprietaire);

            verify(produitRepository).save(argThat(p ->
                    p.getCodeProduit() != null && p.getCodeProduit().endsWith("0006")));
        }

        @Test
        @DisplayName("Nom en double — lève BusinessException, aucun save")
        void nomEnDouble_leveException() {
            when(produitRepository.existsByNomIgnoreCaseAndProprietaireId("Laptop Pro", 1L))
                    .thenReturn(true);

            assertThatThrownBy(() -> produitService.creerProduit(request, proprietaire))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("nom existe déjà");

            verify(produitRepository, never()).save(any());
        }

        @Test
        @DisplayName("Statut null — défaut à ACTIF")
        void statutNull_defautActif() {
            request.setStatut(null);

            when(produitRepository.existsByNomIgnoreCaseAndProprietaireId(any(), any()))
                    .thenReturn(false);
            when(produitRepository
                    .findTopByProprietaireIdAndCodeProduitStartingWithOrderByCodeProduitDesc(
                            any(), any()))
                    .thenReturn(Optional.empty());
            when(produitRepository.save(any(Produit.class))).thenReturn(produit);
            when(produitMapper.toResponse(any())).thenReturn(produitResponse);

            produitService.creerProduit(request, proprietaire);

            verify(produitRepository).save(argThat(p ->
                    p.getStatut() == StatutProduit.ACTIF));
        }
    }

    // ── modifierProduit ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("modifierProduit")
    class ModifierProduit {

        @Test
        @DisplayName("Modification avec nouveau nom unique — succès")
        void nouveauNomUnique_succes() {
            ProduitRequest req = new ProduitRequest();
            req.setNom("Laptop Ultra");
            req.setType(TypeProduit.STOCKABLE);
            req.setPrixHT(new BigDecimal("1500.000"));

            when(produitRepository.findByIdAndProprietaireId(20L, 1L))
                    .thenReturn(Optional.of(produit));
            when(produitRepository.existsByNomIgnoreCaseAndProprietaireId("Laptop Ultra", 1L))
                    .thenReturn(false);
            when(produitRepository.save(produit)).thenReturn(produit);
            when(produitMapper.toResponse(produit)).thenReturn(produitResponse);

            produitService.modifierProduit(20L, req, 1L);

            assertThat(produit.getNom()).isEqualTo("Laptop Ultra");
            assertThat(produit.getDateModification()).isNotNull();
        }

        @Test
        @DisplayName("Même nom — aucune vérification de doublon")
        void memeNom_pasDeVerificationDoublon() {
            ProduitRequest req = new ProduitRequest();
            req.setNom("Laptop Pro");
            req.setType(TypeProduit.STOCKABLE);
            req.setPrixHT(new BigDecimal("1300.000"));

            when(produitRepository.findByIdAndProprietaireId(20L, 1L))
                    .thenReturn(Optional.of(produit));
            when(produitRepository.save(produit)).thenReturn(produit);
            when(produitMapper.toResponse(produit)).thenReturn(produitResponse);

            produitService.modifierProduit(20L, req, 1L);

            verify(produitRepository, never())
                    .existsByNomIgnoreCaseAndProprietaireId(any(), any());
        }

        @Test
        @DisplayName("Nouveau nom en double — lève BusinessException")
        void nouveauNomEnDouble_leveException() {
            ProduitRequest req = new ProduitRequest();
            req.setNom("Laptop Ultra");
            req.setType(TypeProduit.STOCKABLE);
            req.setPrixHT(BigDecimal.TEN);

            when(produitRepository.findByIdAndProprietaireId(20L, 1L))
                    .thenReturn(Optional.of(produit));
            when(produitRepository.existsByNomIgnoreCaseAndProprietaireId("Laptop Ultra", 1L))
                    .thenReturn(true);

            assertThatThrownBy(() -> produitService.modifierProduit(20L, req, 1L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Produit introuvable — lève ResourceNotFoundException")
        void introuvable_leveException() {
            when(produitRepository.findByIdAndProprietaireId(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> produitService.modifierProduit(99L, request, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── archiverProduit ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("archiverProduit")
    class ArchiverProduit {

        @Test
        @DisplayName("Archivage — statut devient ARCHIVE et dateModification valorisée")
        void archivage_statutEtDate() {
            when(produitRepository.findByIdAndProprietaireId(20L, 1L))
                    .thenReturn(Optional.of(produit));
            when(produitRepository.save(produit)).thenReturn(produit);

            produitService.archiverProduit(20L, 1L);

            assertThat(produit.getStatut()).isEqualTo(StatutProduit.ARCHIVE);
            assertThat(produit.getDateModification()).isNotNull();
        }

        @Test
        @DisplayName("Produit introuvable — lève ResourceNotFoundException")
        void introuvable_leveException() {
            when(produitRepository.findByIdAndProprietaireId(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> produitService.archiverProduit(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}