package org.example.discountcode.geoip;

import java.time.Duration;
import org.example.discountcode.coupon.domain.CountryCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class IpApiCountryResolver implements CountryResolver {

    private final RestClient restClient;

    public IpApiCountryResolver(RestClient.Builder restClientBuilder, Duration timeout) {
        this(restClientBuilder, timeout, "http://ip-api.com");
    }

    IpApiCountryResolver(RestClient.Builder restClientBuilder, Duration timeout, String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public String resolveCountryCode(String ipAddress) {
        IpApiResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/json/{ipAddress}")
                            .queryParam("fields", "status,message,countryCode")
                            .build(ipAddress))
                    .retrieve()
                    .body(IpApiResponse.class);
        } catch (RestClientException exception) {
            throw CountryResolutionException.dependencyUnavailable("GeoIP provider is unavailable.", exception);
        }

        if (response == null || !"success".equals(response.status())) {
            throw CountryResolutionException.countryNotVerified("Country could not be verified.");
        }

        try {
            return CountryCode.normalize(response.countryCode());
        } catch (IllegalArgumentException exception) {
            throw CountryResolutionException.countryNotVerified("Country could not be verified.");
        }
    }

    private record IpApiResponse(String status, String countryCode) {
    }
}
