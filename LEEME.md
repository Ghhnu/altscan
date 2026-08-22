# AltScan — instrucciones

Este .zip ya es un proyecto Gradle/Fabric COMPLETO (no una plantilla a
medias): tiene `build.gradle`, `settings.gradle`, `gradle.properties`,
el código del mod y el workflow de GitHub Actions. No hace falta
descargar nada de fabricmc.net ni fusionar archivos.

Versiones fijadas ahora mismo (Minecraft 26.1.2):
- Fabric Loader 0.19.3
- Fabric Loom 1.17-SNAPSHOT
- Fabric API 0.155.2+26.1.2
- Java 25

Si tu servidor usa otra versión de Minecraft, edita `gradle.properties`
con los valores de https://fabricmc.net/develop (y si cambia el Java
requerido, ajusta también `sourceCompatibility`/`targetCompatibility`
en `build.gradle` y `java-version` en `.github/workflows/build.yml`).

## 1. Sube el proyecto a GitHub

Desde la carpeta `altscan/` (la raíz, donde está `build.gradle`):

```
git init
git add .
git commit -m "AltScan inicial"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/TU_REPO.git
git push -u origin main
```

## 2. Deja que GitHub Actions compile el .jar

En cuanto hagas el push, la pestaña Actions de tu repo lanzará el
workflow "Build AltScan" solo. Espera a que salga el check verde,
ábrelo, y al final de la página encontrarás el artifact
"altscan-jar" — descárgalo y descomprímelo, dentro está tu
altscan-1.0.0.jar.

(El workflow instala Gradle 9.4.0 él mismo en el runner, así que NO
depende de un gradlew local — no hace falta que exista ese archivo
en el repo.)

## 3. Instálalo en el servidor

1. Asegúrate de que el servidor ya tiene Fabric Loader instalado y
   Fabric API puesto en la carpeta mods/.
2. Copia altscan-1.0.0.jar también a mods/.
3. Reinicia el servidor.

## 4. Uso

- /altscan on -> escanea una vez a todos los jugadores conectados en
  ese momento, ejecutando "alts <nombre>" por cada uno (con ~0.5s de
  separación entre jugador y jugador), y publica cada resultado en el
  chat con el prefijo [AltScan].
- /altscan off -> cancela el escaneo si aún no ha terminado.

Requiere permiso de operador nivel 3, igual que ya tienes para /alts.

## Si quieres compilarlo también en tu PC (opcional)

Necesitas Gradle 9.x instalado localmente (o generar el wrapper una
vez con "gradle wrapper" si tienes Gradle instalado). No es necesario
para que funcione el workflow de GitHub, solo para probarlo en local
antes de subirlo.

## Notas

- El comando se llama /altscan (no /alts) a propósito, para no
  chocar con el /alts <nombre> que ya te da tu otro plugin.
- El ritmo entre jugador y jugador (TICKS_BETWEEN_CHECKS en
  AltScanMod.java, 10 ticks = medio segundo) es ajustable si el
  plugin de /alts tiene cooldown propio.
