package duelmasters;

import duelmasters.gui.DuelArena;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Punto de entrada principal para la aplicación Duel Masters Arena
 * Sistema de duelos de cartas basado en Yu-Gi-Oh API
 * 
 * @author Sistema DS3 - Duel Masters Team
 * @version 2.0
 */
public class DuelMastersLauncher {

    /**
     * Configuración inicial de la aplicación
     */
    private static void setupApplication() {
        try {
            // Establecer look and feel del sistema para mejor integración
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | 
                 IllegalAccessException | UnsupportedLookAndFeelException e) {
            System.err.println("Advertencia: No se pudo establecer el look and feel del sistema");
        }
        
        // Configurar propiedades del sistema para mejor rendimiento
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    /**
     * Método principal que inicia la aplicación Duel Masters Arena
     * @param args Argumentos de línea de comandos (no utilizados)
     */
    public static void main(String[] args) {
        // Mostrar información de inicio
        System.out.println("=== DUEL MASTERS ARENA ===");
        System.out.println("Iniciando sistema de duelos...");
        System.out.println("Versión: 2.0 - DS3 Edition");
        System.out.println("==========================");
        
        // Configurar aplicación
        setupApplication();
        
        // Iniciar interfaz gráfica en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> {
            try {
                DuelArena arena = new DuelArena();
                arena.setVisible(true);
                System.out.println("Arena de duelos iniciada correctamente");
            } catch (Exception e) {
                System.err.println("Error crítico al iniciar la arena: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
