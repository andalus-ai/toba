package com.dremioplatform.security;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerifiedActorFilterTest {
  @Test
  void canonicalUuidSubject() {
    assertEquals("123e4567-e89b-12d3-a456-426614174000",
      VerifiedActorFilter.canonicalActor("123e4567-e89b-12d3-a456-426614174000"));
  }

  @Test
  void invalidSubjectFailsClosed() {
    WebApplicationException failure = assertThrows(WebApplicationException.class,
      () -> VerifiedActorFilter.canonicalActor("external-subject"));
    assertEquals(403, failure.getResponse().getStatus());
  }

  @Test
  void emptySubjectFailsClosed() {
    WebApplicationException failure = assertThrows(WebApplicationException.class,
      () -> VerifiedActorFilter.canonicalActor(""));
    assertEquals(403, failure.getResponse().getStatus());
  }
}
