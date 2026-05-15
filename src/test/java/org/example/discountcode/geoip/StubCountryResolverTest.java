package org.example.discountcode.geoip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class StubCountryResolverTest {

    @Test
    void resolvesCountryFromConfiguredIpMap() {
        StubCountryResolver resolver = new StubCountryResolver(Map.of("203.0.113.10", "pl"), "DE");

        assertThat(resolver.resolveCountryCode("203.0.113.10")).isEqualTo("PL");
    }

    @Test
    void resolvesConfiguredDefaultCountryWhenIpIsNotMapped() {
        StubCountryResolver resolver = new StubCountryResolver(Map.of(), "de");

        assertThat(resolver.resolveCountryCode("203.0.113.10")).isEqualTo("DE");
    }

    @Test
    void failsWhenNoMappingOrDefaultCountryExists() {
        StubCountryResolver resolver = new StubCountryResolver(Map.of(), null);

        assertThatThrownBy(() -> resolver.resolveCountryCode("203.0.113.10"))
                .isInstanceOfSatisfying(CountryResolutionException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(CountryResolutionException.Reason.COUNTRY_NOT_VERIFIED));
    }
}
