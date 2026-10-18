package com.dremioplatform.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VerifiedActorFilterTest {
  @Test
  void canonicalUuidSubject() {
    assertEquals("123e4567-e89b-12d3-a456-426614174000",
      VerifiedActorFilter.canonicalActor("123e4567-e89b-12d3-a456-426614174000"));
  }
}
