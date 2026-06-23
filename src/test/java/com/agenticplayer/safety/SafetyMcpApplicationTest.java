package com.agenticplayer.safety;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "safety.cpsc.base-url=http://localhost:65534")
class SafetyMcpApplicationTest {

    @Test
    void contextLoads() {
    }
}
