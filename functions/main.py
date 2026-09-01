from datetime import timedelta

from firebase_admin import firestore, initialize_app, messaging
from firebase_functions import firestore_fn, options

from alerting import caregiver_alert_data, caregiver_tokens

initialize_app()
options.set_global_options(region=options.SupportedRegion.US_CENTRAL1, max_instances=10)


@firestore_fn.on_document_created(
    document="families/{family_id}/patients/{patient_id}/alerts/{alert_id}",
)
def send_caregiver_alert(event: firestore_fn.Event[firestore_fn.DocumentSnapshot | None]) -> None:
    if event.data is None:
        return

    family_id = event.params.get("family_id", "").strip()
    patient_id = event.params.get("patient_id", "").strip()
    alert_id = event.params.get("alert_id", "").strip()
    if not family_id or not patient_id or not alert_id:
        return

    members = (
        firestore.client()
        .collection("families")
        .document(family_id)
        .collection("members")
        .stream()
    )
    tokens = caregiver_tokens([member.to_dict() or {} for member in members])
    if not tokens:
        return

    # ponytail: 500 tokens is FCM's multicast ceiling; batch again if families exceed it.
    messaging.send_each_for_multicast(
        messaging.MulticastMessage(
            tokens=tokens,
            data=caregiver_alert_data(family_id, patient_id, alert_id),
            android=messaging.AndroidConfig(
                priority="high",
                ttl=timedelta(minutes=10),
            ),
        )
    )
