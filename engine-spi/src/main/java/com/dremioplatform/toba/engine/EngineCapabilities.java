package com.dremioplatform.toba.engine;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Immutable capability declaration. Never infer support from engine name/version. */
public record EngineCapabilities(Set<EngineCapability> supported) {
    public EngineCapabilities {
        supported = supported == null || supported.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(supported));
    }

    public boolean supports(EngineCapability capability) {
        return supported.contains(capability);
    }

    public static EngineCapabilities none() {
        return new EngineCapabilities(Set.of());
    }
}
