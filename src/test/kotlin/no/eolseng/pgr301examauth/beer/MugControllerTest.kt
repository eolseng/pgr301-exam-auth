package no.eolseng.pgr301examauth.beer

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MugControllerTest : ControllerTestBase() {

    @BeforeEach
    fun localSetup() {
        RestAssured.basePath = "/v1/keg/"
    }

    @Test
    fun `register mug`() {
        val session = loginUser(registerUser())
        val mugId = createMug(session, 5)
        val mug = mugRepo.findById(mugId)
        assertEquals(true, mug.isPresent)
    }

    @Test
    fun `retrieve mug by ID`() {

        val session = loginUser(registerUser())
        val mugId = createMug(session, 5)

        // Not authenticated - should fail
        RestAssured.basePath = "/v1/mug"
        RestAssured.given().accept(ContentType.JSON)
                .get("/$mugId")
                .then().assertThat()
                .statusCode(401)

        // Authenticated
        RestAssured.basePath = "/v1/mug"
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .get("/$mugId")
                .then().assertThat()
                .statusCode(200)
                .body("data.id", equalTo(mugId))
                .body("data.currentVolume", equalTo(0))
    }

    @Test
    fun `retrieve all user mugs`() {

        val session = loginUser(registerUser())

        RestAssured.basePath = "/v1/mug"
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .get("/")
                .then().assertThat()
                .statusCode(200)
                .body("data.size()", equalTo(0))

        val mug1Id = createMug(session, 5)
        RestAssured.basePath = "/v1/mug"
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .get("/")
                .then().assertThat()
                .statusCode(200)
                .body("data.size()", equalTo(1))
                .body("data.any {it.id == $mug1Id}", equalTo(true))

        val mug2Id = createMug(session, 3)
        RestAssured.basePath = "/v1/mug"
        RestAssured.given().accept(ContentType.JSON)
                .cookie("JSESSIONID", session)
                .get("/")
                .then().assertThat()
                .statusCode(200)
                .body("data.size()", equalTo(2))
                .body("data.any {it.id == $mug1Id}", equalTo(true))
                .body("data.any {it.id == $mug2Id}", equalTo(true))

    }

}