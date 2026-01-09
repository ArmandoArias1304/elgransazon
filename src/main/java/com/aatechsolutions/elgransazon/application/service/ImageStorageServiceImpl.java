package com.aatechsolutions.elgransazon.application.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ImageStorageServiceImpl implements ImageStorageService {
    
    @Value("${file.upload.base-path:src/main/resources/static}")
    private String basePath;
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    
    @Override
    public String saveImage(MultipartFile file, String folder, String fileName) throws Exception {
        if (!isValidImage(file)) {
            throw new IllegalArgumentException("Archivo de imagen inválido");
        }
        
        // Crear directorio si no existe
        String uploadDir = basePath + "/uploads/" + folder;
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generar nombre de archivo basado en el nombre del producto
        String cleanFileName = cleanFileName(fileName);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String finalFileName = cleanFileName + "_" + timestamp + ".webp";
        String filePath = uploadDir + "/" + finalFileName;
        
        // Leer imagen original
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IllegalArgumentException("No se pudo leer la imagen");
        }
        
        // Convertir y guardar como WEBP con compresión
        Thumbnails.of(originalImage)
                .scale(1.0) // Mantener tamaño original
                .outputFormat("webp")
                .outputQuality(0.8) // 80% de calidad
                .toFile(filePath);
        
        // Retornar ruta relativa
        return "/uploads/" + folder + "/" + finalFileName;
    }
    
    /**
     * Limpia el nombre del archivo eliminando caracteres especiales
     * y reemplazando espacios por guiones
     */
    private String cleanFileName(String name) {
        if (name == null || name.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        
        // Convertir a minúsculas y eliminar acentos
        String cleaned = name.toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
                .replaceAll("[^a-z0-9\\s-]", "") // Solo letras, números, espacios y guiones
                .replaceAll("\\s+", "-") // Reemplazar espacios por guiones
                .replaceAll("-+", "-") // Eliminar guiones múltiples
                .trim();
        
        // Si después de limpiar queda vacío, usar UUID
        if (cleaned.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        
        // Limitar longitud a 50 caracteres
        if (cleaned.length() > 50) {
            cleaned = cleaned.substring(0, 50);
        }
        
        return cleaned;
    }
    
    @Override
    public void deleteImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return;
        }
        
        try {
            // La ruta viene como "/uploads/menu-items/xxx.webp"
            Path filePath = Paths.get(basePath + imagePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but don't throw exception
            System.err.println("Error al eliminar imagen: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        // Validar tamaño
        if (file.getSize() > MAX_FILE_SIZE) {
            return false;
        }
        
        // Validar tipo MIME
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            return false;
        }
        
        // Validar extensión
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }
        
        String extension = getFileExtension(originalFilename);
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
}
