package com.bot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import com.bot.model.FaqItem;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FaqService {
    private final ObjectMapper mapper = new ObjectMapper();
    private List<FaqItem> faqs = new ArrayList<>();
    private final String filePath = "faqs.json";

    @PostConstruct
    public void init() {
        loadFaqs();
    }

    public synchronized void loadFaqs() {
    try {
        File file = new File(filePath);

        if (!file.exists()) {
            try (var is = getClass().getClassLoader().getResourceAsStream("faqs.json")) {
                if (is != null) {
                    java.nio.file.Files.copy(is, file.toPath());
                }
            }
        }

        if (file.exists()) {
            faqs = mapper.readValue(file, new TypeReference<List<FaqItem>>() {});
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    private synchronized void saveToFile() {
        try {
            File file = new File(filePath);
            mapper.writeValue(file, faqs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean addFaq(String keywordsRaw, String answer) {
        if (keywordsRaw == null || answer == null || keywordsRaw.isBlank() || answer.isBlank()) {
            return false;
        }

        List<String> keywords = Arrays.stream(keywordsRaw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (keywords.isEmpty()) return false;

        faqs.add(new FaqItem(keywords, answer.trim()));
        saveToFile();
        return true;
    }

    public synchronized boolean deleteFaq(String keywordToDelete) {
        String target = keywordToDelete.trim().toLowerCase();
        boolean removed = faqs.removeIf(item -> 
            item.keywords().stream().anyMatch(kw -> kw.equalsIgnoreCase(target))
        );

        if (removed) {
            saveToFile();
        }
        return removed;
    }

    public synchronized String listFaqs() {
        if (faqs.isEmpty()) {
            return "No hay preguntas frecuentes registradas.";
        }
        StringBuilder sb = new StringBuilder("📋 *Lista de FAQs actuales:*\n\n");
        for (int i = 0; i < faqs.size(); i++) {
            FaqItem item = faqs.get(i);
            sb.append(i + 1).append(". *Keywords:* `")
              .append(String.join(", ", item.keywords()))
              .append("`\n   *Respuesta:* ")
              .append(item.answer())
              .append("\n\n");
        }
        return sb.toString();
    }

    public String findAnswer(String userText) {
        String normalized = userText.toLowerCase();
        for (FaqItem item : faqs) {
            for (String kw : item.keywords()) {
                if (normalized.contains(kw.toLowerCase())) {
                    return item.answer();
                }
            }
        }
        return "No encontré una respuesta automática a tu consulta. Un administrador te responderá pronto.";
    }

    public File getFaqFile() {
    return new File(filePath);
}

    public synchronized boolean importFromJsonString(String jsonContent) {
        try {
            List<FaqItem> imported = mapper.readValue(jsonContent, new TypeReference<List<FaqItem>>() {});
            for (FaqItem item : imported) {
                if (item.keywords() == null || item.keywords().isEmpty() || item.answer() == null || item.answer().isBlank()) {
                    return false;
                }
            }
            this.faqs = imported;
            saveToFile();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized boolean importFromCsvString(String csvContent) {
    try {
        List<FaqItem> imported = new ArrayList<>();
        String[] lines = csvContent.split("\\r?\\n");

        for (String line : lines) {
            if (line.isBlank() || line.toLowerCase().startsWith("keywords")) continue; // Ignora líneas vacías y cabecera

            // Permite separador ';' o ','
            String[] parts = line.contains(";") ? line.split(";", 2) : line.split(",", 2);
            if (parts.length < 2) continue;

            List<String> keywords = Arrays.stream(parts[0].split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            String answer = parts[1].trim();

            if (!keywords.isEmpty() && !answer.isBlank()) {
                imported.add(new FaqItem(keywords, answer));
            }
        }

        if (imported.isEmpty()) return false;

        this.faqs = imported;
        saveToFile();
        return true;
    } catch (Exception e) {
        return false;
    }
}

    
}