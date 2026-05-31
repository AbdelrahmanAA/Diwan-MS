package com.diwan.smarthome.service;

import com.diwan.smarthome.dto.DeviceResponse;
import com.diwan.smarthome.security.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AlexaSmartHomeService {

    private final SmartDeviceService smartDeviceService;
    private final JwtTokenService jwtTokenService;

    @Value("${smarthome.alexa.lambda-secret:}")
    private String lambdaSecret;

    public AlexaSmartHomeService(SmartDeviceService smartDeviceService, JwtTokenService jwtTokenService) {
        this.smartDeviceService = smartDeviceService;
        this.jwtTokenService = jwtTokenService;
    }

    public Map<String, Object> handleDirective(Map<String, Object> body, String authorizationHeader, String lambdaSecretHeader) {
        validateLambdaSecret(lambdaSecretHeader);
        Long userId = jwtTokenService.extractUserIdFromAuthorizationHeader(authorizationHeader);

        Map<String, Object> directive = asMap(body.get("directive"), "Missing directive");
        Map<String, Object> header = asMap(directive.get("header"), "Missing directive header");

        String namespace = asString(header.get("namespace"), "Missing namespace");
        String name = asString(header.get("name"), "Missing directive name");
        String correlationToken = (String) header.get("correlationToken");
        String payloadVersion = header.get("payloadVersion") == null ? "3" : String.valueOf(header.get("payloadVersion"));

        if ("Alexa.Discovery".equals(namespace) && "Discover".equals(name)) {
            return buildDiscoveryResponse(payloadVersion, correlationToken, userId);
        }

        if ("Alexa.PowerController".equals(namespace) && ("TurnOn".equals(name) || "TurnOff".equals(name))) {
            Map<String, Object> endpoint = asMap(directive.get("endpoint"), "Missing endpoint");
            String endpointId = asString(endpoint.get("endpointId"), "Missing endpointId");
            String state = "TurnOn".equals(name) ? "ON" : "OFF";
            DeviceResponse updated = smartDeviceService.controlByExternalDeviceId(userId, endpointId, state);
            return buildPowerControlResponse(payloadVersion, correlationToken, updated);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unsupported Alexa directive: " + namespace + "." + name);
    }

    private void validateLambdaSecret(String lambdaSecretHeader) {
        if (lambdaSecret == null || lambdaSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Alexa lambda secret is not configured");
        }
        if (lambdaSecretHeader == null || !lambdaSecret.equals(lambdaSecretHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid lambda secret");
        }
    }

    private Map<String, Object> buildDiscoveryResponse(String payloadVersion, String correlationToken, Long userId) {
        List<DeviceResponse> devices = smartDeviceService.listDevices(userId);

        List<Map<String, Object>> endpoints = new ArrayList<>();
        for (DeviceResponse d : devices) {
            Map<String, Object> endpoint = new HashMap<>();
            endpoint.put("endpointId", d.deviceId);
            endpoint.put("manufacturerName", "Diwan");
            endpoint.put("friendlyName", d.name);
            endpoint.put("description", "Diwan smart home device " + d.deviceId);
            endpoint.put("displayCategories", List.of("LIGHT"));
            endpoint.put("cookie", Map.of());
            endpoint.put("capabilities", List.of(
                    Map.of(
                            "type", "AlexaInterface",
                            "interface", "Alexa",
                            "version", "3"
                    ),
                    Map.of(
                            "type", "AlexaInterface",
                            "interface", "Alexa.PowerController",
                            "version", "3",
                            "properties", Map.of(
                                    "supported", List.of(Map.of("name", "powerState")),
                                    "proactivelyReported", false,
                                    "retrievable", true
                            )
                    )
            ));
            endpoints.add(endpoint);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("endpoints", endpoints);

        Map<String, Object> header = responseHeader("Alexa.Discovery", "Discover.Response", payloadVersion, correlationToken);

        Map<String, Object> event = new HashMap<>();
        event.put("header", header);
        event.put("payload", payload);

        return Map.of("event", event);
    }

    private Map<String, Object> buildPowerControlResponse(String payloadVersion, String correlationToken, DeviceResponse device) {
        Map<String, Object> property = new HashMap<>();
        property.put("namespace", "Alexa.PowerController");
        property.put("name", "powerState");
        property.put("value", "ON".equalsIgnoreCase(device.state) ? "ON" : "OFF");
        property.put("timeOfSample", Instant.now().toString());
        property.put("uncertaintyInMilliseconds", 200);

        Map<String, Object> context = new HashMap<>();
        context.put("properties", List.of(property));

        Map<String, Object> endpoint = new HashMap<>();
        endpoint.put("endpointId", device.deviceId);

        Map<String, Object> payload = Map.of();

        Map<String, Object> header = responseHeader("Alexa", "Response", payloadVersion, correlationToken);

        Map<String, Object> event = new HashMap<>();
        event.put("header", header);
        event.put("endpoint", endpoint);
        event.put("payload", payload);

        Map<String, Object> response = new HashMap<>();
        response.put("context", context);
        response.put("event", event);
        return response;
    }

    private Map<String, Object> responseHeader(String namespace, String name, String payloadVersion, String correlationToken) {
        Map<String, Object> header = new HashMap<>();
        header.put("namespace", namespace);
        header.put("name", name);
        header.put("payloadVersion", payloadVersion);
        header.put("messageId", UUID.randomUUID().toString());
        if (correlationToken != null && !correlationToken.isBlank()) {
            header.put("correlationToken", correlationToken);
        }
        return header;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object input, String message) {
        if (!(input instanceof Map<?, ?> raw)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            map.put(String.valueOf(e.getKey()), e.getValue());
        }
        return map;
    }

    private String asString(Object input, String message) {
        if (input == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        String value = String.valueOf(input).trim();
        if (value.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }
}
