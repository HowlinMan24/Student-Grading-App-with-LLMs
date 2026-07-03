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
    private String API_KEY;

    @Value("${openai.api.url}")
    private String API_URL;

    private static final List<String> MODELS = List.of(
            "google/gemini-2.5-flash",
            "google/gemini-2.5-flash-lite",
            "openai/gpt-4o-mini",
            "google/gemma-4-31b-it:free",
            "google/gemma-4-26b-a4b-it:free"
    );
    private static final int MAX_IMAGES = 5;
    private static final String BASE_SYSTEM_PROMPT =
            "You are an educational assistant for EduEval. You handle two kinds " +
            "of requests: (1) grading a submitted image of already-completed " +
            "student work, and (2) ordinary educational questions — explaining a " +
            "concept, method, or topic — with or without an image attached. " +
            "Answer generic questions directly and helpfully like a normal " +
            "tutor; the ONLY thing you refuse is solving or answering questions " +
            "on behalf of a student who is currently taking a live, in-progress " +
            "exam. " +
            "When grading a submitted image that contains a printed grading " +
            "rubric (point values per task, a task/points table, and/or a grade " +
            "scale mapping total points to a grade band): extract that exact " +
            "rubric and use it verbatim — read the actual printed numbers and " +
            "grade-band labels character by character; never substitute a " +
            "generic, invented, or letter-grade (A/B/C/D/F) scale, and never " +
            "guess a max-points value you cannot clearly read (say so " +
            "explicitly instead). For each task: quote the maximum points " +
            "exactly as printed, briefly describe what the student actually " +
            "wrote or attempted and what in it was correct or incorrect, then " +
            "award the points earned (full, partial, or zero) based on that " +
            "description. The points awarded for a task must never exceed that " +
            "task's printed maximum. Before presenting your answer, silently " +
            "verify that the awarded points per task sum to a total that does " +
            "not exceed the printed grand-total maximum (or the sum of the " +
            "per-task maximums if no grand total is printed) — if your numbers " +
            "don't reconcile, re-read the rubric and redo the arithmetic before " +
            "responding rather than presenting an inflated or unchecked total. " +
            "Enumerate every task shown in the printed rubric — never omit one; " +
            "if a task's max points are unclear, include it and say so rather " +
            "than dropping it. The 'Total' row's Max must equal both the sum " +
            "of the per-task Max values you listed AND the printed grand-total " +
            "maximum, if one is printed — reconcile these before answering. " +
            "Then determine the grade band by checking which printed threshold " +
            "range actually contains your computed total (e.g. a total of 60 " +
            "must map to whichever printed range 60 falls inside, not to a " +
            "range for a different total) — never state a grade-band label " +
            "whose printed threshold range does not contain the total you just " +
            "computed. Present this as a table (Task | Points earned / Max | " +
            "Why, where Why includes what you checked in the student's work) " +
            "followed by the verified total and the resulting grade band, " +
            "using the exact label and threshold text printed on the page. " +
            "If no rubric is printed on the page, fall back to qualitative " +
            "formative feedback without inventing a point total. " +
            "Resolve the mapping of problems to rubric lines internally, then " +
            "write the final table and total exactly once, in a single " +
            "coherent pass — do not restate or recompute the total more than " +
            "once, and do not show alternate, abandoned, or scratch attempts " +
            "at the total in the visible response.";

    private static final String STUDENT_ADDENDUM =
            " Address the student directly ('you'), in an encouraging, " +
            "formative tone — the goal is to help them learn from their own " +
            "mistakes.";

    private static final String TEACHER_ADDENDUM =
            " You are assisting a teacher reviewing a student's submission, " +
            "not the student themself — address the teacher directly, use a " +
            "concise analytical tone, and where relevant call out patterns of " +
            "error worth addressing in class rather than only formative " +
            "encouragement.";

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> chat(
            @RequestPart("prompt") String prompt,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication auth
    ) {
        System.out.println("Chat request from: " + (auth != null ? auth.getName() : "unauthenticated"));

        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
        }

        List<Map<String, Object>> contentParts = new ArrayList<>();
        contentParts.add(Map.of("type", "text", "text", prompt));

        if (images != null) {
            int count = 0;
            for (MultipartFile img : images) {
                if (count >= MAX_IMAGES) break;
                try {
                    String base64 = Base64.getEncoder().encodeToString(img.getBytes());
                    String dataUrl = "data:" + img.getContentType() + ";base64," + base64;
                    contentParts.add(Map.of(
                            "type", "image_url",
                            "image_url", Map.of("url", dataUrl)
                    ));
                    count++;
                } catch (Exception e) {
                    System.out.println("Failed to read image: " + img.getOriginalFilename() + " - " + e.getMessage());
                }
            }
        }

        boolean isTeacher = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
        String systemPrompt = BASE_SYSTEM_PROMPT + (isTeacher ? TEACHER_ADDENDUM : STUDENT_ADDENDUM);

        Map<String, Object> systemMessage = Map.of("role", "system", "content", systemPrompt);
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", contentParts);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("HTTP-Referer", "https://edueval.local");
        headers.set("X-Title", "EduEval");

        String lastError = "All models failed";
        for (String model : MODELS) {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", List.of(systemMessage, userMessage));
            body.put("max_tokens", 16000);
            body.put("temperature", 0.25);
            body.put("frequency_penalty", 0.4);
            body.put("presence_penalty", 0.2);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            try {
                ResponseEntity<Map> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, Map.class);
                Map<String, Object> responseBody = response.getBody();
                if (responseBody == null) continue;

                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices == null || choices.isEmpty()) continue;

                Map<String, Object> messageResp = (Map<String, Object>) choices.get(0).get("message");
                if (messageResp == null) continue;

                String content = (String) messageResp.get("content");
                if (content == null) continue;

                if (isDegenerateRepetition(content)) {
                    System.out.println("Model " + model + " produced degenerate repeated output, trying next model");
                    lastError = model + ": degenerate repeated output";
                    continue;
                }

                System.out.println("Responded using model: " + model);
                return ResponseEntity.ok(Map.of("response", content.trim()));

            } catch (HttpClientErrorException e) {
                lastError = model + ": " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
                System.out.println("Model " + model + " failed: " + lastError);
            } catch (Exception e) {
                lastError = model + ": " + e.getMessage();
                System.out.println("Model " + model + " error: " + lastError);
            }
        }

        return ResponseEntity.status(500).body(Map.of("error", lastError));
    }

    private static final java.util.regex.Pattern DEGENERATE_REPETITION =
            java.util.regex.Pattern.compile("(.{1,20}?)\\1{29,}", java.util.regex.Pattern.DOTALL);

    private boolean isDegenerateRepetition(String content) {
        return DEGENERATE_REPETITION.matcher(content).find();
    }
}
