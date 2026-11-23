# The Folk — mod de terror para Minecraft Java 1.20.1 (Forge)

## Qué hace
### The Folk
Entidad tipo dweller de 3 bloques de alto. Ciclo de fases que se repite:

| Fase | Normal | ERROR / COMING | Comportamiento |
|---|---|---|---|
| 1 | 5 días | 2 días | Aparece lejos, a la vista, y solo te mira. Desaparece si te acercas a menos de 12 bloques. |
| 2 | 8 días | 4 días | Aparece a la vista y camina lento hacia ti. Al llegar a ~5 bloques se queda quieto y desaparece. |
| 3 | 5 días | 2 días | Aparece escondido (14-24 bloques), te persigue 1 minuto más rápido que tú, rompe bloques y golpea (3 corazones). |
| 4 | no existe | 2 días | Igual que la 3 pero con el DOBLE de velocidad. |

### FolkConfig (el engranaje)
Item `thefolk:folkconfig`. Clic derecho abre un menú con 3 botones:
- **Normal**: el mod funciona como siempre.
- **ERROR**: las fases pasan más rápido y se agrega la fase 4 (The Folk al doble de velocidad).
- **THE FOLK IS COMING**: todo lo de ERROR, y además aparece The Thing.

El modo se guarda en el mundo. Para conseguir el item: pestaña "Herramientas y utilidades" del modo creativo,
o `/give @s thefolk:folkconfig`.

### The Thing (solo en THE FOLK IS COMING)
Criatura nueva, más rápida que The Folk. Aparece escondida cerca de ti, muestra el texto rojo "Im C0MING"
en pantalla, abre puertas, hace ruidos saturados (gritos a todo volumen superpuestos), te ataca con 12 corazones
de daño y desaparece a los 20 segundos. Vuelve a aparecer cada 2.5 a 7.5 minutos.

Ambas entidades son invulnerables (solo el modo creativo o /kill las eliminan). Solo aparecen en el Overworld,
fuera de dificultad Pacífico y no con jugadores en creativo/espectador.

## Comandos de prueba (necesitan permisos de operador / trucos activados)
- `/thefolk spawn 1|2|3|4` — invoca a The Folk en esa fase
- `/thefolk thing` — invoca a The Thing
- `/thefolk mode normal|error|coming` — cambia el modo sin usar el item
- `/thefolk status` — muestra modo, día del mundo, día del ciclo y fase
- `/time add 24000` suma un día para saltar de fase

## Ajustes
Bloque CONFIGURACIÓN al inicio de estos archivos:
- `FolkDirector.java`: duración de las fases por modo, cada cuánto aparecen, distancias
- `TheFolkEntity.java`: velocidades, daño, duración de la persecución
- `TheThingEntity.java`: velocidad, daño, duración, volumen de los ruidos

## Compilar
Sube el contenido de esta carpeta a tu repositorio de GitHub (src, build.gradle, settings.gradle,
gradle.properties y .github/workflows/build.yml) y descarga el .jar desde Actions → Artifacts.

## Instalar en TLauncher
1. Versión **1.20.1 con Forge**.
2. Pon el `.jar` en `%appdata%\.minecraft\mods`.
3. Abre el juego con el perfil de Forge.
