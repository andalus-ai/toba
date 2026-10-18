package com.dremioplatform.identity.service;

import com.dremioplatform.common.error.ConflictException;
import com.dremioplatform.common.error.EntityNotFoundException;
import com.dremioplatform.identity.domain.ServiceAccount;
import com.dremioplatform.identity.repository.ServiceAccountRepository;
import com.dremioplatform.identity.service.dto.CreateServiceAccountRequest;
import com.dremioplatform.identity.service.dto.ServiceAccountResponse;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ServiceAccountService {

    @Inject ServiceAccountRepository serviceAccountRepository;

    @WithTransaction
    public Uni<ServiceAccountResponse> create(CreateServiceAccountRequest request) {
        return serviceAccountRepository.existsByName(request.name())
                .chain(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(new ConflictException(
                                "IDENTITY-409-SERVICE-ACCOUNT-EXISTS",
                                "A service account named '" + request.name() + "' already exists"));
                    }
                    ServiceAccount sa = new ServiceAccount();
                    sa.id = UUID.randomUUID();
                    sa.name = request.name();
                    sa.description = request.description();
                    sa.ownerUserId = request.ownerUserId();
                    return serviceAccountRepository.persist(sa).map(ServiceAccountResponse::from);
                });
    }

    @WithTransaction
    public Uni<ServiceAccountResponse> disable(UUID id) {
        return serviceAccountRepository.findById(id)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "IDENTITY-404-SERVICE-ACCOUNT-NOT-FOUND", "ServiceAccount", id))
                .map(sa -> {
                    sa.enabled = false;
                    return sa;
                })
                .map(ServiceAccountResponse::from);
    }

    public Uni<List<ServiceAccountResponse>> listAll() {
        return serviceAccountRepository.listAll()
                .map(list -> list.stream().map(ServiceAccountResponse::from).toList());
    }
}
