package com.investigation.service.mock;

import com.investigation.dto.CreateUserDto;
import com.investigation.dto.UserDto;
import com.investigation.service.UserService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MockUserService implements UserService {

    private final List<UserDto> users = new ArrayList<>(List.of(
            new UserDto("u-admin", "admin", "ADMIN", true, Instant.now().minusSeconds(86400 * 30)),
            new UserDto("u-investigator", "investigator", "INVESTIGATOR", true, Instant.now().minusSeconds(86400 * 14))
    ));

    @Override
    public synchronized List<UserDto> getAllUsers() {
        return new ArrayList<>(users);
    }

    @Override
    public synchronized UserDto createUser(CreateUserDto request) {
        UserDto u = new UserDto(
                UUID.randomUUID().toString(),
                request.getUsername(),
                request.getRole() != null ? request.getRole() : "INVESTIGATOR",
                request.isEnabled(),
                Instant.now()
        );
        users.add(u);
        return u;
    }

    @Override
    public synchronized void deleteUser(String id) {
        users.removeIf(u -> u.getId().equals(id));
    }
}

