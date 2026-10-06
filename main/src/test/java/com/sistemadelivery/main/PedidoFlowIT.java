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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración del ciclo de vida completo de un pedido
 * (MockMvc + PostgreSQL real):
 *
 * 1. ADMIN crea comercio y producto.
 * 2. CLIENTE crea pedido → total calculado en servidor (Q71.00 = 2 x 25.50 + envío Q20.00)
 *    y stock descontado.
 * 3. Cancelación → stock devuelto una única vez.
 * 4. ADMIN aprueba EN_PREPARACION → REPARTIDOR acepta y avanza EN_CAMINO → ENTREGADO.
 * 5. Otro cliente recibe 403 al consultar el pedido ajeno.
 * 6. Stock insuficiente → 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PedidoFlowIT {

    private static final String EMAIL_ADMIN = "admin@test.local";
    private static final String PASSWORD_ADMIN = "Admin#12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(Object cuerpo) throws Exception {
        return objectMapper.writeValueAsString(cuerpo);
    }

    private String loginYToken(String email, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    private long crearComercio(String tokenAdmin) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/comercios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombre", "Pollería Central IT",
                                "categoria", "RESTAURANTE",
                                "direccion", "Zona 1, Guatemala"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private long crearProducto(String tokenAdmin, long comercioId) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/comercios/" + comercioId + "/productos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombre", "Pollo 1/4 con papas",
                                "descripcion", "Porción individual",
                                "precio", new BigDecimal("25.50"),
                                "stock", 10))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private int stockActual(long productoId) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/productos/" + productoId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("stock").asInt();
    }

    @Test
    void cicloDeVidaCompletoDelPedido() throws Exception {
        /* ---------- ADMIN: comercio + producto ---------- */
        String tokenAdmin = loginYToken(EMAIL_ADMIN, PASSWORD_ADMIN);
        long comercioId = crearComercio(tokenAdmin);
        long productoId = crearProducto(tokenAdmin, comercioId);
        assertThat(stockActual(productoId)).isEqualTo(10);

        /* ---------- CLIENTE registra y crea pedido ---------- */
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombre", "Cliente del Flujo",
                                "email", "cliente.flow@test.com",
                                "password", "Clave#123",
                                "direccion", "Zona 10"))))
                .andExpect(status().isCreated());
        String tokenCliente = loginYToken("cliente.flow@test.com", "Clave#123");

        MvcResult creacion = mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "comercioId", comercioId,
                                "productos", List.of(Map.of("productoId", productoId, "cantidad", 2))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.costoEnvio").value(20.00))
                .andExpect(jsonPath("$.montoTotal").value(71.00))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(2))
                .andExpect(jsonPath("$.detalles[0].precioUnitario").value(25.50))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(51.00))
                .andReturn();
        long pedidoId = objectMapper.readTree(creacion.getResponse().getContentAsString()).get("id").asLong();

        // El stock se descontó (10 - 2 = 8).
        assertThat(stockActual(productoId)).isEqualTo(8);

        // Un segundo cliente NO puede ver el pedido del primero (403).
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombre", "Cliente Curioso",
                                "email", "curioso.flow@test.com",
                                "password", "Clave#123"))))
                .andExpect(status().isCreated());
        String tokenAjeno = loginYToken("curioso.flow@test.com", "Clave#123");
        mockMvc.perform(get("/api/v1/pedidos/" + pedidoId)
                        .header("Authorization", "Bearer " + tokenAjeno))
                .andExpect(status().isForbidden());

        /* ---------- Cancelación del pedido PENDIENTE: devolución de stock ---------- */
        mockMvc.perform(patch("/api/v1/pedidos/" + pedidoId + "/cancelar")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
        assertThat(stockActual(productoId)).isEqualTo(10); // 8 + 2, una sola vez

        // Una segunda cancelación del mismo pedido → 409 (el stock ya se devolvió).
        mockMvc.perform(patch("/api/v1/pedidos/" + pedidoId + "/cancelar")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CANCELACION_NO_PERMITIDA"));
        assertThat(stockActual(productoId)).isEqualTo(10); // sin doble devolución

        /* ---------- Segundo pedido: ciclo ADMIN → REPARTIDOR → ENTREGADO ---------- */
        MvcResult segundo = mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "comercioId", comercioId,
                                "productos", List.of(Map.of("productoId", productoId, "cantidad", 2))))))
                .andExpect(status().isCreated())
                .andReturn();
        long pedido2 = objectMapper.readTree(segundo.getResponse().getContentAsString()).get("id").asLong();

        // ADMIN: PENDIENTE → EN_PREPARACION.
        mockMvc.perform(patch("/api/v1/pedidos/" + pedido2 + "/estado")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("estado", "EN_PREPARACION"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"));

        // ADMIN crea un repartidor y el repartidor inicia sesión.
        mockMvc.perform(post("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombre", "Repartidor Uno",
                                "email", "repartidor.flow@test.com",
                                "password", "Repa#12345",
                                "rol", "REPARTIDOR"))))
                .andExpect(status().isCreated());
        String tokenRepartidor = loginYToken("repartidor.flow@test.com", "Repa#12345");

        // El pedido preparado aparece en el tablero de pedidos disponibles.
        mockMvc.perform(get("/api/v1/repartidores/pedidos-disponibles")
                        .header("Authorization", "Bearer " + tokenRepartidor))
                .andExpect(status().isOk());

        // Asignación atómica.
        mockMvc.perform(post("/api/v1/repartidores/pedidos/" + pedido2 + "/aceptar")
                        .header("Authorization", "Bearer " + tokenRepartidor))
                .andExpect(status().isOk());

        // Otro repartidor NO puede aceptarlo de nuevo → 409.
        mockMvc.perform(post("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombre", "Repartidor Dos",
                                "email", "repa2.flow@test.com",
                                "password", "Repa#12345",
                                "rol", "REPARTIDOR"))))
                .andExpect(status().isCreated());
        String tokenRepartidor2 = loginYToken("repa2.flow@test.com", "Repa#12345");
        mockMvc.perform(post("/api/v1/repartidores/pedidos/" + pedido2 + "/aceptar")
                        .header("Authorization", "Bearer " + tokenRepartidor2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_ASIGNADO"));

        // EN_PREPARACION → EN_CAMINO → ENTREGADO.
        mockMvc.perform(patch("/api/v1/repartidores/pedidos/" + pedido2 + "/estado")
                        .header("Authorization", "Bearer " + tokenRepartidor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("estado", "EN_CAMINO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CAMINO"));

        mockMvc.perform(patch("/api/v1/repartidores/pedidos/" + pedido2 + "/estado")
                        .header("Authorization", "Bearer " + tokenRepartidor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("estado", "ENTREGADO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));

        // Historial completo: creación (PENDIENTE), EN_PREPARACION, EN_CAMINO, ENTREGADO.
        mockMvc.perform(get("/api/v1/pedidos/" + pedido2 + "/historial")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[1].estado").value("EN_PREPARACION"))
                .andExpect(jsonPath("$[2].estado").value("EN_CAMINO"))
                .andExpect(jsonPath("$[3].estado").value("ENTREGADO"));

        // Un pedido ENTREGADO ya no se puede modificar → la transición siguiente falla.
        mockMvc.perform(patch("/api/v1/repartidores/pedidos/" + pedido2 + "/estado")
                        .header("Authorization", "Bearer " + tokenRepartidor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("estado", "EN_CAMINO"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
    }

    @Test
    void pedidoConStockInsuficienteDevuelve409() throws Exception {
        String tokenAdmin = loginYToken(EMAIL_ADMIN, PASSWORD_ADMIN);
        long comercioId = crearComercio(tokenAdmin);
        long productoId = crearProducto(tokenAdmin, comercioId);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nombre", "Cliente Sin Suerte",
                                "email", "sinlucky.flow@test.com",
                                "password", "Clave#123"))))
                .andExpect(status().isCreated());
        String tokenCliente = loginYToken("sinlucky.flow@test.com", "Clave#123");

        mockMvc.perform(post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "comercioId", comercioId,
                                "productos", List.of(Map.of("productoId", productoId, "cantidad", 99))))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));

        // El stock no cambió.
        assertThat(stockActual(productoId)).isEqualTo(10);
    }
}