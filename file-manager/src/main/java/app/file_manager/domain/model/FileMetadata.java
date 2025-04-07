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
