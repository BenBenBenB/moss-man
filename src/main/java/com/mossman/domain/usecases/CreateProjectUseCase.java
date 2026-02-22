package com.mossman.domain.usecases;

import com.mossman.domain.entities.Project;
import com.mossman.domain.events.DomainEventBus;
import com.mossman.domain.events.ProjectCreatedEvent;
import com.mossman.domain.repositories.ProjectRepository;
import java.time.Instant;

public class CreateProjectUseCase {
    private final ProjectRepository projectRepository;
    private final DomainEventBus eventBus;

    public CreateProjectUseCase(ProjectRepository projectRepository, DomainEventBus eventBus) {
        this.projectRepository = projectRepository;
        this.eventBus = eventBus;
    }

    public Project execute(Project.Builder projectBuilder, java.util.UUID creatorId, String creatorUsername) {
        // Initial owner member
        com.mossman.domain.entities.Member owner = new com.mossman.domain.entities.Member(0, creatorId, creatorUsername, "Project Owner", com.mossman.domain.entities.Permission.OWNER);
        
        Project project = projectBuilder
                .members(java.util.List.of(owner))
                .build();
        
        Project savedProject = projectRepository.save(project);
        eventBus.publish(new ProjectCreatedEvent(savedProject, Instant.now()));
        return savedProject;
    }
}
