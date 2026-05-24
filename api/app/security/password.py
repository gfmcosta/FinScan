import hashlib
import bcrypt


_HASH_PREFIX = "bcrypt_sha256$"


def _prehash_password(password: str) -> bytes:
    # Keep bcrypt input length fixed and avoid the 72-byte input limitation.
    return hashlib.sha256(password.encode("utf-8")).digest()


def get_password_hash(password: str) -> str:
    hashed = bcrypt.hashpw(_prehash_password(password), bcrypt.gensalt())
    return f"{_HASH_PREFIX}{hashed.decode('utf-8')}"


def verify_password(plain_password: str, hashed_password: str) -> bool:
    try:
        if hashed_password.startswith(_HASH_PREFIX):
            stored = hashed_password[len(_HASH_PREFIX):].encode("utf-8")
            return bcrypt.checkpw(_prehash_password(plain_password), stored)

        # Backward compatibility for legacy bcrypt hashes already stored in DB.
        return bcrypt.checkpw(plain_password.encode("utf-8"), hashed_password.encode("utf-8"))
    except ValueError:
        return False
