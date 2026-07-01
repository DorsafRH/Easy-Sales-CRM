package com.crm.modules.vente.service;

import com.crm.modules.catalogue.repository.ProduitRepository;
import com.crm.modules.client.entity.Client;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.reporting.service.IActiviteService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.modules.vente.dto.DevisResponse;
import com.crm.modules.vente.dto.FactureResponse;
import com.crm.modules.vente.entity.Devis;
import com.crm.modules.vente.entity.Facture;
import com.crm.modules.vente.mapper.DevisMapper;
import com.crm.modules.vente.mapper.FactureMapper;
import com.crm.modules.vente.repository.DevisRepository;
import com.crm.modules.vente.repository.FactureRepository;
import com.crm.modules.vente.repository.OpportuniteRepository;
import com.crm.shared.enums.StatutDevis;
import com.crm.shared.enums.StatutFacture;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link DevisService} — transitions de statut valides/invalides,
 * conversion devis → facture (uniquement si accepté, pas de doublon), suppression.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class DevisServiceTest {

    @Mock private DevisRepository devisRepository;
    @Mock private FactureRepository factureRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private OpportuniteRepository opportuniteRepository;
    @Mock private ProduitRepository produitRepository;
    @Mock private ProprietaireRepository proprietaireRepository;
    @Mock private DevisMapper devisMapper;
    @Mock private FactureMapper factureMapper;
    @Mock private IActiviteService activiteService;

    @InjectMocks
    private DevisService devisService;

    private ProprietaireEntreprise proprietaire;

    @BeforeEach
    void setUp() {
        proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);
    }

    private Devis devis(StatutDevis statut) {
        return Devis.builder()
                .id(40L)
                .numero("DV-2026-0001")
                .statut(statut)
                .client(mock(Client.class))
                .proprietaire(proprietaire)
                .lignes(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("changerStatut")
    class ChangerStatut {

        @Test
        @DisplayName("BROUILLON → ENVOYE : transition valide")
        void brouillonVersEnvoye_ok() {
            when(devisRepository.findByIdAndProprietaireId(40L, 1L)).thenReturn(Optional.of(devis(StatutDevis.BROUILLON)));
            when(devisRepository.save(any(Devis.class))).thenAnswer(i -> i.getArgument(0));
            when(devisMapper.toResponse(any(Devis.class))).thenReturn(mock(DevisResponse.class));
            when(factureRepository.existsByDevisOrigineId(40L)).thenReturn(false);

            devisService.changerStatut(40L, StatutDevis.ENVOYE, 1L);

            ArgumentCaptor<Devis> captor = ArgumentCaptor.forClass(Devis.class);
            verify(devisRepository).save(captor.capture());
            assertThat(captor.getValue().getStatut()).isEqualTo(StatutDevis.ENVOYE);
        }

        @Test
        @DisplayName("BROUILLON → ACCEPTE : transition invalide")
        void brouillonVersAccepte_leve() {
            when(devisRepository.findByIdAndProprietaireId(40L, 1L)).thenReturn(Optional.of(devis(StatutDevis.BROUILLON)));

            assertThatThrownBy(() -> devisService.changerStatut(40L, StatutDevis.ACCEPTE, 1L))
                    .isInstanceOf(BusinessException.class);
            verify(devisRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("convertirEnFacture")
    class ConvertirEnFacture {

        @Test
        @DisplayName("devis non accepté → BusinessException")
        void nonAccepte_leve() {
            when(devisRepository.findByIdAndProprietaireId(40L, 1L)).thenReturn(Optional.of(devis(StatutDevis.BROUILLON)));

            assertThatThrownBy(() -> devisService.convertirEnFacture(40L, 1L))
                    .isInstanceOf(BusinessException.class);
            verify(factureRepository, never()).save(any());
        }

        @Test
        @DisplayName("devis déjà converti → BusinessException")
        void dejaConverti_leve() {
            when(devisRepository.findByIdAndProprietaireId(40L, 1L)).thenReturn(Optional.of(devis(StatutDevis.ACCEPTE)));
            when(factureRepository.existsByDevisOrigineId(40L)).thenReturn(true);

            assertThatThrownBy(() -> devisService.convertirEnFacture(40L, 1L))
                    .isInstanceOf(BusinessException.class);
            verify(factureRepository, never()).save(any());
        }

        @Test
        @DisplayName("devis accepté → crée une facture BROUILLON numérotée FA-…")
        void accepte_creeFacture() {
            when(devisRepository.findByIdAndProprietaireId(40L, 1L)).thenReturn(Optional.of(devis(StatutDevis.ACCEPTE)));
            when(factureRepository.existsByDevisOrigineId(40L)).thenReturn(false);
            when(factureRepository.findTopByProprietaireIdAndNumeroStartingWithOrderByNumeroDesc(eq(1L), anyString()))
                    .thenReturn(Optional.empty());
            when(factureRepository.save(any(Facture.class))).thenAnswer(i -> i.getArgument(0));
            when(factureMapper.toResponse(any(Facture.class))).thenReturn(mock(FactureResponse.class));

            devisService.convertirEnFacture(40L, 1L);

            ArgumentCaptor<Facture> captor = ArgumentCaptor.forClass(Facture.class);
            verify(factureRepository).save(captor.capture());
            assertThat(captor.getValue().getStatut()).isEqualTo(StatutFacture.BROUILLON);
            assertThat(captor.getValue().getNumero()).startsWith("FA-");
        }
    }

    @Nested
    @DisplayName("supprimer / obtenir")
    class SupprimerObtenir {

        @Test
        @DisplayName("supprimer un devis non brouillon → BusinessException")
        void supprimer_nonBrouillon_leve() {
            when(devisRepository.findByIdAndProprietaireId(40L, 1L)).thenReturn(Optional.of(devis(StatutDevis.ENVOYE)));

            assertThatThrownBy(() -> devisService.supprimer(40L, 1L))
                    .isInstanceOf(BusinessException.class);
            verify(devisRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("supprimer un brouillon → OK")
        void supprimer_brouillon_ok() {
            when(devisRepository.findByIdAndProprietaireId(40L, 1L)).thenReturn(Optional.of(devis(StatutDevis.BROUILLON)));

            devisService.supprimer(40L, 1L);

            verify(devisRepository).deleteById(40L);
        }

        @Test
        @DisplayName("obtenir : devis introuvable → ResourceNotFoundException")
        void obtenir_introuvable() {
            when(devisRepository.findByIdAndProprietaireId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> devisService.obtenir(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
