package no.eolseng.pgr301examauth.beer

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.eolseng.pg6102.utils.wrappedresponse.RestResponseFactory
import no.eolseng.pg6102.utils.wrappedresponse.WrappedResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.math.min

// DTO for filling a mug from a keg
data class TapDto(val kegId: Int, val mugId: Int)

@Api(value = "/api/v1/tap", description = "Endpoints for tapping a beer")
@RestController
@RequestMapping("/api/v1/tap")
class TapController(
        private val tapService: TapService
) {

    @ApiOperation("Fills a keg with beer - uses the difference between current volume and capacity in millis to respond (one millisecond / deciliter)")
    @PostMapping(
            path = ["/"],
            consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun tapBeer(
            @RequestBody dto: TapDto,
            auth: Authentication
    ): ResponseEntity<WrappedResponse<Void>> {
        return when (tapService.tapBeer(dto.kegId, dto.mugId, auth.name)) {
            TapService.TapResult.NONEXISTENT_KEG ->
                RestResponseFactory.notFound("Cannot find keg with ID ${dto.kegId}")
            TapService.TapResult.NONEXISTENT_MUG ->
                RestResponseFactory.notFound("Cannot find mug with ID ${dto.mugId}")
            TapService.TapResult.NOT_OWNER_OF_KEG ->
                RestResponseFactory.userError(message = "Logged in user is not owner of keg", httpStatusCode = 401)
            TapService.TapResult.NOT_OWNER_OF_MUG ->
                RestResponseFactory.userError(message = "Logged in user is not owner of mug", httpStatusCode = 401)
            TapService.TapResult.EMPTY_KEG ->
                RestResponseFactory.userError(message = "Keg with id ${dto.kegId} is empty")
            TapService.TapResult.SUCCESS ->
                RestResponseFactory.noPayload(200)
        }
    }
}

@Service
class TapService(
        private val kegRepo: KegRepository,
        private val mugRepo: MugRepository,
        private val meterRegistry: MeterRegistry
) {

    fun tapBeer(kegId: Int, mugId: Int, username: String): TapResult {

        val keg = kegRepo.findByIdOrNull(kegId)
        val mug = mugRepo.findByIdOrNull(mugId)

        return when {
            keg == null -> TapResult.NONEXISTENT_KEG
            mug == null -> TapResult.NONEXISTENT_MUG
            keg.owner!!.username != username -> TapResult.NOT_OWNER_OF_KEG
            mug.owner!!.username != username -> TapResult.NOT_OWNER_OF_MUG
            keg.currentVolume == 0 -> TapResult.EMPTY_KEG
            else -> {
                // Get amount to fill, up to the remaining amount in the keg
                val deficit = mug.capacity!!.volume - mug.currentVolume
                val amountToTap = min(deficit, keg.currentVolume)
                // Subtract amount tapped from keg
                keg.currentVolume -= amountToTap
                kegRepo.save(keg)
                // Add amount tapped to mug
                mug.currentVolume += amountToTap
                mugRepo.save(mug)
                // Sleep for the amount to fill in millis * 10 (1 dl = 0.01 seconds)
                Thread.sleep(amountToTap.toLong() * 10)
                // Record the volume filled
                DistributionSummary
                        .builder("beer.taps.volume")
                        .baseUnit("dl")
                        .register(meterRegistry)
                        .record(amountToTap.toDouble())
                // Return success
                TapResult.SUCCESS
            }
        }
    }
    enum class TapResult {
        SUCCESS,
        EMPTY_KEG,
        NOT_OWNER_OF_KEG,
        NOT_OWNER_OF_MUG,
        NONEXISTENT_KEG,
        NONEXISTENT_MUG
    }
}
