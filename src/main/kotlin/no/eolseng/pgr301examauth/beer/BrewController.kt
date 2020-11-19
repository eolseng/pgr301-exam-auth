package no.eolseng.pgr301examauth.beer

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.eolseng.pg6102.utils.wrappedresponse.RestResponseFactory
import no.eolseng.pg6102.utils.wrappedresponse.WrappedResponse
import no.eolseng.pgr301examauth.db.UserRepository
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import kotlin.math.absoluteValue

@Api(value = "/api/v1/brew", description = "Endpoints for filling kegs")
@RestController
@RequestMapping("/api/v1/brew")
class BrewController (
        private val kegRepo: KegRepository
) {

    @ApiOperation("Fills a keg with beer - uses the difference between current volume and capacity in millis to respond (one millisecond / deciliter)")
    @PostMapping(path = ["/{id}"])
    fun fillKeg(
            @PathVariable("id") pathId: String,
            auth: Authentication
    ): ResponseEntity<WrappedResponse<Any>> {

        // Convert path variable to Int
        val id = try {
            pathId.toInt()
        } catch (e: Exception) {
            return RestResponseFactory.userError(message = "Id must be convertible to type Int", httpStatusCode = 400)
        }
        // Check if keg exists
        var keg = kegRepo.findById(id).orElse(null)
                ?: return RestResponseFactory.notFound("Cannot find keg with ID $id")
        // User must be owner of keg
        if (keg.owner!!.username != auth.name)
            return RestResponseFactory.userError(message = "Logged in user is not owner of keg", httpStatusCode = 401)

        val amountToFill = (keg.capacity - keg.currentVolume).absoluteValue

        // Sleep for the amount to fill in millis
        Thread.sleep(amountToFill.toLong())

        keg.currentVolume += amountToFill
        keg = kegRepo.save(keg)

        return RestResponseFactory.payload(200, keg.toDto())

    }
}