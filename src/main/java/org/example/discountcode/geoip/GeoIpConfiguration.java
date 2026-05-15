package org.example.discountcode.geoip;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GeoIpProperties.class)
class GeoIpConfiguration {

    @Bean
    CountryResolver countryResolver(GeoIpProperties properties) {
        return switch (properties.getProvider()) {
            case "ip-api" -> new IpApiCountryResolver(RestClient.builder(), properties.getTimeout());
            case "stub" -> new StubCountryResolver(
                    properties.getStub().getCountryByIp(),
                    properties.getStub().getDefaultCountryCode()
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported GeoIP provider: " + properties.getProvider()
            );
        };
    }
}
