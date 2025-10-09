package com.tss.springsecurity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class TestController {

    @GetMapping("/cors")
    public ResponseEntity<?> testCors() {
        return ResponseEntity.ok(Map.of(
            "message", "CORS is working!",
            "timestamp", System.currentTimeMillis(),
            "status", "success"
        ));
    }

    @PostMapping("/cors")
    public ResponseEntity<?> testCorsPost(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
            "message", "CORS POST is working!",
            "receivedData", body != null ? body : "No data",
            "timestamp", System.currentTimeMillis(),
            "status", "success"
        ));
    }
}
