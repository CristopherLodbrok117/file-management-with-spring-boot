package app.file_manager.application.service;

import app.file_manager.application.usecase.FIleService;
import app.file_manager.domain.exception.FileException;
import app.file_manager.domain.model.FileMetadata;
import app.file_manager.domain.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FIleService {

    @Value("${file.storage.location}")
    private String storageLocation;

    @Value("${file.max-size}")
    private long maxFileSize;

    @Value("${file.allowed-types}")
    private HashSet<String> allowedTypes;

    private final FileMetadataRepository fileMetadataRepository;

    @Override
    public FileMetadata saveFile(MultipartFile file) {
        // 1. Hacer validaciones

        validateFile(file);

        String fileLocation = storageLocation + File.separator + file.getOriginalFilename();

        try{
            // 2. Crear directorio si no existe
            Path storagePath = Paths.get(storageLocation);
            if(!Files.exists(storagePath)){
                Files.createDirectories(storagePath);
            }

            // 3. Guardar archivo

            Path filePath = Paths.get(fileLocation);

            Files.copy(file.getInputStream(), filePath);

        }
        catch(IOException ex){

            throw new RuntimeException(ex.getMessage());
        }

        // 4. Guardar metadatos en BD y retornarlos
        System.out.println("4. Guardar metatdata");
        FileMetadata fileMetadata = FileMetadata.builder()
                .originalName(file.getOriginalFilename())
                .path(fileLocation)
                .size(file.getSize())
                .type(file.getContentType())
                .uploadDate(LocalDateTime.now())
                .build();

        return fileMetadataRepository.save(fileMetadata);
    }

    @Override
    public FileMetadata getMetadata(Long id) {

        return fileMetadataRepository.findById(id)
                .orElseThrow(() -> new FileException("File not fot found with ID: " + id));
    }

    @Override
    public byte[] getFile(Long id)  {
        FileMetadata metadata = getMetadata(id);
        Path path = Paths.get(metadata.getPath());

        if(!Files.exists(path)){
            throw  new FileException("El archivo no se encuentra en el sistema de archivos");
        }

        try{
            return Files.readAllBytes(path);
        }
        catch(IOException ex){
            throw new RuntimeException(ex.getMessage());
        }


    }

    @Override
    public void validateFile(MultipartFile file) {

        if(file.isEmpty()){
            throw new FileException("El archivo esta vacio");
        }
        if(file.getSize() > maxFileSize){
            throw new FileException("El archivo excede el tamaño maximo permitido de " +
                    (maxFileSize/1024) + " KB");
        }
        if(!allowedTypes.contains(file.getContentType())){
            throw new FileException("Tipo de arcihvo no permitido " + file.getContentType());
        }

    }

    @Override
    public void deleteFile(Long id) {
        // 1. Buscar los metadatos del archivo en la BD
        FileMetadata metadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new FileException("No se encontró el archivo con ID: " + id));

        // 2. Obtener la ruta del archivo y eliminarlo del sistema de archivos
        Path filePath = Paths.get(metadata.getPath());
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar el archivo: " + e.getMessage());
        }

        // 3. Eliminar los metadatos de la BD
        fileMetadataRepository.delete(metadata);
    }

}
