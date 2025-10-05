package duelmasters.entities;

import java.util.Objects;

/**
 * Representación de una carta de duelo en el sistema Duel Masters Arena
 * Encapsula toda la información necesaria para una carta de batalla
 * 
 * @author Sistema DS3 - Duel Masters Team
 * @version 2.0
 */
public class DuelCard {
    
    // Identificadores únicos y básicos
    private final int cardId;
    private final String cardName;
    private final String cardType;
    
    // Estadísticas de combate
    private final int attackPower;
    private final int defensePower;
    private final int level;
    
    // Información descriptiva
    private final String cardDescription;
    private final String imageUrl;
    private final String rarity;
    
    // Estado de la carta
    private boolean isActivated;
    private boolean isInBattle;

    /**
     * Constructor completo para crear una nueva carta de duelo
     * 
     * @param cardId Identificador único de la carta
     * @param cardName Nombre de la carta
     * @param cardType Tipo de carta (Monster, Spell, Trap, etc.)
     * @param attackPower Poder de ataque
     * @param defensePower Poder de defensa
     * @param cardDescription Descripción detallada
     * @param imageUrl URL de la imagen de la carta
     */
    public DuelCard(int cardId, String cardName, String cardType, 
                   int attackPower, int defensePower, String cardDescription, String imageUrl) {
        this.cardId = cardId;
        this.cardName = cardName != null ? cardName : "Carta Misteriosa";
        this.cardType = cardType != null ? cardType : "Desconocido";
        this.attackPower = Math.max(0, attackPower);
        this.defensePower = Math.max(0, defensePower);
        this.cardDescription = cardDescription != null ? cardDescription : "Sin descripción disponible";
        this.imageUrl = imageUrl != null ? imageUrl : "";
        
        // Calcular rareza basada en estadísticas
        this.rarity = calculateRarity(attackPower, defensePower);
        
        // Calcular nivel basado en poder total
        this.level = calculateLevel(attackPower + defensePower);
        
        // Estado inicial
        this.isActivated = false;
        this.isInBattle = false;
    }

    /**
     * Calcula la rareza de la carta basada en sus estadísticas
     */
    private String calculateRarity(int atk, int def) {
        int totalPower = atk + def;
        if (totalPower >= 4000) return "LEGENDARIA";
        if (totalPower >= 3000) return "ÉPICA";
        if (totalPower >= 2000) return "RARA";
        if (totalPower >= 1000) return "COMÚN";
        return "BÁSICA";
    }

    /**
     * Calcula el nivel de la carta basado en su poder total
     */
    private int calculateLevel(int totalPower) {
        if (totalPower >= 4500) return 10;
        if (totalPower >= 4000) return 9;
        if (totalPower >= 3500) return 8;
        if (totalPower >= 3000) return 7;
        if (totalPower >= 2500) return 6;
        if (totalPower >= 2000) return 5;
        if (totalPower >= 1500) return 4;
        if (totalPower >= 1000) return 3;
        if (totalPower >= 500) return 2;
        return 1;
    }

    // Getters para propiedades inmutables
    public int getCardId() { return cardId; }
    public String getCardName() { return cardName; }
    public String getCardType() { return cardType; }
    public int getAttackPower() { return attackPower; }
    public int getDefensePower() { return defensePower; }
    public String getCardDescription() { return cardDescription; }
    public String getImageUrl() { return imageUrl; }
    public String getRarity() { return rarity; }
    public int getLevel() { return level; }

    // Getters y setters para estado mutable
    public boolean isActivated() { return isActivated; }
    public void setActivated(boolean activated) { this.isActivated = activated; }
    
    public boolean isInBattle() { return isInBattle; }
    public void setInBattle(boolean inBattle) { this.isInBattle = inBattle; }

    /**
     * Determina si la carta es una criatura de combate
     */
    public boolean isBattleCreature() {
        return cardType != null && 
               (cardType.toLowerCase().contains("monster") || 
                cardType.toLowerCase().contains("creature") ||
                cardType.toLowerCase().contains("beast"));
    }

    /**
     * Calcula el poder total de combate de la carta
     */
    public int getTotalBattlePower() {
        return attackPower + defensePower;
    }

    /**
     * Verifica si esta carta puede derrotar a otra en combate
     */
    public boolean canDefeat(DuelCard opponent) {
        if (opponent == null || !this.isBattleCreature() || !opponent.isBattleCreature()) {
            return false;
        }
        return this.attackPower > opponent.defensePower;
    }

    /**
     * Representación textual mejorada de la carta
     */
    @Override
    public String toString() {
        return String.format("%s [%s] - ATK:%d/DEF:%d (Nivel %d, %s)", 
                           cardName, cardType, attackPower, defensePower, level, rarity);
    }

    /**
     * Representación compacta para interfaz de usuario
     */
    public String toDisplayString() {
        return String.format("⚔️ %s (%d/%d)", cardName, attackPower, defensePower);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DuelCard)) return false;
        DuelCard other = (DuelCard) obj;
        return cardId == other.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardId);
    }
}
