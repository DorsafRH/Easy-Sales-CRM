package com.crm.modules.marketing.service;

import com.crm.modules.marketing.dto.request.LeadAutomationRequestDTO;
import com.crm.modules.marketing.entity.CompteSocialConnecte;
import com.crm.modules.marketing.repository.CompteSocialConnecteRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.vente.dto.LeadQualifieRequest;
import com.crm.modules.vente.dto.LeadResponse;
import com.crm.modules.vente.service.ILeadService;
import com.crm.shared.enums.SourceLead;
import com.crm.shared.enums.TypeReseau;
import com.crm.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du pont Marketing → Commercial : qualification d'un lead reçu de Messenger.
 * Vérifie la résolution multi-tenant (pageId → propriétaire) et la délégation au service Lead.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class MarketingAutomationServiceTest {

    @Mock private CompteSocialConnecteRepository compteSocialRepository;
    @Mock private ILeadService leadService;

    @InjectMocks
    private MarketingAutomationService automationService;

    private LeadAutomationRequestDTO requete() {
        LeadAutomationRequestDTO req = new LeadAutomationRequestDTO();
        req.setPageId("PAGE_123");
        req.setPsid("PSID_1");
        req.setNom("Dorsaf Riahi");
        req.setEmail("dorsaf@example.com");
        req.setTelephone("99221133");
        req.setSource(SourceLead.MESSENGER);
        req.setScore(80);
        req.setResume("Cherche un site web vitrine");
        return req;
    }

    @Test
    @DisplayName("Résout le propriétaire via pageId puis délègue la création du lead qualifié")
    void creerLeadDepuisMessenger_resoutOwnerEtDelegue() {
        ProprietaireEntreprise owner = new ProprietaireEntreprise();
        owner.setId(7L);
        CompteSocialConnecte compte = CompteSocialConnecte.builder()
                .identifiantExterne("PAGE_123")
                .typeReseau(TypeReseau.FACEBOOK)
                .proprietaire(owner)
                .build();

        when(compteSocialRepository
                .findFirstByIdentifiantExterneAndTypeReseau("PAGE_123", TypeReseau.FACEBOOK))
                .thenReturn(Optional.of(compte));
        LeadResponse attendu = mock(LeadResponse.class);
        when(leadService.creerQualifie(any(LeadQualifieRequest.class), eq(7L))).thenReturn(attendu);

        LeadResponse resultat = automationService.creerLeadDepuisMessenger(requete());

        assertThat(resultat).isSameAs(attendu);

        ArgumentCaptor<LeadQualifieRequest> captor = ArgumentCaptor.forClass(LeadQualifieRequest.class);
        verify(leadService).creerQualifie(captor.capture(), eq(7L));
        LeadQualifieRequest envoye = captor.getValue();
        assertThat(envoye.getNom()).isEqualTo("Dorsaf Riahi");
        assertThat(envoye.getEmail()).isEqualTo("dorsaf@example.com");
        assertThat(envoye.getTelephone()).isEqualTo("99221133");
        assertThat(envoye.getSource()).isEqualTo(SourceLead.MESSENGER);
        assertThat(envoye.getScore()).isEqualTo(80);
        assertThat(envoye.getResume()).isEqualTo("Cherche un site web vitrine");
    }

    @Test
    @DisplayName("Rejette si la page n'est pas connectée (jamais de lead orphelin)")
    void creerLeadDepuisMessenger_pageInconnue_leve() {
        when(compteSocialRepository
                .findFirstByIdentifiantExterneAndTypeReseau(anyString(), eq(TypeReseau.FACEBOOK)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> automationService.creerLeadDepuisMessenger(requete()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(leadService);
    }
}
