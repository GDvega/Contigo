# Contigo — Android

Repositorio centrado en la app móvil nativa (`android/`). Al modificar código:

- Sigue convenciones Kotlin, Jetpack Compose y Hilt del proyecto.
- No reintroduzcas dependencias web ni backends Node en la raíz.
- Datos en producción: Room local + Firebase (Firestore, FCM, Storage); ver `android/ARCHITECTURE.md` y `docs/FIREBASE_RULES.md`.
# Instrucciones para Codex

Responde siempre en español.

Antes de analizar o editar código, usa CodeGraph si el cambio involucra arquitectura, dependencias, flujos, refactors, bugs o sincronización.

Como las tools MCP `codegraph_*` pueden no estar expuestas en esta sesión, usa CodeGraph por CLI:

- `codegraph status`
- `codegraph search <símbolo>`
- `codegraph callers <símbolo>`
- `codegraph callees <símbolo>`
- `codegraph impact <símbolo>` si está disponible

También puedes consultar directamente:

- `.codegraph/codegraph.db`
- `docs/full-codegraph.dot`

Reglas:
1. No uses solo `rg`/`grep`; úsalos solo para confirmar código real después de consultar CodeGraph.
2. Antes de editar archivos críticos, muestra:
   - símbolo central
   - callers
   - callees
   - impacto esperado
   - plan mínimo de cambio
3. Espera aprobación antes de editar si el cambio toca:
   - FirebaseSyncManager
   - firestore.rules
   - Room / DAOs
   - recordatorios
   - backup
   - sincronización
   - navegación
4. Después de editar, ejecuta:
   - `./gradlew testDebugUnitTest`
   - `./gradlew :app:compileDebugKotlin`
5. Trabaja por hallazgo. No corrijas varios hallazgos en una sola tarea.
