package com.ldg.mcadmin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "docker.host=tcp://localhost:2375"
})
class McAdminApplicationTest {

    @Test
    void contextLoads() {
    }
}
