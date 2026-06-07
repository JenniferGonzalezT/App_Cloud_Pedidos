package com.duoc.pedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoResponseDTO {

    private Long id;
    private String codigoPedido;
    private String transportista;
    private String detallePedido;
    private String destinatario;
    private Double montoTotal;
    private LocalDateTime fechaCreacion;
    private String s3Url; // Ruta del archivo en AWS S3 para su posterior descarga/consulta
}
