package com.dremioplatform.security;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;

@QuarkusTest
class RestAuthenticationTest {
  @Test
  void anonymousRestAccessIsDenied() {
    given().when().get("/api/v1/embeddings").then().statusCode(401);
  }
}
