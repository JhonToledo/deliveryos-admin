# DeliveryOS Admin (app Android del local)

## Que es
Shell de Capacitor que carga el sitio en vivo: `server.url` = `https://deliveryosapp.com/admin?bypass=plux2026test` (`capacitor.config.json`). Paquete `com.deliveryosapp.admin`, repo `github.com/JhonToledo/deliveryos-admin`.
**Toda la UI y la logica viven en el backend** (repo `deliveryos`, `templates/admin/dashboard.html` y `app.py`). Cambios de HTML/CSS/JS/backend NO requieren recompilar el APK; solo hace falta compilar para cambiar plugins, permisos, iconos, canales de notificacion o `capacitor.config.json`.
Lee primero el `CLAUDE.md` del repo `deliveryos` (reglas generales: espanol, dark mode, paleta, nunca el guion largo, documentar cada cambio, tiempo real por Socket.IO).

## Que aporta lo nativo
- Push FCM (`@capacitor/push-notifications`): pedido nuevo y cancelacion del cliente. Token via `POST /api/admin/fcm-token` (lo hace `_capSetupPush()` del dashboard solo si `Capacitor.isNativePlatform()`).
- `@capacitor/haptics` + permiso `VIBRATE` (vibracion del modal de decision de pedidos).
- `MainActivity.java`: crea el canal **`pedidos_v4`** (sonido `res/raw/alerta_pedido.mp3`, vibracion ~3 s, `USAGE_NOTIFICATION_RINGTONE`), borra `pedidos`, `pedidos_v2` y `pedidos_v3`, y pide la exencion del ahorro de bateria (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; si el usuario dice No, no insiste antes de 12 h).

## Reglas que no se pueden olvidar
- **Los canales de notificacion Android son inmutables**: si cambia el sonido o la vibracion hay que usar un id nuevo (`pedidos_v5`) en TRES sitios: `MainActivity.java`, `PUSH_CHANNEL_ORDERS` en `app.py` (repo deliveryos) y el borrado de canales viejos en `admin/dashboard.html`.
- El nombre de un recurso en `res/raw` va en minusculas, sin guiones y sin empezar por digito (`static/3_firma_doble.mp3` del servidor es `alerta_pedido.mp3` aqui).
- `appendUserAgent: DeliveryOSAdmin/<build>` (hoy 5) deja que el dashboard distinga el APK instalado. Subirlo con cada build junto a `versionCode`/`versionName` de `android/app/build.gradle` (hoy 5 / 1.4).
- El `?bypass=` del `server.url` evita el gate "Proximamente" (`COMING_SOON=1`); sin el, el WebView muestra "Proximamente" y nunca llega al login.
- `node_modules/` e `.idea/` no se versionan (verifica que `.gitignore` termine en salto de linea antes de anexar nada: un `printf >>` ya pego `.idea/` a `node_modules/` y metio 2.000 archivos).

## Compilar (Windows, sin Android Studio)
```bash
export JAVA_HOME="$LOCALAPPDATA/Programs/jdk/jdk-21.0.12.1+1"   # JDK 21 completo (el JBR de PyCharm no sirve: sin jlink)
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
npx cap sync android                      # si cambio capacitor.config.json o un plugin
cd android && ./gradlew.bat assembleDebug --no-daemon
# APK: android/app/build/outputs/apk/debug/app-debug.apk
```
En un equipo nuevo falta `android/local.properties` (ignorado por git): `sdk.dir=C:/Users/<usuario>/AppData/Local/Android/Sdk` (barras normales). El entorno completo (JDK, SDK 36, sdkmanager) esta en el CLAUDE.md de `deliveryos`, seccion "Compilacion de las apps". Tras compilar, copia el APK a `deliveryos/apk/` (carpeta ignorada por git) para instalarlo.

## Firebase
`android/app/google-services.json` (proyecto Firebase "deliveryosapp", una sola cuenta para las 3 apps). Las credenciales server-to-server (`firebase-adminsdk.json`) viven solo en el servidor, nunca en git.

## Flujo de trabajo
Documenta aqui todo cambio nativo (version, canal, permiso). Commits en espanol, primera linea maximo 72 caracteres. Antes de editar archivos compartidos, `git status`.
