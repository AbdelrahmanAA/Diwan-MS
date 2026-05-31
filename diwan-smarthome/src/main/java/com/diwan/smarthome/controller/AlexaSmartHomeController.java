package com.diwan.smarthome.controller;

import com.diwan.smarthome.service.AlexaSmartHomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/smart-home/alexa")
public class AlexaSmartHomeController {

    private final AlexaSmartHomeService alexaSmartHomeService;

    public AlexaSmartHomeController(AlexaSmartHomeService alexaSmartHomeService) {
        this.alexaSmartHomeService = alexaSmartHomeService;
    }

    @PostMapping("/directive")
    public ResponseEntity<Map<String, Object>> handleDirective(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Alexa-Lambda-Secret", required = false) String lambdaSecret
    ) {
        return ResponseEntity.ok(alexaSmartHomeService.handleDirective(body, authorization, lambdaSecret));
    }
}
