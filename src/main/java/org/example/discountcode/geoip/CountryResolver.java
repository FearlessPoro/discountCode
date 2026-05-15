package org.example.discountcode.geoip;

public interface CountryResolver {

    String resolveCountryCode(String ipAddress);
}
