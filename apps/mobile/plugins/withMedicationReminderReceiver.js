const {
  AndroidConfig,
  withAndroidManifest,
  createRunOncePlugin,
} = require("expo/config-plugins");

const pkg = {
  name: "withMedicationReminderReceiver",
  version: "1.0.0",
};

function ensureArray(value) {
  return Array.isArray(value) ? value : [];
}

function ensureReceiver(mainApplication, receiverName) {
  const receivers = ensureArray(mainApplication.receiver);
  const existing = receivers.find(
    (item) => item?.$?.["android:name"] === receiverName
  );

  if (existing) {
    existing.$["android:exported"] = "false";
    existing.$["android:enabled"] = "true";
    mainApplication.receiver = receivers;
    return;
  }

  receivers.push({
    $: {
      "android:name": receiverName,
      "android:enabled": "true",
      "android:exported": "false",
    },
  });

  mainApplication.receiver = receivers;
}

const withMedicationReminderReceiver = (config) => {
  return withAndroidManifest(config, (config) => {
    const mainApplication =
      AndroidConfig.Manifest.getMainApplicationOrThrow(config.modResults);

    AndroidConfig.Permissions.ensurePermission(
      config.modResults,
      "android.permission.SCHEDULE_EXACT_ALARM"
    );

    ensureReceiver(
      mainApplication,
      "com.cuidavoz.notificationchannel.MedicationReminderReceiver"
    );

    return config;
  });
};

module.exports = createRunOncePlugin(
  withMedicationReminderReceiver,
  pkg.name,
  pkg.version
);
