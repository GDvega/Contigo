const {
  AndroidConfig,
  withAndroidManifest,
  createRunOncePlugin,
} = require("expo/config-plugins");

const pkg = {
  name: "withCuidaVozNative",
  version: "1.0.0",
};

/**
 * Agrega servicios y permisos necesarios para los recordatorios médicos nativos.
 */
const withCuidaVozNative = (config) => {
  return withAndroidManifest(config, (config) => {
    const mainApplication =
      AndroidConfig.Manifest.getMainApplicationOrThrow(config.modResults);

    // 1. Permisos Críticos
    AndroidConfig.Permissions.ensurePermission(
      config.modResults,
      "android.permission.SCHEDULE_EXACT_ALARM"
    );
    AndroidConfig.Permissions.ensurePermission(
      config.modResults,
      "android.permission.FOREGROUND_SERVICE"
    );
    // Para Android 14+ se requiere permiso específico de tipo de servicio
    AndroidConfig.Permissions.ensurePermission(
      config.modResults,
      "android.permission.FOREGROUND_SERVICE_SPECIAL_USE"
    );
    AndroidConfig.Permissions.ensurePermission(
      config.modResults,
      "android.permission.WAKE_LOCK"
    );

    // 2. Registrar BroadcastReceiver para la alarma
    const receivers = mainApplication.receiver || [];
    const alarmReceiverName = "com.cuidavoz.mobile.nativemodules.AlarmReceiver";

    if (!receivers.find(r => r.$["android:name"] === alarmReceiverName)) {
      receivers.push({
        $: {
          "android:name": alarmReceiverName,
          "android:enabled": "true",
          "android:exported": "false",
        },
      });
    }
    mainApplication.receiver = receivers;

    // 3. Registrar ForegroundService para voz/notificación persistente
    const services = mainApplication.service || [];
    const serviceName = "com.cuidavoz.mobile.nativemodules.ReminderService";

    if (!services.find(s => s.$["android:name"] === serviceName)) {
      services.push({
        $: {
          "android:name": serviceName,
          "android:enabled": "true",
          "android:exported": "false",
          "android:foregroundServiceType": "specialUse"
        },
      });
    }
    mainApplication.service = services;

    return config;
  });
};

module.exports = createRunOncePlugin(
  withCuidaVozNative,
  pkg.name,
  pkg.version
);
