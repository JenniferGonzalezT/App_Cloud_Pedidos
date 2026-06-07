package com.duoc.pedidos.controller;

import com.duoc.pedidos.dto.PedidoRequestDTO;
import com.duoc.pedidos.dto.PedidoResponseDTO;
import com.duoc.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    // Inyección de dependencias por constructor
    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /**
     * Requerimiento: Crear un pedido, generar la guía en EFS y subirla a S3.
     * Método: POST sobre /api/pedidos
     */
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> crearPedido(@Valid @RequestBody PedidoRequestDTO request) {
        PedidoResponseDTO nuevoPedido = pedidoService.crearPedido(request);
        return new ResponseEntity<>(nuevoPedido, HttpStatus.CREATED);
    }

    /**
     * Requerimiento: Consultar el historial filtrando opcionalmente por transportista y/o fecha.
     * Método: GET sobre /api/pedidos
     * Ejemplos: 
     * - /api/pedidos (Trae todo)
     * - /api/pedidos?transportista=varmontt
     * - /api/pedidos?fecha=2026-06-07
     * - /api/pedidos?transportista=varmontt&fecha=2026-06-07
     */
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> consultarHistorial(
            @RequestParam(required = false) String transportista,
            @RequestParam(required = false) String fecha) {
        
        List<PedidoResponseDTO> historial = pedidoService.consultarHistorial(transportista, fecha);
        return ResponseEntity.ok(historial);
    }

    /**
     * Requerimiento: Descargar el archivo físico de la guía directo desde S3.
     * Método: GET sobre /api/pedidos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> descargarGuia(@PathVariable Long id) {
        byte[] archivoBytes = pedidoService.descargarGuia(id);

        // Configuramos las cabeceras HTTP para forzar la descarga de un archivo de texto en Postman/Navegador
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "guia_despacho_" + id + ".txt");

        return new ResponseEntity<>(archivoBytes, headers, HttpStatus.OK);
    }

    /**
     * Requerimiento: Modificar o actualizar los datos del pedido y regenerar el archivo en S3.
     * Método: PUT sobre /api/pedidos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> actualizarPedido(
            @PathVariable Long id, 
            @Valid @RequestBody PedidoRequestDTO request) {
        
        PedidoResponseDTO pedidoActualizado = pedidoService.actualizarPedido(id, request);
        return ResponseEntity.ok(pedidoActualizado);
    }

    /**
     * Requerimiento: Eliminar de base de datos y borrar el archivo en S3 de forma permanente.
     * Método: DELETE sobre /api/pedidos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPedido(@PathVariable Long id) {
        pedidoService.eliminarPedido(id);
        return ResponseEntity.noContent().build(); // Retorna un código 204 No Content (Éxito estándar para eliminaciones)
    }
}
