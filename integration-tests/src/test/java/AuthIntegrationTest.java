import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AuthIntegrationTest {

  @BeforeAll
  static void setUp() {
    RestAssured.baseURI = "http://localhost:4004";
  }

  @Test
  public void shouldReturnOKWithValidToken() {
    String loginPayload =
        """
        {
        "email": "testuser@test.com",
        "password": "password123"
        }
        """;

    Response response =
        RestAssured.given()
            .contentType("application/json")
            .body(loginPayload)
            .when()
            .post("/auth/login")
            .then()
            .statusCode(200)
            .body("token", Matchers.notNullValue())
            .extract()
            .response();

    System.out.println("Generated token: " + response.jsonPath().getString("token"));
  }

  @Test
  public void shouldReturnUnauthorizedOnInvalidLogin() {
    String loginPayload =
        """
        {
        "email": "invaliduser@test.com",
        "password": "password123"
        }
        """;

    RestAssured.given()
        .contentType("application/json")
        .body(loginPayload)
        .when()
        .post("/auth/login")
        .then()
        .statusCode(401);
  }
}
