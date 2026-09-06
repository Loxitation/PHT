"""API key generation/verification for device sync auth."""
import hashlib
import secrets

API_KEY_PREFIX_LEN = 8


def generate_api_key() -> tuple[str, str, str]:
    """Returns (raw_key, prefix, hash). The raw key is only ever shown once."""
    raw_key = secrets.token_urlsafe(32)
    prefix = raw_key[:API_KEY_PREFIX_LEN]
    key_hash = hash_api_key(raw_key)
    return raw_key, prefix, key_hash


def hash_api_key(raw_key: str) -> str:
    return hashlib.sha256(raw_key.encode("utf-8")).hexdigest()


def verify_api_key(raw_key: str, key_hash: str) -> bool:
    return secrets.compare_digest(hash_api_key(raw_key), key_hash)
