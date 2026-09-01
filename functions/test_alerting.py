import unittest

from alerting import MAX_FCM_TARGETS, caregiver_alert_data, caregiver_tokens


class AlertingTest(unittest.TestCase):
    def test_filters_deduplicates_and_caps_caregiver_tokens(self) -> None:
        members = [
            {"role": "patient", "fcmToken": "patient-token"},
            {"role": "caregiver", "fcmToken": " caregiver-token "},
            {"role": "caregiver", "fcmToken": "caregiver-token"},
            {"role": "caregiver", "fcmToken": ""},
        ] + [
            {"role": "caregiver", "fcmToken": f"token-{index}"}
            for index in range(MAX_FCM_TARGETS)
        ]

        tokens = caregiver_tokens(members)

        self.assertEqual(MAX_FCM_TARGETS, len(tokens))
        self.assertEqual("caregiver-token", tokens[0])
        self.assertNotIn("patient-token", tokens)

    def test_payload_contains_only_routing_identifiers(self) -> None:
        self.assertEqual(
            {
                "kind": "caregiver_alert",
                "familyId": "family-1",
                "patientId": "patient-1",
                "alertId": "alert-1",
            },
            caregiver_alert_data("family-1", "patient-1", "alert-1"),
        )


if __name__ == "__main__":
    unittest.main()
