package org.example.discountcode.geoip;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.geoip")
public class GeoIpProperties {

    private String provider = "ip-api";
    private Duration timeout = Duration.ofSeconds(2);
    private final Stub stub = new Stub();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Stub getStub() {
        return stub;
    }

    public static class Stub {

        private String defaultCountryCode;
        private Map<String, String> countryByIp = new LinkedHashMap<>();

        public String getDefaultCountryCode() {
            return defaultCountryCode;
        }

        public void setDefaultCountryCode(String defaultCountryCode) {
            this.defaultCountryCode = defaultCountryCode;
        }

        public Map<String, String> getCountryByIp() {
            return countryByIp;
        }

        public void setCountryByIp(Map<String, String> countryByIp) {
            this.countryByIp = countryByIp;
        }
    }
}
