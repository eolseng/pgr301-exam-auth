package no.eolseng.pgr301examauth.beer

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BrewControllerTest : ControllerTestBase() {

    @BeforeEach
    fun localSetup() {
        RestAssured.basePath = "/v1/brew/"
    }

    @Test
    fun `fill a keg`() {
        val session = loginUser(registerUser())

        val capacity = 500

        val kegId = createKeg(session, capacity)

        RestAssured.basePath = "/v1/brew/"
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .post("/$kegId")
                .then().assertThat()
                .statusCode(200)
                .body("data.id", CoreMatchers.equalTo(kegId))
                .body("data.capacity", CoreMatchers.equalTo(capacity))
                .body("data.currentVolume", CoreMatchers.equalTo(capacity))

    }

}