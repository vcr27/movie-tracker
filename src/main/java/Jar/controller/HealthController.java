package Jar.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "API is running";
    }

    @GetMapping("/health/secure")
    public String secureEndpoint() {
        return "You are authenticated!";
    }

}