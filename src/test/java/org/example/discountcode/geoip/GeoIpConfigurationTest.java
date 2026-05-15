package org.example.discountcode.geoip;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GeoIpConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GeoIpConfiguration.class);

    @Test
    void selectsIpApiCountryResolver() {
        contextRunner
                .withPropertyValues("app.geoip.provider=ip-api")
                .run(context -> assertThat(context).hasSingleBean(IpApiCountryResolver.class));
    }

    @Test
    void selectsStubCountryResolver() {
        contextRunner
                .withPropertyValues(
                        "app.geoip.provider=stub",
                        "app.geoip.stub.default-country-code=PL"
                )
                .run(context -> assertThat(context).hasSingleBean(StubCountryResolver.class));
    }

    @Test
    void failsContextForUnknownProvider() {
        contextRunner
                .withPropertyValues("app.geoip.provider=unknown")
                .run(context -> assertThat(context).hasFailed());
    }
}
