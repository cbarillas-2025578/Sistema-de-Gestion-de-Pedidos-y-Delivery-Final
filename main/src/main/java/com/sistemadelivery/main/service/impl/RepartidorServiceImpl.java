package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.dto.request.EstadoPedidoRequest;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.dto.response.PedidoResponse;
import com.sistemadelivery.main.entity.Pedido;
import com.sistemadelivery.main.entity.Usuario;
import com.sistemadelivery.main.entity.enums.EstadoPedido;
import com.sistemadelivery.main.entity.enums.Rol;
import com.sistemadelivery.main.exception.ConflictException;
import com.sistemadelivery.main.exception.ForbiddenException;
import com.sistemadelivery.main.exception.ResourceNotFoundException;
import com.sistemadelivery.main.exception.UnauthorizedException;
import com.sistemadelivery.main.mapper.PedidoMapper;
import com.sistemadelivery.main.repository.PedidoRepository;
import com.sistemadelivery.main.repository.UsuarioRepository;
import com.sistemadelivery.main.service.AuditoriaService;
import com.sistemadelivery.main.service.PedidoEstadoService;
import com.sistemadelivery.main.service.RepartidorService;
import com.sistemadelivery.main.util.SeguridadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepartidorServiceImpl implements RepartidorService {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoMapper mapper;
    private final PedidoEstadoService pedidoEstadoService;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> pedidosDisponibles(int page, int size) {
        Page<Pedido> pedidos = pedidoRepository.findByRepartidorNullAndEstado(
                EstadoPedido.EN_PREPARACION,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "fechaPedido")));
        return PageResponse.de(pedidos.map(mapper::aRespuesta));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> misPedidos(int page, int size) {
        Usuario repartidor = repartidorAutenticado();
        Page<Pedido> pedidos = pedidoRepository.findByRepartidorId(repartidor.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaPedido")));
        return PageResponse.de(pedidos.map(mapper::aRespuesta));
    }

    @Override
    @Transactional
    public PedidoResponse aceptar(Long pedidoId) {
        String email = SeguridadUtil.emailActual();
        Usuario repartidor = repartidorAutenticado();
        if (!Boolean.TRUE.equals(repartidor.getActivo())) {
            throw new ForbiddenException("El repartidor está desactivado");
        }

        // SELECT ... FOR UPDATE: dos repartidores simultáneos se serializan sobre la
        // misma fila; el segundo observa repartidor != null y recibe 409.
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> ResourceNotFoundException.de("Pedido", pedidoId));

        if (pedido.getRepartidor() != null) {
            throw new ConflictException("PEDIDO_ASIGNADO", "El pedido ya tiene un repartidor asignado");
        }
        if (pedido.getEstado() != EstadoPedido.EN_PREPARACION) {
            throw new ConflictException("PEDIDO_NO_DISPONIBLE",
                    "El pedido no está disponible para entrega; estado actual: " + pedido.getEstado());
        }

        pedido.setRepartidor(repartidor);
        auditoriaService.registrar(email, "PEDIDO_ASIGNACION", "Pedido", pedidoId,
                "Pedido aceptado por el repartidor " + repartidor.getEmail());
        return mapper.aRespuesta(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse actualizarEstado(Long pedidoId, EstadoPedidoRequest request) {
        String email = SeguridadUtil.emailActual();

        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> ResourceNotFoundException.de("Pedido", pedidoId));

        if (pedido.getRepartidor() == null || !pedido.getRepartidor().getEmail().equals(email)) {
            throw new ForbiddenException("El pedido no está asignado a este repartidor");
        }

        EstadoPedido destino = request.estado();
        if (destino != EstadoPedido.EN_CAMINO && destino != EstadoPedido.ENTREGADO) {
            throw new ForbiddenException("Un repartidor solo puede ejecutar EN_CAMINO o ENTREGADO");
        }

        // Valida la máquina de estados y registra historial + auditoría + notificación.
        pedidoEstadoService.aplicar(pedido, destino, email);
        return mapper.aRespuesta(pedido);
    }

    private Usuario repartidorAutenticado() {
        String email = SeguridadUtil.emailActual();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("USUARIO_NO_ENCONTRADO",
                        "El usuario autenticado no existe"));
        if (usuario.getRol() != Rol.REPARTIDOR) {
            throw new ForbiddenException("La operación requiere rol REPARTIDOR");
        }
        return usuario;
    }
}
