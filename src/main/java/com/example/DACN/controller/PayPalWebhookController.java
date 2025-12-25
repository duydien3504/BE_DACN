package com.example.DACN.controller;

import com.example.DACN.dto.request.PaypalWebhookRequest;
import com.example.DACN.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/paypal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "PayPal Webhook (Plural)", description = "PayPal webhook handling APIs - plural endpoint")
public class PayPalWebhookController {

    private final ShopService shopService;

    @PostMapping("/webhook")
    @Operation(summary = "Handle PayPal webhook", description = "Processes PayPal webhook events for payment completion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid webhook payload"),
            @ApiResponse(responseCode = "401", description = "Webhook signature verification failed")
    })
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody PaypalWebhookRequest webhookRequest,
            HttpServletRequest request) {

        log.info("Received PayPal webhook (plural endpoint): {}", webhookRequest.getEventType());

        try {
            // Extract headers for signature verification
            Map<String, String> headers = extractHeaders(request);

            // Note: In production, you should verify webhook signature here
            // For now, we'll process the webhook directly

            // Process the webhook
            shopService.processPaymentWebhook(webhookRequest);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Webhook processed successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing PayPal webhook", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Extract headers from HTTP request
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (headerName.toLowerCase().startsWith("paypal-")) {
                headers.put(headerName.toLowerCase(), request.getHeader(headerName));
            }
        }

        return headers;
    }

    @GetMapping("/success")
    @Operation(summary = "PayPal payment success", description = "Handles successful PayPal payment redirect")
    public ResponseEntity<String> paymentSuccess(@RequestParam("token") String token) {
        log.info("Payment successful for token: {}", token);
        return ResponseEntity.ok("Payment successful! You can close this window. Your shop will be approved shortly.");
    }

    @GetMapping("/cancel")
    @Operation(summary = "PayPal payment cancelled", description = "Handles cancelled PayPal payment redirect")
    public ResponseEntity<String> paymentCancel(@RequestParam("token") String token) {
        log.info("Payment cancelled for token: {}", token);
        return ResponseEntity.ok("Payment cancelled. You can try again later.");
    }
}
