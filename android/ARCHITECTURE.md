# Contigo Android — Arquitectura

## Capas

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| UI | `ui.screens`, `ui.navigation`, `ui.components` | Compose, navegación type-safe |
| Presentación | `ui.viewmodel` | Estado de pantalla con `StateFlow` |
| Dominio | `domain` | Reglas de negocio puras (sin Android) |
| Datos | `data.repository`, `data.local`, `data.sync` | Room, DataStore, Firebase |

## Inyección de dependencias (Hilt)

- `@HiltAndroidApp` en `ContigoApp`
- Módulos en `di/`: `DatabaseModule`, `AppModule`, `CoroutinesModule`
- ViewModels: `@HiltViewModel` + `hiltViewModel()` en Compose
- Servicios en segundo plano (alarmas, receivers): `ContigoApp.appContainer` (singleton Hilt)

## Navegación

Rutas serializables en `ContigoDestination` (Navigation Compose 2.8+).

```kotlin
navController.navigate(ContigoDestination.PatientHome)
```

## Datos de usuario

- La app **no siembra** pacientes, medicamentos ni contactos de demostración.
- `LegacyDemoDataCleaner` elimina datos demo de versiones antiguas y migra el ID interno `patient_maria` → `patient_primary`.
- Los rangos de presión por defecto se crean solo al completar el onboarding del paciente.

## Escalabilidad futura

1. **Módulos Gradle**: extraer `:core:domain`, `:core:data`, `:feature:patient`
2. **Health Connect / BLE**: nuevo módulo `:feature:devices` con repositorios dedicados
3. **Use cases**: capa `domain` con interactors cuando la lógica crezca
4. **Tests**: unitarios en `test/`; instrumentados de onboarding en `androidTest/` (requieren emulador/dispositivo)
