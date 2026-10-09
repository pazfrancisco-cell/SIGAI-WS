package ar.edu.wellspring.sigaiws.common.storage;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Almacenamiento de archivos en el filesystem del servidor (seccion 3.2:
 * "Documentos e imagenes en almacenamiento de servidor; datos estructurados
 * en BD"). Seccion 7.2: "nombre de archivo aleatorio en disco (no predecible
 * por enumeracion)"; el nombre original se conserva solo en la base de datos
 * (DocumentoAlumno.nombreArchivo) para mostrarlo al usuario.
 */
@Service
public class AlmacenamientoService {

    @Value("${sigai.archivos.storage-path:./storage}")
    private String storagePath;

    public record ArchivoGuardado(String pathRelativo, String nombreOriginal, long tamanoBytes) {
    }

    public ArchivoGuardado guardar(MultipartFile archivo, String subcarpeta, long maxSizeMb) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("El archivo esta vacio.");
        }
        long maxBytes = maxSizeMb * 1024 * 1024;
        if (archivo.getSize() > maxBytes) {
            throw new BusinessException("El archivo supera el tamaño maximo permitido (" + maxSizeMb + "MB).");
        }
        try {
            String extension = extraerExtension(archivo.getOriginalFilename());
            String nombreAleatorio = UUID.randomUUID() + extension;

            Path directorio = Path.of(storagePath, subcarpeta);
            Files.createDirectories(directorio);

            Path destino = directorio.resolve(nombreAleatorio);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            String pathRelativo = subcarpeta + "/" + nombreAleatorio;
            return new ArchivoGuardado(pathRelativo, archivo.getOriginalFilename(), archivo.getSize());
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar el archivo: " + e.getMessage());
        }
    }

    /** Persiste contenido generado por el propio sistema (PDFs de pase, boletines, etc.), no subido por el usuario. */
    public ArchivoGuardado guardarBytes(byte[] contenido, String subcarpeta, String nombreLegible, String extension) {
        try {
            String nombreAleatorio = UUID.randomUUID() + extension;
            Path directorio = Path.of(storagePath, subcarpeta);
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(nombreAleatorio);
            Files.write(destino, contenido);
            return new ArchivoGuardado(subcarpeta + "/" + nombreAleatorio, nombreLegible, contenido.length);
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar el archivo generado: " + e.getMessage());
        }
    }

    public void eliminar(String pathRelativo) {
        try {
            Files.deleteIfExists(Path.of(storagePath, pathRelativo));
        } catch (IOException e) {
            // No se relanza: un archivo huerfano en disco no debe hacer fallar la operacion de negocio.
        }
    }

    private String extraerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "";
        }
        return nombreOriginal.substring(nombreOriginal.lastIndexOf('.'));
    }
}
