package com.shopmesh.checkout;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** One RestClient per downstream service, each with hard connect/read timeouts so a slow service cannot hang checkout. */
@Configuration
public class ClientConfig {
    @Value("${app.http.connect-timeout-ms:1000}")
    private int connectTimeoutMs;
    @Value("${app.http.read-timeout-ms:3000}")
    private int readTimeoutMs;

    @Bean
    RestClient cartHttp(@Value("${app.services.cart-url}") String url) {
        return build(url);
    }

    @Bean
    RestClient catalogHttp(@Value("${app.services.catalog-url}") String url) {
        return build(url);
    }

    @Bean
    RestClient orderHttp(@Value("${app.services.order-url}") String url) {
        return build(url);
    }

    private RestClient build(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
