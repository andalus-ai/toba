package com.dremioplatform.identity;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthResourceTest {

    @Test
    void login_validLocalCredentials_returnsBearerToken() {
        given().contentType(ContentType.JSON).body("""
                {"username": "authuser", "email": "authuser@example.com", "localPassword": "correct-horse-battery-staple"}
                """)
                .when().post("/api/v1/users")
                .then().statusCode(201);

        given().contentType(ContentType.JSON).body("""
                {"username": "authuser", "password": "correct-horse-battery-staple"}
                """)
                .when().post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .body("accessToken", notNullValue());
    }

    @Test
    void login_badPassword_returns401() {
        given().contentType(ContentType.JSON).body("""
                {"username": "nosuchuser", "password": "wrong"}
                """)
                .when().post("/api/v1/auth/login")
                .then()
                .statusCode(401)
                .body("errorCode", equalTo("IDENTITY-401-BAD-CREDENTIALS"));
    }
}
