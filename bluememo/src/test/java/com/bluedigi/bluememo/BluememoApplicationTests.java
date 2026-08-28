package com.bluedigi.bluememo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.bluedigi.bluememo.testsupport.IntegrationTest;

@SpringBootTest
@ActiveProfiles("test")
@IntegrationTest
class BluememoApplicationTests {

	@Test
	void contextLoads() {
	}

}
