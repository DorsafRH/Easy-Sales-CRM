package com.crm.modules.reporting.service;

import com.crm.modules.reporting.entity.Activite;
import com.crm.modules.reporting.repository.ActiviteRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.TypeActivite;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link ActiviteService} — enregistrement de la traçabilité CRM
 * (best-effort : une erreur de persistance ne doit jamais remonter).
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class ActiviteServiceTest {

    @Mock private ActiviteRepository activiteRepository;

    @InjectMocks
    private ActiviteService activiteService;

    @Test
    @DisplayName("enregistre une activité avec les bons champs")
    void enregistrer_champs() {
        ProprietaireEntreprise proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);

        activiteService.enregistrer(TypeActivite.LEAD_CREE, "Nouveau lead", "Ali",
                50L, "LEAD", null, proprietaire);

        ArgumentCaptor<Activite> captor = ArgumentCaptor.forClass(Activite.class);
        verify(activiteRepository).save(captor.capture());
        Activite a = captor.getValue();
        assertThat(a.getType()).isEqualTo(TypeActivite.LEAD_CREE);
        assertThat(a.getTitre()).isEqualTo("Nouveau lead");
        assertThat(a.getEntiteId()).isEqualTo(50L);
        assertThat(a.getEntiteType()).isEqualTo("LEAD");
        assertThat(a.getProprietaire()).isSameAs(proprietaire);
    }

    @Test
    @DisplayName("une erreur de persistance est avalée (best-effort)")
    void enregistrer_erreurAvalee() {
        when(activiteRepository.save(any(Activite.class))).thenThrow(new RuntimeException("DB down"));

        assertThatCode(() -> activiteService.enregistrer(
                TypeActivite.LEAD_CREE, "t", "d", 1L, "LEAD", null, new ProprietaireEntreprise()))
                .doesNotThrowAnyException();
    }
}
