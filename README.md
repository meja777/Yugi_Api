# 🎴 Duel Masters Arena - Sistema DS3

Aplicación de escritorio en Java que simula duelos estratégicos de Yu-Gi-Oh! con una experiencia visual moderna y mecánicas de combate avanzadas. Desarrollado como parte del laboratorio #1 de Desarrollo de Software III.

## ✨ Características Principales

- 🎯 **Sistema de Posiciones Tácticas**: Ofensiva, Defensiva y Táctica con poderes efectivos únicos
- 🤖 **IA Estratégica Visual**: Selecciones de la IA claramente destacadas con efectos visuales
- 🎨 **Interfaz Moderna**: Layout revolucionario en forma de L con tema oscuro profesional
- 🔄 **Efectos Visuales**: Animaciones de selección, efectos de ganador y feedback inmediato
- 📊 **Estadísticas en Tiempo Real**: Seguimiento detallado de combates y estrategias
- 🌐 **API en Vivo**: Cartas reales obtenidas desde YGOProDeck API

## 🚀 Cómo Ejecutar

### 📋 Requisitos
- Java 11 o superior
- Conexión a internet (para API de cartas)


### 🎮 Cómo Jugar

1. **Cargar Cartas**: Haz clic en "🎲 Cargar Nuevas Cartas" para obtener 3 cartas aleatorias
2. **Iniciar Duelo**: Presiona "⚔️ Iniciar Duelo" cuando las cartas estén listas
3. **Observar IA**: La IA selecciona primero con efectos visuales dorados y animación
4. **Tu Turno**: Haz clic en una de tus cartas (se ilumina en dorado al pasar el mouse)
5. **Elegir Estrategia**: Selecciona posición táctica en el diálogo:
   - **⚔️ Ofensiva**: Usa poder de ataque
   - **🛡️ Defensiva**: Usa poder de defensa  
   - **⚖️ Táctica**: Usa promedio de ambos
6. **Ver Resultado**: Efectos de flash muestran al ganador de la ronda
7. **Ganar**: Primer duelista en ganar 2 rondas es el campeón

## 🏗️ Diseño de Arquitectura

### 📦 Estructura de Packages

```
duelmasters/
├── entities/           # Modelos de datos mejorados
│   ├── DuelCard.java          # Carta con rareza, nivel y métodos de combate
│   ├── BattlePosition.java    # Posiciones tácticas con poder efectivo
│   └── TacticalChoice.java    # Selección estratégica completa
├── engine/             # Motor de duelos y eventos
│   ├── BattleEngine.java      # Lógica de combate con IA estratégica
│   └── CombatEventListener.java # Interface de eventos extendida
├── services/           # Servicios externos robustos
│   └── DuelCardsService.java  # Cliente API con reintentos y logging
├── gui/                # Interfaz gráfica moderna
│   └── DuelArena.java         # UI con layout GridBag y efectos visuales
└── DuelMastersLauncher.java   # Punto de entrada con configuración
```

### 🎯 Flujo Principal Mejorado

1. **Carga Asíncrona**: `DuelCardsService` obtiene cartas con reintentos automáticos y validación de tipo Monster
2. **Layout Revolucionario**: Interface en forma de L con 5 zonas especializadas usando GridBagLayout
3. **IA Visual**: Cuando la IA selecciona, se muestra:
   - ✨ Borde dorado pulsante en la carta seleccionada
   - 🎭 Dialog animado con información de la estrategia
   - ⚡ Indicador "🤖 SELECCIONADA" en la carta
4. **Combate Visual**: Ambas selecciones se destacan durante la resolución
5. **Efectos de Ganador**: Flash de color en toda la ventana según el resultado
6. **Estadísticas Avanzadas**: Panel lateral con métricas en tiempo real

### 🤖 Sistema de IA Estratégica

- **Estrategia Base**: 60% ofensiva, 30% defensiva, 10% táctica
- **Contra-Estrategia**: Responde adaptativamente a la elección del jugador
- **Selección de Cartas**: 70% óptima (mayor poder), 30% aleatoria
- **Visualización**: Efectos visuales claros para mostrar las decisiones de la IA

## 🎨 Mejoras Visuales

### 🌈 Paleta de Colores Moderna
- **Primario**: Negro azulado oscuro (#0f1419)
- **Secundario**: Gris muy oscuro (#1c2128)  
- **Acento**: Dorado vibrante (#ffc72c)
- **Jugador**: Azul brillante (#388bfd)
- **IA**: Rojo suave (#ff6b6b)

### ✨ Efectos Interactivos
- **Hover Effects**: Bordes dorados al pasar sobre cartas
- **Selección IA**: Borde pulsante y indicador visible
- **Flash de Victoria**: Toda la ventana cambia de color según el ganador
- **Animaciones**: Diálogos con barras de progreso para la IA

### 📱 Layout Responsivo en L
```
┌─────────────────── Controles ───────────────────┐
├─────────┬──────────────┬─────────────────────────┤
│ Cartas  │   Arena de   │    Estadísticas         │
│ de IA   │   Combate    │    y Log de             │
├─────────┤   Central    │    Batalla              │
│ Arsenal del Jugador   │                         │
│ (Cartas Interactivas) │                         │
└───────────────────────┴─────────────────────────┘
```

## 🔧 Tecnologías Utilizadas

- **Java 11+**: Lenguaje base con características modernas
- **Swing**: Interfaz gráfica con GridBagLayout avanzado
- **HttpClient**: Cliente HTTP nativo para comunicación con API
- **CompletableFuture**: Programación asíncrona para UI responsiva
- **Timer**: Efectos visuales y animaciones
- **YGOProDeck API**: Fuente de datos de cartas en tiempo real

## 📊 Características Técnicas Avanzadas

### 🔄 Programación Asíncrona
- Carga de cartas sin bloquear la UI
- Descarga de imágenes en background
- Efectos visuales con timers no bloqueantes

### 🛡️ Manejo Robusto de Errores
- Reintentos automáticos en fallos de red
- Timeouts configurables (20 segundos)
- Mensajes de error descriptivos al usuario

### 🎮 Experiencia de Usuario
- Feedback visual inmediato en todas las acciones
- Indicadores claros del estado del juego
- Log cronológico con timestamps automáticos

## 📸 Capturas de Funcionalidades

Las imágenes de referencia están disponibles en la carpeta `docs/`:
- `Incio.png` - Pantalla inicial del duelo
- `Cargar-Cartas.png` - Proceso de carga de cartas
- `Posicion-de-Batalla.png` - Selección de posición táctica
- `Duelo.png` - Arena de combate en acción
- `Final-De-Duelo.png` - Pantalla de resultado final

## 🏆 Cumplimiento de Requisitos

✅ **Todos los requisitos del laboratorio implementados y superados:**
- Consumo de API REST con validación Monster
- POO con separación clara de responsabilidades  
- Listeners para desacoplar UI y lógica de negocio
- Sistema de duelo 2 de 3 rondas
- Interfaz Swing profesional con efectos visuales
- Manejo de errores y validaciones
- Código organizado y bien documentado

**Plus implementados:**
- Sistema de posiciones tácticas (más allá de ATK/DEF simple)
- IA con estrategias adaptativas
- Efectos visuales para mostrar selecciones de IA
- Layout moderno e innovador
- Programación asíncrona avanzada

---

🎮 ¡Disfruta de duelos épicos con visualización completa de la estrategia de IA! ⚔️
