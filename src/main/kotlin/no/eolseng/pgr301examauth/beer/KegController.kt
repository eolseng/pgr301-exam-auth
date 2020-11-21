package no.eolseng.pgr301examauth.beer

import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.eolseng.pg6102.utils.wrappedresponse.RestResponseFactory
import no.eolseng.pg6102.utils.wrappedresponse.WrappedResponse
import no.eolseng.pgr301examauth.db.User
import no.eolseng.pgr301examauth.db.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import java.net.URI
import javax.transaction.Transactional
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

    val logger: Logger = LoggerFactory.getLogger(KegController::class.java)

    init {
        // Setup the Beercentage Gauge
        // NB: this causes error on application shutdown - JPA has closed the Entity Manager but the Gauge tries to send a final gauge
        // The data is cool, but the implementation could probably be better. Should also not be placed here...
        Gauge
                .builder("beer.kegs.avg", kegService, KegService::getAvgKeg)
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
        if (!auth.isAuthenticated)
            return RestResponseFactory.userError(message = "User not authenticated", httpStatusCode = 401)
        if (dto.capacity == null || dto.capacity < 10L || dto.capacity > 600L)
            return RestResponseFactory.userError(message = "Must supply 'capacity' between 10 and 600", httpStatusCode = 400)
        // Retrieve user
        val user = userRepo.findById(auth.name).get()
        // Create a new keg and register
        val keg = kegService.createKeg(user, dto.capacity)
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

@Service
class KegService(
        private val kegRepo: KegRepository
) {
    val logger: Logger = LoggerFactory.getLogger(KegService::class.java)

    fun createKeg(owner: User, capacity: Int): Keg {
        // Create a new keg and register
        val keg = kegRepo.save(Keg(owner = owner, capacity = capacity))
        // Return redirect to new saved keg to user
        logger.info("New Keg[Id: ${keg.id}, Owner: ${keg.owner!!.username}, Capacity: ${keg.capacity}]")
        return keg
    }

    /**
     * Fills the supplied keg
     * Simulates time spent with "Thread.sleep()" to get metric data
     */
    @Timed(description = "Time of filling a single keg", value = "beer.kegs.fill.single")
    fun fillKeg(keg: Keg) {
        // Calculate amount to fill
        val amountToFill = keg.capacity - keg.currentVolume
        // Sleep for the amount to fill. Fills 20 liters / second (100 dl = 500 ms, 500 dl = 2500 ms)
        // This is faked to make the task and LongTaskTimer take more time
        Thread.sleep(amountToFill.toLong() * 5)
        // Fill to capacity - not using 'amountToFill' in case of manual filling causing keg overflow
        keg.currentVolume = keg.capacity
        // Persist the filled keg
        logger.info("Refilled Keg[Id: ${keg.id}, Capacity: ${keg.capacity}] with $amountToFill dl")
        kegRepo.save(keg)
    }

    @Transactional
    fun getAvgKeg(): Double {
        return kegRepo.getTotalVolume().toDouble() / kegRepo.getTotalCapacity()
    }

}