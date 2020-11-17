package no.eolseng.pg6102.utils.wrappedresponse

import io.swagger.annotations.ApiModelProperty

/**
 * Wrapper DTO for REST responses
 *
 * Based on org.tsdes.advanced.rest.dto.WrappedResponse, which is based on JSend (https://labs.omniti.com/labs/jsend)
 */
open class WrappedResponse<T>(

    @ApiModelProperty("The HTTP status code of the response")
    var code: Int? = null,

    @ApiModelProperty("The wrapped payload")
    var data: T? = null,

    @ApiModelProperty("Error message in case of error")
    var message: String? = null,

    @ApiModelProperty("String representing either 'success', user error ('error') or server failure ('fail') ")
    var status: ResponseStatus? = null

) {

    /**
     * Method to set 'status' field based on 'code' if missing, or validate match between 'code' and 'status' if present.
     * Also validates that failed responses has a 'message' set.
     *
     * @throws IllegalStateException on invalid HTTP status code (outside 100..599)
     * @throws IllegalArgumentException on mismatch between 'code' and 'status' and on missing 'message' on failed response
     */
    fun validate(): WrappedResponse<T> {

        val code: Int = code ?: throw IllegalStateException("Missing HTTP status code")

        if (code !in 100..599) {
            throw IllegalStateException("Invalid HTTP status code: $code")
        }

        // Set HTTP status code or validate status code to status string
        if (status == null) {
            status = when (code) {
                in 100..399 -> ResponseStatus.SUCCESS
                in 400..499 -> ResponseStatus.ERROR
                in 500..599 -> ResponseStatus.FAIL
                else -> throw IllegalStateException("Invalid HTTP status code: $code")
            }
        } else {
            val wrong = when (code) {
                in 100..399 -> (status == ResponseStatus.SUCCESS)
                in 400..499 -> (status == ResponseStatus.ERROR)
                in 500..599 -> (status == ResponseStatus.FAIL)
                else -> throw IllegalStateException("Invalid HTTP status code: $code")
            }
            if (wrong) {
                throw IllegalArgumentException("Status $status is not correct for HTTP status code $code")
            }
        }

        // Must provide error message if response is not successful
        if (status != ResponseStatus.SUCCESS && message == null) {
            throw IllegalArgumentException("Failed response must have describing 'message'")
        }

        return this

    }

    enum class ResponseStatus {
        SUCCESS, ERROR, FAIL
    }

}