# Seguridad Firebase — Contigo

Checklist operativo para proteger el proyecto `cuidavoz-b4197` y la app Android `com.cuidavoz.mobile`.

## 1. Rotar y restringir la API key de Android

El archivo real `android/app/google-services.json` no se sube a Git. Copia `android/app/google-services.example.json`, completa los datos desde Firebase Console y manten el archivo real solo local/CI.

La API key de Android aparece en el APK. **No es un secreto de servidor**, pero si estuvo expuesta en Git debe rotarse y quedar restringida:

1. Abre [Google Cloud Console → Credentials](https://console.cloud.google.com/apis/credentials?project=cuidavoz-b4197).
2. Rota o crea una API key Android nueva para Firebase.
3. **Application restrictions** → Android apps → añade:
   - Package: `com.cuidavoz.mobile`
   - SHA-1 del certificado de **debug** y de **release** (Play App Signing incluido).
4. **API restrictions** → limita a:
   - Firebase Installations API
   - Cloud Firestore API
   - Firebase Cloud Messaging API
   - Firebase Storage API
   - Identity Toolkit API (Auth)
5. Descarga el nuevo `google-services.json` y guardalo como `android/app/google-services.json`.

## 2. Activar Firebase App Check

La app ya instala App Check en código:

- **Debug:** `DebugAppCheckProviderFactory` (registra el token debug en Firebase Console).
- **Release:** `PlayIntegrityAppCheckProviderFactory`.

Pasos en consola:

1. Firebase Console → App Check → registrar la app Android.
2. Habilitar **Play Integrity** para producción.
3. En Firestore y Storage, activar ** enforcement** cuando las métricas muestren tráfico legítimo con App Check.

## 3. Reglas desplegadas

Despliega las reglas versionadas en el repo:

```bash
firebase deploy --only firestore:rules,storage
```

Cambios recientes:

- `linkCodes/{code}`: `get` solo si el código **no expiró**.
- Storage: imágenes limitadas a 5 MB y `image/*`.

## 4. Emulador local (solo desarrollo)

No hardcodear IPs personales en el repo.

1. Copia `android/local.properties.example` → `android/local.properties`.
2. Opcional: `firebase.emulator.host=10.0.2.2` (emulador Android) o tu IP LAN.
3. Cleartext HTTP en debug solo para `10.0.2.2`, `localhost` y `127.0.0.1`.

## 5. Respaldos cifrados

Los respaldos exportados desde la app se cifran con **AES-256-GCM** y **PBKDF2** (contraseña del usuario, mínimo 8 caracteres).

- Guarda la contraseña aparte del archivo.
- Los respaldos antiguos sin cifrado siguen importables dejando la contraseña vacía.

## 6. Códigos de vinculación

- Nuevos códigos: **10 caracteres** alfanuméricos (`LinkCodeGenerator`).
- Compatibilidad con códigos legacy de 6 dígitos.
- Rate limit local: 5 intentos fallidos / 15 minutos (`LinkCodeRateLimiter`).

## 7. Archivos que no deben subirse a Git

Ya ignorados o movidos a plantillas:

- `android/local.properties`
- `*.keystore`, `*.jks`
- `.cursor/`, `.codex/`, `.claude/`, `.mcp.json`
- `.codegraph/daemon.*`, `.codegraph/*.db`

## 8. Rotación periódica

| Elemento | Frecuencia sugerida |
|----------|---------------------|
| API key Firebase | Tras filtración o cada 12 meses |
| Reglas Firestore/Storage | Revisar en cada release |
| Certificado release | Según política de Play Store |
| Tokens debug App Check | Al cambiar de máquina de desarrollo |
