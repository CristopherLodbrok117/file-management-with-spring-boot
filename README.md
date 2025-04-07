# File Management API with Spring Boot



Dependencies:
Spring Web
Spring Data JPA 
MySQL Driver
Lombok

#### The properties file
- Write the common configuration to connect to MySQL database
- Define the files directory name
- Max file size
- File types supported	

```java
spring.application.name=file-manager

# Configuración de MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/files_db_v1?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Configuración de JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Directorio para almacenar archivos
file.storage.location=uploads

# Tamaño máximo de archivo en bytes (ejemplo: 5MB)
file.max-size=5242880

# Tipos de archivos permitidos (separados por comas)
file.allowed-types=image/png,image/jpeg,application/pdf
```
<br>

#### FileMetadata
We need an entity to save files' metadata
```java
package app.file_manager.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder

@Entity
@Table(name = "files_metadata")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "path")
    private String path;

    private String type;
    private long size;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;
}
```
<br>

#### FileMetadataRepository
A repository to interact with the database
```java
package app.file_manager.domain.repository;

import app.file_manager.domain.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
}
```
<br>

#### FileService
Write the business logic in our service layer
```java
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
```
<br>

#### FileController
Implement the controller to handle clients requests 
```java
package app.file_manager.web.controller;

import app.file_manager.application.service.FileServiceImpl;
import app.file_manager.domain.model.FileMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileServiceImpl fileService;

    @GetMapping
    public String sayHi(){
        return "Hello user";
    }

    @PostMapping("/upload")
    public ResponseEntity<FileMetadata> upload(@RequestParam MultipartFile file){
        return ResponseEntity.ok(fileService.saveFile(file)); // 200 - OK
    }

    @GetMapping("/{id}/metadata")
    public ResponseEntity<FileMetadata> getMetadata(@PathVariable long id){
        return ResponseEntity.ok(fileService.getMetadata(id)); // 200 - OK
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable long id){
        FileMetadata metadata = fileService.getMetadata(id);
        byte[] file = fileService.getFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getType()))
                .contentLength(metadata.getSize())
                .header(HttpHeaders.CONTENT_DISPOSITION
                        , "attatchment; filename=\"" + metadata.getOriginalName() + "\"")
                .body(file); // 200 - OK
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build(); // 204 - NO CONTENT
    }
}
```
<br>

#### FieException
A custom exception to wrap file exceptions, extends from RuntimeException
```java
package app.file_manager.domain.exception;

public class FileException extends RuntimeException{
    public FileException(String msg){
        super(msg);
    }
}
```
<br>

#### GlobalExceptionHandler
A rest controller advice to handle all the application exceptions
```java
package app.file_manager.web.exception;

import app.file_manager.domain.exception.FileException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileException.class)
    public ResponseEntity<Map<String, String>> handleFileException(FileException ex){
        Map<String, String> response = new HashMap<>();

        response.put("error", ex.getMessage());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> genericExceptionHandler(Exception ex){
        Map<String, String> response = new HashMap<>();

        response.put("error", ex.getMessage());

        return ResponseEntity.internalServerError().body(response);
    }
}
```
<br>

## Run application

In insomnia (or Postman) configure a POST request to include a file
- Add body > Form Data 
- Name it "file" and load a document, image, etc.
  
![configure upload request image](https://github.com/CristopherLodbrok117/file-management-with-spring-boot/blob/eadb44ef6260bfc58596b1cc0f948bbb55f75380/screenshots/05%20-%20configure%20upload%20request.png)
<br>

### Responses
Upload files

![upload file image](https://github.com/CristopherLodbrok117/file-management-with-spring-boot/blob/eadb44ef6260bfc58596b1cc0f948bbb55f75380/screenshots/00%20-%20upload.png)
<br>

Get our files' metadata

![get metadata image](https://github.com/CristopherLodbrok117/file-management-with-spring-boot/blob/eadb44ef6260bfc58596b1cc0f948bbb55f75380/screenshots/01%20-%20getMetadata.png)
<br>

Delete the last file

![delete file image](https://github.com/CristopherLodbrok117/file-management-with-spring-boot/blob/eadb44ef6260bfc58596b1cc0f948bbb55f75380/screenshots/04%20-%20deleted.png)

<br>

Great! we can download when needed

![download file image](https://github.com/CristopherLodbrok117/file-management-with-spring-boot/blob/eadb44ef6260bfc58596b1cc0f948bbb55f75380/screenshots/02%20-%20download.png)

<br>

Remember that it's important to add headers in our response

![download response headers image](https://github.com/CristopherLodbrok117/file-management-with-spring-boot/blob/eadb44ef6260bfc58596b1cc0f948bbb55f75380/screenshots/03%20-%20headers.png)


