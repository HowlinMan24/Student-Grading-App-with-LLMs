package com.webapp.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Value("${openai.api.key}")
    private String OPENAI_API_KEY;

    @Value("${openai.api.url}")
    private String OPENAI_API_URL;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request, Authentication auth) {
        System.out.println("Chat request from: " + (auth != null ? auth.getName() : "unauthenticated"));

        String prompt = request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            System.err.println("Invalid prompt: " + prompt);
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + OPENAI_API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"model\": \"gpt-4o-mini\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}], \"max_tokens\": 150}",
                prompt.replace("\"", "\\\"")
        );

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                System.err.println("Empty response from OpenAI");
                return ResponseEntity.status(500).body(Map.of("error", "Empty response from OpenAI"));
            }

            Map<String, Object> choice = ((java.util.List<Map<String, Object>>) responseBody.get("choices")).get(0);
            String content = (String) ((Map<String, String>) choice.get("message")).get("content");

            System.out.println("OpenAI response: " + content);
            return ResponseEntity.ok(Map.of("response", content.trim()));
        } catch (HttpClientErrorException e) {
            System.err.println("OpenAI API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to process request: " + e.getResponseBodyAsString()));
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }
}