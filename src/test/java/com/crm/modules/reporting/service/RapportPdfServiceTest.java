package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.RapportCommercialResponse;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Activites;
import com.crm.modules.reporting.dto.RapportCommercialResponse.DevisFactures;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Entreprise;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Periode;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Pipeline;
import com.crm.modules.reporting.dto.RapportCommercialResponse.SourceCount;
import com.crm.modules.reporting.dto.RapportCommercialResponse.StatutCount;
import com.crm.modules.reporting.dto.RapportCommercialResponse.Synthese;
import com.crm.modules.reporting.dto.RapportCommercialResponse.TypeCount;
import com.crm.shared.enums.PeriodeRapport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de fumée : vérifie que le rendu HTML→PDF (openhtmltopdf) ne lève pas
 * d'exception et produit bien un fichier PDF non vide. {@link RapportPdfService}
 * n'ayant aucune dépendance Spring, on l'instancie directement.
 */
class RapportPdfServiceTest {

    private final RapportPdfService service = new RapportPdfService();

    @Test
    void genererPdf_produitUnPdfNonVide() {
        byte[] pdf = service.genererPdf(rapportExemple());

        assertThat(pdf).isNotEmpty();
        // En-tête magique d'un fichier PDF : "%PDF"
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    private RapportCommercialResponse rapportExemple() {
        return RapportCommercialResponse.builder()
                .entreprise(Entreprise.builder()
                        .proprietaireId(9L).email("test@example.com")
                        .nomProprietaire("Dorra Ben Ali").nomEntreprise("Predixia").build())
                .periode(Periode.builder()
                        .type(PeriodeRapport.MOIS).libelle("Mois de mai 2026")
                        .debut(LocalDate.of(2026, 5, 1)).fin(LocalDate.of(2026, 5, 31)).build())
                .synthese(Synthese.builder()
                        .caRealise(new BigDecimal("431084.64")).caPeriodePrecedente(BigDecimal.ZERO)
                        .variationCaPct(null).nouveauxLeads(7).leadsConvertis(7)
                        .tauxConversionLeads(100.0).build())
                .pipeline(Pipeline.builder()
                        .valeur(new BigDecimal("2000.00"))
                        .repartitionParStatut(List.of(
                                StatutCount.builder().statut("NEGOCIATION").count(4).build(),
                                StatutCount.builder().statut("GAGNEE").count(10).build()))
                        .topOpportunites(List.of()).build())
                .activites(Activites.builder()
                        .total(0)
                        .parType(List.of(TypeCount.builder().type("APPEL").count(0).build()))
                        .parResultat(List.of()).build())
                .devisFactures(DevisFactures.builder()
                        .devisEmis(22).tauxAcceptationDevis(63.6).facturesImpayees(3)
                        .montantImpaye(new BigDecimal("581873.52"))
                        .caEncaisse(new BigDecimal("431084.64")).build())
                .leadsParSource(List.of(
                        SourceCount.builder().source("SALON").count(1).build(),
                        SourceCount.builder().source("AUTRE").count(6).build()))
                .syntheseIa("Bon mois de mai : CA en hausse et conversion à 100%.")
                .build();
    }
}
