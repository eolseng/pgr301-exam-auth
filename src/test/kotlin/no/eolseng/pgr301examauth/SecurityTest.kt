package no.eolseng.pgr301examauth

import io.restassured.RestAssured
import io.restassured.http.ContentType
import no.eolseng.pgr301examauth.db.UserRepository
import no.eolseng.pgr301examauth.dto.AuthDto
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@SpringBootTest(
        classes = [(AuthApplication::class)],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SecurityTest {

    @LocalServerPort
    private var port = 0

    @Autowired
    private lateinit var repository: UserRepository

    companion object {
        // ID Counter for creating unique usernames
        private var idCounter = 0
        fun getId(): Int {
            return idCounter++;
        }
    }

    @BeforeEach
    fun initialize() {
        // Configure RestAssured
        RestAssured.baseURI = "http://localhost"
        RestAssured.basePath = "/api/v1/auth"
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()

        // Clean database before each test
        repository.deleteAll()
    }

    /**
     * Utility function to register a new user
     * @return the authenticated 'SESSION' cookie
     */
    private fun registerUser(authDto: AuthDto): String {
        // Get 201 on successful registration. Should have Location and Set-Cookie header.
        return RestAssured.given().contentType(ContentType.JSON)
                .body(authDto)
                .post("/signup")
                .then().assertThat()
                .statusCode(201)
                .header("Location", CoreMatchers.containsString("/api/v1/auth/user"))
                .header("Set-Cookie", CoreMatchers.containsString("JSESSIONID"))
                .cookie("JSESSIONID")
                .extract().cookie("JSESSIONID")
    }

    /**
     * Utility function to check if the session cookie is valid
     */
    private fun checkAuthenticatedCookie(cookie: String, expectedCode: Int) {
        RestAssured.given().cookie("JSESSIONID", cookie)
                .get("/user")
                .then().assertThat()
                .statusCode(expectedCode)
    }

    private fun getUniqueAuthDto(): AuthDto {
        return AuthDto(username = "test_username_${getId()}", password = "test_password")
    }

    @Test
    fun `test register user`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)
        checkAuthenticatedCookie(cookie, 200)
    }

    @Test
    fun `test already registered username`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)
        checkAuthenticatedCookie(cookie, 200)

        // Get 400 on failed registration. Body should contain message describing error.
        RestAssured.given().contentType(ContentType.JSON)
                .body(dto)
                .post("/signup")
                .then().assertThat()
                .statusCode(400)
                .body("message", CoreMatchers.anything())
    }

    @Test
    fun `test valid login`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)

        // Get 204 on successful login.
        val login = RestAssured.given().contentType(ContentType.JSON)
                .body(dto)
                .post("/login")
                .then().assertThat()
                .statusCode(204)
                .cookie("JSESSIONID")
                .extract().cookie("JSESSIONID")

        // New login should create a new session cookie
        assertNotEquals(login, cookie)
        checkAuthenticatedCookie(login, 200)
    }

    @Test
    fun `test login with invalid username`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)
        checkAuthenticatedCookie(cookie, 200)

        val invalidPasswordUser = AuthDto(username = dto.username + "_fail", password = dto.password)

        // Get 400 on invalid username. Body should contain message with error description.
        val invalidLogin = RestAssured.given().contentType(ContentType.JSON)
                .body(invalidPasswordUser)
                .post("/login")
                .then().assertThat()
                .statusCode(400)
                .body("message", CoreMatchers.anything())
                .extract().cookie("JSESSIONID")
        // Should not generate session cookie with bad username
        assertNull(invalidLogin)
    }

    @Test
    fun `test login with invalid password`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)
        checkAuthenticatedCookie(cookie, 200)

        val invalidPasswordUser = AuthDto(username = dto.username, password = dto.password + 123)

        // Get 401 on failed login. Body should contain message with error description.
        val invalidLogin = RestAssured.given().contentType(ContentType.JSON)
                .body(invalidPasswordUser)
                .post("/login")
                .then().assertThat()
                .statusCode(401)
//                .body("message", CoreMatchers.anything()) // No body when not using Spring Cloud Session
                .cookie("JSESSIONID")
                .extract().cookie("JSESSIONID")
        // Should create new session cookie
        assertNotEquals(invalidLogin, cookie)
        // New cookie should not be valid
        checkAuthenticatedCookie(invalidLogin, 401)

    }

    @Test
    fun `test user endpoint - unauthorized`() {
        // Get 401 on unauthorized request
        RestAssured.given().get("/user")
                .then().assertThat()
                .statusCode(401)
    }

    @Test
    fun `test user endpoint - authorized`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)
        checkAuthenticatedCookie(cookie, 200)

        // Get 200 on authorized request. Body should have username and roles
        RestAssured.given().cookie("JSESSIONID", cookie)
                .get("/user")
                .then().assertThat()
                .statusCode(200)
                .body("data.username", CoreMatchers.equalTo(dto.username))
                .body("data.roles", Matchers.contains("ROLE_USER"))
    }

    @Test
    fun `test logout`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)
        checkAuthenticatedCookie(cookie, 200)

        // Get 204 on logout
        RestAssured.given().cookie("JSESSIONID", cookie)
                .post("/logout")
                .then().assertThat()
                .statusCode(204)

        // Session cookie should be invalid after logout
        checkAuthenticatedCookie(cookie, 401)
    }
}