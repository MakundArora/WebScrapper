# WebScrapper: web and PDF content analyser (Spring Boot + LLM)

A small REST service that **scrapes a web page or extracts text from a PDF** and
sends the content to an **LLM (Llama 4 Scout via the Groq API)** for analysis
or summarisation.

![java](https://img.shields.io/badge/Java-17-orange) ![spring](https://img.shields.io/badge/Spring%20Boot-3.3-green)

## Features

| Endpoint | Input | What it does |
|---|---|---|
| `POST /api/scrape` | `{"url": "https://..."}` | Fetches the page with **jsoup** and returns its title and body text |
| `POST /api/analyze/text` | `{"text": "..."}` | Sends the text to the LLM and returns the response |
| `POST /api/analyze/pdf` | multipart `file` | Extracts text with **Apache PDFBox** (checks for encrypted, empty and non-PDF files), then analyses it with the LLM |

## Tech stack

Java 17, Spring Boot 3.3, jsoup, Apache PDFBox, OkHttp, Groq OpenAI-compatible
chat API, Docker, and Render (deployment config in `render.yaml`).

## Run locally

```bash
export GROQ_API_KEY=your_key_here      # free key from https://console.groq.com
./mvnw spring-boot:run                 # starts on http://localhost:8080
```

```bash
curl -X POST localhost:8080/api/scrape -H "Content-Type: application/json" \
     -d '{"url": "https://example.com"}'

curl -X POST localhost:8080/api/analyze/text -H "Content-Type: application/json" \
     -d '{"text": "Summarise: Spring Boot makes it easy to create stand-alone applications."}'

curl -X POST localhost:8080/api/analyze/pdf -F "file=@paper.pdf"
```

### Docker

```bash
docker build -t webscrapper .
docker run -p 10000:10000 -e PORT=10000 -e GROQ_API_KEY=$GROQ_API_KEY webscrapper
```

## Project structure

```
src/main/java/com/example/WebScrapper/
  Controller/ScrapeController.java   REST endpoints
  Service/ScrapeService.java         jsoup scraping + PDFBox text extraction
  Service/GroqService.java           LLM call (Groq chat completions)
  Config/WebConfig.java              CORS configuration
```

The API key is read from the `GROQ_API_KEY` environment variable and is never
stored in the repository.
