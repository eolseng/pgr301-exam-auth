package no.eolseng.pgr301examauth.beer

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KegControllerTest : ControllerTestBase() {

    @BeforeEach
    fun localSetup() {
        RestAssured.basePath = "/v1/keg/"
    }

    @Test
    fun `register keg`() {
        val session = loginUser(registerUser())
        val kegId = createKeg(session, 500)
        val keg = kegRepo.findById(kegId)
        assertEquals(true, keg.isPresent)
    }

    @Test
    fun `retrieve keg by ID`() {

        val session = loginUser(registerUser())
        val kegId = createKeg(session, 500)

        // Not authenticated - should fail
        RestAssured.basePath = "/v1/keg"
        RestAssured.given().accept(ContentType.JSON)
                .get("/$kegId")
                .then().assertThat()
                .statusCode(401)

        // Authenticated
        RestAssured.basePath = "/v1/keg"
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .get("/$kegId")
                .then().assertThat()
                .statusCode(200)
                .body("data.id", equalTo(kegId))
                .body("data.currentVolume", equalTo(0))
    }

    @Test
    fun `retrieve all user kegs`() {

        val session = loginUser(registerUser())

        RestAssured.basePath = "/v1/keg"
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .get("/")
                .then().assertThat()
                .statusCode(200)
                .body("data.size()", equalTo(0))

        val keg1Id = createKeg(session, 500)
        RestAssured.basePath = "/v1/keg"
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .get("/")
                .then().assertThat()
                .statusCode(200)
                .body("data.size()", equalTo(1))
                .body("data.any {it.id == $keg1Id}", equalTo(true))

        val keg2Id = createKeg(session, 300)
        RestAssured.basePath = "/v1/keg"
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .get("/")
                .then().assertThat()
                .statusCode(200)
                .body("data.size()", equalTo(2))
                .body("data.any {it.id == $keg1Id}", equalTo(true))
                .body("data.any {it.id == $keg2Id}", equalTo(true))

    }

}