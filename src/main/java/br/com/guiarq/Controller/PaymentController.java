package br.com.guiarq.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class PaymentController {

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> testEndpoint(@RequestBody(required = false) Map<String, Object> body) {
        System.out.println("✅ Endpoint Stripe chamado com sucesso!");
        return ResponseEntity.ok(Map.of("id", "test_session_12345"));
    }
}
