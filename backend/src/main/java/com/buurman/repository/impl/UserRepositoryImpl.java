package com.buurman.repository.impl;

import com.buurman.domain.User;
import com.buurman.mapper.UserRecordMapper;
import com.buurman.repository.UserRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.USERS;

/**
 * JOOQ implementation of UserRepository.
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final DSLContext dsl;
    private final UserRecordMapper mapper;

    public UserRepositoryImpl(DSLContext dsl, UserRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return dsl.selectFrom(USERS)
                .where(USERS.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (user.getId() == null) {
            // INSERT
            UUID newId = UUID.randomUUID();
            dsl.insertInto(USERS)
                    .set(USERS.ID, newId)
                    .set(USERS.KEYCLOAK_ID, user.getKeycloakId())
                    .set(USERS.EMAIL, user.getEmail())
                    .set(USERS.FIRST_NAME, user.getFirstName())
                    .set(USERS.LAST_NAME, user.getLastName())
                    .set(USERS.CREATED_AT, now)
                    .set(USERS.UPDATED_AT, now)
                    .execute();

            user.setId(newId);
            user.setCreatedAt(now.toInstant(ZoneOffset.UTC));
            user.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        } else {
            // UPDATE
            dsl.update(USERS)
                    .set(USERS.KEYCLOAK_ID, user.getKeycloakId())
                    .set(USERS.EMAIL, user.getEmail())
                    .set(USERS.FIRST_NAME, user.getFirstName())
                    .set(USERS.LAST_NAME, user.getLastName())
                    .set(USERS.UPDATED_AT, now)
                    .where(USERS.ID.eq(user.getId()))
                    .execute();

            user.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        }

        return user;
    }

    @Override
    public void deleteById(UUID id) {
        dsl.deleteFrom(USERS)
                .where(USERS.ID.eq(id))
                .execute();
    }

    @Override
    public Optional<User> findByKeycloakId(String keycloakId) {
        return dsl.selectFrom(USERS)
                .where(USERS.KEYCLOAK_ID.eq(keycloakId))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return dsl.selectFrom(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return dsl.fetchExists(
                dsl.selectFrom(USERS)
                        .where(USERS.EMAIL.eq(email))
        );
    }
}
