package com.diwan.medical.service;

import com.diwan.common.web.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedicineCatalogTest {

    private final MedicineCatalog catalog = new MedicineCatalog(new ClassPathResource("medicines/test-drugs.csv"));

    @Test
    void readsQuotedNames() {
        assertThat(catalog.size()).isEqualTo(4);
        assertThat(catalog.search("adol")).extracting(MedicineCatalog.Medicine::trade).contains("ADOL, EXTRA");
    }

    @Test
    void tradeNamesStartingWithTheQueryComeFirstThenScientificNames() {
        assertThat(catalog.search("para")).extracting(MedicineCatalog.Medicine::trade)
                .containsExactly("PARAMOL", "ADOL, EXTRA", "PANADOL");
        assertThat(catalog.search("Metformin")).extracting(MedicineCatalog.Medicine::trade).containsExactly("GLUCOPHAGE");
    }

    @Test
    void aTooShortQueryIsRefused() {
        assertThatThrownBy(() -> catalog.search("p")).isInstanceOf(ApiException.class);
    }

    @Test
    void withoutTheFileTheCatalogueIsEmptyNotBroken() {
        MedicineCatalog empty = new MedicineCatalog(new ClassPathResource("medicines/missing.csv"));
        assertThat(empty.search("panadol")).isEmpty();
    }
}
