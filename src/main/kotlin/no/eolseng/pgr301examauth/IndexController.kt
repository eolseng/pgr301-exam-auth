package no.eolseng.pgr301examauth

import io.swagger.annotations.Api
import no.eolseng.pg6102.utils.wrappedresponse.RestResponseFactory
import no.eolseng.pg6102.utils.wrappedresponse.WrappedResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api(value = "/", description = "Index page of the application")
@RestController
@RequestMapping("/")
class IndexController {

    @GetMapping(path = ["/"])
    fun getIndex(): ResponseEntity<WrappedResponse<Any>> {
        return RestResponseFactory.payload(200, "Hello world! This service is active.")
    }

}