package com.duoc.pedidos.repository;

import com.duoc.pedidos.model.Pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Filtro 1: Buscar pedidos por transportista exacto
    List<Pedido> findByTransportistaIgnoreCase(String transportista);

    // Filtro 2: Buscar pedidos creados en un rango de fechas
    List<Pedido> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);

    // Filtro Combinado: Buscar por transportista Y rango de fechas
    List<Pedido> findByTransportistaIgnoreCaseAndFechaCreacionBetween(
            String transportista, LocalDateTime inicio, LocalDateTime fin);
}
