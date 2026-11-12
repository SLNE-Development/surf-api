package dev.slne.surf.database.utils

import io.r2dbc.spi.*
import org.jetbrains.exposed.v1.r2dbc.ExposedR2dbcException

/**
 * Unwraps the underlying R2DBC exception from this ExposedR2dbcException.
 *
 * @return the underlying R2DBC exception cause
 * @throws ExposedR2dbcException if the cause is not an R2dbcException
 */
fun ExposedR2dbcException.unwrap(): R2dbcException {
    return cause as? R2dbcException ?: throw this
}

/**
 * Unwraps the underlying R2DBC exception and casts it to the specified type.
 *
 * @param E the expected exception type
 * @return the underlying exception cast to type E
 * @throws ExposedR2dbcException if the cause cannot be cast to type E
 */
@JvmName($$"unwrap$reified")
inline fun <reified E : R2dbcException> ExposedR2dbcException.unwrap(): E {
    return cause as? E ?: throw this
}

/**
 * Alias for [unwrap]. Unwraps and casts the underlying exception to the specified type.
 *
 * @param E the expected exception type
 * @return the underlying exception cast to type E
 * @throws ExposedR2dbcException if the cause cannot be cast to type E
 * @see unwrap
 */
inline fun <reified E : R2dbcException> ExposedR2dbcException.causeAs(): E = unwrap<E>()

/**
 * Checks if the underlying cause is a data integrity violation.
 *
 * @return true if the cause is a data integrity violation, false otherwise
 * @see R2dbcDataIntegrityViolationException
 */
fun ExposedR2dbcException.isDataIntegrityViolation() = cause is R2dbcDataIntegrityViolationException

/**
 * Unwraps and returns the underlying exception as a data integrity violation.
 *
 * @return the underlying data integrity violation exception
 * @throws ExposedR2dbcException if the cause is not a data integrity violation
 * @see R2dbcDataIntegrityViolationException
 */
fun ExposedR2dbcException.asDataIntegrityViolation() = unwrap<R2dbcDataIntegrityViolationException>()

/**
 * Checks if the underlying cause is a bad grammar exception.
 *
 * @return true if the cause is a bad grammar exception, false otherwise
 * @see R2dbcBadGrammarException
 */
fun ExposedR2dbcException.isBadGrammar() = cause is R2dbcBadGrammarException

/**
 * Unwraps and returns the underlying exception as a bad grammar exception.
 *
 * @return the underlying bad grammar exception
 * @throws ExposedR2dbcException if the cause is not a bad grammar exception
 * @see R2dbcBadGrammarException
 */
fun ExposedR2dbcException.asBadGrammar() = unwrap<R2dbcBadGrammarException>()

/**
 * Checks if the underlying cause is a non-transient resource exception.
 *
 * @return true if the cause is a non-transient resource exception, false otherwise
 * @see R2dbcNonTransientResourceException
 */
fun ExposedR2dbcException.isNonTransientResourceException() = cause is R2dbcNonTransientResourceException

/**
 * Unwraps and returns the underlying exception as a non-transient resource exception.
 *
 * @return the underlying non-transient resource exception
 * @throws ExposedR2dbcException if the cause is not a non-transient resource exception
 * @see R2dbcNonTransientResourceException
 */
fun ExposedR2dbcException.asNonTransientResourceException() = unwrap<R2dbcNonTransientResourceException>()

/**
 * Checks if the underlying cause is a permission denied exception.
 *
 * @return true if the cause is a permission denied exception, false otherwise
 * @see R2dbcPermissionDeniedException
 */
fun ExposedR2dbcException.isPermissionDenied() = cause is R2dbcPermissionDeniedException

/**
 * Unwraps and returns the underlying exception as a permission denied exception.
 *
 * @return the underlying permission denied exception
 * @throws ExposedR2dbcException if the cause is not a permission denied exception
 * @see R2dbcPermissionDeniedException
 */
fun ExposedR2dbcException.asPermissionDenied() = unwrap<R2dbcPermissionDeniedException>()

/**
 * Checks if the underlying cause is a rollback exception.
 *
 * @return true if the cause is a rollback exception, false otherwise
 * @see R2dbcRollbackException
 */
fun ExposedR2dbcException.isRollbackException() = cause is R2dbcRollbackException

/**
 * Unwraps and returns the underlying exception as a rollback exception.
 *
 * @return the underlying rollback exception
 * @throws ExposedR2dbcException if the cause is not a rollback exception
 * @see R2dbcRollbackException
 */
fun ExposedR2dbcException.asRollbackException() = unwrap<R2dbcRollbackException>()

/**
 * Checks if the underlying cause is a timeout exception.
 *
 * @return true if the cause is a timeout exception, false otherwise
 * @see R2dbcTimeoutException
 */
fun ExposedR2dbcException.isTimeoutException() = cause is R2dbcTimeoutException

/**
 * Unwraps and returns the underlying exception as a timeout exception.
 *
 * @return the underlying timeout exception
 * @throws ExposedR2dbcException if the cause is not a timeout exception
 * @see R2dbcTimeoutException
 */
fun ExposedR2dbcException.asTimeoutException() = unwrap<R2dbcTimeoutException>()

/**
 * Checks if the underlying cause is a transient resource exception.
 *
 * @return true if the cause is a transient resource exception, false otherwise
 * @see R2dbcTransientResourceException
 */
fun ExposedR2dbcException.isTransientResourceException() = cause is R2dbcTransientResourceException

/**
 * Unwraps and returns the underlying exception as a transient resource exception.
 *
 * @return the underlying transient resource exception
 * @throws ExposedR2dbcException if the cause is not a transient resource exception
 * @see R2dbcTransientResourceException
 */
fun ExposedR2dbcException.asTransientResourceException() = unwrap<R2dbcTransientResourceException>()