package no.eolseng.pgr301examauth.beer

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TapControllerTest : ControllerTestBase() {

    @BeforeEach
    fun localSetup() {
        RestAssured.basePath = "/v1/brew/"
    }

    @Test
    fun `fill a keg`() {

        val kegCapacity = 500
        val mugCapacity = 5

        val session = loginUser(registerUser())
        val kegId = createKeg(session, kegCapacity)
        val mugId = createMug(session, mugCapacity)

        // Fill the empty keg with beer
        var start = System.currentTimeMillis()
        RestAssured.basePath = "/v1/brew/"
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .post("/$kegId")
                .then().assertThat()
                .statusCode(200)
                .body("data.id", CoreMatchers.equalTo(kegId))
                .body("data.capacity", CoreMatchers.equalTo(kegCapacity))
                .body("data.currentVolume", CoreMatchers.equalTo(kegCapacity))
        var end = System.currentTimeMillis()

        // Response should have taken more milliseconds than the capacity of the keg
        assertTrue((end - start) >= kegCapacity)

        // Tap a beer
        start = System.currentTimeMillis()
        val tapDto = TapDto(kegId = kegId, mugId = mugId)
        RestAssured.basePath = "/v1/tap/"
        RestAssured.given().contentType(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .body(tapDto)
                .post("/")
                .then().assertThat()
                .statusCode(200)
        end = System.currentTimeMillis()

        // Response should have taken more milliseconds * 10 than the capacity of the mug
        assertTrue((end - start) >= mugCapacity * 10)

        // Check that the keg has been drained
        RestAssured.basePath = "/v1/keg"
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .get("/$kegId")
                .then().assertThat()
                .statusCode(200)
                .body("data.id", CoreMatchers.equalTo(kegId))
                .body("data.currentVolume", CoreMatchers.equalTo(kegCapacity - mugCapacity))

        // Check that the mug has been filled
        RestAssured.basePath = "/v1/mug"
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .get("/$mugId")
                .then().assertThat()
                .statusCode(200)
                .body("data.id", CoreMatchers.equalTo(mugId))
                .body("data.currentVolume", CoreMatchers.equalTo(mugCapacity))

    }

}