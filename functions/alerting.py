MAX_FCM_TARGETS = 500


def caregiver_tokens(members: list[dict]) -> list[str]:
    tokens: list[str] = []
    seen: set[str] = set()
    for member in members:
        token = member.get("fcmToken")
        if member.get("role") != "caregiver" or not isinstance(token, str):
            continue
        token = token.strip()
        if not token or token in seen:
            continue
        seen.add(token)
        tokens.append(token)
        if len(tokens) == MAX_FCM_TARGETS:
            break
    return tokens


def caregiver_alert_data(family_id: str, patient_id: str, alert_id: str) -> dict[str, str]:
    return {
        "kind": "caregiver_alert",
        "familyId": family_id,
        "patientId": patient_id,
        "alertId": alert_id,
    }
