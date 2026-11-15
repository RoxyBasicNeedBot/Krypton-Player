"""
Firebase Admin SDK User Ban & Management Utility
Can be imported into your Telegram Bot (e.g. server.py or bot.py).

Rule 4 Compliance:
- auth.update_user(uid, disabled=True)
- auth.revoke_refresh_tokens(uid)
- firestore.client().collection('users').document(uid).update({'banned': True, ...})
- auth.verify_id_token(token, check_revoked=True)
"""

import os
import firebase_admin
from firebase_admin import credentials, auth, firestore

# Initialize Admin SDK once
def init_firebase_admin(service_account_path: str = None):
    if not firebase_admin._apps:
        # Load from path or environment variable
        cert_path = service_account_path or os.getenv("FIREBASE_SERVICE_ACCOUNT_KEY") or "service-account.json"
        if os.path.exists(cert_path):
            cred = credentials.Certificate(cert_path)
            firebase_admin.initialize_app(cred)
        else:
            firebase_admin.initialize_app()


def ban_user(uid: str, reason: str = "Account disabled by administrator") -> bool:
    """
    Completely bans a user:
    1. Disables user in Firebase Auth so new tokens cannot be minted.
    2. Immediately revokes existing refresh tokens so active sessions are terminated.
    3. Sets banned=True in Firestore users/{uid} for real-time and security rule enforcement.
    """
    try:
        # 1. Disable in Firebase Auth
        auth.update_user(uid, disabled=True)

        # 2. Revoke active refresh tokens
        auth.revoke_refresh_tokens(uid)

        # 3. Update Firestore banned flag
        db = firestore.client()
        db.collection("users").document(uid).set({
            "banned": True,
            "bannedReason": reason
        }, merge=True)

        return True
    except Exception as e:
        print(f"Error banning user: {e}")
        return False


def unban_user(uid: str) -> bool:
    """
    Restores access to a banned user.
    """
    try:
        auth.update_user(uid, disabled=False)
        db = firestore.client()
        db.collection("users").document(uid).set({
            "banned": False,
            "bannedReason": None
        }, merge=True)
        return True
    except Exception as e:
        print(f"Error unbanning user: {e}")
        return False


def verify_user_token(id_token: str) -> dict:
    """
    Verifies token while strictly enforcing revoked/disabled status.
    """
    return auth.verify_id_token(id_token, check_revoked=True)
