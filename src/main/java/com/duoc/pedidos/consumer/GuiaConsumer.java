package com.duoc.pedidos.consumer;

import com.duoc.pedidos.config.RabbitMQConfig;
import com.duoc.pedidos.model.GuiaPedido;
import com.duoc.pedidos.model.Pedido;
import com.duoc.pedidos.repository.GuiaPedidoRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class GuiaConsumer {

    private final GuiaPedidoRepository guiaPedidoRepository;

    public GuiaConsumer(GuiaPedidoRepository guiaPedidoRepository) {
        this.guiaPedidoRepository = guiaPedidoRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.COLA_PRINCIPAL)
    public void consumirGuia(Pedido pedido) throws Exception {
        System.out.println("====== CONSUMER: Mensaje recibido desde la Cola 1 ======");
        System.out.println("Procesando guía para el pedido: " + pedido.getCodigoPedido());

        try {

            // --- CÓDIGO DE PRUEBA PARA DEMOSTRAR DLQ ---
            if (pedido.getCodigoPedido().contains("ERROR")) {
                throw new RuntimeException("Fallo inducido para demostrar el enrutamiento a la Cola 2");
            }
            // -------------------------------------------

            // Transformar el Pedido a GuiaPedido y guardar en la nueva tabla
            GuiaPedido nuevaGuia = new GuiaPedido();
            nuevaGuia.setCodigoPedidoOriginal(pedido.getCodigoPedido());
            nuevaGuia.setTransportista(pedido.getTransportista());
            nuevaGuia.setEstadoGuia("PROCESADA_EXITOSAMENTE");

            guiaPedidoRepository.save(nuevaGuia);

            System.out.println("====== CONSUMER: Guía guardada exitosamente en la tabla GUIA_PROCESADA ======");

        } catch (Exception e) {
            System.err.println("ERROR procesando la guía: " + e.getMessage());
            System.err.println("Reenviando mensaje a la Cola de Errores (Cola 2)...");
            // Lanzar la excepción hace que RabbitMQ aplique el 
            // Dead Letter Exchange y mueva el mensaje a la Cola 2.
            throw e; 
        }
    }
}
