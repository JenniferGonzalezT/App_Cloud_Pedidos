package com.duoc.pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRequestDTO {

    @NotBlank(message = "El código del pedido no puede estar vacío.")
    private String codigoPedido;

    @NotBlank(message = "El nombre del transportista es obligatorio.")
    private String transportista;

    @NotBlank(message = "El detalle de la mercancía no puede estar vacío.")
    private String detallePedido;

    @NotBlank(message = "El destinatario de la carga es obligatorio.")
    private String destinatario;

    @NotNull(message = "El monto total no puede ser nulo.")
    @Positive(message = "El monto total debe ser mayor a cero.")
    private Double montoTotal;
}
