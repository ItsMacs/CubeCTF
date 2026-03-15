package eu.macsworks.cubectf;

import eu.macsworks.cubectf.commands.CTFCommand;
import eu.macsworks.cubectf.game.GameManager;
import eu.macsworks.cubectf.game.teams.Team;
import eu.macsworks.cubectf.game.teams.TeamManager;
import eu.macsworks.cubectf.listeners.DropFlagListener;
import eu.macsworks.cubectf.listeners.PickupFlagBlockListener;
import eu.macsworks.cubectf.listeners.PickupFlagItemListener;
import eu.macsworks.cubectf.utils.LoadedConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;

@Getter
public final class CubeCTF extends JavaPlugin {

    private TeamManager teamManager;
    private GameManager gameManager;

    private LoadedConfig loadedConfig;

    @Override
    public void onEnable() {
        loadedConfig = new LoadedConfig(this);
        loadedConfig.loadConfig();
        loadedConfig.loadMessages();

        loadManagers();

        loadListeners();
        loadCommands();
        loadTasks();

        getLogger().info("CubeCTF by Alice enabled successfully!");
    }

    private void loadManagers() {
        teamManager = new TeamManager(this);
        gameManager = new GameManager(this);

        gameManager.init();
        teamManager.init();
    }

    private void loadListeners() {
        Bukkit.getPluginManager().registerEvents(new DropFlagListener(teamManager), this);
        Bukkit.getPluginManager().registerEvents(new PickupFlagItemListener(teamManager), this);
        Bukkit.getPluginManager().registerEvents(new PickupFlagBlockListener(teamManager), this);
    }

    private void loadCommands() {
        PaperCommandManager<CommandSourceStack> manager = PaperCommandManager.builder()
                .executionCoordinator(ExecutionCoordinator.asyncCoordinator())
                .buildOnEnable(this);

        AnnotationParser<CommandSourceStack> annotationParser = new AnnotationParser<>(manager, CommandSourceStack.class);
        annotationParser.parse(new CTFCommand(this));
    }

    private void loadTasks() {
        Bukkit.getScheduler().runTaskTimer(this, gameManager::tick, 0, 20);
    }

    @Override
    public void onDisable() {
        //Clean up the game, so that no residuals are left
        teamManager.getTeams().forEach(Team::cleanup);

        getLogger().info("CubeCTF by Alice disabled successfully!");
    }
}
