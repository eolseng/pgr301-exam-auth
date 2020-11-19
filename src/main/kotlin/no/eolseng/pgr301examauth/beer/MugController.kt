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
import java.net.URI
import javax.validation.Valid

@Api(value = "/api/v1/mug", description = "Endpoints for managing beer mugs")
@RestController
@RequestMapping("/api/v1/mug")
class MugController(
        private val userRepo: UserRepository,
        private val mugRepo: MugRepository
) {

    @ApiOperation("Register a new mug to the authorized user")
    @PostMapping(
            path = ["/"],
            consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun registerMug(
            @RequestBody(required = true) @Valid dto: MugDto,
            auth: Authentication?
    ): ResponseEntity<WrappedResponse<Void>> {
        // Authenticate user and DTO
        auth ?: return RestResponseFactory.userError(message = "User not logged in", httpStatusCode = 401)
        if (!auth.isAuthenticated) return RestResponseFactory.userError(message = "User not authenticated", httpStatusCode = 401)
        if (dto.capacity == null || Mug.getCapacityByInt(dto.capacity) == null) return RestResponseFactory.userError(message = "Must supply 'capacity' between 10 and 600", httpStatusCode = 400)
        // Retrieve user
        val user = userRepo.findById(auth.name).get()
        // Create a new keg and register
        var mug = Mug(owner = user, capacity = Mug.getCapacityByInt(dto.capacity))
        mug = mugRepo.save(mug)
        // Return redirect to new saved keg to user
        return RestResponseFactory.created(URI.create("/api/v1/mug/${mug.id}"))
    }

    @ApiOperation("Get a single mug based on ID")
    @GetMapping(path = ["/{id}"])
    fun getMugById(
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
        val mug = mugRepo.findById(id).orElse(null)
                ?: return RestResponseFactory.notFound("Cannot find keg with ID $id")
        // User must be owner of keg
        if (mug.owner!!.username != auth.name)
            return RestResponseFactory.userError(message = "Logged in user is not owner of keg", httpStatusCode = 401)
        // Convert keg to DTO for transfer
        val kegDto = mug.toDto()
        // Send the DTO
        return RestResponseFactory.payload(httpStatusCode = 200, data = kegDto)
    }

    @ApiOperation("Get all mugs belonging to authenticated user")
    @GetMapping(path = ["/"])
    fun getAllUserMugs(
            auth: Authentication
    ): ResponseEntity<WrappedResponse<Any>> {
        val user = userRepo.findById(auth.name).get()
        val kegs = mugRepo.findAllByOwner(user)
        val kegsDto = kegs.map { it.toDto() }
        return RestResponseFactory.payload(httpStatusCode = 200, data = kegsDto)
    }

}