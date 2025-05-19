package com.example.WebScrapper.Controller;

import com.example.WebScrapper.Service.GroqService;
import com.example.WebScrapper.Service.ScrapeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
public class ScrapeController {
    private static final Logger LOGGER = Logger.getLogger(ScrapeController.class.getName());

    @Autowired
    private ScrapeService scrapeService;

    @Autowired
    private GroqService groqService;

    @PostMapping("/scrape")
    public ResponseEntity<Map<String, Object>> scrapePost(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        LOGGER.info("Scraping URL: " + url);
        Map<String, Object> result = scrapeService.scrapeUrl(url);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze/text")
    public ResponseEntity<Map<String, Object>> analyzeText(@RequestBody Map<String, String> body) {
        String prompt = body.get("text");
        LOGGER.info("Analyzing text with length: " + (prompt != null ? prompt.length() : "null"));

        if (prompt == null || prompt.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "No text provided for analysis");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        String result = groqService.getLlamaResponse(prompt);
        Map<String, Object> response = new HashMap<>();
        response.put("response", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze/pdf")
    public ResponseEntity<Map<String, Object>> analyzePdf(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        // Validate file
        if (file == null || file.isEmpty()) {
            result.put("error", "No file uploaded or file is empty");
            return ResponseEntity.badRequest().body(result);
        }

        LOGGER.info("Processing PDF file: " + file.getOriginalFilename() +
                ", size: " + file.getSize() +
                ", content type: " + file.getContentType());

        try {
            // Extract text from PDF
            String extractedText = scrapeService.extractTextFromPdf(file.getInputStream());

            // Check if text was extracted
            if (extractedText == null || extractedText.trim().isEmpty()) {
                result.put("error", "No text could be extracted from the PDF");
                return ResponseEntity.ok(result);
            }

            LOGGER.info("Successfully extracted " + extractedText.length() + " characters from PDF");

            // Truncate text if needed for API limits
            String textForAnalysis = extractedText;
            int maxLength = 8000; // Adjust based on Groq API limits
            if (extractedText.length() > maxLength) {
                textForAnalysis = extractedText.substring(0, maxLength);
                LOGGER.info("Text truncated from " + extractedText.length() +
                        " to " + maxLength + " characters for API call");
            }

            // Add a summary request to the prompt
            textForAnalysis = "Please analyze and summarize the following document: \n\n" + textForAnalysis;

            // Get response from Groq API
            String groqResponse = groqService.getLlamaResponse(textForAnalysis);

            // Check if Groq API returned an error
            if (groqResponse != null && groqResponse.startsWith("Error:")) {
                LOGGER.warning("Groq API returned an error: " + groqResponse);
                result.put("extracted_text", extractedText);
                result.put("groq_response", groqResponse);
                result.put("warning", "API call failed but text extraction was successful");
            } else {
                result.put("extracted_text", extractedText);
                result.put("groq_response", groqResponse);
                result.put("status", "success");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing PDF", e);
            result.put("error", "Failed to process PDF: " + e.getMessage());
            result.put("stack_trace", e.getStackTrace()[0].toString());

            // Add diagnostic information
            result.put("file_name", file.getOriginalFilename());
            result.put("file_size", file.getSize());
            result.put("content_type", file.getContentType());
        }

        return ResponseEntity.ok(result);
    }
}