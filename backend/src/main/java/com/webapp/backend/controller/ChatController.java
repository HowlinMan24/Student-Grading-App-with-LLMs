package com.webapp.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Value("${openai.api.key}")
    private String OPENAI_API_KEY;

    @Value("${openai.api.url}")
    private String OPENAI_API_URL;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> chat(
            @RequestPart("prompt") String prompt,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication auth
    ) {
        System.out.println("Chat request from: " + (auth != null ? auth.getName() : "unauthenticated"));

        System.out.println(images);

        if (prompt == null || prompt.trim().isEmpty()) {
            System.err.println("Invalid prompt: " + prompt);
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OPENAI_API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<Map<String, Object>> contentParts = new ArrayList<>();

        contentParts.add(Map.of(
                "type", "text",
                "text", "You are an educational assistant. You provide formative feedback on " +
                        "students' practice work. You may estimate how a teacher might score " +
                        "their answers according to a rubric, but you do NOT help with live " +
                        "tests or exams."
        ));

        contentParts.add(Map.of(
                "type", "text",
                "text", prompt
        ));

        if (images != null) {
            for (MultipartFile img : images) {
                try {
                    String base64 = Base64.getEncoder().encodeToString(img.getBytes());
                    String dataUrl = "data:" + img.getContentType() + ";base64," + base64;
                    Map<String, Object> imageUrl = new HashMap<>();
                    imageUrl.put("url", dataUrl);

                    contentParts.add(Map.of(
                            "type", "image_url",
                            "image_url", imageUrl
                    ));
                } catch (Exception e) {
                    System.err.println("Failed to read image: " + img.getOriginalFilename() + " - " + e.getMessage());
                }
            }
        }

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", contentParts);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4o-mini");
        body.put("messages", List.of(message));
        body.put("max_tokens", 700);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
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

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                System.err.println("No choices in OpenAI response");
                return ResponseEntity.status(500).body(Map.of("error", "Invalid response from OpenAI"));
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> messageResp = (Map<String, Object>) choice.get("message");
            String content = messageResp != null ? (String) messageResp.get("content") : null;

            if (content == null) {
                System.err.println("No content in OpenAI response");
                return ResponseEntity.status(500).body(Map.of("error", "No content in OpenAI response"));
            }

            System.out.println("OpenAI response: " + content);
            return ResponseEntity.ok(Map.of("response", content.trim()));
        } catch (HttpClientErrorException e) {
            System.err.println("OpenAI API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "Failed to process request: " + e.getResponseBodyAsString()));
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }
}
