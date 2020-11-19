package no.eolseng.pgr301examauth.beer

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.eolseng.pg6102.utils.wrappedresponse.RestResponseFactory
import no.eolseng.pg6102.utils.wrappedresponse.WrappedResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

// DTO for filling a mug from a keg
data class TapDto(val kegId: Int, val mugId: Int)

@Api(value = "/api/v1/tap", description = "Endpoints for tapping a beer")
@RestController
@RequestMapping("/api/v1/tap")
class TapController(
        private val kegRepo: KegRepository,
        private val mugRepo: MugRepository
) {

    @ApiOperation("Fills a keg with beer - uses the difference between current volume and capacity in millis to respond (one millisecond / deciliter)")
    @PostMapping(
            path = ["/"],
            consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun fillMug(
            @RequestBody dto: TapDto,
            auth: Authentication
    ): ResponseEntity<WrappedResponse<Void>> {

        // Check if keg exists
        val keg = kegRepo.findById(dto.kegId).orElse(null)
                ?: return RestResponseFactory.notFound("Cannot find keg with ID ${dto.kegId}")
        // User must be owner of keg
        if (keg.owner!!.username != auth.name)
            return RestResponseFactory.userError(message = "Logged in user is not owner of keg", httpStatusCode = 401)

        // Check if mug exists
        val mug = mugRepo.findById(dto.mugId).orElse(null)
                ?: return RestResponseFactory.notFound("Cannot find mug with ID ${dto.mugId}")
        // User must be owner of keg
        if (mug.owner!!.username != auth.name)
            return RestResponseFactory.userError(message = "Logged in user is not owner of mug", httpStatusCode = 401)

        // Get amount to fill, up to the remaining amount in the keg
        val deficit = (mug.capacity!!.volume - mug.currentVolume).absoluteValue
        val amountToTap = min(deficit, keg.currentVolume)

        // Sleep for the amount to fill in millis * 10 (1 dl = 0.01 seconds)
        Thread.sleep(amountToTap.toLong() * 10)

        // Subtract amount tapped from keg
        keg.currentVolume -= amountToTap
        kegRepo.save(keg)

        // Add amount tapped to mug
        mug.currentVolume += amountToTap
        mugRepo.save(mug)

        return RestResponseFactory.noPayload(200)

    }
}
