package com.sistemadelivery.main.mapper;

import com.sistemadelivery.main.dto.response.AuditoriaResponse;
import com.sistemadelivery.main.entity.RegistroAuditoria;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaMapper {

    public AuditoriaResponse aRespuesta(RegistroAuditoria registro) {
        return new AuditoriaResponse(
                registro.getId(),
                registro.getUsuario(),
                registro.getAccion(),
                registro.getEntidad(),
                registro.getEntidadId(),
                registro.getDetalles(),
                registro.getFecha());
    }
}
