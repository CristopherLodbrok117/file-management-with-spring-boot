package app.file_manager.domain.repository;

import app.file_manager.domain.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    public Optional<FileMetadata> findByName(String name);
}
