package com.mossman;

import com.mossman.adapters.commands.MossManCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mossman.infrastructure.config.ConfigManager;
import com.mossman.infrastructure.events.SimpleEventBus;
import com.mossman.infrastructure.persistence.DatabaseManager;
import com.mossman.infrastructure.persistence.OrmLiteProjectRepository;
import com.mossman.infrastructure.persistence.OrmLiteTicketRepository;
import com.mossman.domain.usecases.CreateProjectUseCase;
import com.mossman.domain.usecases.CreateTicketUseCase;
import com.mossman.domain.usecases.UpdateTicketUseCase;
import com.mossman.domain.usecases.UpdateProjectUseCase;

import java.io.File;
import java.sql.SQLException;

public class MossManMod implements ModInitializer {
    public static final String MOD_ID = "mossman";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static DatabaseManager databaseManager;
    private static SimpleEventBus eventBus;
    private static CreateProjectUseCase createProjectUseCase;
    private static CreateTicketUseCase createTicketUseCase;
    private static UpdateTicketUseCase updateTicketUseCase;
    private static UpdateProjectUseCase updateProjectUseCase;
    private static OrmLiteProjectRepository projectRepository;
    private static OrmLiteTicketRepository ticketRepository;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing MossMan Tracker...");

        ConfigManager.loadConfig();

        try {
            File dbFile = new File(FabricLoader.getInstance().getGameDir().toFile(), "mossman.db");
            String dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            databaseManager = new DatabaseManager(dbUrl);
            eventBus = new SimpleEventBus();
            
            projectRepository = new OrmLiteProjectRepository(databaseManager.getProjectDao());
            ticketRepository = new OrmLiteTicketRepository(databaseManager.getTicketDao());
            
            createProjectUseCase = new CreateProjectUseCase(projectRepository, eventBus);
            createTicketUseCase = new CreateTicketUseCase(ticketRepository, eventBus);
            updateTicketUseCase = new UpdateTicketUseCase(ticketRepository, eventBus);
            updateProjectUseCase = new UpdateProjectUseCase(projectRepository, eventBus);
            LOGGER.info("Database and Use Cases initialized successfully.");
        } catch (SQLException e) {
            LOGGER.error("Failed to initialize Database Manager", e);
        }

        if (ConfigManager.getConfig().enableCommands) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                MossManCommand.register(dispatcher);
            });
            LOGGER.info("MossMan commands registered.");
        } else {
            LOGGER.info("MossMan commands are disabled by configuration.");
        }
    }

    public static CreateProjectUseCase getCreateProjectUseCase() { return createProjectUseCase; }
    public static CreateTicketUseCase getCreateTicketUseCase() { return createTicketUseCase; }
    public static UpdateTicketUseCase getUpdateTicketUseCase() { return updateTicketUseCase; }
    public static UpdateProjectUseCase getUpdateProjectUseCase() { return updateProjectUseCase; }
    public static OrmLiteProjectRepository getProjectRepository() { return projectRepository; }
    public static OrmLiteTicketRepository getTicketRepository() { return ticketRepository; }
}
