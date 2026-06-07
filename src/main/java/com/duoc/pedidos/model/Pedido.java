package com.duoc.pedidos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "PEDIDO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código identificador único del pedido (ej: CP-10023)
    @Column(nullable = false, unique = true)
    private String codigoPedido;

    // Nombre o identificador del transportista (ej: transportistaX)
    @Column(nullable = false)
    private String transportista;

    // Detalles del pedido o mercancía transportada
    @Column(nullable = false)
    private String detallePedido;

    // Destinatario de la carga
    @Column(nullable = false)
    private String destinatario;

    // Monto total asociado (opcional)
    private Double montoTotal;

    // Fecha de registro del pedido
    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    // Almacena la URL o ruta del objeto dentro del Bucket S3 (ej: /2026/transportistaX/pedido123.txt)
    @Column(length = 500)
    private String s3Url;

    /**
     * Ciclo de vida JPA: Antes de insertar el registro en H2, 
     * asignamos automáticamente la fecha y hora actual si viene vacía.
     */
    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }
}
