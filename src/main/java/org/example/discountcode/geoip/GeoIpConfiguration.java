package org.example.discountcode.geoip;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GeoIpProperties.class)
class GeoIpConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.geoip", name = "provider", havingValue = "ip-api", matchIfMissing = true)
    CountryResolver ipApiCountryResolver(GeoIpProperties properties) {
        return new IpApiCountryResolver(RestClient.builder(), properties.getTimeout());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.geoip", name = "provider", havingValue = "stub")
    CountryResolver stubCountryResolver(GeoIpProperties properties) {
        return new StubCountryResolver(properties.getStub().getCountryByIp(), properties.getStub().getDefaultCountryCode());
    }

    @Bean
    ApplicationRunner geoIpProviderValidator(GeoIpProperties properties) {
        return args -> {
            String provider = properties.getProvider();
            if (!"ip-api".equals(provider) && !"stub".equals(provider)) {
                throw new IllegalArgumentException("Unsupported GeoIP provider: " + provider);
            }
        };
    }
}
