package no.eolseng.pg6102.utils.wrappedresponse

import org.springframework.http.ResponseEntity
import java.net.URI

/**
 * Based on org.tsdes.advanced.rest.dto.RestResponseFactory
 */
object RestResponseFactory {

    fun noPayload(httpStatusCode: Int): ResponseEntity<WrappedResponse<Void>> {
        val wrappedResponse = WrappedResponse<Void>(code = httpStatusCode).validate()
        return ResponseEntity.status(httpStatusCode).body(wrappedResponse)
    }

    fun <T> payload(httpStatusCode: Int, data: T): ResponseEntity<WrappedResponse<T>> {
        val wrappedResponse = WrappedResponse(code = httpStatusCode, data = data).validate()
        return ResponseEntity.status(httpStatusCode).body(wrappedResponse)
    }

    fun created(uri: URI): ResponseEntity<WrappedResponse<Void>> {
        val httpStatusCode = 201
        val wrappedResponse = WrappedResponse<Void>(code = httpStatusCode).validate()
        return ResponseEntity.created(uri).body(wrappedResponse)
    }

    fun <T> userError(message: String, httpStatusCode: Int = 400): ResponseEntity<WrappedResponse<T>> {
        val wrappedResponse = WrappedResponse<T>(code = httpStatusCode, message = message).validate()
        return ResponseEntity.status(httpStatusCode).body(wrappedResponse)
    }

    fun <T> notFound(message: String): ResponseEntity<WrappedResponse<T>> {
        val httpStatusCode = 404
        val wrappedResponse = WrappedResponse<T>(code = httpStatusCode, message = message).validate()
        return ResponseEntity.status(httpStatusCode).body(wrappedResponse)
    }

    fun serverFailure(message: String, httpStatusCode: Int = 500): ResponseEntity<WrappedResponse<Void>> {
        val wrappedResponse = WrappedResponse<Void>(code = httpStatusCode, message = message).validate()
        return ResponseEntity.status(httpStatusCode).body(wrappedResponse)
    }
}