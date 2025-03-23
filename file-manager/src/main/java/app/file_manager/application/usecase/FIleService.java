package app.file_manager.application.usecase;

import app.file_manager.domain.model.FileMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FIleService {

    public FileMetadata saveFile(MultipartFile file) throws IOException;

    public FileMetadata getMetadata(Long id) throws IOException;

    public byte[] getFile(Long id) throws IOException;

    public void validateFile(MultipartFile file);

    public void deleteFile(Long id);
}
