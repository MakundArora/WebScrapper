package com.example.WebScrapper.Service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ScrapeService {
    private static final Logger LOGGER = Logger.getLogger(ScrapeService.class.getName());

    public Map<String, Object> scrapeUrl(String url) {
        Map<String, Object> result = new HashMap<>();
        try {
            Document doc = Jsoup.connect(url).get();
            result.put("title", doc.title());
            result.put("content", doc.body().text());
            result.put("status", "success");
        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }

    public String extractTextFromPdf(InputStream inputStream) throws IOException {
        // Make sure we can mark and reset the stream
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }

        // Mark the start so we can reset if needed
        inputStream.mark(Integer.MAX_VALUE);

        try (PDDocument document = PDDocument.load(inputStream)) {
            if (document.isEncrypted()) {
                throw new IOException("PDF is encrypted");
            }

            if (document.getNumberOfPages() == 0) {
                throw new IOException("PDF has no pages");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Log some info about the extraction
            LOGGER.info("Extracted " + text.length() + " characters from PDF with " +
                    document.getNumberOfPages() + " pages");

            return text;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "PDF extraction failed", e);

            // Try to reset the stream and check if it's actually a PDF
            try {
                inputStream.reset();
                byte[] header = new byte[5];
                int read = inputStream.read(header);

                if (read < 5 || !new String(header).startsWith("%PDF-")) {
                    throw new IOException("Not a valid PDF file (missing PDF header)");
                }
            } catch (IOException resetException) {
                LOGGER.log(Level.SEVERE, "Failed to check PDF header", resetException);
            }

            throw new IOException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }
}