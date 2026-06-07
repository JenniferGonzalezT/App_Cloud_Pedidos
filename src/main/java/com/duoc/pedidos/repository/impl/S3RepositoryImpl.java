package com.duoc.pedidos.repository.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.duoc.pedidos.repository.S3Repository;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class S3RepositoryImpl implements S3Repository {

    private final S3Client s3Client;
    private final String bucketName;

    // El constructor inyecta automáticamente el bean de S3 y el nombre del bucket
    public S3RepositoryImpl(S3Client s3Client, @Value("${app.aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public String subirArchivo(String key, File archivo) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Subimos el archivo físico
            s3Client.putObject(putObjectRequest, RequestBody.fromFile(archivo));
            
            // Retornamos la clave (key) que sirve como identificador único en S3
            return key;
        } catch (S3Exception e) {
            throw new RuntimeException("Error al subir/modificar archivo en AWS S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public byte[] descargarArchivo(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Descargamos el archivo como un arreglo de bytes para transferirlo de forma segura por la API
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            
            return objectBytes.asByteArray();
        } catch (S3Exception e) {
            throw new RuntimeException("Error al descargar archivo desde AWS S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public void eliminarArchivo(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            throw new RuntimeException("Error al eliminar archivo en AWS S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public List<String> listarArchivosPorPrefijo(String prefijo) {
        try {
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefijo) // Ejemplo: "2026/transportistaX/"
                    .build();

            ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
            
            // Mapeamos la lista de objetos de S3 a un List<String> con sus rutas (Keys)
            return listObjectsV2Response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (S3Exception e) {
            throw new RuntimeException("Error al listar archivos en AWS S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
}
