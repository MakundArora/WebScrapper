package com.example.WebScrapper.Service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class GroqService {
    private static final Logger LOGGER = Logger.getLogger(GroqService.class.getName());

    @Value("${groq.api.key}")
    private String apiKey;
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient();

    public String getLlamaResponse(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return "Error: Empty prompt provided";
        }

        LOGGER.info("Preparing Groq API request with prompt length: " + userPrompt.length());

        try {
            // Check if API key is properly set
            if (apiKey == null || apiKey.trim().isEmpty()) {
                LOGGER.severe("API key is null or empty. Check your application.properties/yml file.");
                return "Error: API key not configured";
            }

            // Required message format
            JSONObject messageObj = new JSONObject()
                    .put("role", "user")
                    .put("content", userPrompt);

            // Build the JSON payload
            JSONObject requestBody = new JSONObject()
                    .put("model", "meta-llama/llama-4-scout-17b-16e-instruct")
                    .put("messages", new JSONArray().put(messageObj))
                    .put("temperature", 1)
                    .put("max_tokens", 1024)
                    .put("top_p", 1)
                    .put("stream", false)
                    .put("stop", JSONObject.NULL);

            // Debug log (mask most of the API key for security)
            String maskedKey = "****" + apiKey.substring(Math.max(0, apiKey.length() - 4));
            LOGGER.info("Using API key (masked): " + maskedKey);

            // Prepare request
            RequestBody body = RequestBody.create(
                    MediaType.get("application/json"),
                    requestBody.toString());

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            // Execute request
            LOGGER.info("Sending request to Groq API...");
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = "";
                    if (response.body() != null) {
                        errorBody = response.body().string();
                    }
                    LOGGER.warning("API error response: " + response.code() + " - " + errorBody);
                    return "Error: " + response.code() + " - " + response.message();
                }

                String responseBody = response.body().string();
                JSONObject responseJson = new JSONObject(responseBody);

                String content = responseJson
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                LOGGER.info("Received successful response from Groq API");
                return content;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException occurred during API call", e);
            return "IOException occurred: " + e.getMessage();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during API call", e);
            return "Unexpected error: " + e.getMessage();
        }
    }
}