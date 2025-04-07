# File Management API with Spring Boot



### Dependencies:
- Spring Web
- Spring Data JPA 
- MySQL Driver
- Lombok

### Files supported
- Images: jpg, jpeg, png, gif, webp
- Documents: pdf, doc, docx
- Excel: xlsx, csv, xls
- Serializables: json, xml
- Development: .c, .cpp, .java, .py, .js, .ts, .php, .rb, .swift, .kt, .go, .rs, .cs, .html, .css

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

#JPA/HIBERNATE
spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.MySQLDialect
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update

spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
spring.servlet.multipart.resolve-lazily=true
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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "files_metadata")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private short tag;

    @Column(name = "name")
    private String name;

    @Column(name = "path")
    private String path;

    @Column(name = "file_type")
    private String fileType;

    private long size;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

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

import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    public Optional<FileMetadata> findByName(String name);
}

```
<br>

#### FileService
Write the business logic in our service layer
```java
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

```
<br>

#### FileController
Implement the controller to handle clients requests 
```java
package app.file_manager.web.controller;

import lombok.RequiredArgsConstructor;
import app.file_manager.application.usecase.FileService;
import app.file_manager.web.dto.FileMetadataDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

//    @GetMapping
//    public String sayHi(){
//        return "Hello, user!";
//    }

    @GetMapping("/{id}/metadata")
    public ResponseEntity<FileMetadataDto> metadata(@PathVariable long id){
        return ResponseEntity.ok(fileService.getMetadata(id));
    }

    @GetMapping
    public ResponseEntity<List<FileMetadataDto>> getAllFilesByGroup(){
        return ResponseEntity.ok(fileService.getAll());
    }

    @PostMapping("/upload")
    public ResponseEntity<FileMetadataDto> upload(@RequestParam(name = "file") MultipartFile file){
        FileMetadataDto metadataDto = fileService.saveFile(file);

        return ResponseEntity.ok(metadataDto);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable long id){
        FileMetadataDto metadataDto = fileService.getMetadata(id);
        byte[] file = fileService.getFile(metadataDto.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadataDto.getFileType()))
                .contentLength(metadataDto.getSize())
                .header(HttpHeaders.CONTENT_DISPOSITION
                        , "attachment; filename=\"" + metadataDto.getName() + "\"")
                .body(file);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> delete(@PathVariable long id){
        fileService.deleteFile(id);

        return ResponseEntity.noContent().build();
    }

}

```
<br>

#### FileConfig
File configuration. Bean to define file types allowed
```java
package app.file_manager.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class FileConfig {

    @Bean
    public Set<String> allowedTypes(){

        return new HashSet<>(
                Set.of(
                        "image/png",
                        "image/jpeg",
                        "image/gif",
                        "image/webp",
                        "application/pdf",
                        "application/xml",
                        "application/csv",
                        "application/json",
                        "application/msword",
                        "application/java-archive",
                        "application/octet-stream",
                        "application/javascript",
                        "application/x-httpd-php",
                        "application/rls-services+xml",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "text/x-java-source",
                        "text/csv",
                        "text/xml",
                        "text/plain",
                        "text/x-c",
                        "text/html",
                        "text/css",
                        "text/javascript",
                        "video/mp2t"
                        )
        );

    }
}

```
<br>

#### FileException
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(FileException.class)
    ResponseEntity<Map<String,String>> fileExceptionHandler(FileException ex){
        return retrieveErrorResponse(HttpStatus.BAD_REQUEST,
                "file error", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> multipartMaxSizeExceptionHandler(MaxUploadSizeExceededException ex){
        return retrieveErrorResponse(HttpStatus.BAD_REQUEST,
                "Multipart file error", ex.getMessage());
    }

    ResponseEntity<Map<String, String>> retrieveErrorResponse(HttpStatus statusCode,
                                                              String errorType, String message){
        return ResponseEntity.status(statusCode)
                .body(Map.of(errorType, message));
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


