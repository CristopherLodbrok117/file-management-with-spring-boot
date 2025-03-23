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
