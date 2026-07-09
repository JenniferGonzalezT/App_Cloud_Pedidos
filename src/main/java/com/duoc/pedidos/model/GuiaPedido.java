package com.duoc.pedidos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "GUIA_PROCESADA")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuiaPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codigoPedidoOriginal;

    @Column(nullable = false)
    private String transportista;

    // Estado para saber que pasó por la cola exitosamente
    @Column(nullable = false)
    private String estadoGuia;

    // Fecha en que RabbitMQ procesó la guía
    @Column(nullable = false)
    private LocalDateTime fechaProcesamiento;

    @PrePersist
    protected void onCreate() {
        if (this.fechaProcesamiento == null) {
            this.fechaProcesamiento = LocalDateTime.now();
        }
    }
}
