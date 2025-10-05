package duelmasters.entities;

/**
 * Clase que encapsula la selección táctica de una carta para el combate
 * Combina la carta elegida con su posición de batalla estratégica
 * 
 * @author Sistema DS3 - Duel Masters Team
 * @version 2.0
 */
public class TacticalChoice {
    
    private final DuelCard selectedCard;
    private final BattlePosition battlePosition;
    private final String playerName;
    private final long selectionTimestamp;

    /**
     * Constructor para crear una nueva elección táctica
     * 
     * @param selectedCard La carta seleccionada para el combate
     * @param battlePosition La posición táctica elegida
     * @param playerName Nombre del jugador que hace la elección
     */
    public TacticalChoice(DuelCard selectedCard, BattlePosition battlePosition, String playerName) {
        this.selectedCard = selectedCard;
        this.battlePosition = battlePosition;
        this.playerName = playerName != null ? playerName : "Jugador Anónimo";
        this.selectionTimestamp = System.currentTimeMillis();
    }

    /**
     * Constructor simplificado con jugador por defecto
     */
    public TacticalChoice(DuelCard selectedCard, BattlePosition battlePosition) {
        this(selectedCard, battlePosition, "Jugador");
    }

    /**
     * Calcula el poder efectivo de esta elección táctica
     * 
     * @return El poder de combate efectivo considerando carta y posición
     */
    public int getEffectiveBattlePower() {
        if (selectedCard == null || battlePosition == null) {
            return 0;
        }
        return battlePosition.getEffectivePower(
            selectedCard.getAttackPower(), 
            selectedCard.getDefensePower()
        );
    }

    /**
     * Determina si esta elección puede derrotar a otra en combate
     * 
     * @param opponent La elección táctica del oponente
     * @return true si esta elección gana el combate
     */
    public boolean defeatsOpponent(TacticalChoice opponent) {
        if (opponent == null) {
            return true;
        }
        
        int thisPower = this.getEffectiveBattlePower();
        int opponentPower = opponent.getEffectiveBattlePower();
        
        // En caso de empate, gana quien tenga mayor nivel de carta
        if (thisPower == opponentPower) {
            return this.selectedCard.getLevel() > opponent.selectedCard.getLevel();
        }
        
        return thisPower > opponentPower;
    }

    /**
     * Obtiene una descripción táctica detallada de la elección
     */
    public String getTacticalDescription() {
        if (selectedCard == null || battlePosition == null) {
            return "Elección inválida";
        }
        
        return String.format("%s eligió: %s en %s (Poder efectivo: %d)", 
                           playerName, 
                           selectedCard.getCardName(),
                           battlePosition.getDisplayName(),
                           getEffectiveBattlePower());
    }

    /**
     * Verifica si la elección es válida para el combate
     */
    public boolean isValidChoice() {
        return selectedCard != null && 
               selectedCard.isBattleCreature() && 
               battlePosition != null;
    }

    // Getters
    public DuelCard getSelectedCard() { return selectedCard; }
    public BattlePosition getBattlePosition() { return battlePosition; }
    public String getPlayerName() { return playerName; }
    public long getSelectionTimestamp() { return selectionTimestamp; }

    @Override
    public String toString() {
        return String.format("🎯 %s: %s %s (Poder: %d)", 
                           playerName,
                           selectedCard != null ? selectedCard.getCardName() : "Sin carta",
                           battlePosition != null ? battlePosition.getSymbol() : "❓",
                           getEffectiveBattlePower());
    }

    /**
     * Representación compacta para la interfaz de usuario
     */
    public String toCompactString() {
        return String.format("%s %s (%d)", 
                           battlePosition != null ? battlePosition.getSymbol() : "❓",
                           selectedCard != null ? selectedCard.getCardName() : "???",
                           getEffectiveBattlePower());
    }
}
