package roomescape.controller.api;

import static org.hamcrest.Matchers.contains;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import roomescape.controller.dto.LoginRequest;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@Sql(scripts = "/data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/truncate.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class UserThemeControllerTest {

    private String userToken;

    @BeforeEach
    void login() {
        LoginRequest user = new LoginRequest("user@a.com", "123a!");

        userToken = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(user)
            .when().post("/login")
            .then().extract().cookie("token");
    }

    @DisplayName("성공: 테마 조회 -> 200")
    @Test
    void findAll() {
        RestAssured.given().log().all()
            .cookie("token", userToken)
            .contentType(ContentType.JSON)
            .when().get("/themes")
            .then().log().all()
            .statusCode(200)
            .body("id", contains(1, 2, 3))
            .body("name", contains("theme1", "theme2", "theme3"))
            .body("description", contains("desc1", "desc2", "desc3"));
    }

    @DisplayName("성공: 상위 10개의 인기 테마 조회 -> 200")
    @Test
    void findPopular() {
        RestAssured.given().log().all()
            .when().get("/themes/trending")
            .then().log().all()
            .statusCode(200)
            .body("id", contains(1, 3, 2));
    }
}