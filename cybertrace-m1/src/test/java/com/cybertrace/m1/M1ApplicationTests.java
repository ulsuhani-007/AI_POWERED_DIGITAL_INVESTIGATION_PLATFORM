package com.cybertrace.m1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class M1ApplicationTests {

    @Test
    void contextLoads() {
        assertTrue(true, "Application context loaded successfully");
    }
}

