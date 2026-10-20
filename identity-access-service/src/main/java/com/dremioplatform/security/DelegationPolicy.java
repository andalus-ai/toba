package com.dremioplatform.security;

import java.util.Collection;
import java.util.Set;

/** Pure policy checks shared by the REST guard and assignment service. */
public final class DelegationPolicy {
    private DelegationPolicy() {}

    public static boolean canDelegate(Set<String> actorPermissions, Collection<String> rolePermissions) {
        return actorPermissions != null && rolePermissions != null && actorPermissions.containsAll(rolePermissions);
    }

    public static boolean isWithinScope(String root, String candidate) {
        return root != null && !root.isBlank() && candidate != null
                && (candidate.equals(root) || candidate.startsWith(root + "."));
    }
}
