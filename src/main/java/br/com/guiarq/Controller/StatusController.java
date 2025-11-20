package br.com.guiarq.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StatusController {

    @GetMapping("/api/status")
    public Map<String, Object> status() {
        return Map.of(
                "status", "OK",
                "timestamp", System.currentTimeMillis(),
                "service", "Guia RQ Backend"
        );
    }
}
