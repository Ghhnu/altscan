# AltScan — instrucciones

## 1. Genera la base del proyecto (recomendado)

Como Minecraft ha cambiado a versionado por año (26.1, 26.2...) y las
versiones de Loom/Gradle/Fabric API cambian a menudo, lo más fiable es
generar tú mismo la plantilla oficial con las versiones exactas de tu
servidor, y luego pegar encima los 2 archivos de este paquete:

1. Ve a https://fabricmc.net/develop/template/
2. Elige la versión de Minecraft de tu servidor (la real, revisa con
   `/version` en consola o mira el jar del server).
3. Marca solo "Dedicated Server" como entorno (o "Client and Server" si
   quieres poder probarlo en singleplayer también).
4. Descarga el zip generado y descomprímelo.

## 2. Copia los archivos de este paquete

Sustituye/añade dentro de esa plantilla descargada:

- `src/main/java/com/tuusuario/altscan/AltScanMod.java`
- `src/main/resources/fabric.mod.json` (si la plantilla ya trae uno,
  fusiona el `entrypoints` y el `depends` con el tuyo, no lo borres entero)

Si quieres, cambia el paquete `com.tuusuario.altscan` por el tuyo propio
(y ajusta la ruta de carpetas igual).

## 3. Compila el .jar

Desde la carpeta del proyecto:

```
./gradlew build
```

(en Windows: `gradlew.bat build`)

El .jar final aparece en `build/libs/altscan-1.0.0.jar`.

## 4. Instálalo en el servidor

1. Asegúrate de que el servidor ya tiene **Fabric Loader** instalado y
   **Fabric API** puesto en la carpeta `mods/` (AltScan depende de ella).
2. Copia `altscan-1.0.0.jar` también a la carpeta `mods/` del servidor.
3. Reinicia el servidor.

## 5. Uso

- `/altscan on` → escanea una vez a todos los jugadores conectados en ese
  momento, ejecutando `alts <nombre>` por cada uno con ~0.5s de separación,
  y publica cada resultado en el chat con el prefijo `[AltScan]`.
- `/altscan off` → cancela el escaneo si aún no ha terminado de recorrer
  a todos los jugadores.

Requiere nivel de permiso de operador 3 (el mismo nivel típico de comandos
de staff), igual que ya tienes para `/alts`.

## Notas

- El comando se llama `/altscan` (no `/alts`) a propósito, para no chocar
  con el `/alts <nombre>` que ya te da tu otro plugin.
- El ritmo entre jugador y jugador (`TICKS_BETWEEN_CHECKS` en el código,
  10 ticks = medio segundo) es ajustable si el plugin de `/alts` tiene
  cooldown propio y necesitas ir más despacio.
- Si tu servidor es realmente survival vanilla, no supermodded, este mod
  no toca nada del gameplay: solo añade el comando.
