package duelmasters.services;

import duelmasters.entities.DuelCard;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio especializado para la obtención y gestión de cartas de duelo
 * Utiliza la API de Yu-Gi-Oh como fuente de datos para las cartas
 * 
 * @author Sistema DS3 - Duel Masters Team
 * @version 2.0
 */
public class DuelCardsService {

    private static final String API_BASE_URL = "https://db.ygoprodeck.com/api/v7";
    private static final String USER_AGENT = "DuelMastersArena/2.0 (+https://duelmasters.ds3)";
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int REQUEST_TIMEOUT_SECONDS = 20;

    private final HttpClient httpClient;

    /**
     * Constructor que inicializa el cliente HTTP con configuración optimizada
     */
    public DuelCardsService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_2) // Usar HTTP/2 para mejor rendimiento
            .build();
    }

    /**
     * Busca una carta específica por su nombre exacto
     * 
     * @param cardName Nombre de la carta a buscar
     * @return Optional con la carta encontrada, o vacío si no existe
     * @throws IOException Si hay problemas de conectividad
     * @throws InterruptedException Si la operación es interrumpida
     */
    public Optional<DuelCard> searchCardByName(String cardName) throws IOException, InterruptedException {
        if (cardName == null || cardName.trim().isEmpty()) {
            return Optional.empty();
        }

        String encodedName = URLEncoder.encode(cardName.trim(), StandardCharsets.UTF_8);
        String requestUrl = API_BASE_URL + "/cardinfo.php?name=" + encodedName;
        
        try {
            String responseBody = executeHttpRequest(requestUrl);
            List<DuelCard> parsedCards = parseCardsFromJson(responseBody);
            
            if (!parsedCards.isEmpty()) {
                System.out.println("✓ Carta encontrada: " + parsedCards.get(0).getCardName());
                return Optional.of(parsedCards.get(0));
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error al buscar carta '" + cardName + "': " + e.getMessage());
        }
        
        return Optional.empty();
    }

    /**
     * Obtiene un conjunto aleatorio de cartas de batalla (solo monstruos)
     * 
     * @param requestedAmount Cantidad de cartas deseadas
     * @return Lista de cartas de batalla obtenidas
     * @throws IOException Si hay problemas de conectividad
     * @throws InterruptedException Si la operación es interrumpida
     */
    public List<DuelCard> fetchRandomBattleCards(int requestedAmount) throws IOException, InterruptedException {
        List<DuelCard> battleCards = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = requestedAmount * 10; // Límite de intentos para evitar bucles infinitos
        
        System.out.println("🎲 Obteniendo " + requestedAmount + " cartas de batalla aleatorias...");
        
        while (battleCards.size() < requestedAmount && attempts < maxAttempts) {
            attempts++;
            
            try {
                String responseBody = executeHttpRequest(API_BASE_URL + "/randomcard.php");
                List<DuelCard> fetchedCards = parseCardsFromJson(responseBody);
                
                for (DuelCard card : fetchedCards) {
                    if (card.isBattleCreature() && !battleCards.contains(card)) {
                        battleCards.add(card);
                        System.out.println("  ⚔️ Agregada: " + card.getCardName() + 
                                         " (Poder: " + card.getTotalBattlePower() + ")");
                        
                        if (battleCards.size() >= requestedAmount) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Intento " + attempts + " fallido: " + e.getMessage());
                // Pequeña pausa antes del siguiente intento
                Thread.sleep(100);
            }
        }

        if (battleCards.size() < requestedAmount) {
            throw new IOException(String.format(
                "Solo se pudieron obtener %d de %d cartas solicitadas después de %d intentos", 
                battleCards.size(), requestedAmount, attempts));
        }

        // Mezclar las cartas para mayor aleatoriedad
        Collections.shuffle(battleCards, ThreadLocalRandom.current());
        
        System.out.println("✅ Obtenidas " + battleCards.size() + " cartas de batalla exitosamente");
        return battleCards;
    }

    /**
     * Ejecuta una petición HTTP con manejo de errores y reintentos
     */
    private String executeHttpRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
            .build();

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    return response.body();
                } else {
                    throw new IOException("Respuesta HTTP inválida: " + response.statusCode());
                }
            } catch (IOException e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    throw new IOException("Falló después de " + MAX_RETRY_ATTEMPTS + " intentos: " + e.getMessage());
                }
                // Pausa exponencial entre reintentos
                Thread.sleep(attempt * 500);
            }
        }
        
        throw new IOException("No se pudo completar la petición HTTP");
    }

    /**
     * Parsea las cartas desde una respuesta JSON de la API
     */
    private List<DuelCard> parseCardsFromJson(String jsonResponse) throws IOException {
        // Buscar el array "data" en la respuesta
        int dataIndex = jsonResponse.indexOf("\"data\"");
        if (dataIndex < 0) {
            return List.of();
        }

        int arrayStartIndex = jsonResponse.indexOf('[', dataIndex);
        if (arrayStartIndex < 0) {
            return List.of();
        }

        int currentIndex = arrayStartIndex + 1;
        List<DuelCard> parsedCards = new ArrayList<>();

        // Procesar cada objeto de carta en el array
        while (currentIndex < jsonResponse.length()) {
            // Saltar espacios en blanco
            while (currentIndex < jsonResponse.length() && 
                   Character.isWhitespace(jsonResponse.charAt(currentIndex))) {
                currentIndex++;
            }

            if (currentIndex >= jsonResponse.length() || jsonResponse.charAt(currentIndex) == ']') {
                break;
            }

            if (jsonResponse.charAt(currentIndex) != '{') {
                currentIndex++;
                continue;
            }

            // Extraer objeto JSON completo
            int braceDepth = 0;
            int objectStart = currentIndex;
            
            while (currentIndex < jsonResponse.length()) {
                char currentChar = jsonResponse.charAt(currentIndex);
                if (currentChar == '{') {
                    braceDepth++;
                } else if (currentChar == '}') {
                    braceDepth--;
                    if (braceDepth == 0) {
                        currentIndex++;
                        break;
                    }
                }
                currentIndex++;
            }

            if (braceDepth == 0) {
                String cardJsonObject = jsonResponse.substring(objectStart, currentIndex);
                DuelCard parsedCard = parseIndividualCard(cardJsonObject);
                if (parsedCard != null) {
                    parsedCards.add(parsedCard);
                }
            }

            // Saltar comas y espacios
            while (currentIndex < jsonResponse.length() && 
                   (Character.isWhitespace(jsonResponse.charAt(currentIndex)) || 
                    jsonResponse.charAt(currentIndex) == ',')) {
                currentIndex++;
            }
        }

        return parsedCards;
    }

    /**
     * Parsea una carta individual desde su representación JSON
     */
    private DuelCard parseIndividualCard(String cardJson) {
        try {
            int cardId = extractIntegerValue(cardJson, "\"id\"\\s*:\\s*(\\d+)", 0);
            String cardName = extractStringValue(cardJson, "\"name\"\\s*:\\s*\"([^\"]+)\"");
            String cardType = extractStringValue(cardJson, "\"type\"\\s*:\\s*\"([^\"]+)\"");
            int attackPower = extractIntegerValue(cardJson, "\"atk\"\\s*:\\s*(null|\\d+)", 0);
            int defensePower = extractIntegerValue(cardJson, "\"def\"\\s*:\\s*(null|\\d+)", 0);
            String description = extractStringValue(cardJson, "\"desc\"\\s*:\\s*\"([^\"]+)\"");
            String imageUrl = extractCardImageUrl(cardJson);

            return new DuelCard(cardId, cardName, cardType, attackPower, 
                               defensePower, description, imageUrl);
        } catch (Exception e) {
            System.err.println("⚠️ Error al parsear carta individual: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extrae un valor entero usando expresión regular
     */
    private int extractIntegerValue(String json, String regex, int defaultValue) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            String value = matcher.group(1);
            if (value != null && !value.equals("null")) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    /**
     * Extrae un valor de texto usando expresión regular
     */
    private String extractStringValue(String json, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            String value = matcher.group(1);
            return unescapeJsonString(value);
        }
        return null;
    }

    /**
     * Extrae la URL de la imagen de la carta desde el array card_images
     */
    private String extractCardImageUrl(String cardJson) {
        int imagesIndex = cardJson.indexOf("\"card_images\"");
        if (imagesIndex < 0) return "";

        int arrayStart = cardJson.indexOf('[', imagesIndex);
        if (arrayStart < 0) return "";

        int index = arrayStart + 1;
        
        // Buscar el primer objeto de imagen
        while (index < cardJson.length()) {
            while (index < cardJson.length() && Character.isWhitespace(cardJson.charAt(index))) {
                index++;
            }
            
            if (index >= cardJson.length() || cardJson.charAt(index) == ']') break;
            if (cardJson.charAt(index) != '{') { index++; continue; }

            int depth = 0;
            int objStart = index;
            
            while (index < cardJson.length()) {
                char c = cardJson.charAt(index);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) { index++; break; }
                }
                index++;
            }

            String imageObj = cardJson.substring(objStart, index);
            String url = extractStringValue(imageObj, "\"image_url\"\\s*:\\s*\"([^\"]+)\"");
            return url != null ? url : "";
        }
        
        return "";
    }

    /**
     * Decodifica caracteres escapados en strings JSON
     */
    private String unescapeJsonString(String input) {
        if (input == null) return null;
        
        return input.replaceAll("\\\\\"", "\"")
                   .replaceAll("\\\\/", "/")
                   .replaceAll("\\\\n", "\n")
                   .replaceAll("\\\\r", "\r")
                   .replaceAll("\\\\t", "\t")
                   .replaceAll("\\\\\\\\", "\\\\");
    }
}
