package no.eolseng.pgr301examauth.beer

import io.micrometer.core.instrument.MeterRegistry
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.eolseng.pg6102.utils.wrappedresponse.RestResponseFactory
import no.eolseng.pg6102.utils.wrappedresponse.WrappedResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api(value = "/api/v1/sip", description = "Endpoint for taking a sip of beer from a mug")
@RestController
@RequestMapping("/api/v1/sip")
class SipController(
        private val sipService: SipService
) {

    @ApiOperation("Takes a sip of beer from a mug")
    @PostMapping(path = [""])
    fun takeSip(
            @RequestParam id: String,
            auth: Authentication
    ): ResponseEntity<WrappedResponse<Void>> {

        // Convert path variable to Int
        val mugId = try {
            id.toInt()
        } catch (e: Exception) {
            return RestResponseFactory.userError(message = "Id must be convertible to type Int", httpStatusCode = 400)
        }

         return when (sipService.takeSip(mugId, auth.name)) {
             SipService.SipResult.NONEXISTENT_MUG ->
                 RestResponseFactory.notFound("Cannot find mug with ID $mugId")
             SipService.SipResult.NOT_OWNER ->
                 RestResponseFactory.userError(message = "Logged in user is not owner of mug with id $mugId", httpStatusCode = 401)
             SipService.SipResult.EMPTY_MUG ->
                 RestResponseFactory.userError(message = "Mug with id $mugId is empty")
             SipService.SipResult.SUCCESS ->
                 RestResponseFactory.noPayload(200)
         }
    }


}

@Service
class SipService(
        private val mugRepo: MugRepository,
        private val meterRegistry: MeterRegistry
) {

    fun takeSip(mugId: Int, username: String): SipResult {

        val mug = mugRepo.findById(mugId).orElse(null)

        return when {
            // Mug does not exist
            mug == null -> {
                meterRegistry.counter("beer.sips.count", "result", SipResult.NONEXISTENT_MUG.toString()).increment()
                SipResult.NONEXISTENT_MUG
            }
            // Not owner of mug
            mug.owner!!.username != username -> {
                meterRegistry.counter("beer.sips.count", "result", SipResult.NOT_OWNER.toString()).increment()
                SipResult.NOT_OWNER
            }
            // Mug is empty
            mug.currentVolume == 0 -> {
                meterRegistry.counter("beer.sips.count", "result", SipResult.EMPTY_MUG.toString()).increment()
                SipResult.EMPTY_MUG
            }
            // Take a sip
            else -> {
                mug.currentVolume -= 1
                mugRepo.save(mug)
                meterRegistry.counter("beer.sips.count", "result", SipResult.SUCCESS.toString()).increment()
                SipResult.SUCCESS
            }
        }
    }

    enum class SipResult {
        SUCCESS,
        EMPTY_MUG,
        NOT_OWNER,
        NONEXISTENT_MUG
    }

}