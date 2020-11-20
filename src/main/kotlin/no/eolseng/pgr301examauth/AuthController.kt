package no.eolseng.pgr301examauth

import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.eolseng.pg6102.utils.wrappedresponse.RestResponseFactory
import no.eolseng.pg6102.utils.wrappedresponse.WrappedResponse
import no.eolseng.pgr301examauth.db.UserDetailsServiceImpl
import no.eolseng.pgr301examauth.db.UserService
import no.eolseng.pgr301examauth.dto.AuthDto
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*
import java.net.URI

@Api(value = "/api/v1/auth", description = "Authorization API for signup, login, logout and roles")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
        private val service: UserService,
        private val authManager: AuthenticationManager,
        private val userDetailsService: UserDetailsServiceImpl,
        private val meterRegistry: MeterRegistry
) {

    @ApiOperation("Retrieve name and roles of signed in user")
    @GetMapping("/user")
    fun user(user: Authentication): ResponseEntity<WrappedResponse<Map<String, Any>>> {
        val map = mutableMapOf<String, Any>()
        map["username"] = user.name
        map["roles"] = AuthorityUtils.authorityListToSet(user.authorities)
        return RestResponseFactory.payload(200, map)
    }

    @ApiOperation("Create a new user")
    @PostMapping(
            path = ["/signup"],
            consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun signup(@RequestBody dto: AuthDto): ResponseEntity<WrappedResponse<Void>> {

        meterRegistry.counter("http.requests", "uri", "/api/v1/auth/signup", "method", HttpMethod.POST.toString()).increment()

        // Extract data from the DTO - lower casing for easier logons
        val username = dto.username.toLowerCase()
        val password = dto.password

        // Attempt to register user
        val registered = service.createUser(username, password, setOf("USER"))
        if (!registered) return RestResponseFactory.userError(message = "Username already exists")

        // Attempt to retrieve the user from database
        val userDetails = try {
            userDetailsService.loadUserByUsername(username)
        } catch (e: UsernameNotFoundException) {
            return RestResponseFactory.serverFailure("Could not retrieve user from database")
        }

        // Create authentication token and authenticate it
        val token = UsernamePasswordAuthenticationToken(userDetails, password, userDetails.authorities)
        authManager.authenticate(token)
        if (token.isAuthenticated) SecurityContextHolder.getContext().authentication = token

        // User successfully created - redirect to /user endpoint
        return RestResponseFactory.created(URI.create("/api/v1/auth/user"))

    }

    @ApiOperation("Login a registered user")
    @PostMapping(
            path = ["/login"],
            consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun login(@RequestBody dto: AuthDto): ResponseEntity<WrappedResponse<Void>> {

        // Extract data from the DTO
        val username = dto.username.toLowerCase()
        val password = dto.password

        // Attempt to retrieve the user from database
        val userDetails = try {
            userDetailsService.loadUserByUsername(username)
        } catch (e: UsernameNotFoundException) {
            return RestResponseFactory.userError("Username not found")
        }

        // Create authentication token and authenticate it
        val token = UsernamePasswordAuthenticationToken(userDetails, password, userDetails.authorities)
        authManager.authenticate(token)
        if (token.isAuthenticated) {
            SecurityContextHolder.getContext().authentication = token
            return RestResponseFactory.noPayload(204)
        }

        // Fallback in case authentication fails - Wrong password gets handled by 'authManager.authenticate(token)'
        return RestResponseFactory.userError("Authentication failed")
    }

}