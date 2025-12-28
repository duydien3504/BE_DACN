package com.example.DACN.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
public class PaypalService {

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.webhook-id}")
    private String webhookId;

    @Value("${paypal.mode}")
    private String mode;

    private static final BigDecimal VND_TO_USD_RATE = new BigDecimal("26000");
    private static final String SANDBOX_API_BASE = "https://api-m.sandbox.paypal.com";
    private static final String LIVE_API_BASE = "https://api-m.paypal.com";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get PayPal OAuth access token
     */
    public String getAccessToken() {
        try {
            String apiBase = getApiBase();
            String url = apiBase + "/v1/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            String body = "grant_type=client_credentials";

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String accessToken = jsonNode.get("access_token").asText();
                log.info("Successfully obtained PayPal access token");
                return accessToken;
            } else {
                throw new RuntimeException("Failed to obtain PayPal access token");
            }
        } catch (Exception e) {
            log.error("Error getting PayPal access token", e);
            throw new RuntimeException("Failed to obtain PayPal access token: " + e.getMessage());
        }
    }

    /**
     * Create PayPal order for shop registration
     */
    public Map<String, String> createOrder(BigDecimal amountVND) {
        try {
            String accessToken = getAccessToken();
            String apiBase = getApiBase();
            String url = apiBase + "/v2/checkout/orders";

            // Convert VND to USD
            BigDecimal amountUSD = convertVNDtoUSD(amountVND);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("intent", "CAPTURE");

            Map<String, Object> purchaseUnit = new HashMap<>();
            Map<String, Object> amount = new HashMap<>();
            amount.put("currency_code", "USD");
            amount.put("value", amountUSD.toString());
            purchaseUnit.put("amount", amount);
            purchaseUnit.put("description", "Shop Registration Fee");

            orderRequest.put("purchase_units", Collections.singletonList(purchaseUnit));

            Map<String, Object> applicationContext = new HashMap<>();
            applicationContext.put("return_url", "http://localhost:7979/api/v1/payment/paypal/success");
            applicationContext.put("cancel_url", "http://localhost:7979/api/v1/payment/paypal/cancel");
            orderRequest.put("application_context", applicationContext);

            String requestBody = objectMapper.writeValueAsString(orderRequest);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String orderId = jsonNode.get("id").asText();

                // Extract approval URL
                String approvalUrl = null;
                JsonNode links = jsonNode.get("links");
                if (links != null && links.isArray()) {
                    for (JsonNode link : links) {
                        if ("approve".equals(link.get("rel").asText())) {
                            approvalUrl = link.get("href").asText();
                            break;
                        }
                    }
                }

                if (approvalUrl == null) {
                    throw new RuntimeException("Approval URL not found in PayPal response");
                }

                Map<String, String> result = new HashMap<>();
                result.put("orderId", orderId);
                result.put("approvalUrl", approvalUrl);

                log.info("Successfully created PayPal order: {}", orderId);
                return result;
            } else {
                throw new RuntimeException("Failed to create PayPal order");
            }
        } catch (Exception e) {
            log.error("Error creating PayPal order", e);
            throw new RuntimeException("Failed to create PayPal order: " + e.getMessage());
        }
    }

    /**
     * Create PayPal order for customer order payment
     */
    public Map<String, String> createOrder(BigDecimal amountVND, Long orderId) {
        try {
            String accessToken = getAccessToken();
            String apiBase = getApiBase();
            String url = apiBase + "/v2/checkout/orders";

            // Convert VND to USD
            BigDecimal amountUSD = convertVNDtoUSD(amountVND);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("intent", "CAPTURE");

            Map<String, Object> purchaseUnit = new HashMap<>();
            Map<String, Object> amount = new HashMap<>();
            amount.put("currency_code", "USD");
            amount.put("value", amountUSD.toString());
            purchaseUnit.put("amount", amount);
            purchaseUnit.put("description", "Order #" + orderId + " Payment");
            purchaseUnit.put("custom_id", orderId.toString()); // Important for webhook processing

            orderRequest.put("purchase_units", Collections.singletonList(purchaseUnit));

            Map<String, Object> applicationContext = new HashMap<>();
            // Include orderId in return URL for payment capture
            applicationContext.put("return_url",
                    "http://localhost:7979/api/v1/payment/paypal/success?orderId=" + orderId);
            applicationContext.put("cancel_url", "http://localhost:7979/api/v1/payment/paypal/cancel");
            orderRequest.put("application_context", applicationContext);

            String requestBody = objectMapper.writeValueAsString(orderRequest);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String paypalOrderId = jsonNode.get("id").asText();

                // Extract approval URL
                String approvalUrl = null;
                JsonNode links = jsonNode.get("links");
                if (links != null && links.isArray()) {
                    for (JsonNode link : links) {
                        if ("approve".equals(link.get("rel").asText())) {
                            approvalUrl = link.get("href").asText();
                            break;
                        }
                    }
                }

                if (approvalUrl == null) {
                    throw new RuntimeException("Approval URL not found in PayPal response");
                }

                Map<String, String> result = new HashMap<>();
                result.put("orderId", paypalOrderId);
                result.put("approvalUrl", approvalUrl);

                log.info("Successfully created PayPal order for customer order: {}", orderId);
                return result;
            } else {
                throw new RuntimeException("Failed to create PayPal order");
            }
        } catch (Exception e) {
            log.error("Error creating PayPal order for customer order: {}", orderId, e);
            throw new RuntimeException("Failed to create PayPal order: " + e.getMessage());
        }
    }

    /**
     * Verify PayPal webhook signature
     */
    public boolean verifyWebhookSignature(Map<String, String> headers, String requestBody) {
        try {
            String accessToken = getAccessToken();
            String apiBase = getApiBase();
            String url = apiBase + "/v1/notifications/verify-webhook-signature";

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.setBearerAuth(accessToken);

            Map<String, Object> verificationRequest = new HashMap<>();
            verificationRequest.put("transmission_id", headers.get("paypal-transmission-id"));
            verificationRequest.put("transmission_time", headers.get("paypal-transmission-time"));
            verificationRequest.put("cert_url", headers.get("paypal-cert-url"));
            verificationRequest.put("auth_algo", headers.get("paypal-auth-algo"));
            verificationRequest.put("transmission_sig", headers.get("paypal-transmission-sig"));
            verificationRequest.put("webhook_id", webhookId);
            verificationRequest.put("webhook_event", objectMapper.readTree(requestBody));

            String requestBodyJson = objectMapper.writeValueAsString(verificationRequest);
            HttpEntity<String> request = new HttpEntity<>(requestBodyJson, httpHeaders);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String verificationStatus = jsonNode.get("verification_status").asText();
                boolean isValid = "SUCCESS".equals(verificationStatus);

                if (isValid) {
                    log.info("PayPal webhook signature verified successfully");
                } else {
                    log.warn("PayPal webhook signature verification failed");
                }

                return isValid;
            } else {
                log.error("Failed to verify PayPal webhook signature");
                return false;
            }
        } catch (Exception e) {
            log.error("Error verifying PayPal webhook signature", e);
            return false;
        }
    }

    /**
     * Convert VND to USD
     */
    private BigDecimal convertVNDtoUSD(BigDecimal amountVND) {
        return amountVND.divide(VND_TO_USD_RATE, 2, RoundingMode.HALF_UP);
    }

    /**
     * Get API base URL based on mode
     */
    private String getApiBase() {
        return "sandbox".equalsIgnoreCase(mode) ? SANDBOX_API_BASE : LIVE_API_BASE;
    }
}
