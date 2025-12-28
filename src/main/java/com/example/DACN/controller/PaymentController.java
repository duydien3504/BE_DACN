package com.example.DACN.controller;

import com.example.DACN.dto.request.PaypalWebhookRequest;
import com.example.DACN.service.PaymentService;
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
@RequestMapping("/api/v1/payment/paypal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Webhook", description = "PayPal webhook handling APIs")
public class PaymentController {

    private final ShopService shopService;
    private final PaymentService paymentService;

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

        log.info("Received PayPal webhook: {}", webhookRequest.getEventType());

        try {
            // Extract headers for signature verification
            Map<String, String> headers = extractHeaders(request);

            // Note: In production, you should verify webhook signature here
            // For now, we'll process the webhook directly
            // Uncomment the following lines to enable signature verification:
            // if (!paypalService.verifyWebhookSignature(headers, requestBody)) {
            // log.error("PayPal webhook signature verification failed");
            // return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            // }

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
    @Operation(summary = "PayPal payment success", description = "Handles successful PayPal payment redirect and captures payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment captured successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid token or order ID"),
            @ApiResponse(responseCode = "500", description = "Failed to capture payment")
    })
    public ResponseEntity<String> paymentSuccess(
            @RequestParam("token") String token,
            @RequestParam(value = "orderId", required = false) Long orderId) {

        log.info("Payment successful for token: {}, orderId: {}", token, orderId);

        try {
            if (orderId != null) {
                // This is for order payment - capture the payment
                paymentService.capturePayPalPayment(token, orderId);
                return ResponseEntity.ok(
                        "<!DOCTYPE html>" +
                                "<html>" +
                                "<head>" +
                                "  <title>Payment Successful</title>" +
                                "  <meta charset='UTF-8'>" +
                                "</head>" +
                                "<body style='font-family: Arial, sans-serif; text-align: center; padding: 50px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white;'>"
                                +
                                "  <div style='background: white; color: #333; padding: 40px; border-radius: 10px; max-width: 500px; margin: 0 auto; box-shadow: 0 10px 40px rgba(0,0,0,0.3);'>"
                                +
                                "    <div style='font-size: 60px; color: #4CAF50; margin-bottom: 20px;'>✓</div>" +
                                "    <h1 style='color: #4CAF50; margin: 0 0 20px 0;'>Payment Successful!</h1>" +
                                "    <p style='font-size: 18px; margin: 20px 0;'>Your order <strong>#" + orderId
                                + "</strong> has been confirmed.</p>" +
                                "    <p style='color: #666; margin: 20px 0;'>Thank you for your purchase!</p>" +
                                "    <p style='color: #999; font-size: 14px;'>This window will close automatically in 3 seconds...</p>"
                                +
                                "  </div>" +
                                "  <script>setTimeout(function(){ window.close(); }, 3000);</script>" +
                                "</body>" +
                                "</html>");
            } else {
                // This is for shop registration - capture the payment
                paymentService.captureShopRegistrationPayment(token);
                return ResponseEntity.ok(
                        "<!DOCTYPE html>" +
                                "<html>" +
                                "<head>" +
                                "  <title>Payment Successful</title>" +
                                "  <meta charset='UTF-8'>" +
                                "</head>" +
                                "<body style='font-family: Arial, sans-serif; text-align: center; padding: 50px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white;'>"
                                +
                                "  <div style='background: white; color: #333; padding: 40px; border-radius: 10px; max-width: 500px; margin: 0 auto; box-shadow: 0 10px 40px rgba(0,0,0,0.3);'>"
                                +
                                "    <div style='font-size: 60px; color: #4CAF50; margin-bottom: 20px;'>✓</div>" +
                                "    <h1 style='color: #4CAF50; margin: 0 0 20px 0;'>Payment Successful!</h1>" +
                                "    <p style='font-size: 18px; margin: 20px 0;'>Your shop registration payment has been confirmed.</p>"
                                +
                                "    <p style='color: #666; margin: 20px 0;'>Your shop will be approved shortly!</p>" +
                                "    <p style='color: #999; font-size: 14px;'>This window will close automatically in 3 seconds...</p>"
                                +
                                "  </div>" +
                                "  <script>setTimeout(function(){ window.close(); }, 3000);</script>" +
                                "</body>" +
                                "</html>");
            }
        } catch (Exception e) {
            log.error("Error capturing payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head>" +
                            "  <title>Payment Error</title>" +
                            "  <meta charset='UTF-8'>" +
                            "</head>" +
                            "<body style='font-family: Arial, sans-serif; text-align: center; padding: 50px; background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white;'>"
                            +
                            "  <div style='background: white; color: #333; padding: 40px; border-radius: 10px; max-width: 500px; margin: 0 auto; box-shadow: 0 10px 40px rgba(0,0,0,0.3);'>"
                            +
                            "    <div style='font-size: 60px; color: #f44336; margin-bottom: 20px;'>✗</div>" +
                            "    <h1 style='color: #f44336; margin: 0 0 20px 0;'>Payment Processing Error</h1>" +
                            "    <p style='font-size: 16px; margin: 20px 0;'>There was an error processing your payment.</p>"
                            +
                            "    <p style='color: #666; margin: 20px 0;'>Please contact support for assistance.</p>" +
                            "    <p style='color: #999; font-size: 14px;'>Error: " + e.getMessage() + "</p>" +
                            "  </div>" +
                            "</body>" +
                            "</html>");
        }
    }

    @GetMapping("/cancel")
    @Operation(summary = "PayPal payment cancelled", description = "Handles cancelled PayPal payment redirect")
    public ResponseEntity<String> paymentCancel(@RequestParam("token") String token) {
        log.info("Payment cancelled for token: {}", token);
        return ResponseEntity.ok(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><title>Payment Cancelled</title></head>" +
                        "<body style='font-family: Arial; text-align: center; padding: 50px;'>" +
                        "<h1 style='color: orange;'>Payment Cancelled</h1>" +
                        "<p>You have cancelled the payment.</p>" +
                        "<p>You can try again later.</p>" +
                        "<script>setTimeout(function(){ window.close(); }, 3000);</script>" +
                        "</body>" +
                        "</html>");
    }
}
