package org.example.discountcode.coupon.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.example.discountcode.coupon.infrastructure.CouponRepository;
import org.example.discountcode.coupon.infrastructure.CouponUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.geoip.provider=stub",
        "app.geoip.stub.default-country-code=PL"
})
class CouponApiIntegrationTest {

    @Autowired
    private CouponUsageRepository couponUsageRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CouponRepository couponRepository;

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeEach
    void cleanDatabase() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
        couponUsageRepository.deleteAll();
        couponRepository.deleteAll();
    }

    @Test
    void createsAndFetchesCouponThroughApi() throws Exception {
        ApiResult created = postJson("/api/coupons", """
                {"code":"save10","maxUses":5,"countryCode":"pl"}
                """);

        assertThat(created.status().value()).isEqualTo(201);
        assertThat(json(created).get("code").asText()).isEqualTo("SAVE10");
        assertThat(json(created).get("maxUses").asInt()).isEqualTo(5);
        assertThat(json(created).get("currentUses").asInt()).isEqualTo(0);
        assertThat(json(created).get("countryCode").asText()).isEqualTo("PL");

        ApiResult fetched = getJson("/api/coupons/save10");

        assertThat(fetched.status().value()).isEqualTo(200);
        assertThat(json(fetched).get("code").asText()).isEqualTo("SAVE10");
    }

    @Test
    void couponCreationValidationAndDuplicateErrorsAreStructured() throws Exception {
        assertError(postJson("/api/coupons", """
                {"code":"","maxUses":5,"countryCode":"PL"}
                """), 400, "INVALID_REQUEST");

        assertError(postJson("/api/coupons", """
                {"code":"SAVE10","maxUses":0,"countryCode":"PL"}
                """), 400, "INVALID_REQUEST");

        assertError(postJson("/api/coupons", """
                {"code":"SAVE10","maxUses":5,"countryCode":"XX"}
                """), 400, "INVALID_REQUEST");

        createCoupon("SAVE10", 5, "PL");

        assertError(postJson("/api/coupons", """
                {"code":"save10","maxUses":5,"countryCode":"PL"}
                """), 409, "DUPLICATE_COUPON_CODE");
    }

    @Test
    void missingAndBlankLookupReturnNotFound() throws Exception {
        assertError(getJson("/api/coupons/MISSING"), 404, "COUPON_NOT_FOUND");
        assertError(getJson("/api/coupons/%20"), 404, "COUPON_NOT_FOUND");
    }

    @Test
    void redeemsCouponAndDoesNotExposeCurrentUses() throws Exception {
        createCoupon("PLONLY", 2, "PL");

        ApiResult redeemed = postJson("/api/coupons/plonly/redeem", """
                {"userId":"user-1"}
                """, "X-Forwarded-For", "203.0.113.10");

        assertThat(redeemed.status().value()).isEqualTo(200);
        assertThat(json(redeemed).get("couponCode").asText()).isEqualTo("PLONLY");
        assertThat(json(redeemed).get("userId").asText()).isEqualTo("user-1");
        assertThat(json(redeemed).get("status").asText()).isEqualTo("REDEEMED");
        assertThat(json(redeemed).has("currentUses")).isFalse();
    }

    @Test
    void redemptionBusinessErrorsUseExpectedStatuses() throws Exception {
        assertError(postJson("/api/coupons/missing/redeem", """
                {"userId":"user-1"}
                """), 404, "COUPON_NOT_FOUND");

        createCoupon("USONLY", 1, "US");

        assertError(postJson("/api/coupons/usonly/redeem", """
                {"userId":"user-1"}
                """), 403, "COUPON_COUNTRY_MISMATCH");

        createCoupon("LIMIT1", 1, "PL");
        redeem("LIMIT1", "user-1");

        assertError(postJson("/api/coupons/limit1/redeem", """
                {"userId":"user-2"}
                """), 403, "COUPON_USAGE_LIMIT_REACHED");

        createCoupon("ONCE", 2, "PL");
        redeem("ONCE", "user-1");

        assertError(postJson("/api/coupons/once/redeem", """
                {"userId":"user-1"}
                """), 403, "COUPON_ALREADY_REDEEMED");
    }

    @Test
    void validationErrorsDoNotEchoFullFailedCouponCode() throws Exception {
        ApiResult result = getJson("/api/coupons/THISCOUPONDOESNOTEXIST123");

        assertThat(result.status().value()).isEqualTo(404);
        assertThat(json(result).get("message").asText()).doesNotContain("THISCOUPONDOESNOTEXIST123");
    }

    private void createCoupon(String code, int maxUses, String countryCode) throws Exception {
        assertThat(postJson("/api/coupons", """
                {"code":"%s","maxUses":%d,"countryCode":"%s"}
                """.formatted(code, maxUses, countryCode)).status().value()).isEqualTo(201);
    }

    private void redeem(String code, String userId) throws Exception {
        assertThat(postJson("/api/coupons/%s/redeem".formatted(code), """
                {"userId":"%s"}
                """.formatted(userId)).status().value()).isEqualTo(200);
    }

    private void assertError(ApiResult result, int status, String code) throws Exception {
        assertThat(result.status().value()).isEqualTo(status);
        assertThat(json(result).get("code").asText()).isEqualTo(code);
    }

    private ApiResult getJson(String uri) {
        return exchange(HttpMethod.GET, uri, null, null, null);
    }

    private ApiResult postJson(String uri, String body) {
        return postJson(uri, body, null, null);
    }

    private ApiResult postJson(String uri, String body, String headerName, String headerValue) {
        return exchange(HttpMethod.POST, uri, body, headerName, headerValue);
    }

    private ApiResult exchange(
            HttpMethod method,
            String uri,
            String body,
            String headerName,
            String headerValue
    ) {
        return restClient.method(method)
                .uri(uri)
                .headers(headers -> {
                    headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    if (body != null) {
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    }
                    if (headerName != null) {
                        headers.set(headerName, headerValue);
                    }
                })
                .body(body == null ? "" : body)
                .exchange((request, response) -> new ApiResult(
                        response.getStatusCode(),
                        StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8)
                ));
    }

    private JsonNode json(ApiResult result) throws IOException {
        return objectMapper.readTree(result.body());
    }

    private record ApiResult(HttpStatusCode status, String body) {
    }
}
