package com.duoc.pedidos.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Definición de nombres estáticos para evitar errores de tipeo
    public static final String COLA_PRINCIPAL = "guia.despacho.cola1";
    public static final String COLA_ERRORES = "guia.despacho.cola2";
    public static final String EXCHANGE = "pedidos.exchange";
    public static final String ROUTING_KEY = "rutear.guia";

    // 1. Configuración de la Cola 2 (Cola de errores / Dead Letter Queue)
    @Bean
    public Queue colaErrores() {
        return new Queue(COLA_ERRORES, true);
    }

    // 2. Configuración de la Cola 1 (Principal)
    // Vinculamos la Cola 1 con la Cola 2 para el manejo de errores.
    @Bean
    public Queue colaPrincipal() {
        return QueueBuilder.durable(COLA_PRINCIPAL)
                .withArgument("x-dead-letter-exchange", "") // Usa el exchange por defecto para el ruteo de errores
                .withArgument("x-dead-letter-routing-key", COLA_ERRORES) // Si el mensaje falla, se redirige a la Cola 2
                .build();
    }

    // 3. Creación del Intercambiador (Exchange) tipo Direct
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    // 4. Binding: Une el Exchange con la Cola 1 usando la Routing Key
    @Bean
    public Binding bindingPrincipal(Queue colaPrincipal, DirectExchange exchange) {
        return BindingBuilder.bind(colaPrincipal).to(exchange).with(ROUTING_KEY);
    }

    // Conversor para enviar objetos Java como JSON
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Inyectamos el conversor al Template de RabbitMQ
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
