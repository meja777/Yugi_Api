package duelmasters.engine;

import duelmasters.entities.DuelCard;
import duelmasters.entities.BattlePosition;
import duelmasters.entities.TacticalChoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Motor principal de duelos que gestiona las batallas entre jugadores
 * Implementa la lógica de combate estratégico y manejo de turnos
 * 
 * @author Sistema DS3 - Duel Masters Team
 * @version 2.0
 */
public class BattleEngine {

    // Configuración del duelo
    private static final int WINNING_SCORE = 2;
    private static final int INITIAL_LIVES = 3;
    private static final int MIN_DECK_SIZE = 3;

    // Estado del duelo
    private final List<DuelCard> playerDeck;
    private final List<DuelCard> aiDeck;
    private final CombatEventListener eventListener;
    private final Random strategicRandom;

    // Cartas disponibles durante el duelo
    private final List<DuelCard> playerActiveCards;
    private final List<DuelCard> aiActiveCards;

    // Puntuación y estado
    private int playerVictories;
    private int aiVictories;
    private int playerLivesRemaining;
    private int aiLivesRemaining;
    private boolean isPlayerTurn;
    private boolean isDuelActive;

    // Selecciones pendientes
    private TacticalChoice pendingPlayerChoice;
    private TacticalChoice pendingAiChoice;

    // Estadísticas del duelo
    private int totalRoundsPlayed;
    private int playerDirectAttacks;
    private int aiDirectAttacks;

    /**
     * Constructor para inicializar un nuevo motor de duelos
     * 
     * @param playerDeck Mazo de cartas del jugador humano
     * @param aiDeck Mazo de cartas de la IA
     * @param eventListener Listener para eventos del duelo
     */
    public BattleEngine(List<DuelCard> playerDeck, List<DuelCard> aiDeck, 
                       CombatEventListener eventListener) {
        this.playerDeck = new ArrayList<>(Objects.requireNonNull(playerDeck, "El mazo del jugador no puede ser null"));
        this.aiDeck = new ArrayList<>(Objects.requireNonNull(aiDeck, "El mazo de la IA no puede ser null"));
        this.eventListener = Objects.requireNonNull(eventListener, "El listener de eventos no puede ser null");
        this.strategicRandom = new Random();

        // Inicializar cartas activas como copias de los mazos originales
        this.playerActiveCards = new ArrayList<>(this.playerDeck);
        this.aiActiveCards = new ArrayList<>(this.aiDeck);

        // Mezclar para variabilidad estratégica
        Collections.shuffle(this.playerActiveCards, ThreadLocalRandom.current());
        Collections.shuffle(this.aiActiveCards, ThreadLocalRandom.current());
    }

    /**
     * Inicia un nuevo duelo con validación de condiciones iniciales
     */
    public void commenceBattle() {
        // Validar condiciones iniciales
        if (playerDeck.size() < MIN_DECK_SIZE || aiDeck.size() < MIN_DECK_SIZE) {
            eventListener.onErrorOccurred(
                String.format("Ambos duelistas necesitan al menos %d cartas para iniciar", MIN_DECK_SIZE), 
                null);
            return;
        }

        // Resetear estado del duelo
        resetDuelState();

        // Determinar quién inicia estratégicamente
        this.isPlayerTurn = strategicRandom.nextBoolean();
        String initiator = isPlayerTurn ? "Jugador Humano" : "Inteligencia Artificial";

        // Notificar inicio del duelo
        this.isDuelActive = true;
        eventListener.onScoreUpdated(playerVictories, aiVictories);
        eventListener.onDuelCommenced(initiator);
        eventListener.onDuelStatusChanged("INICIANDO", "El duelo ha comenzado. " + initiator + " hace el primer movimiento.");

        // Si la IA inicia, hacer su selección automáticamente
        if (!isPlayerTurn) {
            executeAiFirstSelection();
        }
    }

    /**
     * Resetea todos los valores del estado del duelo
     */
    private void resetDuelState() {
        this.playerVictories = 0;
        this.aiVictories = 0;
        this.playerLivesRemaining = INITIAL_LIVES;
        this.aiLivesRemaining = INITIAL_LIVES;
        this.totalRoundsPlayed = 0;
        this.playerDirectAttacks = 0;
        this.aiDirectAttacks = 0;
        this.pendingPlayerChoice = null;
        this.pendingAiChoice = null;

        // Restaurar cartas activas
        this.playerActiveCards.clear();
        this.playerActiveCards.addAll(playerDeck);
        this.aiActiveCards.clear();
        this.aiActiveCards.addAll(aiDeck);
    }

    /**
     * Establece la elección táctica del jugador humano
     */
    public synchronized boolean setPlayerTacticalChoice(TacticalChoice playerChoice) {
        if (!isDuelActive) {
            eventListener.onErrorOccurred("No hay un duelo activo en curso", null);
            return false;
        }

        if (playerChoice == null || !playerChoice.isValidChoice()) {
            eventListener.onErrorOccurred("La elección táctica no es válida", null);
            return false;
        }

        if (!playerActiveCards.contains(playerChoice.getSelectedCard())) {
            eventListener.onErrorOccurred("La carta seleccionada no está disponible", null);
            return false;
        }

        this.pendingPlayerChoice = playerChoice;

        // Si es turno del jugador, la IA responde automáticamente
        if (isPlayerTurn) {
            executeAiResponse();
        }

        return true;
    }

    /**
     * Ejecuta la primera selección de la IA cuando es su turno
     */
    private void executeAiFirstSelection() {
        DuelCard aiSelectedCard = selectOptimalAiCard();
        if (aiSelectedCard == null) {
            eventListener.onErrorOccurred("La IA no tiene cartas disponibles", null);
            return;
        }

        BattlePosition aiPosition = selectAiStrategy();
        this.pendingAiChoice = new TacticalChoice(aiSelectedCard, aiPosition, "IA Estratégica");

        eventListener.onAiInitialSelection(pendingAiChoice);
        eventListener.onDuelStatusChanged("ESPERANDO_JUGADOR", 
            "La IA ha seleccionado su carta. Es tu turno de responder.");
    }

    /**
     * Ejecuta la respuesta automática de la IA
     */
    private void executeAiResponse() {
        DuelCard aiCard = selectOptimalAiCard();
        if (aiCard == null) {
            eventListener.onErrorOccurred("La IA no tiene cartas disponibles para responder", null);
            return;
        }

        BattlePosition aiPosition = selectCounterStrategy(pendingPlayerChoice);
        this.pendingAiChoice = new TacticalChoice(aiCard, aiPosition, "IA Táctica");

        eventListener.onDuelStatusChanged("RESOLVIENDO", "Ambos jugadores han seleccionado. Resolviendo combate...");
    }

    /**
     * Selecciona la estrategia óptima de la IA
     */
    private BattlePosition selectAiStrategy() {
        // Estrategia básica: 60% ofensiva, 30% defensiva, 10% táctica
        double strategy = strategicRandom.nextDouble();
        if (strategy < 0.6) return BattlePosition.OFFENSIVE;
        if (strategy < 0.9) return BattlePosition.DEFENSIVE;
        return BattlePosition.TACTICAL;
    }

    /**
     * Selecciona una contra-estrategia basada en la elección del jugador
     */
    private BattlePosition selectCounterStrategy(TacticalChoice playerChoice) {
        if (playerChoice == null) return selectAiStrategy();

        BattlePosition playerPosition = playerChoice.getBattlePosition();
        
        // Estrategia reactiva inteligente
        switch (playerPosition) {
            case OFFENSIVE:
                // Contra ataque: 50% defensa, 30% táctica, 20% contraataque
                double counter = strategicRandom.nextDouble();
                if (counter < 0.5) return BattlePosition.DEFENSIVE;
                if (counter < 0.8) return BattlePosition.TACTICAL;
                return BattlePosition.OFFENSIVE;
                
            case DEFENSIVE:
                // Contra defensa: 70% ataque, 30% táctica
                return strategicRandom.nextDouble() < 0.7 ? 
                       BattlePosition.OFFENSIVE : BattlePosition.TACTICAL;
                
            case TACTICAL:
                // Contra táctica: 40% cada una, equilibrado
                double tacticalCounter = strategicRandom.nextDouble();
                if (tacticalCounter < 0.4) return BattlePosition.OFFENSIVE;
                if (tacticalCounter < 0.8) return BattlePosition.DEFENSIVE;
                return BattlePosition.TACTICAL;
                
            default:
                return BattlePosition.DEFENSIVE;
        }
    }

    /**
     * Resuelve la ronda pendiente de combate
     */
    public synchronized void resolvePendingRound() {
        if (!isDuelActive) {
            return;
        }

        if (pendingPlayerChoice == null || pendingAiChoice == null) {
            eventListener.onErrorOccurred("No hay selecciones pendientes para resolver", null);
            return;
        }

        // Calcular poderes efectivos
        int playerPower = pendingPlayerChoice.getEffectiveBattlePower();
        int aiPower = pendingAiChoice.getEffectiveBattlePower();
        int powerDifference = playerPower - aiPower;

        // Notificar estadísticas
        eventListener.onBattleStatistics(playerPower, aiPower, powerDifference);

        // Determinar ganador del round
        String roundWinner = determineRoundWinner(pendingPlayerChoice, pendingAiChoice);
        boolean playerWins = roundWinner.equals("Jugador Humano");

        // Actualizar vidas y puntuación
        updateGameState(playerWins, roundWinner);

        // Remover cartas usadas
        List<DuelCard> removedPlayerCards = List.of(pendingPlayerChoice.getSelectedCard());
        List<DuelCard> removedAiCards = List.of(pendingAiChoice.getSelectedCard());
        
        playerActiveCards.remove(pendingPlayerChoice.getSelectedCard());
        aiActiveCards.remove(pendingAiChoice.getSelectedCard());

        // Notificar eventos
        eventListener.onRoundResolved(pendingPlayerChoice, pendingAiChoice, 
                                    isPlayerTurn ? "Jugador Humano" : "IA Estratégica", roundWinner);
        eventListener.onCardsRemovedFromBattle(removedPlayerCards, removedAiCards);
        eventListener.onScoreUpdated(playerVictories, aiVictories);

        // Limpiar selecciones
        pendingPlayerChoice = null;
        pendingAiChoice = null;
        totalRoundsPlayed++;

        // Verificar condiciones de finalización
        if (shouldEndDuel()) {
            finalizeDuel();
            return;
        }

        // Alternar turno y continuar
        isPlayerTurn = !isPlayerTurn;
        
        if (!isPlayerTurn) {
            executeAiFirstSelection();
        } else {
            eventListener.onDuelStatusChanged("TURNO_JUGADOR", "Es tu turno. Selecciona tu carta y posición.");
        }
    }

    /**
     * Determina el ganador de una ronda específica
     */
    private String determineRoundWinner(TacticalChoice playerChoice, TacticalChoice aiChoice) {
        if (playerChoice.defeatsOpponent(aiChoice)) {
            return "Jugador Humano";
        } else if (aiChoice.defeatsOpponent(playerChoice)) {
            return "IA Estratégica";
        } else {
            return "Empate Táctico";
        }
    }

    /**
     * Actualiza el estado del juego después de una ronda
     */
    private void updateGameState(boolean playerWins, String roundWinner) {
        if (roundWinner.equals("Jugador Humano")) {
            aiLivesRemaining--;
            playerVictories++;
            playerDirectAttacks++;
        } else if (roundWinner.equals("IA Estratégica")) {
            playerLivesRemaining--;
            aiVictories++;
            aiDirectAttacks++;
        }
        // En caso de empate, no se cambian las vidas ni puntuaciones

        // Asegurar que las vidas no sean negativas
        playerLivesRemaining = Math.max(0, playerLivesRemaining);
        aiLivesRemaining = Math.max(0, aiLivesRemaining);
    }

    /**
     * Verifica si el duelo debe terminar
     */
    private boolean shouldEndDuel() {
        return playerVictories >= WINNING_SCORE || 
               aiVictories >= WINNING_SCORE || 
               playerActiveCards.isEmpty() || 
               aiActiveCards.isEmpty() ||
               (playerLivesRemaining <= 0 || aiLivesRemaining <= 0);
    }

    /**
     * Finaliza el duelo y determina el ganador final
     */
    private void finalizeDuel() {
        this.isDuelActive = false;
        
        String finalWinner;
        if (playerVictories > aiVictories) {
            finalWinner = "🏆 Jugador Humano";
        } else if (aiVictories > playerVictories) {
            finalWinner = "🏆 IA Estratégica";
        } else {
            finalWinner = "🤝 Empate Final";
        }

        eventListener.onDuelCompleted(finalWinner);
        eventListener.onDuelStatusChanged("FINALIZADO", 
            String.format("Duelo completado. Rondas jugadas: %d", totalRoundsPlayed));
    }

    /**
     * Selecciona la carta óptima para la IA usando estrategia
     */
    private DuelCard selectOptimalAiCard() {
        if (aiActiveCards.isEmpty()) {
            return null;
        }

        // Estrategia: 70% mejor carta, 30% aleatoria para impredecibilidad
        if (strategicRandom.nextDouble() < 0.7) {
            // Seleccionar la carta con mayor poder total
            return aiActiveCards.stream()
                .max((c1, c2) -> Integer.compare(c1.getTotalBattlePower(), c2.getTotalBattlePower()))
                .orElse(aiActiveCards.get(0));
        } else {
            // Selección aleatoria para variabilidad
            return aiActiveCards.get(strategicRandom.nextInt(aiActiveCards.size()));
        }
    }

    // Getters para el estado del duelo
    public boolean isDuelActive() { return isDuelActive; }
    public int getPlayerVictories() { return playerVictories; }
    public int getAiVictories() { return aiVictories; }
    public int getPlayerLivesRemaining() { return playerLivesRemaining; }
    public int getAiLivesRemaining() { return aiLivesRemaining; }
    public List<DuelCard> getPlayerActiveCards() { return Collections.unmodifiableList(playerActiveCards); }
    public List<DuelCard> getAiActiveCards() { return Collections.unmodifiableList(aiActiveCards); }
    public TacticalChoice getPendingPlayerChoice() { return pendingPlayerChoice; }
    public TacticalChoice getPendingAiChoice() { return pendingAiChoice; }
    public int getTotalRoundsPlayed() { return totalRoundsPlayed; }

    /**
     * Obtiene estadísticas del duelo actual
     */
    public String getDuelStatistics() {
        return String.format("Rondas: %d | Ataques directos - Jugador: %d, IA: %d", 
                           totalRoundsPlayed, playerDirectAttacks, aiDirectAttacks);
    }
}
