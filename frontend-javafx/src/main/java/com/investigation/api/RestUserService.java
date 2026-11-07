package com.investigation.api;

import com.investigation.dto.CreateUserDto;
import com.investigation.dto.UserDto;
import com.investigation.service.UserService;

import java.util.List;

public class RestUserService implements UserService {

    private final ApiClient apiClient;

    public RestUserService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<UserDto> getAllUsers() throws Exception {
        return apiClient.getList("/api/users", UserDto.class);
    }

    @Override
    public UserDto createUser(CreateUserDto request) throws Exception {
        return apiClient.post("/api/users", request, UserDto.class);
    }

    @Override
    public void deleteUser(String id) throws Exception {
        apiClient.delete("/api/users/" + id);
    }
}

