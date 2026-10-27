package com.silverlingo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "SilverLingo Backend Engine");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("mode", "DEMO_AND_PRODUCTION_READY");
        response.put("message", "SilverLingo 백엔드 서버가 정상 동작 중입니다.");
        return response;
    }
}
