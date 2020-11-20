package no.eolseng.pgr301examauth.beer

import io.micrometer.core.instrument.MeterRegistry
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.eolseng.pg6102.utils.wrappedresponse.RestResponseFactory
import no.eolseng.pg6102.utils.wrappedresponse.WrappedResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api(value = "/api/v1/sip", description = "Endpoint for taking a sip of beer from a mug")
@RestController
@RequestMapping("/api/v1/sip")
class SipController(
        private val mugRepo: MugRepository,
        private val meterRegistry: MeterRegistry
) {

    @ApiOperation("Takes a sip of beer from a mug")
    @PostMapping(path = [""])
    fun fillKeg(
            @RequestParam id: String,
            auth: Authentication
    ): ResponseEntity<WrappedResponse<Void>> {

        // Convert path variable to Int
        val mugId = try {
            id.toInt()
        } catch (e: Exception) {
            return RestResponseFactory.userError(message = "Id must be convertible to type Int", httpStatusCode = 400)
        }

        // Check if mug exists
        val mug = mugRepo.findById(mugId).orElse(null)
        if (mug == null) {
            meterRegistry.counter("beer.sips.count", "result", SipResult.NONEXISTENT_MUG.toString()).increment()
            return RestResponseFactory.notFound("Cannot find mug with ID $mugId")
        }
        // User must be owner of keg
        if (mug.owner!!.username != auth.name) {
            meterRegistry.counter("beer.sips.count", "result", SipResult.NOT_OWNER.toString()).increment()
            return RestResponseFactory.userError(message = "Logged in user is not owner of mug with id $mugId", httpStatusCode = 401)
        }
        // Check that the mug is not empty
        if (mug.currentVolume == 0) {
            meterRegistry.counter("beer.sips.count", "result", SipResult.EMPTY_MUG.toString()).increment()
            return RestResponseFactory.userError(message = "Mug with id $mugId is empty")
        }

        // Take a sip
        mug.currentVolume -= 1

        // Persist new volume
        mugRepo.save(mug)

        // Increase the metric counter for sips
        meterRegistry.counter("beer.sips.count", "result", SipResult.SUCCESS.toString()).increment()

        // Return success
        return RestResponseFactory.noPayload(200)
    }

    enum class SipResult {
        SUCCESS,
        EMPTY_MUG,
        NOT_OWNER,
        NONEXISTENT_MUG
    }

}