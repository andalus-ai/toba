package com.dremioplatform.identity;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserResourceTest {

    @Test
    void createUser_localAccount_persistsAndReturns201() {
        String body = """
                {
                  "username": "jsmith",
                  "email": "jsmith@example.com",
                  "displayName": "Jane Smith",
                  "localPassword": "correct-horse-battery-staple"
                }
                """;

        given()
                .contentType("application/json")
                .body(body)
                .when().post("/api/v1/users")
                .then()
                .statusCode(201)
                .body("username", equalTo("jsmith"))
                .body("email", equalTo("jsmith@example.com"))
                .body("federated", equalTo(false))
                .body("id", notNullValue());
    }

    @Test
    void createUser_duplicateUsername_returns409ProblemDetail() {
        String body = """
                {
                  "username": "dupeuser",
                  "email": "dupe1@example.com",
                  "localPassword": "correct-horse-battery-staple"
                }
                """;
        given().contentType("application/json").body(body)
                .when().post("/api/v1/users")
                .then().statusCode(201);

        String duplicate = """
                {
                  "username": "dupeuser",
                  "email": "dupe2@example.com",
                  "localPassword": "correct-horse-battery-staple"
                }
                """;
        given().contentType("application/json").body(duplicate)
                .when().post("/api/v1/users")
                .then()
                .statusCode(409)
                .body("errorCode", equalTo("IDENTITY-409-USER-EXISTS"));
    }

    @Test
    void createUser_missingPasswordForLocalUser_returns400() {
        String body = """
                {
                  "username": "nopassword",
                  "email": "nopassword@example.com"
                }
                """;
        given().contentType("application/json").body(body)
                .when().post("/api/v1/users")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("IDENTITY-400-PASSWORD-REQUIRED"));
    }
}
