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

    public Project execute(Project.Builder projectBuilder) {
        Project project = projectBuilder.build();
        // Here we would ideally have more validation logic
        Project savedProject = projectRepository.save(project);
        eventBus.publish(new ProjectCreatedEvent(savedProject, Instant.now()));
        return savedProject;
    }
}
