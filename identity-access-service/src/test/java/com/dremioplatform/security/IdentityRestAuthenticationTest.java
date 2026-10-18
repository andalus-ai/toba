package com.dremioplatform.security;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;

@QuarkusTest
class IdentityRestAuthenticationTest {
  @Test
  void anonymousRequestIsRejectedBeforeRouting() {
    given().when().get("/api/v1/users").then().statusCode(401);
  }

  @Test
  void spoofedActorHeaderCannotBypassAuthentication() {
    given().header("X-Actor-Id", "00000000-0000-0000-0000-000000000001")
        .when().get("/api/v1/users").then().statusCode(401);
  }
}
