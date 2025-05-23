package com.yju.team2.seilomun.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class TossPaymentConfig {
    @Value("${payment.toss.test-client-api-key}")
    private String testClientApiKey;

    @Value("${payment.toss.test-secrete-api-key}")
    private String testSecreteKey;

    @Value("${payment.toss.success-url}")
    private String successUrl;

    @Value("${payment.toss.fail-url}")
    private String failUrl;

    public static final String URL = "https://api.tosspayments.com/v1/payments/";
}
