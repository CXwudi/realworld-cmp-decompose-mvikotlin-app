package mikufan.cx.conduit.frontend.logic.repo.api.util

import mikufan.cx.conduit.common.ErrorRsp
import mikufan.cx.conduit.frontend.logic.repo.api.util.ConduitResponse.ErrorResponse
import mikufan.cx.conduit.frontend.logic.repo.api.util.ConduitResponse.Failure
import mikufan.cx.conduit.frontend.logic.repo.api.util.ConduitResponse.Success
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed interface ConduitResponse<T> {
  data class Success<T>(val data: T) : ConduitResponse<T>
  data class ErrorResponse(val error: ErrorRsp) : ConduitResponse<Nothing>
  data class Failure(val throwable: Throwable) : ConduitResponse<Nothing>

  companion object {
    fun <T> success(data: T) = Success(data)
    fun error(error: ErrorRsp) = ErrorResponse(error)
    fun failure(throwable: Throwable) = Failure(throwable)
  }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> ConduitResponse<T>.fold(
  onError: (ErrorRsp) -> Unit,
  onFailure: (Throwable) -> Unit,
  onSuccess: (T) -> Unit,
){
  contract {
    callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
  }
  when (this) {
    is Success -> onSuccess(data)
    is ErrorResponse -> onError(error)
    is Failure -> onFailure(throwable)
  }
  return
}

@Suppress("NOTHING_TO_INLINE") // inline needed for non-local return in onSuccess lambda
inline fun <T> ConduitResponse<T>.getOrThrow(): T {
  fold(
    onError = { errorRsp -> error(errorRsp.messagesAsString) },
    onFailure = { throwable -> throw throwable },
    onSuccess = { data -> return data }
  )
  error("This should not happen")
}