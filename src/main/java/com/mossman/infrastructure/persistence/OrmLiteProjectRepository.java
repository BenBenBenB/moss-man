package com.mossman.infrastructure.persistence;

import com.j256.ormlite.dao.Dao;
import com.mossman.domain.entities.Project;
import com.mossman.domain.repositories.ProjectRepository;
import com.mossman.infrastructure.persistence.models.ProjectDb;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrmLiteProjectRepository implements ProjectRepository {
    private final Dao<ProjectDb, Long> projectDao;

    public OrmLiteProjectRepository(Dao<ProjectDb, Long> projectDao) {
        this.projectDao = projectDao;
    }

    @Override
    public Project save(Project project) {
        try {
            ProjectDb dbModel = new ProjectDb(project);
            projectDao.createOrUpdate(dbModel);
            return dbModel.toDomain();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save project", e);
        }
    }

    @Override
    public Optional<Project> findById(long id) {
        try {
            ProjectDb dbModel = projectDao.queryForId(id);
            return Optional.ofNullable(dbModel).map(ProjectDb::toDomain);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find project by id", e);
        }
    }

    @Override
    public List<Project> findAll() {
        try {
            return projectDao.queryForAll().stream()
                    .map(ProjectDb::toDomain)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all projects", e);
        }
    }

    @Override
    public void delete(long id) {
        try {
            projectDao.deleteById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete project", e);
        }
    }
}
