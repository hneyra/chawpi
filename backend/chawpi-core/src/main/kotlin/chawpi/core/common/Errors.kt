package chawpi.core.common

import org.springframework.http.HttpStatus

data class FieldViolation(
    val field: String,
    val message: String
)

// domain errors carry their http status. handler turns them into problem+json.
// not sealed: modules add their own.
abstract class ChawpiException(
    val status: HttpStatus,
    override val message: String,
    val violations: List<FieldViolation> = emptyList()
) : RuntimeException(message)

class NotFoundException(
    message: String
) : ChawpiException(HttpStatus.NOT_FOUND, message)

class ConflictException(
    message: String
) : ChawpiException(HttpStatus.CONFLICT, message)

class ForbiddenException(
    message: String
) : ChawpiException(HttpStatus.FORBIDDEN, message)

class UnauthorizedException(
    message: String
) : ChawpiException(HttpStatus.UNAUTHORIZED, message)

class ValidationException : ChawpiException {
    constructor(message: String, violations: List<FieldViolation>) :
        super(HttpStatus.BAD_REQUEST, message, violations)

    constructor(message: String, field: String, reason: String) :
        super(HttpStatus.BAD_REQUEST, message, listOf(FieldViolation(field, reason)))
}
