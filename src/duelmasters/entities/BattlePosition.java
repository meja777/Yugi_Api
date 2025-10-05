package duelmasters.entities;

/**
 * Enumeración que define las posiciones tácticas de las cartas en el campo de batalla
 * Cada posición tiene características específicas para el combate
 * 
 * @author Sistema DS3 - Duel Masters Team
 * @version 2.0
 */
public enum BattlePosition {
    
    /** Posición ofensiva - usa poder de ataque para combate */
    OFFENSIVE("Posición Ofensiva", "⚔️", true),
    
    /** Posición defensiva - usa poder de defensa para combate */
    DEFENSIVE("Posición Defensiva", "🛡️", false),
    
    /** Posición táctica - balanceada entre ataque y defensa */
    TACTICAL("Posición Táctica", "⚖️", true);

    private final String displayName;
    private final String symbol;
    private final boolean allowsDirectAttack;

    /**
     * Constructor para definir las características de cada posición
     * 
     * @param displayName Nombre visual para mostrar al usuario
     * @param symbol Símbolo representativo de la posición
     * @param allowsDirectAttack Si permite ataques directos al oponente
     */
    BattlePosition(String displayName, String symbol, boolean allowsDirectAttack) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.allowsDirectAttack = allowsDirectAttack;
    }

    /**
     * Obtiene la posición basada en un índice numérico
     * 
     * @param index Índice (0=OFFENSIVE, 1=DEFENSIVE, 2=TACTICAL)
     * @return La posición correspondiente al índice
     */
    public static BattlePosition fromIndex(int index) {
        switch (index) {
            case 0: return OFFENSIVE;
            case 1: return DEFENSIVE;
            case 2: return TACTICAL;
            default: return DEFENSIVE; // Posición segura por defecto
        }
    }

    /**
     * Obtiene una posición aleatoria para la IA
     */
    public static BattlePosition getRandomPosition() {
        BattlePosition[] positions = values();
        int randomIndex = (int) (Math.random() * positions.length);
        return positions[randomIndex];
    }

    /**
     * Calcula el poder efectivo de una carta en esta posición
     * 
     * @param attackPower Poder de ataque de la carta
     * @param defensePower Poder de defensa de la carta
     * @return El poder efectivo para esta posición
     */
    public int getEffectivePower(int attackPower, int defensePower) {
        switch (this) {
            case OFFENSIVE:
                return attackPower;
            case DEFENSIVE:
                return defensePower;
            case TACTICAL:
                return (attackPower + defensePower) / 2;
            default:
                return defensePower;
        }
    }

    // Getters
    public String getDisplayName() { return displayName; }
    public String getSymbol() { return symbol; }
    public boolean allowsDirectAttack() { return allowsDirectAttack; }

    @Override
    public String toString() {
        return symbol + " " + displayName;
    }
}
