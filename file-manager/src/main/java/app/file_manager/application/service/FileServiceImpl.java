package app.file_manager.application.service;

import lombok.RequiredArgsConstructor;
import app.file_manager.domain.repository.FileMetadataRepository;
import app.file_manager.application.usecase.FileService;
import app.file_manager.domain.model.FileMetadata;
import app.file_manager.domain.exception.FileException;
import app.file_manager.web.dto.FileMetadataDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 5MB
    private static final String STORAGE_LOCATION = "uploads";

    private final FileMetadataRepository fileMetadataRepository;
    private final Set<String> allowedTypes;

    @Override
    public List<FileMetadataDto> getAll() {
        List<FileMetadata> metadataList = fileMetadataRepository.findAll();

        return metadataList.stream()
                .map(FileMetadataDto::fromEntity)
                .toList();
    }

    @Override
    public FileMetadataDto saveFile(MultipartFile file) {
        /* 1. Validar archivo */
        validateFile(file);

        short tag = 1;

        try{
            /* 2. Cerar directorio si no existe */
            Path storagePath = Paths.get(STORAGE_LOCATION);
            if(!Files.exists(storagePath)){
                Files.createDirectories(storagePath);
            }

            /* 3. Guardar archivo */
            String fileLocation = STORAGE_LOCATION + File.separator + file.getOriginalFilename();
            Path filePath = Paths.get(fileLocation);

            /* To replace */
            FileMetadata fileMetadata;
            String fileName = file.getOriginalFilename();

            if(Files.exists(filePath)){
                fileMetadata = fileMetadataRepository.findByName(fileName)
                        .orElseThrow(() -> new FileException("No se encontro el archivo con el nombre: " + fileName));

                Files.delete(filePath);
            }
            else{
                fileMetadata = FileMetadata.builder()
                        .tag(tag)
                        .name(fileName)
                        .path(fileLocation)
                        .fileType(file.getContentType())

                        .build();
            }

            Files.copy(file.getInputStream(), filePath);

            /* 4. Guardar metadatos en BD */
            fileMetadata.setSize(file.getSize());
            fileMetadata.setUploadedAt(LocalDateTime.now());

            fileMetadata = fileMetadataRepository.save(fileMetadata);

            /* 5. Crear DTO y retornar */
            return FileMetadataDto.fromEntity(fileMetadata);
        }
        catch(IOException ex) {
            throw new FileException(ex.getMessage());
        }
    }

    @Override
    public FileMetadataDto getMetadata(long id) {
        FileMetadata metadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new FileException("No se encontro el archivo con ID: " + id));

        return FileMetadataDto.fromEntity(metadata);
    }

    @Override
    public byte[] getFile(long id) {
        /* 1. Buscar metadata*/
        FileMetadata metadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new FileException("No se encontro el archivo con ID: " + id));
        Path filePath = Paths.get(metadata.getPath());

        /* 2. Verificar exixtencia en sistema de archivos */
        if(!Files.exists(filePath)){
            throw new FileException("El archivo no se encuentra en el sistema de archivos");
        }

        try{
            /* 3. Retornar archivo */
            return Files.readAllBytes(filePath);
        }
        catch(IOException ex) {
            throw new FileException(ex.getMessage());
        }
    }

    @Override
    public void deleteFile(long id) {
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
            throw new FileException( e.getMessage());
        }

        // 3. Eliminar los metadatos de la BD
        fileMetadataRepository.delete(metadata);
    }

    @Override
    public void validateFile(MultipartFile file) {
        if(file.isEmpty()){
            throw new FileException("El archivo esta vacio");
        }
        if(file.getSize() > MAX_FILE_SIZE){
            throw new FileException("El archivo excede el tamaño maximo permitido de " +
                    (MAX_FILE_SIZE/1024) + " KB");
        }
        if(!allowedTypes.contains(file.getContentType())){
            System.out.println("allowed: " + allowedTypes);
            throw new FileException("Tipo de arcihvo no permitido: " + file.getContentType());
        }
    }

}
