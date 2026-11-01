package backend.model;

import jakarta.persistence.*;
import backend.model.Case;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "evidence")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Evidence title cannot be empty")
    private String title;
    private String description;
    private String filePath;

    public Evidence() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private Case investigationCase;
    public Case getInvestigationCase() {
      return investigationCase;
   }

    public void setInvestigationCase(Case investigationCase) {
      this.investigationCase = investigationCase;
    }
}