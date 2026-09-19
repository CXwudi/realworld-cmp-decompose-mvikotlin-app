package mikufan.cx.conduit.frontend.logic.repo.api.util

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.Converter
import de.jensklingenberg.ktorfit.converter.KtorfitResult
import de.jensklingenberg.ktorfit.converter.TypeData
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import mikufan.cx.conduit.common.ErrorRsp

class ConduitResponseConverterFactory(
  private val json: Json,
) : Converter.Factory {

  override fun suspendResponseConverter(
    typeData: TypeData,
    ktorfit: Ktorfit
  ): Converter.SuspendResponseConverter<HttpResponse, *>? {
    if (typeData.typeInfo.type == ConduitResponse::class) {
      return ConduitResponseConverter(json, typeData)
    }
    return null
  }
}

class ConduitResponseConverter(
  private val json: Json,
  private val typeData: TypeData,
) : Converter.SuspendResponseConverter<HttpResponse, ConduitResponse<*>> {
  override suspend fun convert(result: KtorfitResult): ConduitResponse<*> {
    when (result) {
      is KtorfitResult.Success -> {
        val response = result.response
        val statusCode = response.status
        return if (statusCode.isSuccess()) {
          ConduitResponse.success(response.body(typeData.typeArgs[0].typeInfo) as Any)
        } else {
          // Usually ErrorRsp type is returned with status code 422, but it is not guaranteed for all backends
          // Hence we are just checking if the body is empty or not
          val bodyText = response.bodyAsText()
          if (bodyText.isBlank() || statusCode.value == 422) {
            ConduitResponse.failure(Exception(bodyText.ifBlank { "Request failed with status $statusCode and empty return body" }))
          } else {
            val errorRsp = json.decodeFromString<ErrorRsp>(bodyText)
            ConduitResponse.error(errorRsp)
          }
        }
      }

      is KtorfitResult.Failure -> {
        val throwable = result.throwable
        return ConduitResponse.failure(throwable)
      }
    }
  }
}