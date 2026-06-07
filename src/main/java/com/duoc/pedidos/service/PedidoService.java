package com.duoc.pedidos.service;

import com.duoc.pedidos.dto.PedidoRequestDTO;
import com.duoc.pedidos.dto.PedidoResponseDTO;
import com.duoc.pedidos.model.Pedido;
import com.duoc.pedidos.repository.PedidoRepository;
import com.duoc.pedidos.repository.S3Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final S3Repository s3Repository;
    private final String efsPath;

    // Inyectamos los repositorios y la propiedad del volumen de EFS
    public PedidoService(PedidoRepository pedidoRepository, 
                         S3Repository s3Repository, 
                         @Value("${app.efs.path}") String efsPath) {
        this.pedidoRepository = pedidoRepository;
        this.s3Repository = s3Repository;
        this.efsPath = efsPath;
    }

    /**
     * Requerimiento: Crear pedido, guardar en EFS temporal y subir automáticamente a S3
     */
    public PedidoResponseDTO crearPedido(PedidoRequestDTO request) {
        // 1. Guardar metadatos base en H2 de memoria
        Pedido pedido = new Pedido();
        pedido.setCodigoPedido(request.getCodigoPedido());
        pedido.setTransportista(request.getTransportista());
        pedido.setDetallePedido(request.getDetallePedido());
        pedido.setDestinatario(request.getDestinatario());
        pedido.setMontoTotal(request.getMontoTotal());
        pedido.setFechaCreacion(LocalDateTime.now());

        // 2. [REQUERIMIENTO EFS]: Generar y guardar guía temporal en volumen compartido
        File archivoGuia = generarArchivoGuiaTemporal(pedido);

        // 3. [REQUERIMIENTO S3]: Construir la ruta dinámica estructurada por fecha y transportista
        // Formato solicitado: /AñoMes/nombreTransportista/archivo.txt (Ejemplo: /202606/transportistaX/CP-10023.txt)
        String anioMes = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String s3Key = anioMes + "/" + pedido.getTransportista() + "/" + pedido.getCodigoPedido() + ".txt";

        // Subir archivo real a S3
        s3Repository.subirArchivo(s3Key, archivoGuia);

        // 4. Actualizar la entidad con la ruta de S3 obtenida y persistir definitivamente
        pedido.setS3Url(s3Key);
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        // 5. [DEMOSTRACIÓN DE TEMPORALIDAD EN EFS]:
        // Dejamos la lógica de borrado comentada intencionalmente.
        // Esto permite comprobar que el archivo se genera con éxito en el EFS,
        // garantizando la persistencia intermedia requerida antes de ser enviado a S3.
        /*
        if (archivoGuia.exists()) {
            boolean eliminado = archivoGuia.delete();
            if (eliminado) {
                System.out.println("Archivo temporal eliminado con éxito del volumen EFS.");
            }
        }
        */

        return convertirADTO(pedidoGuardado);
    }

    /**
     * Requerimiento: Descargar archivo físico desde S3
     */
    public byte[] descargarGuia(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El pedido con ID " + id + " no existe."));
        
        if (pedido.getS3Url() == null || pedido.getS3Url().isEmpty()) {
            throw new RuntimeException("El pedido no cuenta con una guía de despacho asociada en S3.");
        }

        return s3Repository.descargarArchivo(pedido.getS3Url());
    }

    /**
     * Requerimiento: Modificar o Actualizar guía existente (Base de Datos + Reemplazo en S3)
     */
    public PedidoResponseDTO actualizarPedido(Long id, PedidoRequestDTO request) {
        Pedido pedidoExistente = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el pedido para actualizar."));

        // Modificamos campos permitidos
        pedidoExistente.setDetallePedido(request.getDetallePedido());
        pedidoExistente.setDestinatario(request.getDestinatario());
        pedidoExistente.setMontoTotal(request.getMontoTotal());

        // Regeneramos el documento con la nueva información sobre el volumen EFS
        File archivoModificado = generarArchivoGuiaTemporal(pedidoExistente);

        // Mandamos a S3 en la misma Key original para que se actualice/sobrescriba por completo
        s3Repository.subirArchivo(pedidoExistente.getS3Url(), archivoModificado);

        // Guardamos los cambios en H2
        Pedido pedidoActualizado = pedidoRepository.save(pedidoExistente);
        return convertirADTO(pedidoActualizado);
    }

    /**
     * Requerimiento: Eliminar de base de datos y borrar archivo específico en S3
     */
    public void eliminarPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El pedido a eliminar no existe."));

        // Eliminar físicamente de la nube de Amazon S3
        if (pedido.getS3Url() != null && !pedido.getS3Url().isEmpty()) {
            s3Repository.eliminarArchivo(pedido.getS3Url());
        }

        // Eliminar el registro lógico en memoria H2
        pedidoRepository.deleteById(id);
    }

    /**
     * Requerimiento: Consultar historial filtrando dinámicamente por Transportista y/o Fecha
     */
    public List<PedidoResponseDTO> consultarHistorial(String transportista, String fechaStr) {
        List<Pedido> pedidos;

        if (transportista != null && !transportista.isEmpty() && fechaStr != null && !fechaStr.isEmpty()) {
            // Filtrar por ambos parámetros
            LocalDateTime inicioDia = LocalDate.parse(fechaStr).atStartOfDay();
            LocalDateTime finDia = LocalDate.parse(fechaStr).atTime(23, 59, 59);
            pedidos = pedidoRepository.findByTransportistaIgnoreCaseAndFechaCreacionBetween(transportista, inicioDia, finDia);
        } else if (transportista != null && !transportista.isEmpty()) {
            // Filtrar solo por transportista
            pedidos = pedidoRepository.findByTransportistaIgnoreCase(transportista);
        } else if (fechaStr != null && !fechaStr.isEmpty()) {
            // Filtrar solo por fecha específica
            LocalDateTime inicioDia = LocalDate.parse(fechaStr).atStartOfDay();
            LocalDateTime finDia = LocalDate.parse(fechaStr).atTime(23, 59, 59);
            pedidos = pedidoRepository.findByFechaCreacionBetween(inicioDia, finDia);
        } else {
            // Sin filtros, trae todo el historial
            pedidos = pedidoRepository.findAll();
        }

        return pedidos.stream().map(this::convertirADTO).collect(Collectors.toList());
    }

    // ===================================================================
    // MÉTODOS DE APOYO INTERNOS (Manejo de archivos EFS y mapeo DTO)
    // ===================================================================
    
    private File generarArchivoGuiaTemporal(Pedido pedido) {
        try {
            // Asegurar que la raíz del directorio EFS local o compartido exista físicamente
            Path directorioEfs = Paths.get(efsPath);
            if (!Files.exists(directorioEfs)) {
                Files.createDirectories(directorioEfs);
            }

            // Definir la ruta física del archivo temporal dentro del EFS
            String nombreArchivo = "GUIA_" + pedido.getCodigoPedido() + ".txt";
            File archivoTemporal = new File(directorioEfs.toFile(), nombreArchivo);

            // Escribir el contenido estructurado del documento de despacho
            try (FileWriter writer = new FileWriter(archivoTemporal)) {
                writer.write("====================================================\n");
                writer.write("          GUÍA DE DESPACHO ELECTRÓNICA              \n");
                writer.write("====================================================\n");
                writer.write("Código Pedido  : " + pedido.getCodigoPedido() + "\n");
                writer.write("Transportista  : " + pedido.getTransportista() + "\n");
                writer.write("Destinatario   : " + pedido.getDestinatario() + "\n");
                writer.write("Fecha Emisión  : " + pedido.getFechaCreacion() + "\n");
                writer.write("----------------------------------------------------\n");
                writer.write("DETALLE MERCANCÍA:\n");
                writer.write(pedido.getDetallePedido() + "\n");
                writer.write("----------------------------------------------------\n");
                writer.write("VALOR TOTAL NETO: $" + pedido.getMontoTotal() + "\n");
                writer.write("====================================================\n");
            }

            return archivoTemporal;
        } catch (IOException e) {
            throw new RuntimeException("Falla crítica al escribir en el almacenamiento compartido EFS: " + e.getMessage(), e);
        }
    }

    private PedidoResponseDTO convertirADTO(Pedido pedido) {
        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getCodigoPedido(),
                pedido.getTransportista(),
                pedido.getDetallePedido(),
                pedido.getDestinatario(),
                pedido.getMontoTotal(),
                pedido.getFechaCreacion(),
                pedido.getS3Url()
        );
    }
}
