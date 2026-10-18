package com.dremioplatform.identity.service;

import com.dremioplatform.common.error.ConflictException;
import com.dremioplatform.common.error.EntityNotFoundException;
import com.dremioplatform.identity.domain.User;
import com.dremioplatform.identity.event.EventPublisher;
import com.dremioplatform.identity.event.UserLifecycleEvent;
import com.dremioplatform.identity.repository.UserRepository;
import com.dremioplatform.identity.service.dto.CreateUserRequest;
import com.dremioplatform.identity.service.dto.UserResponse;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.elytron.security.common.BcryptUtil;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserService {

    @Inject UserRepository userRepository;
    @Inject EventPublisher eventPublisher;

    @WithTransaction
    public Uni<UserResponse> createUser(CreateUserRequest request, UUID actorUserId) {
        return userRepository.existsByUsernameOrEmail(request.username(), request.email())
                .chain(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new ConflictException("IDENTITY-409-USER-EXISTS",
                                        "A user with that username or email already exists"));
                    }
                    User user = new User();
                    user.id = UUID.randomUUID();
                    user.username = request.username();
                    user.email = request.email();
                    user.displayName = request.displayName();
                    user.identityProviderId = request.identityProviderId();
                    user.externalSubject = request.externalSubject();
                    if (request.identityProviderId() == null) {
                        if (request.localPassword() == null) {
                            return Uni.createFrom().failure(
                                    new com.dremioplatform.common.error.ValidationException(
                                            "IDENTITY-400-PASSWORD-REQUIRED",
                                            "localPassword is required for non-federated users"));
                        }
                        user.localPasswordHash = BcryptUtil.bcryptHash(request.localPassword());
                    }
                    return userRepository.persist(user)
                            .call(u -> eventPublisher.publish(
                                    UserLifecycleEvent.of(u.id, u.username, UserLifecycleEvent.Transition.CREATED, actorUserId)))
                            .map(UserResponse::from);
                });
    }

    @WithTransaction
    public Uni<UserResponse> suspendUser(UUID userId, UUID actorUserId) {
        return userRepository.findById(userId)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException("IDENTITY-404-USER-NOT-FOUND", "User", userId))
                .chain(user -> {
                    user.status = User.UserStatus.SUSPENDED;
                    return eventPublisher.publish(
                                    UserLifecycleEvent.of(user.id, user.username, UserLifecycleEvent.Transition.SUSPENDED, actorUserId))
                            .replaceWith(user);
                })
                .map(UserResponse::from);
    }

    @WithTransaction
    public Uni<UserResponse> deprovisionUser(UUID userId, UUID actorUserId) {
        return userRepository.findById(userId)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException("IDENTITY-404-USER-NOT-FOUND", "User", userId))
                .chain(user -> {
                    user.status = User.UserStatus.DEPROVISIONED;
                    return eventPublisher.publish(
                                    UserLifecycleEvent.of(user.id, user.username, UserLifecycleEvent.Transition.DEPROVISIONED, actorUserId))
                            .replaceWith(user);
                })
                .map(UserResponse::from);
    }

    public Uni<UserResponse> getById(UUID userId) {
        return userRepository.findById(userId)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException("IDENTITY-404-USER-NOT-FOUND", "User", userId))
                .map(UserResponse::from);
    }

    public Uni<List<UserResponse>> listAll() {
        return userRepository.listAll().map(users -> users.stream().map(UserResponse::from).toList());
    }
}
