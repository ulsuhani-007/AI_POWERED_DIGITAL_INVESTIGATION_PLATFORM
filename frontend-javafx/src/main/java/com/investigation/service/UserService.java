package com.investigation.service;

import com.investigation.dto.CreateUserDto;
import com.investigation.dto.UserDto;

import java.util.List;

public interface UserService {
    List<UserDto> getAllUsers() throws Exception;
    UserDto createUser(CreateUserDto request) throws Exception;
    void deleteUser(String id) throws Exception;
}

