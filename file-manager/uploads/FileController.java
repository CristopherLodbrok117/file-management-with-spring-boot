package mx.ingsoftwareii.sinaloa_backend.web.controllers;

import lombok.RequiredArgsConstructor;
import mx.ingsoftwareii.sinaloa_backend.application.usecases.FileService;
import mx.ingsoftwareii.sinaloa_backend.web.dtos.FileMetadataDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
//@PreAuthorize(value = "hasAnyRole('USER','SUPERUSER')")
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
    public ResponseEntity<List<FileMetadataDto>> getAllFilesByGroup(@RequestParam(name = "group") long groupId){
        return ResponseEntity.ok(fileService.getAllByGroup(groupId));
    }

    @PostMapping("/upload")
    public ResponseEntity<FileMetadataDto> upload(@RequestParam(name = "file") MultipartFile file,
                                                  @RequestParam(name = "user") long userId,
                                                  @RequestParam(name = "group") long groupId){
        FileMetadataDto metadataDto = fileService.saveFile(file, userId, groupId);

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
