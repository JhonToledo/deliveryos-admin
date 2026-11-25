# DeliveryOS Admin (app Android del local)

## Que es
Shell de Capacitor que carga el sitio en vivo: `server.url` = `https://deliveryosapp.com/admin` (`capacitor.config.json`; sin `?bypass` desde el 2026-11-24). Paquete `com.deliveryosapp.admin`, repo `github.com/JhonToledo/deliveryos-admin`.
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
- El `server.url` **ya no lleva `?bypass=`** (quitado el 2026-11-24, paso M15: la tienda esta abierta). Si algun dia se vuelve a cerrar con `COMING_SOON=1`, esta app mostraria "Proximamente" y no llegaria al login. No volver a meter la clave en una app que va a las tiendas.
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

## Firma de lanzamiento para Google Play (paso M17, 2026-11-24)
- Play pide un `.aab` firmado; un APK de depuracion no sirve. Con Play App Signing la llave nuestra es la **llave de subida** (la definitiva la guarda Google).
- **Una sola llave para las tres apps**: `C:\Users\jairo\DeliveryOS-firma-android\deliveryos-subida.jks` (alias `upload`, RSA 4096, valida hasta 2054), FUERA de todo repositorio. La contraseña la eligio el dueño y no esta en ningun archivo: nunca pedirla en el chat ni pasarla escrita en un comando. Huella SHA-256 del certificado (publica): `4D:90:1C:FC:2F:58:7E:5D:E1:2E:7C:26:58:7D:4F:8A:CB:87:23:0C:14:A8:DE:36:B7:43:00:08:20:B0:B4:2C`.
- `build.gradle` no tiene `signingConfig` a proposito: se compila sin firmar y firma `C:\Users\jairo\DeliveryOS-firma-android\firmar.ps1`, que ejecuta **el dueño** (pide la contraseña en su terminal y la borra al terminar). Al lado hay un `LEEME.txt`.
- Version nueva: subir `versionCode` (Play no acepta dos veces el mismo) y `versionName` en `android/app/build.gradle` (y el numero de `appendUserAgent` si la app lo usa), `npx cap sync android`, `cd android && ./gradlew.bat clean bundleRelease --no-daemon`, y el dueño ejecuta `firmar.ps1`. El programa firma las TRES apps a la vez: las tres deben estar compiladas. Salen en `C:\Users\jairo\Documents\DeliveryOS-AAB\` junto con `verificacion-de-firma.txt`.
- `clean` borra `android/app/build`: antes, copiar cualquier APK que se quiera conservar.
- Primer paquete firmado (2026-11-24): `DeliveryOS-Admin-1.4-codigo5.aab`. Revisado por dentro: paquete, version, API 36, no es de depuracion, abre la direccion correcta sin `?bypass` y esta firmado con la llave nueva. **No probado en un telefono**. Subirlo a Play Console lo hace el dueño.
- Un telefono con el APK de depuracion instalado debe **desinstalarlo** antes de instalar la app desde Play (la firma es distinta).
- `server.allowNavigation` entra en este paquete y **nunca se probo en un telefono**: antes de publicar, probar el pago con tarjeta con verificacion del banco (3DS) y que regrese a la app.

## Flujo de trabajo
Documenta aqui todo cambio nativo (version, canal, permiso). Commits en espanol, primera linea maximo 72 caracteres. Antes de editar archivos compartidos, `git status`.

## iOS (fase F7, 2026-11-24): proyecto creado en Windows, SIN compilar ni probar
**Estado**: la carpeta `ios/` existe (`npx cap add ios`: Capacitor 8 con Swift Package Manager, iOS 15 o superior) y está configurada, pero **nunca se ha abierto en Xcode ni compilado**: esta PC es Windows y no hay Mac. Bundle id `com.deliveryosapp.admin`, nombre en pantalla «DeliveryOS Admin», versión 1.0 (compilación 1). Nada de esto toca el APK de Android (excepto lo que se avisa abajo).
**Qué se configuró**:
- `ios/App/App/Info.plist`: permisos con texto en español (ubicación mientras se usa (mapa del local), cámara y fotos (logo, portada y productos)), modos en segundo plano (`remote-notification`) y `ITSAppUsesNonExemptEncryption = false`.
- `ios/App/App/App.entitlements` (`aps-environment` y `com.apple.developer.usernotifications.time-sensitive`: los pedidos nuevos atraviesan No molestar), enlazado en `project.pbxproj` (Debug y Release). El `aps-environment` queda en `development`; Xcode lo pasa a `production` al firmar para la App Store.
- `ios/App/App/AppDelegate.swift`: entrega al plugin de push el token (y el error) de registro; usa Firebase solo si está enlazado (ver «Push» abajo).
- `capacitor.config.json`: bloque `ios` (`contentInset: never`, porque las zonas seguras las maneja el sitio con `viewport-fit=cover`, y `appendUserAgent: DeliveryOSAdmin/ios-1`) y `server.allowNavigation` con `*.pagoplux.com` y `*.cardinalcommerce.com` (el reto 3DS de la tarjeta del local navega a esos sitios y vuelve; sin esto Capacitor los abriría fuera de la app).
- Icono (1024x1024 sin transparencia, como exige la App Store) y pantalla de carga con el logo sobre `#1A202C`, generados con `scripts/gen_ios_assets.py` (`python scripts/gen_ios_assets.py ios/App/App/Assets.xcassets`).
- `codemagic.yaml`: plantilla **sin probar** para compilar y subir a TestFlight desde la nube.
- Comprobado desde Windows (87 comprobaciones estáticas entre los 3 proyectos): llaves del `project.pbxproj` balanceadas, `Info.plist` y entitlements legibles, imágenes de las medidas correctas, `Package.swift` con Capacitor 8 y `codemagic.yaml` con sintaxis válida. **No se compiló nada.**
**Regla**: `ios/App/CapApp-SPM/Package.swift` lo regenera Capacitor en cada `npx cap sync` (no editarlo a mano) y apunta a los plugins dentro de `node_modules`: antes de compilar siempre `npm ci`. `ios/App/App/public` y `ios/App/App/capacitor.config.json` se generan con `npx cap sync ios` y no se versionan.

**Lo que falta (en este orden; nada de esto se puede hacer desde Windows)**:
1. **Cuenta de Apple Developer Program** (99 USD al año) y crear la app en App Store Connect con el bundle id `com.deliveryosapp.admin` (de ahí sale el «Apple ID» numérico que pide `codemagic.yaml`).
2. **Compilar sin Mac**: Codemagic. Crear la integración con App Store Connect (clave de API `.p8` con rol App Manager), ponerle el nombre `DeliveryOS_ASC` (o cambiarlo en `codemagic.yaml`), poner `APP_STORE_APPLE_ID` y lanzar el flujo `ios-testflight`. La plantilla no se ha corrido nunca: el primer intento puede pedir ajustes (versión de Xcode, número de compilación, firma).
3. **Push (FCM)**: en Firebase (proyecto `deliveryosapp`) agregar una app iOS con el bundle id `com.deliveryosapp.admin` y descargar `GoogleService-Info.plist`; subir la llave APNs (`.p8`) en Firebase > Configuración del proyecto > Cloud Messaging > Configuración de la app de Apple; y agregar al target `App` el paquete `https://github.com/firebase/firebase-ios-sdk` con el producto `FirebaseMessaging` y el plist como recurso (en Xcode: File > Add Package Dependencies; sin Mac se puede hacer editando `ios/App/App.xcodeproj/project.pbxproj`, trabajo que se hace desde Windows cuando se tenga el plist). **El `AppDelegate.swift` ya está listo**: usa Firebase solo si el paquete está enlazado y el plist existe (`#if canImport`). Sin esto la push entrega el token de APNs y el servidor lo rechaza con 400 `apns_token` (necesita el de FCM).
4. **Sonido de la alerta**: convertir `static/3_firma_doble.mp3` del repo `deliveryos` a `alerta_pedido.caf` (en una Mac `afconvert -f caff -d LEI16 3_firma_doble.mp3 alerta_pedido.caf`; con ffmpeg `-f caf`, máximo 30 s) y agregarlo al paquete de la app; sin él las ofertas y los pedidos nuevos suenan con el sonido normal de iOS (`send_push` arma el bloque APNs con `<sonido>.caf`).
5. **Probar** en el simulador y después en un iPhone real con TestFlight: isla y barra de inicio (el sitio usa `viewport-fit=cover` y `env(safe-area-inset-*)`; si se ve doble margen, revisar `contentInset`), teclado abierto (cómo reduce la pantalla), giro a horizontal, modo oscuro, notificación con la app cerrada y su sonido, subir una foto de producto y comprar un plan con tarjeta (3DS).
6. **App Store**: textos de privacidad («Privacidad de la app»), capturas, cuenta de prueba para el revisor y las guías de abajo.

**Riesgos propios de esta app**:
- `server.url` ya no lleva `?bypass` (quitado el 2026-11-24, paso M15).
- `server.allowNavigation` está en el `capacitor.config.json` compartido: **también rige en la próxima compilación de Android** (el APK 1.4 actual no lo trae). No se probó en ninguna de las dos plataformas.
- El dashboard crea y borra canales de notificación solo de Android dentro de `try/catch`: en iOS falla en silencio (no rompe nada).
- Guía 4.2 (app que solo envuelve un sitio): es una herramienta de negocio con notificaciones nativas; dar una cuenta de administrador de prueba al revisor.
