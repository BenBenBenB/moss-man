package com.mossman.domain.usecases;

import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.TicketType;
import com.mossman.domain.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateProjectTicketTypesUseCaseTest {

    private ProjectRepository projectRepository;
    private UpdateProjectTicketTypesUseCase useCase;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        useCase = new UpdateProjectTicketTypesUseCase(projectRepository);
    }

    @Test
    void testExecute_Success() {
        UUID editorId = UUID.randomUUID();
        Project existingProject = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, editorId, "Editor", "Editor", Permission.EDITOR)))
                .ticketTypes(List.of(new TicketType("BUG", "red")))
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));
        when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArguments()[0]);

        List<TicketType> newTypes = List.of(
                new TicketType("BUG", "red"),
                new TicketType("TASK", "green")
        );

        Project updatedProject = useCase.execute(1L, editorId, newTypes);

        assertNotNull(updatedProject);
        assertEquals(2, updatedProject.getTicketTypes().size());
        assertEquals("TASK", updatedProject.getTicketTypes().get(1).name());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void testExecute_ThrowsExceptionIfProjectNotFound() {
        UUID requesterId = UUID.randomUUID();
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(1L, requesterId, List.of());
        });
    }

    @Test
    void testExecute_ThrowsExceptionIfUserDoesNotHavePermission() {
        UUID viewerId = UUID.randomUUID();
        Project existingProject = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, viewerId, "Viewer", "Viewer", Permission.VIEWER)))
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));

        assertThrows(SecurityException.class, () -> {
            useCase.execute(1L, viewerId, List.of());
        });
    }

    @Test
    void testExecute_ThrowsExceptionIfTicketTypeNamesNotUnique() {
        UUID editorId = UUID.randomUUID();
        Project existingProject = Project.builder()
                .id(1L)
                .members(List.of(new Member(0, editorId, "Editor", "Editor", Permission.EDITOR)))
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));

        List<TicketType> duplicateTypes = List.of(
                new TicketType("TASK", "blue"),
                new TicketType("task", "red") // Duplicate ignoring case
        );

        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(1L, editorId, duplicateTypes);
        });
        
        verify(projectRepository, never()).save(any());
    }
}
