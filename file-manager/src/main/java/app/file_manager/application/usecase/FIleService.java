package app.file_manager.application.usecase;

import app.file_manager.web.dto.FileMetadataDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileService {

    public List<FileMetadataDto> getAll() ;

    public FileMetadataDto saveFile(MultipartFile file) ;

    public FileMetadataDto getMetadata(long id) ;

    public byte[] getFile(long id) ;

    public void deleteFile(long id);

    public void validateFile(MultipartFile file);
}
