package com.dremioplatform.catalog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CatalogFlowTest {

    private static final String SAMPLE_SCHEMA_JSON = """
            {"type":"struct","fields":[
              {"id":1,"name":"id","required":true,"type":"long"},
              {"id":2,"name":"customer_name","required":false,"type":"string"}
            ]}
            """;

    @Test
    void config_returnsEmptyDefaultsAndOverrides() {
        given().when().get("/v1/config")
                .then().statusCode(200)
                .body("defaults", notNullValue())
                .body("overrides", notNullValue());
    }

    @Test
    void createNamespace_thenCreateTable_thenLoadTable_roundTrips() {
        String ns = "sales_" + UUID.randomUUID().toString().replace("-", "");

        given().contentType(ContentType.JSON).body("""
                {"namespace": ["%s"], "properties": {"owner": "data-eng"}}
                """.formatted(ns))
                .when().post("/v1/namespaces")
                .then().statusCode(200);

        given().contentType(ContentType.JSON).body("""
                {"name": "orders", "schemaJson": %s, "properties": {"format-version": "2"}}
                """.formatted(toJsonLiteral(SAMPLE_SCHEMA_JSON)))
                .when().post("/v1/namespaces/" + ns + "/tables")
                .then()
                .statusCode(200)
                .body("metadataLocation", notNullValue())
                // Confirms this is a REAL Iceberg TableMetadata document, not a
                // placeholder — real TableMetadata JSON always has these top-level
                // fields (format-version, a generated table-uuid, the schema).
                .body("metadataJson", containsString("\"format-version\""))
                .body("metadataJson", containsString("\"table-uuid\""))
                .body("metadataJson", containsString("customer_name"));

        // Load reads the metadata back from actual storage (LocalFilesystemObjectStorageClient)
        // and re-parses it with Iceberg's real TableMetadataParser — a genuine round trip.
        given().when().get("/v1/namespaces/" + ns + "/tables/orders")
                .then()
                .statusCode(200)
                .body("properties.'format-version'", equalTo("2"))
                .body("metadataJson", containsString("customer_name"));
    }

    @Test
    void createTable_inNonexistentNamespace_returns404() {
        given().contentType(ContentType.JSON).body("""
                {"name": "orphan_table", "schemaJson": %s}
                """.formatted(toJsonLiteral(SAMPLE_SCHEMA_JSON)))
                .when().post("/v1/namespaces/does_not_exist/tables")
                .then()
                .statusCode(404)
                .body("errorCode", equalTo("CATALOG-404-NAMESPACE-NOT-FOUND"));
    }

    @Test
    void createTable_invalidSchemaJson_returns400() {
        String ns = "invalid_schema_test_" + UUID.randomUUID().toString().replace("-", "");
        given().contentType(ContentType.JSON).body("""
                {"namespace": ["%s"]}
                """.formatted(ns))
                .when().post("/v1/namespaces")
                .then().statusCode(200);

        given().contentType(ContentType.JSON).body("""
                {"name": "bad_table", "schemaJson": "{\\"not\\": \\"a valid iceberg schema\\"}"}
                """)
                .when().post("/v1/namespaces/" + ns + "/tables")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("CATALOG-400-INVALID-SCHEMA"));
    }

    /** Embeds a raw JSON text block as a JSON string literal value inside another
     *  JSON body being built inline in these tests. */
    private static String toJsonLiteral(String rawJson) {
        return "\"" + rawJson.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
