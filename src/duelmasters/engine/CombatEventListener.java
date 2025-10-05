package duelmasters.engine;

import duelmasters.entities.TacticalChoice;
import duelmasters.entities.DuelCard;

import java.util.List;

/**
 * Interface para escuchar eventos del motor de duelos
 * Define todos los callbacks necesarios para la gestión de batallas
 * 
 * @author Sistema DS3 - Duel Masters Team
 * @version 2.0
 */
public interface CombatEventListener {
    
    /**
     * Se llama cuando inicia un nuevo duelo
     * 
     * @param initiatingPlayer Nombre del jugador que inicia el duelo
     */
    void onDuelCommenced(String initiatingPlayer);

    /**
     * Se llama cuando se resuelve una ronda de combate
     * 
     * @param playerChoice Elección táctica del jugador humano
     * @param aiChoice Elección táctica de la IA
     * @param attackingPlayer Nombre del jugador que ataca primero
     * @param roundVictor Nombre del ganador de la ronda
     */
    void onRoundResolved(TacticalChoice playerChoice, TacticalChoice aiChoice, 
                        String attackingPlayer, String roundVictor);

    /**
     * Se llama cuando cambia el marcador del duelo
     * 
     * @param playerVictories Victorias del jugador humano
     * @param aiVictories Victorias de la IA
     */
    void onScoreUpdated(int playerVictories, int aiVictories);

    /**
     * Se llama cuando el duelo ha terminado
     * 
     * @param finalWinner Nombre del ganador final del duelo
     */
    void onDuelCompleted(String finalWinner);

    /**
     * Se llama cuando ocurre un error durante el duelo
     * 
     * @param errorMessage Mensaje descriptivo del error
     * @param exception Excepción que causó el error (puede ser null)
     */
    void onErrorOccurred(String errorMessage, Throwable exception);

    /**
     * Se llama cuando se necesita reemplazar cartas agotadas
     * 
     * @param isPlayerSide true si es el lado del jugador, false si es la IA
     */
    void onCardReplacementNeeded(boolean isPlayerSide);

    /**
     * Se llama cuando se remueven cartas del campo de batalla
     * 
     * @param removedPlayerCards Lista de cartas removidas del jugador
     * @param removedAiCards Lista de cartas removidas de la IA
     */
    void onCardsRemovedFromBattle(List<DuelCard> removedPlayerCards, 
                                 List<DuelCard> removedAiCards);

    /**
     * Se llama cuando la IA hace su primera selección estratégica
     * 
     * @param aiTacticalChoice La elección táctica de la IA
     */
    void onAiInitialSelection(TacticalChoice aiTacticalChoice);

    /**
     * Se llama cuando se actualiza el estado del duelo
     * 
     * @param currentPhase Fase actual del duelo
     * @param statusMessage Mensaje de estado actual
     */
    void onDuelStatusChanged(String currentPhase, String statusMessage);

    /**
     * Se llama cuando se calcular estadísticas de batalla
     * 
     * @param playerPower Poder total del jugador
     * @param aiPower Poder total de la IA
     * @param powerDifference Diferencia de poder
     */
    void onBattleStatistics(int playerPower, int aiPower, int powerDifference);
}
