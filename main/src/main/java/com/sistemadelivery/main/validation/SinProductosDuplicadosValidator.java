package com.sistemadelivery.main.validation;

import com.sistemadelivery.main.dto.request.ItemPedidoRequest;
import com.sistemadelivery.main.dto.request.PedidoRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.Set;

public class SinProductosDuplicadosValidator implements ConstraintValidator<SinProductosDuplicados, PedidoRequest> {

    @Override
    public boolean isValid(PedidoRequest value, ConstraintValidatorContext context) {
        // Si no hay productos o son nulos, se encargan @NotEmpty / @Valid.
        if (value == null || value.productos() == null) {
            return true;
        }
        Set<Long> vistos = new HashSet<>();
        for (ItemPedidoRequest item : value.productos()) {
            if (item == null || item.productoId() == null) {
                continue;
            }
            if (!vistos.add(item.productoId())) {
                return false;
            }
        }
        return true;
    }
}
