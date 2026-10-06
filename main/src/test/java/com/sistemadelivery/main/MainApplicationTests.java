package com.sistemadelivery.main;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test del contexto: verifica que la aplicación arranca con Flyway
 * (esquema válido para ddl-auto: validate) contra la base de pruebas.
 */
@SpringBootTest
@ActiveProfiles("test")
class MainApplicationTests {

	@Test
	void contextLoads() {
	}
}