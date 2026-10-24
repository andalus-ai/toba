package com.dremioplatform.catalog.resource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.dremioplatform.catalog.security.CatalogAccessGuard;
import com.dremioplatform.catalog.service.NamespaceService;
import com.dremioplatform.catalog.service.TableService;
import jakarta.ws.rs.ForbiddenException;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** No catalog service may execute when the authorization boundary denies. */
class CatalogEndpointAuthorizationTest {
    private TableResource tables;
    private NamespaceResource namespaces;
    private TableService tableService;
    private NamespaceService namespaceService;

    @BeforeEach
    void setup() {
        tableService = mock(TableService.class);
        namespaceService = mock(NamespaceService.class);
        CatalogAccessGuard guard = mock(CatalogAccessGuard.class);
        when(guard.authorize(any(), anyString(), anyString()))
                .thenReturn(Uni.createFrom().failure(new ForbiddenException("denied")));
        tables = new TableResource();
        tables.tableService = tableService;
        tables.access = guard;
        namespaces = new NamespaceResource();
        namespaces.namespaceService = namespaceService;
        namespaces.access = guard;
    }

    @Test void tableListCannotReachService() {
        assertThrows(ForbiddenException.class, () -> tables.list("sales").await().indefinitely());
        verifyNoInteractions(tableService);
    }

    @Test void tableLoadCannotReachService() {
        assertThrows(ForbiddenException.class, () -> tables.load("sales", "orders").await().indefinitely());
        verifyNoInteractions(tableService);
    }

    @Test void tableDropCannotReachService() {
        assertThrows(ForbiddenException.class, () -> tables.drop("sales", "orders").await().indefinitely());
        verifyNoInteractions(tableService);
    }

    @Test void tableCreateCannotReachService() {
        assertThrows(ForbiddenException.class, () -> tables.create("sales", null).await().indefinitely());
        verifyNoInteractions(tableService);
    }

    @Test void namespaceListCannotReachService() {
        assertThrows(ForbiddenException.class, () -> namespaces.list(null).await().indefinitely());
        verifyNoInteractions(namespaceService);
    }

    @Test void namespaceReadCannotReachService() {
        assertThrows(ForbiddenException.class, () -> namespaces.get("sales").await().indefinitely());
        verifyNoInteractions(namespaceService);
    }

    @Test void namespaceHeadCannotReachService() {
        assertThrows(ForbiddenException.class, () -> namespaces.exists("sales").await().indefinitely());
        verifyNoInteractions(namespaceService);
    }

    @Test void namespaceDeleteCannotReachService() {
        assertThrows(ForbiddenException.class, () -> namespaces.delete("sales").await().indefinitely());
        verifyNoInteractions(namespaceService);
    }

    @Test void namespaceCreateCannotReachService() {
        assertThrows(ForbiddenException.class, () -> namespaces.create(null).await().indefinitely());
        verifyNoInteractions(namespaceService);
    }
}
