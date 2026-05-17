package org.example.discountcode.geoip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @ValueSource(strings = {"pl", "PL", "us", "DE"})
    void normalizesValidDefaultCountries(String rawCountryCode) {
        StubCountryResolver resolver = new StubCountryResolver(Map.of(), rawCountryCode);

        assertThat(resolver.resolveCountryCode("203.0.113.10")).isEqualTo(rawCountryCode.toUpperCase());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void failsWhenDefaultCountryIsMissingOrBlank(String rawCountryCode) {
        assertThatThrownBy(() -> new StubCountryResolver(Map.of(), rawCountryCode)
                .resolveCountryCode("203.0.113.10"))
                .isInstanceOfSatisfying(CountryResolutionException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(CountryResolutionException.Reason.COUNTRY_NOT_VERIFIED));
    }

    @ParameterizedTest
    @ValueSource(strings = {"XX", "P1", "POL", "日本"})
    void rejectsInvalidDefaultCountryAtConstruction(String rawCountryCode) {
        assertThatThrownBy(() -> new StubCountryResolver(Map.of(), rawCountryCode))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"XX", "P1", "POL", "日本"})
    void failsWhenMappedCountryIsInvalid(String mappedCountryCode) {
        StubCountryResolver resolver = new StubCountryResolver(Map.of("203.0.113.10", mappedCountryCode), "PL");

        assertThatThrownBy(() -> resolver.resolveCountryCode("203.0.113.10"))
                .isInstanceOfSatisfying(CountryResolutionException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(CountryResolutionException.Reason.COUNTRY_NOT_VERIFIED));
    }
}
