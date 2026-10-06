package com.sistemadelivery.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración del flujo de autenticación JWT (MockMvc + PostgreSQL real):
 * - registro público crea rol CLIENTE y devuelve token;
 * - login válido/inválido;
 * - 401 sin token, 403 para un cliente en rutas de administración;
 * - 409 para email duplicado; 400 para solicitudes inválidas.
 *
 * La base de destino es sistema_delivery_test (perfil "test"); cada prueba
 * se ejecuta en una transacción que se revierte al terminar (rollback).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(Map<String, Object> cuerpo) throws Exception {
        return objectMapper.writeValueAsString(cuerpo);
    }

    private String loginYToken(String email, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode nodo = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return nodo.get("token").asText();
    }

    @Test
    void registroLoginYTokensFuncionan() throws Exception {
        // Registro público → 201 con token y rol CLIENTE (nunca admin).
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombre", "Ana López",
                                "email", "ana.flow@test.com",
                                "password", "Clave#123",
                                "direccion", "Zona 10",
                                "telefono", "55550102"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.email").value("ana.flow@test.com"));

        // Login → 200 con token.
        String token = loginYToken("ana.flow@test.com", "Clave#123");

        // El endpoint protegido responde 200 con el Bearer.
        mockMvc.perform(get("/api/v1/pedidos/mis-pedidos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").isNumber());
    }

    @Test
    void registroRechazaEmailDuplicadoCon409() throws Exception {
        Map<String, Object> cuerpo = Map.of(
                "nombre", "Ana López",
                "email", "duplicado.flow@test.com",
                "password", "Clave#123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cuerpo)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cuerpo)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_DUPLICADO"));
    }

    @Test
    void loginConContrasenaIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "quien@test.com", "password", "Incorrecta#1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void registroInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nombre", "Sin Contraseña", "email", "invalido@test.com"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void endpointProtegidoRequiereToken() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos/mis-pedidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoAccedeARutasDeAdministracion() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombre", "Cliente Básico",
                                "email", "basico.flow@test.com",
                                "password", "Clave#123"))))
                .andExpect(status().isCreated());

        String token = loginYToken("basico.flow@test.com", "Clave#123");

        // Listado administrativo de pedidos: 403 para CLIENTE.
        mockMvc.perform(get("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // Zona /admin: 403 para CLIENTE.
        mockMvc.perform(get("/api/v1/admin/estadisticas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}