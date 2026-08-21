package com.bot;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.bot.service.FaqService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException; 

@Component
public class TelegramBotHandler extends TelegramLongPollingBot {

    private final FaqService faqService;
    private final String botUsername;
    private final String TelegramBotToken;

    @Value("#{'${telegram.bot.admin-ids}'.split(',')}")
    private List<Long> adminIds;

    private boolean isAdmin(long userId) {
        return adminIds != null && adminIds.contains(userId);
    }
    
    public TelegramBotHandler(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            FaqService faqService) {
        super(botToken);
        this.TelegramBotToken = botToken;
        this.botUsername = botUsername;
        this.faqService = faqService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) 
    {
        if (!update.hasMessage()) return;

        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();
        boolean userIsAdmin = isAdmin(userId);

        // 1. Manejo prioritario de archivos adjuntos (Protegido para Admin)
        if (update.getMessage().hasDocument()) {
            if (!userIsAdmin) {
                sendMessage(chatId, "No tienes permisos para ejecutar este comando.");
                return;
            }
            handleDocumentImport(chatId, update);
            return;
        }

        // 2. Si no es archivo ni texto se ignora
        if (!update.getMessage().hasText()) return;

        String messageText = update.getMessage().getText().trim();

        // 3. Bloqueo total de comandos para no-admins
        if (!userIsAdmin && messageText.startsWith("/")) {
            sendMessage(chatId, "No tienes permisos para ejecutar este comando.");
            return;
        }

        // 4. Comandos exclusivos para admins
        if (userIsAdmin && messageText.startsWith("/")) 
            {
            if (messageText.equalsIgnoreCase("/start")) {
                sendMessage(chatId, "¡Hola! Estoy listo para responder consultas. Usa /help para ver las opciones disponibles.");
            } 
            else if (messageText.equalsIgnoreCase("/help")) {
                String helpText = """
                    *Comandos disponibles:*
                    
                    • `/listfaqs` - Ver todas las preguntas guardadas
                    • `/addfaq kw1, kw2.... | Respuesta` - Agregar una nueva FAQ (importante poner el | entre la kw y la rta)
                    • `/delfaq keyword` - Eliminar una FAQ por su keyword
                    • `/updatefaqs` - Recargar el archivo JSON en caliente
                    • `/exportfaqs` - Exportar FAQS cargadas en el sistema
                    • `/importfaqs` - Importar FAQS a través de un archivo adjunto (.json o .csv)
                    """;
                sendMessage(chatId, helpText);
            } 
            else if (messageText.equalsIgnoreCase("/listfaqs")) {
                sendMessage(chatId, faqService.listFaqs());
            } 
            else if (messageText.equalsIgnoreCase("/updatefaqs")) {
                faqService.loadFaqs();
                sendMessage(chatId, "Base de FAQs recargada correctamente.");
            } 
            else if (messageText.toLowerCase().startsWith("/addfaq")) {
                handleAddFaq(chatId, messageText);
            } 
            else if (messageText.toLowerCase().startsWith("/delfaq")) {
                handleDeleteFaq(chatId, messageText);
            }
            else if (messageText.toLowerCase().startsWith("/exportfaqs")) {
                handleExportFaqs(chatId);
            } 
            else if (messageText.toLowerCase().startsWith("/importfaqs")) {
                sendMessage(chatId, "Para importar, adjunta un archivo `.json` o `.csv` y escribe `/importfaqs` en el comentario.");
            }
            return;
        }

        // 5. Mensajes regulares (consultas de usuarios normales o admin sin '/')
        String reply = faqService.findAnswer(messageText);
        sendMessage(chatId, reply);
    }

    private void handleAddFaq(long chatId, String fullText) 
    {
        // Formato esperado: /addfaq keyword1, keyword2.... | Respuesta
        String content = fullText.replaceFirst("(?i)/addfaq", "").trim();
        String[] parts = content.split("\\|", 2);

        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            sendMessage(chatId, "Formato inválido.\nUso correcto:\n`/addfaq palabra1, palabra2 | Tu respuesta aquí`");
            return;
        }

        boolean ok = faqService.addFaq(parts[0], parts[1]);
        if (ok) {
            sendMessage(chatId, "FAQ agregada y guardada con éxito.");
        } else {
            sendMessage(chatId, "Error al procesar las palabras clave.");
        }
    }

    private void handleDeleteFaq(long chatId, String fullText) 
    {
        //elimina una rta a traves de una palabra clave
        String keyword = fullText.replaceFirst("(?i)/delfaq", "").trim();
        if (keyword.isBlank()) {
            sendMessage(chatId, "Debes especificar una palabra clave.\nEjemplo: `/delfaq horario`");
            return;
        }

        boolean ok = faqService.deleteFaq(keyword);
        if (ok) {
            sendMessage(chatId, "FAQ eliminada correctamente.");
        } else {
            sendMessage(chatId, "No se encontró ninguna FAQ con la palabra clave: `" + keyword + "`");
        }
    }

    private void sendMessage(long chatId, String text) 
    {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleExportFaqs(long chatId) 
    {
        //importa un archivo json siempre
        File file = faqService.getFaqFile();
        if (!file.exists()) {
            sendMessage(chatId, "No se encontró el archivo de FAQs.");
            return;
        }

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(String.valueOf(chatId));
        sendDocument.setDocument(new InputFile(file, "faqs.json"));
        sendDocument.setCaption("Archivo actual de FAQs.");

        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            sendMessage(chatId, "Error al enviar el archivo.");
        }
    }

    private void handleDocumentImport(long chatId, Update update) 
    {
        //Importa json o csv (pero el sistema trabaja con formato json)
        Document doc = update.getMessage().getDocument();

        if (doc == null) {
            sendMessage(chatId, "Adjunta el archivo con el texto `/importfaqs` en el comentario.");
            return;
        }

        String fileName = doc.getFileName() != null ? doc.getFileName().toLowerCase() : "";
        boolean isJson = fileName.endsWith(".json");
        boolean isCsv = fileName.endsWith(".csv");

        if (!isJson && !isCsv) {
            sendMessage(chatId, "Archivo con extension invalida.\n El archivo cargado debe tener extensión `.json` o `.csv`.");
            return;
        }

        try 
        {
            GetFile getFile = new GetFile();
            getFile.setFileId(doc.getFileId());
            org.telegram.telegrambots.meta.api.objects.File telegramFile = execute(getFile);

            String fileUrl = "https://api.telegram.org/file/bot" + this.TelegramBotToken + "/" + telegramFile.getFilePath();

            try (InputStream in = URI.create(fileUrl).toURL().openStream()) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                boolean success = isJson 
                        ? faqService.importFromJsonString(content) 
                        : faqService.importFromCsvString(content);

                if (success) {
                    sendMessage(chatId, "Base de FAQs importada y actualizada desde `" + doc.getFileName() + "`.");
                } else {
                    sendMessage(chatId, "Estructura de archivo inválida. No se aplicaron cambios.");
                }
            }
        } 
        catch (Exception e) 
        {
            sendMessage(chatId, "Error al procesar el archivo: " + e.getMessage());
        }
    }
}