package no.eolseng.pgr301examauth.beer

import io.restassured.RestAssured
import io.restassured.http.ContentType
import no.eolseng.pgr301examauth.AuthApplication
import no.eolseng.pgr301examauth.db.UserRepository
import no.eolseng.pgr301examauth.dto.AuthDto
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(
        classes = [(AuthApplication::class)],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ControllerTestBase {

    @LocalServerPort
    protected var port = 0

    @Autowired
    lateinit var userRepo: UserRepository

    @Autowired
    lateinit var kegRepo: KegRepository

    @Autowired
    lateinit var mugRepo: MugRepository

    @BeforeEach
    @AfterEach
    fun Setup() {
        RestAssured.baseURI = "http://localhost/api"
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()

        // UserRepo should cascade to all kegs and mugs
        userRepo.deleteAll()
        kegRepo.deleteAll()
        mugRepo.deleteAll()
    }

    companion object {
        private var idCounter = 0
        fun getId(): Int {
            return idCounter++
        }
    }

    fun registerUser(): AuthDto {
        val username = "test_username_${getId()}"
        val password = "test_password"
        val userDto = AuthDto(username, password)

        signupUser(userDto)
        logoutUser()

        return userDto
    }

    fun signupUser(userDto: AuthDto) {
        RestAssured.basePath = "/v1/auth/"
        RestAssured.given().contentType(ContentType.JSON)
                .body(userDto)
                .post("/signup")
                .then()
                .assertThat()
                .statusCode(201)
                .header("Set-Cookie", CoreMatchers.containsString("JSESSIONID"))
                .cookie("JSESSIONID")
    }

    fun loginUser(userDto: AuthDto): String {
        RestAssured.basePath = "/v1/auth/"
        return RestAssured.given().contentType(ContentType.JSON)
                .body(userDto)
                .post("/login")
                .then()
                .assertThat()
                .statusCode(204)
                .header("Set-Cookie", CoreMatchers.containsString("JSESSIONID"))
                .cookie("JSESSIONID")
                .extract()
                .cookie("JSESSIONID")
    }

    fun logoutUser() {
        RestAssured.basePath = "/v1/auth/"
        RestAssured.given().post("/logout")
                .then()
                .assertThat()
                .statusCode(204)
                .header("Set-Cookie", CoreMatchers.containsString("JSESSIONID=;"))
                .cookie("JSESSIONID", Matchers.emptyOrNullString())
    }

    /**
     * Creates a keg and returns the new kegs ID
     */
    fun createKeg(session: String, capacity: Int): Int {
        RestAssured.basePath = "/v1/keg/"
        val dto = KegDto(capacity = capacity)
        val redirect =
                RestAssured.given().contentType(ContentType.JSON)
                        .cookie("JSESSIONID", session)
                        .body(dto)
                        .post("/")
                        .then().assertThat()
                        .statusCode(201)
                        .extract()
                        .header("Location")
        return redirect.substringAfter(RestAssured.basePath).toInt()
    }

    /**
     * Creates a mug and returns the new mugs ID
     */
    fun createMug(session: String, capacity: Int): Int {
        RestAssured.basePath = "/v1/mug/"
        val dto = MugDto(capacity = capacity)
        val redirect =
                RestAssured.given().contentType(ContentType.JSON)
                        .cookie("JSESSIONID", session)
                        .body(dto)
                        .post("/")
                        .then().assertThat()
                        .statusCode(201)
                        .extract()
                        .header("Location")
        return redirect.substringAfter(RestAssured.basePath).toInt()
    }

}