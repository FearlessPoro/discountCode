package org.example.discountcode.geoip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class IpApiCountryResolverTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void returnsNormalizedCountryCodeForSuccessfulProviderResponse() throws Exception {
        CountryResolver resolver = resolverReturning(200, """
                {"status":"success","countryCode":"pl"}
                """);

        assertThat(resolver.resolveCountryCode("203.0.113.10")).isEqualTo("PL");
    }

    @Test
    void treatsMissingCountryCodeAsResolutionFailure() throws Exception {
        CountryResolver resolver = resolverReturning(200, """
                {"status":"success"}
                """);

        assertThatThrownBy(() -> resolver.resolveCountryCode("203.0.113.10"))
                .isInstanceOfSatisfying(CountryResolutionException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(CountryResolutionException.Reason.COUNTRY_NOT_VERIFIED));
    }

    @Test
    void treatsMalformedCountryCodeAsResolutionFailure() throws Exception {
        CountryResolver resolver = resolverReturning(200, """
                {"status":"success","countryCode":"POL"}
                """);

        assertThatThrownBy(() -> resolver.resolveCountryCode("203.0.113.10"))
                .isInstanceOfSatisfying(CountryResolutionException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(CountryResolutionException.Reason.COUNTRY_NOT_VERIFIED));
    }

    @Test
    void handlesLocalPrivateIpFailureClearly() throws Exception {
        CountryResolver resolver = resolverReturning(200, """
                {"status":"fail","message":"private range"}
                """);

        assertThatThrownBy(() -> resolver.resolveCountryCode("127.0.0.1"))
                .isInstanceOfSatisfying(CountryResolutionException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(CountryResolutionException.Reason.COUNTRY_NOT_VERIFIED));
    }

    @Test
    void mapsProviderOutageToDependencyUnavailable() throws Exception {
        CountryResolver resolver = resolverReturning(503, """
                {"status":"fail","message":"unavailable"}
                """);

        assertThatThrownBy(() -> resolver.resolveCountryCode("203.0.113.10"))
                .isInstanceOfSatisfying(CountryResolutionException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(CountryResolutionException.Reason.DEPENDENCY_UNAVAILABLE));
    }

    private CountryResolver resolverReturning(int status, String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/json/203.0.113.10", exchange -> respond(exchange, status, body));
        server.createContext("/json/127.0.0.1", exchange -> respond(exchange, status, body));
        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return new IpApiCountryResolver(RestClient.builder(), Duration.ofSeconds(2), baseUrl);
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
