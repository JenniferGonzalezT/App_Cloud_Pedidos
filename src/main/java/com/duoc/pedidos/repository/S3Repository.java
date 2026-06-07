package com.duoc.pedidos.repository;

import java.io.File;
import java.util.List;

public interface S3Repository {

    /**
     * Sube o modifica un archivo en el bucket de S3.
     * En S3, la operación de subir y modificar usa el mismo método (putObject).
     * Si la ruta (key) ya existe, se reemplaza el contenido automáticamente.
     */
    String subirArchivo(String key, File archivo);

    /**
     * Descarga los bytes de un archivo específico desde S3.
     */
    byte[] descargarArchivo(String key);

    /**
     * Elimina un archivo específico del bucket S3.
     */
    void eliminarArchivo(String key);

    /**
     * Lista las rutas (keys) de los archivos dentro de un prefijo/carpeta específica.
     * Útil para complementar la consulta por transportista o filtros.
     */
    List<String> listarArchivosPorPrefijo(String prefijo);
}
