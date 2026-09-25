package chawpi.core.identity

import javax.crypto.SecretKey

// wraps the jwt signing key in its own type. a plain SecretKey bean is a common java type: an app
// bean of that type (say, for encrypting something unrelated) would otherwise silently become the
// jwt key, or make injection ambiguous. this type can only mean one thing.
class ChawpiJwtKey(
    val key: SecretKey
)
