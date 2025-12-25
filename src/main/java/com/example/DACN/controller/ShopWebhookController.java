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
@RequestMapping("/api/v1/shops/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shop Webhook", description = "PayPal webhook for shop registration payments")
public class ShopWebhookController {

    private final ShopService shopService;

    @PostMapping("/paypal")
    @Operation(summary = "Handle PayPal webhook for shop registration", description = "Processes PayPal webhook events for shop registration payment completion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid webhook payload"),
            @ApiResponse(responseCode = "401", description = "Webhook signature verification failed")
    })
    public ResponseEntity<Map<String, String>> handlePayPalWebhook(
            @RequestBody PaypalWebhookRequest webhookRequest,
            HttpServletRequest request) {

        log.info("Received PayPal webhook for shop registration: {}", webhookRequest.getEventType());

        try {
            // Extract headers for signature verification
            Map<String, String> headers = extractHeaders(request);

            // Process the webhook
            shopService.processPaymentWebhook(webhookRequest);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Webhook processed successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing PayPal webhook for shop registration", e);
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
}
