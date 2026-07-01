package com.crm.modules.vente.service;

import com.crm.modules.reporting.service.IActiviteService;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.vente.dto.FactureResponse;
import com.crm.modules.vente.entity.Facture;
import com.crm.modules.vente.mapper.FactureMapper;
import com.crm.modules.vente.repository.FactureRepository;
import com.crm.modules.vente.repository.OpportuniteRepository;
import com.crm.shared.enums.StatutFacture;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * Tests unitaires de {@link FactureService} — transitions de statut valides/invalides et
 * effets de bord (dates d'émission / paiement).
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class FactureServiceTest {

    @Mock private FactureRepository factureRepository;
    @Mock private FactureMapper factureMapper;
    @Mock private IActiviteService activiteService;
    @Mock private OpportuniteRepository opportuniteRepository;

    @InjectMocks
    private FactureService factureService;

    private ProprietaireEntreprise proprietaire;

    @BeforeEach
    void setUp() {
        proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);
    }

    private Facture facture(StatutFacture statut) {
        return Facture.builder()
                .id(60L)
                .numero("FA-2026-0001")
                .statut(statut)
                .proprietaire(proprietaire)
                .lignes(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("BROUILLON → EMISE : valide et renseigne la date d'émission")
    void brouillonVersEmise_ok() {
        when(factureRepository.findByIdAndProprietaireId(60L, 1L)).thenReturn(Optional.of(facture(StatutFacture.BROUILLON)));
        when(factureRepository.save(any(Facture.class))).thenAnswer(i -> i.getArgument(0));
        when(factureMapper.toResponse(any(Facture.class))).thenReturn(mock(FactureResponse.class));

        factureService.changerStatut(60L, StatutFacture.EMISE, 1L);

        ArgumentCaptor<Facture> captor = ArgumentCaptor.forClass(Facture.class);
        verify(factureRepository).save(captor.capture());
        assertThat(captor.getValue().getStatut()).isEqualTo(StatutFacture.EMISE);
        assertThat(captor.getValue().getDateEmission()).isNotNull();
    }

    @Test
    @DisplayName("BROUILLON → PAYEE : transition invalide")
    void brouillonVersPayee_leve() {
        when(factureRepository.findByIdAndProprietaireId(60L, 1L)).thenReturn(Optional.of(facture(StatutFacture.BROUILLON)));

        assertThatThrownBy(() -> factureService.changerStatut(60L, StatutFacture.PAYEE, 1L))
                .isInstanceOf(BusinessException.class);
        verify(factureRepository, never()).save(any());
    }

    @Test
    @DisplayName("EMISE → PAYEE : valide et renseigne la date de paiement")
    void emiseVersPayee_ok() {
        when(factureRepository.findByIdAndProprietaireId(60L, 1L)).thenReturn(Optional.of(facture(StatutFacture.EMISE)));
        when(factureRepository.save(any(Facture.class))).thenAnswer(i -> i.getArgument(0));
        when(factureMapper.toResponse(any(Facture.class))).thenReturn(mock(FactureResponse.class));

        factureService.changerStatut(60L, StatutFacture.PAYEE, 1L);

        ArgumentCaptor<Facture> captor = ArgumentCaptor.forClass(Facture.class);
        verify(factureRepository).save(captor.capture());
        assertThat(captor.getValue().getStatut()).isEqualTo(StatutFacture.PAYEE);
        assertThat(captor.getValue().getDatePaiement()).isNotNull();
    }

    @Test
    @DisplayName("obtenir : facture introuvable → ResourceNotFoundException")
    void obtenir_introuvable() {
        when(factureRepository.findByIdAndProprietaireId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factureService.obtenir(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
