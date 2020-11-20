package no.eolseng.pgr301examauth.beer

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.eolseng.pg6102.utils.wrappedresponse.RestResponseFactory
import no.eolseng.pg6102.utils.wrappedresponse.WrappedResponse
import no.eolseng.pgr301examauth.db.UserRepository
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.net.URI
import javax.validation.Valid

@Api(value = "/api/v1/keg", description = "Endpoints for managing beer kegs")
@RestController
@RequestMapping("/api/v1/keg")
class KegController(
        private val userRepo: UserRepository,
        private val kegRepo: KegRepository,
        private val kegService: KegService,
        private val meterRegistry: MeterRegistry
) {

    init {
        // Setup the Beercentage Gauge - this causes error on appliation shutdown
        Gauge
                .builder("beer.keg.avg", kegService, KegService::getAvgKeg)
                .register(meterRegistry)

    }

    @ApiOperation("Register a new keg to the authorized user")
    @PostMapping(
            path = ["/"],
            consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun registerKeg(
            @RequestBody(required = true) @Valid dto: KegDto,
            auth: Authentication
    ): ResponseEntity<WrappedResponse<Void>> {
        // Authenticate user and DTO
        if (!auth.isAuthenticated) return RestResponseFactory.userError(message = "User not authenticated", httpStatusCode = 401)
        if (dto.capacity == null || dto.capacity < 10L || dto.capacity > 600L) return RestResponseFactory.userError(message = "Must supply 'capacity' between 10 and 600", httpStatusCode = 400)
        // Retrieve user
        val user = userRepo.findById(auth.name).get()
        // Create a new keg and register
        var keg = Keg(owner = user, capacity = dto.capacity)
        keg = kegRepo.save(keg)
        // Return redirect to new saved keg to user
        return RestResponseFactory.created(URI.create("/api/v1/keg/${keg.id}"))
    }

    @ApiOperation("Get a single keg based on ID")
    @GetMapping(path = ["/{id}"])
    fun getKegById(
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
        val keg = kegRepo.findById(id).orElse(null)
                ?: return RestResponseFactory.notFound("Cannot find keg with ID $id")
        // User must be owner of keg
        if (keg.owner!!.username != auth.name)
            return RestResponseFactory.userError(message = "Logged in user is not owner of keg", httpStatusCode = 401)
        // Convert keg to DTO for transfer
        val kegDto = keg.toDto()
        // Send the DTO
        return RestResponseFactory.payload(httpStatusCode = 200, data = kegDto)
    }

    @ApiOperation("Get all kegs belonging to authenticated user")
    @GetMapping(path = ["/"])
    fun getAllUserKegs(
            auth: Authentication
    ): ResponseEntity<WrappedResponse<Any>> {
        val user = userRepo.findById(auth.name).get()
        val kegs = kegRepo.findAllByOwner(user)
        val kegsDto = kegs.map { it.toDto() }
        return RestResponseFactory.payload(httpStatusCode = 200, data = kegsDto)
    }

}