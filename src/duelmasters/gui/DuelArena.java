package duelmasters.gui;

import duelmasters.services.DuelCardsService;
import duelmasters.engine.CombatEventListener;
import duelmasters.engine.BattleEngine;
import duelmasters.entities.DuelCard;
import duelmasters.entities.BattlePosition;
import duelmasters.entities.TacticalChoice;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Interfaz gráfica principal para el Arena de Duelos
 * Implementa una experiencia visual mejorada con tema oscuro y elementos modernos
 * 
 * @author Sistema DS3 - Duel Masters Team
 * @version 2.0
 */
public class DuelArena extends JFrame implements CombatEventListener {

    // Configuración del juego
    private static final int INITIAL_HAND_SIZE = 3;

    // Paleta de colores moderna y profesional
    private static final Color PRIMARY_DARK = new Color(0x0f1419);      // Negro azulado muy oscuro
    private static final Color SECONDARY_DARK = new Color(0x1c2128);    // Gris muy oscuro
    private static final Color ACCENT_GOLD = new Color(0xffc72c);       // Dorado vibrante
    private static final Color PLAYER_BLUE = new Color(0x388bfd);       // Azul brillante
    private static final Color AI_RED = new Color(0xff6b6b);           // Rojo suave
    private static final Color CARD_BORDER = new Color(0x30363d);       // Gris medio para bordes
    private static final Color SUCCESS_GREEN = new Color(0x56d364);     // Verde éxito
    private static final Color WARNING_ORANGE = new Color(0xdb6e42);    // Naranja advertencia
    private static final Color TEXT_PRIMARY = new Color(0xf0f6fc);      // Blanco principal
    private static final Color TEXT_SECONDARY = new Color(0x8b949e);    // Gris claro

    // Servicios y motores
    private final DuelCardsService cardsService = new DuelCardsService();
    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(4);

    // Componentes de interfaz principales
    private final JPanel playerCardsZone = new JPanel();
    private final JPanel aiCardsZone = new JPanel();
    private final JPanel combatZone = new JPanel();
    private final JTextArea combatLog = new JTextArea();
    
    // Etiquetas de estado
    private final JLabel statusIndicator = new JLabel("🎮 Preparando Arena de Duelos...");
    private final JLabel scoreDisplay = new JLabel("Jugador 0 - 0 IA");
    private final JLabel playerHealthLabel = new JLabel("❤️ Vida: 3");
    private final JLabel aiHealthLabel = new JLabel("🤖 Vida: 3");
    
    // Botones de control
    private final JButton loadCardsButton = new JButton("🎲 Cargar Nuevas Cartas");
    private final JButton startDuelButton = new JButton("⚔️ Iniciar Duelo");
    
    // Zona de combate visual
    private final JLabel playerBattleCard = new JLabel();
    private final JLabel playerBattleInfo = new JLabel("Esperando selección...");
    private final JLabel aiBattleCard = new JLabel();
    private final JLabel aiBattleInfo = new JLabel("IA preparándose...");

    // Estado del juego
    private BattleEngine duelEngine;
    private List<DuelCard> playerHand = List.of();
    private List<DuelCard> aiHand = List.of();
    private final Map<DuelCard, JPanel> playerCardPanels = new HashMap<>();
    private final Map<DuelCard, JPanel> aiCardPanels = new HashMap<>();

    // Datos para resultado de carga
    private static class HandsData {
        final List<DuelCard> playerCards;
        final List<DuelCard> aiCards;

        HandsData(List<DuelCard> playerCards, List<DuelCard> aiCards) {
            this.playerCards = playerCards;
            this.aiCards = aiCards;
        }
    }

    /**
     * Constructor que inicializa la arena de duelos
     */
    public DuelArena() {
        super("⚔️ Duel Masters Arena - Sistema DS3 v2.0");
        initializeArenaTheme();
        buildUserInterface();
        setupEventHandlers();
        
        // Mensaje de bienvenida
        appendToCombatLog("🌟 ¡Bienvenido al Arena de Duelos!");
        appendToCombatLog("🔥 Prepárate para batallas épicas con cartas estratégicas");
        appendToCombatLog("💡 Haz clic en 'Cargar Nuevas Cartas' para comenzar");
    }

    /**
     * Configura el tema visual de la arena
     */
    private void initializeArenaTheme() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1400, 900));
        setLocationRelativeTo(null);
        getContentPane().setBackground(PRIMARY_DARK);

        // Icono de la aplicación (si existiera)
        try {
            // setIconImage(...); // Se podría agregar un icono personalizado
        } catch (Exception e) {
            // Ignorar si no hay icono disponible
        }
    }

    /**
     * Construye toda la interfaz de usuario con layout revolucionario en forma de L
     */
    private void buildUserInterface() {
        // Layout principal usando GridBagLayout para máxima flexibilidad
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.BOTH;

        // 🎮 Panel de Control Superior - Extiende a lo ancho (posición 0,0 - span 3)
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 3; gbc.gridheight = 1;
        gbc.weightx = 1.0; gbc.weighty = 0.1;
        add(createFloatingControlPanel(), gbc);

        // 🤖 Zona IA - Esquina superior izquierda (posición 0,1)
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1; gbc.gridheight = 1;
        gbc.weightx = 0.4; gbc.weighty = 0.35;
        add(createCompactAiPanel(), gbc);

        // ⚔️ Arena de Combate - Centro superior (posición 1,1)
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.gridwidth = 1; gbc.gridheight = 1;
        gbc.weightx = 0.35; gbc.weighty = 0.35;
        add(createVerticalCombatArena(), gbc);

        // 📊 Estadísticas - Esquina superior derecha (posición 2,1)
        gbc.gridx = 2; gbc.gridy = 1;
        gbc.gridwidth = 1; gbc.gridheight = 2;
        gbc.weightx = 0.25; gbc.weighty = 0.9;
        add(createStatsAndLogPanel(), gbc);

        // 🧙‍♂️ Arsenal del Jugador - Fila inferior expandida (posición 0,2 - span 2)
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2; gbc.gridheight = 1;
        gbc.weightx = 0.75; gbc.weighty = 0.55;
        add(createExpandedPlayerArsenal(), gbc);
    }

    /**
     * Crea un panel de control flotante moderno
     */
    private JPanel createFloatingControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
        controlPanel.setBackground(new Color(0x1a1a2e)); // Azul muy oscuro
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedSoftBevelBorder(),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        // Sección izquierda - Estado del juego
        JPanel statusSection = createStatusSection();
        
        // Sección central - Controles principales
        JPanel controlsSection = createControlsSection();
        
        // Sección derecha - Estadísticas en vivo
        JPanel liveStats = createLiveStatsSection();

        controlPanel.add(statusSection);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(controlsSection);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(liveStats);

        return controlPanel;
    }

    /**
     * Crea un panel compacto para las cartas de la IA
     */
    private JScrollPane createCompactAiPanel() {
        JPanel aiPanel = new JPanel();
        aiPanel.setLayout(new GridLayout(0, 2, 8, 8)); // 2 columnas, filas dinámicas
        aiPanel.setBackground(new Color(0x16213e));
        aiPanel.setBorder(BorderFactory.createCompoundBorder(
            createTitledBorder("🤖 Cartas del Oponente", AI_RED),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Reasignar el panel de cartas de IA
        aiCardsZone.removeAll();
        aiCardsZone.setLayout(new GridLayout(0, 2, 8, 8));
        aiCardsZone.setBackground(new Color(0x16213e));

        JScrollPane scrollPane = new JScrollPane(aiCardsZone);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(0x16213e));

        return scrollPane;
    }

    /**
     * Crea una arena de combate vertical innovadora
     */
    private JPanel createVerticalCombatArena() {
        JPanel arena = new JPanel(new BorderLayout(10, 10));
        arena.setBackground(new Color(0x0f0f23));
        arena.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLoweredBevelBorder(),
            createTitledBorder("⚔️ CAMPO DE BATALLA", ACCENT_GOLD)
        ));

        // Arena superior para IA
        JPanel aiArena = createSingleBattleSlot(aiBattleCard, aiBattleInfo, 
                                               "🔥 Zona IA", AI_RED);
        
        // Separador visual dinámico
        JPanel separator = createBattleSeparator();
        
        // Arena inferior para jugador
        JPanel playerArena = createSingleBattleSlot(playerBattleCard, playerBattleInfo, 
                                                   "⚡ Tu Zona", PLAYER_BLUE);

        arena.add(aiArena, BorderLayout.NORTH);
        arena.add(separator, BorderLayout.CENTER);
        arena.add(playerArena, BorderLayout.SOUTH);

        return arena;
    }

    /**
     * Crea panel combinado de estadísticas y log
     */
    private JPanel createStatsAndLogPanel() {
        JPanel container = new JPanel(new BorderLayout(5, 5));
        container.setBackground(PRIMARY_DARK);

        // Panel de estadísticas superior
        JPanel statsPanel = createAdvancedStatsPanel();
        
        // Log de combate inferior
        JScrollPane logScroll = createCompactCombatLog();

        container.add(statsPanel, BorderLayout.NORTH);
        container.add(logScroll, BorderLayout.CENTER);

        return container;
    }

    /**
     * Crea un arsenal expandido horizontal para el jugador
     */
    private JScrollPane createExpandedPlayerArsenal() {
        JPanel arsenalContainer = new JPanel(new BorderLayout());
        arsenalContainer.setBackground(PRIMARY_DARK);

        // Título del arsenal con efectos
        JLabel arsenalTitle = new JLabel("🧙‍♂️ TU ARSENAL ESTRATÉGICO", SwingConstants.CENTER);
        arsenalTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        arsenalTitle.setForeground(ACCENT_GOLD);
        arsenalTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Panel de cartas con layout horizontal mejorado
        playerCardsZone.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));
        playerCardsZone.setBackground(PRIMARY_DARK);

        arsenalContainer.add(arsenalTitle, BorderLayout.NORTH);
        arsenalContainer.add(playerCardsZone, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(arsenalContainer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createLineBorder(PLAYER_BLUE, 2)
        ));
        scrollPane.getViewport().setBackground(PRIMARY_DARK);

        return scrollPane;
    }

    // ========== MÉTODOS AUXILIARES PARA COMPONENTES ==========

    /**
     * Crea la sección de estado del juego
     */
    private JPanel createStatusSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(null);

        configureStatusLabel(statusIndicator, ACCENT_GOLD, Font.BOLD, 14);
        configureStatusLabel(scoreDisplay, TEXT_PRIMARY, Font.BOLD, 16);

        statusIndicator.setAlignmentX(Component.CENTER_ALIGNMENT);
        scoreDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);

        section.add(statusIndicator);
        section.add(Box.createVerticalStrut(5));
        section.add(scoreDisplay);

        return section;
    }

    /**
     * Crea la sección de controles principales
     */
    private JPanel createControlsSection() {
        JPanel section = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        section.setBackground(null);

        stylePrimaryButton(loadCardsButton, SUCCESS_GREEN);
        stylePrimaryButton(startDuelButton, WARNING_ORANGE);
        startDuelButton.setEnabled(false);

        section.add(loadCardsButton);
        section.add(startDuelButton);

        return section;
    }

    /**
     * Crea la sección de estadísticas en vivo
     */
    private JPanel createLiveStatsSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(null);

        configureStatusLabel(playerHealthLabel, PLAYER_BLUE, Font.BOLD, 12);
        configureStatusLabel(aiHealthLabel, AI_RED, Font.BOLD, 12);

        playerHealthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        aiHealthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        section.add(playerHealthLabel);
        section.add(Box.createVerticalStrut(5));
        section.add(aiHealthLabel);

        return section;
    }

    /**
     * Crea un slot individual de batalla
     */
    private JPanel createSingleBattleSlot(JLabel cardImage, JLabel cardInfo, 
                                         String title, Color accentColor) {
        JPanel slot = new JPanel(new BorderLayout(5, 5));
        slot.setBackground(SECONDARY_DARK);
        slot.setPreferredSize(new Dimension(250, 120));
        slot.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(accentColor);

        cardImage.setPreferredSize(new Dimension(80, 80));
        cardImage.setHorizontalAlignment(SwingConstants.CENTER);
        cardImage.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        cardImage.setOpaque(true);
        cardImage.setBackground(PRIMARY_DARK);

        cardInfo.setHorizontalAlignment(SwingConstants.CENTER);
        cardInfo.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        cardInfo.setForeground(TEXT_PRIMARY);

        slot.add(titleLabel, BorderLayout.NORTH);
        slot.add(cardImage, BorderLayout.CENTER);
        slot.add(cardInfo, BorderLayout.SOUTH);

        return slot;
    }

    /**
     * Crea un separador visual para la batalla
     */
    private JPanel createBattleSeparator() {
        JPanel separator = new JPanel();
        separator.setPreferredSize(new Dimension(250, 40));
        separator.setBackground(PRIMARY_DARK);
        
        JLabel vsLabel = new JLabel("⚡ VS ⚡", SwingConstants.CENTER);
        vsLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        vsLabel.setForeground(ACCENT_GOLD);
        
        separator.setLayout(new BorderLayout());
        separator.add(vsLabel, BorderLayout.CENTER);
        
        return separator;
    }

    /**
     * Crea panel de estadísticas avanzadas
     */
    private JPanel createAdvancedStatsPanel() {
        JPanel stats = new JPanel();
        stats.setLayout(new BoxLayout(stats, BoxLayout.Y_AXIS));
        stats.setBackground(SECONDARY_DARK);
        stats.setBorder(BorderFactory.createCompoundBorder(
            createTitledBorder("📊 Estadísticas", ACCENT_GOLD),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        stats.setPreferredSize(new Dimension(280, 150));

        // Etiquetas de estadísticas (se actualizarán dinámicamente)
        JLabel roundsLabel = new JLabel("Rondas: 0");
        JLabel winRateLabel = new JLabel("% Victoria: 0%");
        JLabel avgPowerLabel = new JLabel("Poder Promedio: 0");

        configureStatusLabel(roundsLabel, TEXT_PRIMARY, Font.PLAIN, 11);
        configureStatusLabel(winRateLabel, SUCCESS_GREEN, Font.PLAIN, 11);
        configureStatusLabel(avgPowerLabel, PLAYER_BLUE, Font.PLAIN, 11);

        stats.add(roundsLabel);
        stats.add(Box.createVerticalStrut(5));
        stats.add(winRateLabel);
        stats.add(Box.createVerticalStrut(5));
        stats.add(avgPowerLabel);

        return stats;
    }

    /**
     * Crea un log de combate compacto
     */
    private JScrollPane createCompactCombatLog() {
        combatLog.setEditable(false);
        combatLog.setLineWrap(true);
        combatLog.setWrapStyleWord(true);
        combatLog.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        combatLog.setBackground(SECONDARY_DARK);
        combatLog.setForeground(TEXT_PRIMARY);
        combatLog.setCaretColor(ACCENT_GOLD);
        combatLog.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(combatLog);
        scrollPane.setBorder(createTitledBorder("📜 Log de Batalla", ACCENT_GOLD));
        scrollPane.getViewport().setBackground(SECONDARY_DARK);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        return scrollPane;
    }

    /**
     * Configura los manejadores de eventos
     */
    private void setupEventHandlers() {
        // Botón cargar cartas
        loadCardsButton.addActionListener(e -> loadCardsAsync());

        // Botón iniciar duelo
        startDuelButton.addActionListener(e -> initiateNewDuel());

        // Cerrar aplicación
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                backgroundExecutor.shutdownNow();
                System.exit(0);
            }
        });
    }

    // Métodos auxiliares para UI

    /**
     * Configura una etiqueta de estado
     */
    private void configureStatusLabel(JLabel label, Color color, int style, int size) {
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
    }

    /**
     * Estiliza un botón principal
     */
    private void stylePrimaryButton(JButton button, Color backgroundColor) {
        button.setBackground(backgroundColor);
        button.setForeground(Color.BLACK);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Crea un espaciador horizontal
     */
    private Component createSpacer(int width) {
        return Box.createHorizontalStrut(width);
    }

    /**
     * Crea un borde con título estilizado
     */
    private javax.swing.border.Border createTitledBorder(String title, Color color) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(color, 2),
            title,
            0, 0,
            new Font("Segoe UI", Font.BOLD, 14),
            color
        );
    }

    /**
     * Agrega texto al log de combate con timestamp
     */
    private void appendToCombatLog(String message) {
        SwingUtilities.invokeLater(() -> {
            combatLog.append(String.format("[%02d:%02d] %s%n", 
                System.currentTimeMillis() % 3600000 / 60000,
                System.currentTimeMillis() % 60000 / 1000,
                message));
            combatLog.setCaretPosition(combatLog.getDocument().getLength());
        });
    }

    // ========== IMPLEMENTACIÓN DE CombatEventListener ==========

    @Override
    public void onDuelCommenced(String initiatingPlayer) {
        SwingUtilities.invokeLater(() -> {
            statusIndicator.setText("⚔️ Duelo en curso - " + initiatingPlayer + " inicia");
            appendToCombatLog("🚀 DUELO INICIADO por " + initiatingPlayer);
            startDuelButton.setEnabled(false);
        });
    }

    @Override
    public void onRoundResolved(TacticalChoice playerChoice, TacticalChoice aiChoice, 
                               String attackingPlayer, String roundVictor) {
        SwingUtilities.invokeLater(() -> {
            appendToCombatLog("⚔️ RONDA RESUELTA:");
            appendToCombatLog("  👤 Jugador: " + playerChoice.toCompactString());
            appendToCombatLog("  🤖 IA: " + aiChoice.toCompactString());
            appendToCombatLog("  🏆 Ganador: " + roundVictor);
            
            // Mostrar ambas selecciones claramente
            highlightBothSelections(playerChoice, aiChoice);
            
            // Actualizar zona de combate
            updateBattleZone(playerChoice, aiChoice, roundVictor);
            
            // Efecto visual del ganador
            showRoundWinnerEffect(roundVictor);
        });
    }

    @Override
    public void onScoreUpdated(int playerVictories, int aiVictories) {
        SwingUtilities.invokeLater(() -> {
            scoreDisplay.setText(String.format("Jugador %d - %d IA", playerVictories, aiVictories));
            appendToCombatLog(String.format("📊 Marcador actualizado: %d - %d", 
                            playerVictories, aiVictories));
        });
    }

    @Override
    public void onDuelCompleted(String finalWinner) {
        SwingUtilities.invokeLater(() -> {
            statusIndicator.setText("✅ Duelo completado - " + finalWinner);
            appendToCombatLog("🎊 DUELO FINALIZADO: " + finalWinner);
            appendToCombatLog("🔄 Puedes cargar nuevas cartas para otro duelo");
            
            // Rehabilitar controles
            loadCardsButton.setEnabled(true);
            startDuelButton.setEnabled(false);
            
            // Mostrar dialog de victoria
            showVictoryDialog(finalWinner);
        });
    }

    @Override
    public void onErrorOccurred(String errorMessage, Throwable exception) {
        SwingUtilities.invokeLater(() -> {
            statusIndicator.setText("⚠️ Error: " + errorMessage);
            appendToCombatLog("❌ ERROR: " + errorMessage);
            
            // Rehabilitar controles en caso de error
            loadCardsButton.setEnabled(true);
            
            if (exception != null) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void onCardReplacementNeeded(boolean isPlayerSide) {
        String side = isPlayerSide ? "Jugador" : "IA";
        appendToCombatLog("🔄 " + side + " necesita cartas de reemplazo");
    }

    @Override
    public void onCardsRemovedFromBattle(List<DuelCard> removedPlayerCards, 
                                        List<DuelCard> removedAiCards) {
        SwingUtilities.invokeLater(() -> {
            // Remover cartas usadas de la UI
            removedPlayerCards.forEach(card -> {
                JPanel panel = playerCardPanels.get(card);
                if (panel != null) {
                    playerCardsZone.remove(panel);
                    playerCardPanels.remove(card);
                }
            });
            
            removedAiCards.forEach(card -> {
                JPanel panel = aiCardPanels.get(card);
                if (panel != null) {
                    aiCardsZone.remove(panel);
                    aiCardPanels.remove(card);
                }
            });
            
            playerCardsZone.revalidate();
            playerCardsZone.repaint();
            aiCardsZone.revalidate();
            aiCardsZone.repaint();
            
            appendToCombatLog("🗑️ Cartas usadas removidas del campo");
        });
    }

    @Override
    public void onAiInitialSelection(TacticalChoice aiTacticalChoice) {
        SwingUtilities.invokeLater(() -> {
            appendToCombatLog("🤖 IA seleccionó: " + aiTacticalChoice.toCompactString());
            statusIndicator.setText("🎯 Tu turno - Selecciona tu carta y posición");
            
            // Destacar visualmente la carta seleccionada por la IA
            highlightAiSelectedCard(aiTacticalChoice.getSelectedCard());
            
            // Efecto de animación de selección
            showAiSelectionAnimation(aiTacticalChoice);
        });
    }

    @Override
    public void onDuelStatusChanged(String currentPhase, String statusMessage) {
        SwingUtilities.invokeLater(() -> {
            statusIndicator.setText("📊 " + currentPhase + ": " + statusMessage);
            appendToCombatLog("📋 " + statusMessage);
        });
    }

    @Override
    public void onBattleStatistics(int playerPower, int aiPower, int powerDifference) {
        appendToCombatLog(String.format("⚡ Poder - Jugador: %d, IA: %d (Diferencia: %+d)", 
                        playerPower, aiPower, powerDifference));
    }

    // ========== MÉTODOS DE LÓGICA DEL JUEGO ==========

    /**
     * Carga cartas de forma asíncrona
     */
    private void loadCardsAsync() {
        setLoadingState(true, "🎲 Obteniendo cartas épicas...");
        
        CompletableFuture
            .supplyAsync(this::fetchRandomHands, backgroundExecutor)
            .whenComplete((hands, throwable) -> SwingUtilities.invokeLater(() -> {
                setLoadingState(false, "✅ Cartas cargadas");
                
                if (throwable != null || hands == null) {
                    String message = throwable != null ? throwable.getMessage() : 
                                   "No se pudieron cargar las cartas";
                    onErrorOccurred(message, throwable);
                    return;
                }
                
                this.playerHand = hands.playerCards;
                this.aiHand = hands.aiCards;
                displayPlayerCards();
                displayAiCards();
                startDuelButton.setEnabled(true);
                appendToCombatLog("🎴 Cartas distribuidas - ¡Listo para el duelo!");
            }));
    }

    /**
     * Obtiene cartas aleatorias para ambos jugadores
     */
    private HandsData fetchRandomHands() {
        try {
            List<DuelCard> playerCards = cardsService.fetchRandomBattleCards(INITIAL_HAND_SIZE);
            List<DuelCard> aiCards = cardsService.fetchRandomBattleCards(INITIAL_HAND_SIZE);
            return new HandsData(playerCards, aiCards);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Carga de cartas interrumpida", e);
        } catch (IOException e) {
            throw new RuntimeException("Error de conectividad al cargar cartas: " + e.getMessage(), e);
        }
    }

    /**
     * Inicia un nuevo duelo
     */
    private void initiateNewDuel() {
        if (playerHand.isEmpty() || aiHand.isEmpty()) {
            onErrorOccurred("Primero debes cargar las cartas", null);
            return;
        }
        
        this.duelEngine = new BattleEngine(playerHand, aiHand, this);
        duelEngine.commenceBattle();
        
        // Limpiar zona de combate
        clearBattleZone();
        appendToCombatLog("⚔️ ¡NUEVO DUELO INICIADO!");
    }

    /**
     * Muestra las cartas del jugador
     */
    private void displayPlayerCards() {
        playerCardsZone.removeAll();
        playerCardPanels.clear();
        
        for (DuelCard card : playerHand) {
            JPanel cardPanel = createInteractiveCardPanel(card, true);
            playerCardPanels.put(card, cardPanel);
            playerCardsZone.add(cardPanel);
        }
        
        playerCardsZone.revalidate();
        playerCardsZone.repaint();
    }

    /**
     * Muestra las cartas de la IA con efectos visuales de selección
     */
    private void displayAiCards() {
        aiCardsZone.removeAll();
        aiCardPanels.clear();
        
        for (DuelCard card : aiHand) {
            JPanel cardPanel = createAiCardPanel(card);
            aiCardPanels.put(card, cardPanel);
            aiCardsZone.add(cardPanel);
        }
        
        aiCardsZone.revalidate();
        aiCardsZone.repaint();
    }

    /**
     * Crea un panel para cartas de la IA con efectos de selección
     */
    private JPanel createAiCardPanel(DuelCard card) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(140, 200));
        panel.setBackground(SECONDARY_DARK);
        panel.setBorder(BorderFactory.createLineBorder(AI_RED, 2));

        // Imagen de la carta
        JLabel imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(130, 130));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(PRIMARY_DARK);
        
        // Cargar imagen de forma asíncrona
        loadCardImageAsync(card, imageLabel);

        // Información de la carta
        JLabel infoLabel = new JLabel(String.format("<html><center><b>%s</b><br/>ATK:%d DEF:%d<br/>Nivel %d</center></html>", 
                                    card.getCardName(), card.getAttackPower(), 
                                    card.getDefensePower(), card.getLevel()));
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        infoLabel.setForeground(TEXT_PRIMARY);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Indicador de selección (inicialmente oculto)
        JLabel selectionIndicator = new JLabel("🤖 SELECCIONADA", SwingConstants.CENTER);
        selectionIndicator.setFont(new Font("Segoe UI", Font.BOLD, 10));
        selectionIndicator.setForeground(ACCENT_GOLD);
        selectionIndicator.setOpaque(true);
        selectionIndicator.setBackground(AI_RED);
        selectionIndicator.setVisible(false);

        panel.add(selectionIndicator, BorderLayout.NORTH);
        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(infoLabel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Crea un panel interactivo para cartas del jugador
     */
    private JPanel createInteractiveCardPanel(DuelCard card, boolean isPlayerCard) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(160, 220));
        panel.setBackground(SECONDARY_DARK);
        panel.setBorder(BorderFactory.createLineBorder(
            isPlayerCard ? PLAYER_BLUE : AI_RED, 2));

        // Imagen de la carta
        JLabel imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(150, 150));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(PRIMARY_DARK);
        
        // Cargar imagen de forma asíncrona
        loadCardImageAsync(card, imageLabel);

        // Información de la carta
        JLabel infoLabel = new JLabel(String.format("<html><center><b>%s</b><br/>ATK:%d DEF:%d<br/>Nivel %d</center></html>", 
                                    card.getCardName(), card.getAttackPower(), 
                                    card.getDefensePower(), card.getLevel()));
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        infoLabel.setForeground(TEXT_PRIMARY);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(infoLabel, BorderLayout.SOUTH);

        // Solo las cartas del jugador son clickeables
        if (isPlayerCard) {
            panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            panel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    onPlayerCardSelected(card);
                }
                
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    panel.setBorder(BorderFactory.createLineBorder(ACCENT_GOLD, 3));
                }
                
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    panel.setBorder(BorderFactory.createLineBorder(PLAYER_BLUE, 2));
                }
            });
        }

        return panel;
    }

    /**
     * Crea un panel estático para cartas de la IA
     */
    private JPanel createStaticCardPanel(DuelCard card) {
        return createInteractiveCardPanel(card, false);
    }

    /**
     * Maneja la selección de carta del jugador
     */
    private void onPlayerCardSelected(DuelCard selectedCard) {
        if (duelEngine == null || !duelEngine.isDuelActive()) {
            appendToCombatLog("⚠️ No hay duelo activo");
            return;
        }

        // Mostrar dialog para seleccionar posición
        BattlePosition position = showPositionSelectionDialog();
        if (position == null) {
            return; // Usuario canceló
        }

        TacticalChoice playerChoice = new TacticalChoice(selectedCard, position, "Jugador Humano");
        
        if (duelEngine.setPlayerTacticalChoice(playerChoice)) {
            appendToCombatLog("✅ Seleccionaste: " + playerChoice.toCompactString());
            
            // Si ambos tienen selecciones, resolver ronda
            if (duelEngine.getPendingPlayerChoice() != null && 
                duelEngine.getPendingAiChoice() != null) {
                duelEngine.resolvePendingRound();
            }
        }
    }

    /**
     * Muestra dialog para seleccionar posición de batalla
     */
    private BattlePosition showPositionSelectionDialog() {
        String[] options = {
            "⚔️ Ofensiva (Usar ATK)",
            "🛡️ Defensiva (Usar DEF)", 
            "⚖️ Táctica (Promedio)"
        };
        
        int choice = JOptionPane.showOptionDialog(this,
            "Elige la posición táctica para tu carta:",
            "Selección Estratégica",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
            
        if (choice == -1) return null; // Usuario canceló
        
        return BattlePosition.fromIndex(choice);
    }

    /**
     * Actualiza la zona de combate visual
     */
    private void updateBattleZone(TacticalChoice playerChoice, TacticalChoice aiChoice, String winner) {
        // Actualizar información del jugador
        playerBattleInfo.setText(String.format("<html><center><b>%s</b><br/>%s<br/>Poder: %d</center></html>",
                                playerChoice.getSelectedCard().getCardName(),
                                playerChoice.getBattlePosition().toString(),
                                playerChoice.getEffectiveBattlePower()));

        // Actualizar información de la IA
        aiBattleInfo.setText(String.format("<html><center><b>%s</b><br/>%s<br/>Poder: %d</center></html>",
                            aiChoice.getSelectedCard().getCardName(),
                            aiChoice.getBattlePosition().toString(),
                            aiChoice.getEffectiveBattlePower()));

        // Cargar imágenes de las cartas en combate
        loadCardImageAsync(playerChoice.getSelectedCard(), playerBattleCard);
        loadCardImageAsync(aiChoice.getSelectedCard(), aiBattleCard);
    }

    /**
     * Limpia la zona de combate
     */
    private void clearBattleZone() {
        playerBattleCard.setIcon(null);
        playerBattleCard.setText("Esperando...");
        playerBattleInfo.setText("Selecciona tu carta");
        
        aiBattleCard.setIcon(null);
        aiBattleCard.setText("IA...");
        aiBattleInfo.setText("IA preparándose");
    }

    /**
     * Carga imagen de carta de forma asíncrona
     */
    private void loadCardImageAsync(DuelCard card, JLabel targetLabel) {
        if (card.getImageUrl() == null || card.getImageUrl().isEmpty()) {
            targetLabel.setText(card.getCardName());
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage image = ImageIO.read(URI.create(card.getImageUrl()).toURL());
                return new ImageIcon(image.getScaledInstance(140, 140, Image.SCALE_SMOOTH));
            } catch (Exception e) {
                return null;
            }
        }, backgroundExecutor).whenComplete((icon, throwable) -> SwingUtilities.invokeLater(() -> {
            if (icon != null) {
                targetLabel.setIcon(icon);
                targetLabel.setText("");
            } else {
                targetLabel.setText(card.getCardName());
            }
        }));
    }

    /**
     * Establece estado de carga
     */
    private void setLoadingState(boolean loading, String message) {
        loadCardsButton.setEnabled(!loading);
        loadCardsButton.setText(loading ? "⏳ Cargando..." : "🎲 Cargar Nuevas Cartas");
        if (message != null) {
            statusIndicator.setText(message);
        }
    }

    /**
     * Muestra dialog de victoria
     */
    private void showVictoryDialog(String winner) {
        String message;
        int messageType;
        
        if (winner.contains("Jugador")) {
            message = "🎊 ¡VICTORIA ÉPICA! 🎊\n\nHas demostrado ser un maestro estratega.\n¿Quieres jugar otro duelo?";
            messageType = JOptionPane.INFORMATION_MESSAGE;
        } else if (winner.contains("IA")) {
            message = "💪 La IA ha ganado esta vez.\n\nNo te desanimes, cada derrota es una lección.\n¿Intentar de nuevo?";
            messageType = JOptionPane.WARNING_MESSAGE;
        } else {
            message = "🤝 ¡Empate increíble!\n\nAmbos duelistas mostraron gran habilidad.\n¿Duelo de desempate?";
            messageType = JOptionPane.QUESTION_MESSAGE;
        }
        
        int result = JOptionPane.showConfirmDialog(this, message, "Resultado del Duelo", 
                                                 JOptionPane.YES_NO_OPTION, messageType);
        
        if (result == JOptionPane.YES_OPTION) {
            loadCardsAsync();
        }
    }

    /**
     * Destaca visualmente la carta seleccionada por la IA
     */
    private void highlightAiSelectedCard(DuelCard selectedCard) {
        // Restablecer todas las cartas de IA a estado normal
        resetAllAiCardsHighlight();
        
        // Destacar la carta seleccionada
        JPanel selectedPanel = aiCardPanels.get(selectedCard);
        if (selectedPanel != null) {
            // Cambiar borde a dorado brillante
            selectedPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_GOLD, 4),
                BorderFactory.createLineBorder(Color.WHITE, 1)
            ));
            
            // Mostrar indicador de selección
            Component[] components = selectedPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JLabel && ((JLabel) comp).getText().contains("SELECCIONADA")) {
                    comp.setVisible(true);
                    break;
                }
            }
            
            // Efecto de brillo pulsante
            Timer pulseTimer = new Timer(500, null);
            final boolean[] bright = {true};
            
            pulseTimer.addActionListener(e -> {
                if (bright[0]) {
                    selectedPanel.setBackground(new Color(0x2d3748));
                } else {
                    selectedPanel.setBackground(SECONDARY_DARK);
                }
                bright[0] = !bright[0];
            });
            
            pulseTimer.start();
            
            // Detener el efecto después de 3 segundos
            Timer stopTimer = new Timer(3000, e -> {
                pulseTimer.stop();
                selectedPanel.setBackground(SECONDARY_DARK);
            });
            stopTimer.setRepeats(false);
            stopTimer.start();
        }
    }

    /**
     * Restablece el highlight de todas las cartas de IA
     */
    private void resetAllAiCardsHighlight() {
        for (JPanel panel : aiCardPanels.values()) {
            panel.setBorder(BorderFactory.createLineBorder(AI_RED, 2));
            panel.setBackground(SECONDARY_DARK);
            
            // Ocultar indicadores de selección
            Component[] components = panel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JLabel && ((JLabel) comp).getText().contains("SELECCIONADA")) {
                    comp.setVisible(false);
                    break;
                }
            }
        }
    }

    /**
     * Muestra una animación de selección de la IA
     */
    private void showAiSelectionAnimation(TacticalChoice aiChoice) {
        // Crear un dialog temporal con la selección de la IA
        JDialog selectionDialog = new JDialog(this, "🤖 IA Seleccionando...", true);
        selectionDialog.setSize(400, 200);
        selectionDialog.setLocationRelativeTo(this);
        selectionDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        dialogPanel.setBackground(SECONDARY_DARK);
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Información de la selección
        JLabel selectionInfo = new JLabel(
            String.format("<html><center><h2>🤖 IA ha seleccionado:</h2>" +
                         "<h3>⭐ %s</h3>" +
                         "<p>%s</p>" +
                         "<p>Poder Efectivo: <b>%d</b></p></center></html>",
                         aiChoice.getSelectedCard().getCardName(),
                         aiChoice.getBattlePosition().toString(),
                         aiChoice.getEffectiveBattlePower()),
            SwingConstants.CENTER
        );
        selectionInfo.setForeground(TEXT_PRIMARY);
        selectionInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Barra de progreso para efecto dramático
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Analizando estrategia...");
        progressBar.setForeground(ACCENT_GOLD);
        progressBar.setBackground(PRIMARY_DARK);

        dialogPanel.add(selectionInfo, BorderLayout.CENTER);
        dialogPanel.add(progressBar, BorderLayout.SOUTH);
        selectionDialog.add(dialogPanel);

        // Timer para llenar la barra de progreso
        Timer progressTimer = new Timer(50, null);
        final int[] progress = {0};
        
        progressTimer.addActionListener(e -> {
            progress[0] += 5;
            progressBar.setValue(progress[0]);
            
            if (progress[0] >= 100) {
                progressTimer.stop();
                selectionDialog.dispose();
            }
        });

        // Mostrar dialog y comenzar animación
        SwingUtilities.invokeLater(() -> {
            selectionDialog.setVisible(true);
        });
        
        progressTimer.start();
    }

    /**
     * Limpia todas las selecciones visuales cuando se inicia nueva ronda
     */
    private void clearAllSelections() {
        resetAllAiCardsHighlight();
        
        // También restablecer cartas del jugador
        for (JPanel panel : playerCardPanels.values()) {
            panel.setBorder(BorderFactory.createLineBorder(PLAYER_BLUE, 2));
        }
    }

    /**
     * Destaca visualmente ambas selecciones durante la resolución
     */
    private void highlightBothSelections(TacticalChoice playerChoice, TacticalChoice aiChoice) {
        // Destacar carta del jugador
        JPanel playerPanel = playerCardPanels.get(playerChoice.getSelectedCard());
        if (playerPanel != null) {
            playerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_GOLD, 3),
                BorderFactory.createLineBorder(PLAYER_BLUE, 1)
            ));
        }
        
        // Asegurar que la carta de IA sigue destacada
        highlightAiSelectedCard(aiChoice.getSelectedCard());
    }

    /**
     * Muestra efecto visual del ganador de la ronda
     */
    private void showRoundWinnerEffect(String roundWinner) {
        // Determinar color del efecto según el ganador
        Color effectColor;
        String message;
        
        if (roundWinner.contains("Jugador")) {
            effectColor = SUCCESS_GREEN;
            message = "🎉 ¡VICTORIA DEL JUGADOR!";
        } else if (roundWinner.contains("IA")) {
            effectColor = AI_RED;
            message = "🤖 ¡VICTORIA DE LA IA!";
        } else {
            effectColor = WARNING_ORANGE;
            message = "🤝 ¡EMPATE!";
        }
        
        // Crear efecto de flash en toda la ventana
        Timer flashTimer = new Timer(200, null);
        final int[] flashCount = {0};
        final Color originalBg = getContentPane().getBackground();
        
        flashTimer.addActionListener(e -> {
            if (flashCount[0] % 2 == 0) {
                getContentPane().setBackground(effectColor);
            } else {
                getContentPane().setBackground(originalBg);
            }
            
            flashCount[0]++;
            if (flashCount[0] >= 6) { // 3 flashes
                flashTimer.stop();
                getContentPane().setBackground(originalBg);
            }
        });
        
        // Mostrar mensaje temporal
        statusIndicator.setText(message);
        
        flashTimer.start();
        
        // Restaurar mensaje después de 3 segundos
        Timer restoreTimer = new Timer(3000, e -> {
            statusIndicator.setText("🎮 Preparando siguiente ronda...");
        });
        restoreTimer.setRepeats(false);
        restoreTimer.start();
    }
}
