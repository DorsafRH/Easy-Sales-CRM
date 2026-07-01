package com.crm.modules.agenda.service;

import com.crm.modules.agenda.entity.Reunion;
import com.crm.modules.agenda.mapper.ReunionMapper;
import com.crm.modules.agenda.repository.ReunionRepository;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.shared.enums.StatutReunion;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link ReunionService} — transitions du cycle de vie d'une réunion
 * (terminer / annuler) et multi-tenant.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class ReunionServiceTest {

    @Mock private ReunionRepository reunionRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ProprietaireRepository proprietaireRepository;
    @Mock private ReunionMapper reunionMapper;
    @Mock private ReunionEmailService reunionEmailService;

    @InjectMocks
    private ReunionService reunionService;

    private ProprietaireEntreprise proprietaire;

    @BeforeEach
    void setUp() {
        proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);
    }

    private Reunion reunion(StatutReunion statut) {
        return Reunion.builder().id(70L).statut(statut).proprietaire(proprietaire).build();
    }

    @Test
    @DisplayName("terminer une réunion planifiée → statut TERMINEE")
    void terminer_ok() {
        when(reunionRepository.findByIdAndProprietaireId(70L, 1L)).thenReturn(Optional.of(reunion(StatutReunion.PLANIFIEE)));

        reunionService.terminer(70L, 1L);

        ArgumentCaptor<Reunion> captor = ArgumentCaptor.forClass(Reunion.class);
        verify(reunionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatut()).isEqualTo(StatutReunion.TERMINEE);
    }

    @Test
    @DisplayName("terminer une réunion déjà terminée → BusinessException")
    void terminer_dejaTerminee_leve() {
        when(reunionRepository.findByIdAndProprietaireId(70L, 1L)).thenReturn(Optional.of(reunion(StatutReunion.TERMINEE)));

        assertThatThrownBy(() -> reunionService.terminer(70L, 1L))
                .isInstanceOf(BusinessException.class);
        verify(reunionRepository, never()).save(any());
    }

    @Test
    @DisplayName("annuler une réunion planifiée → statut ANNULEE")
    void annuler_ok() {
        when(reunionRepository.findByIdAndProprietaireId(70L, 1L)).thenReturn(Optional.of(reunion(StatutReunion.PLANIFIEE)));

        reunionService.annuler(70L, 1L);

        ArgumentCaptor<Reunion> captor = ArgumentCaptor.forClass(Reunion.class);
        verify(reunionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatut()).isEqualTo(StatutReunion.ANNULEE);
    }

    @Test
    @DisplayName("annuler une réunion terminée → BusinessException")
    void annuler_terminee_leve() {
        when(reunionRepository.findByIdAndProprietaireId(70L, 1L)).thenReturn(Optional.of(reunion(StatutReunion.TERMINEE)));

        assertThatThrownBy(() -> reunionService.annuler(70L, 1L))
                .isInstanceOf(BusinessException.class);
        verify(reunionRepository, never()).save(any());
    }

    @Test
    @DisplayName("obtenir : réunion introuvable → ResourceNotFoundException")
    void obtenir_introuvable() {
        when(reunionRepository.findByIdAndProprietaireId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reunionService.obtenir(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
