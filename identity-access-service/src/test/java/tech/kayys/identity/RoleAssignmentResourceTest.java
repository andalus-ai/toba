package com.dremioplatform.identity;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RoleAssignmentResourceTest {

    @Test
    void createRole_withKnownPermissions_returns201WithGrantedKeys() {
        String body = """
                {
                  "name": "DATASET_READER_%s",
                  "description": "reads datasets",
                  "permissionKeys": ["catalog.dataset.read"]
                }
                """.formatted(UUID.randomUUID());

        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/v1/roles")
                .then()
                .statusCode(201)
                .body("permissionKeys", hasItem("catalog.dataset.read"));
    }

    @Test
    void createRole_withUnknownPermission_returns400() {
        String body = """
                {
                  "name": "BOGUS_ROLE_%s",
                  "permissionKeys": ["not.a.real.permission"]
                }
                """.formatted(UUID.randomUUID());

        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/v1/roles")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("IDENTITY-400-UNKNOWN-PERMISSION"));
    }

    /**
     * The key behavior the whole scope-path design exists for: a role granted at
     * "space.sales" (an ancestor scope) must apply when checking a path further
     * down the tree, e.g. "space.sales.folder.q4.dataset.orders" — see
     * RoleAssignmentRepository's prefix-match query doc comment.
     */
    @Test
    void effectivePermissions_grantedAtAncestorScope_appliesToDescendantPath() {
        UUID principalId = UUID.randomUUID();
        String roleName = "GRANT_TEST_ROLE_" + UUID.randomUUID();

        String roleBody = """
                {"name": "%s", "permissionKeys": ["catalog.dataset.read"]}
                """.formatted(roleName);
        String roleId = given().contentType(ContentType.JSON).body(roleBody)
                .when().post("/api/v1/roles")
                .then().statusCode(201)
                .extract().path("id");

        String assignmentBody = """
                {
                  "principalType": "USER",
                  "principalId": "%s",
                  "roleId": "%s",
                  "scopeType": "SPACE_OR_SOURCE",
                  "scopePath": "space.sales"
                }
                """.formatted(principalId, roleId);
        given().contentType(ContentType.JSON).body(assignmentBody)
                .when().post("/api/v1/role-assignments")
                .then().statusCode(201);

        given()
                .queryParam("scopePath", "space.sales.folder.q4.dataset.orders")
                .when().get("/api/v1/role-assignments/principal/USER/" + principalId + "/effective-permissions")
                .then()
                .statusCode(200)
                .body("$", hasItem("catalog.dataset.read"));
    }
}
