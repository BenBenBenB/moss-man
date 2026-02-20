package com.mossman.domain.repositories;

import com.mossman.domain.entities.Project;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    Project save(Project project);
    Optional<Project> findById(long id);
    List<Project> findAll();
    void delete(long id);
}
