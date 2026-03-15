package eu.macsworks.cubectf.commands;

import eu.macsworks.cubectf.CubeCTF;
import eu.macsworks.cubectf.game.GameManager;
import eu.macsworks.cubectf.game.states.impl.InProgressGameState;
import eu.macsworks.cubectf.game.states.impl.LobbyGameState;
import eu.macsworks.cubectf.game.teams.Team;
import eu.macsworks.cubectf.game.teams.TeamManager;
import eu.macsworks.cubectf.utils.LoadedConfig;
import eu.macsworks.cubectf.utils.lang.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;

import java.util.List;
import java.util.Optional;

public class CTFCommand {

    private final CubeCTF plugin;
    private final LoadedConfig config;
    private final TeamManager teamManager;
    private final GameManager gameManager;

    public CTFCommand(CubeCTF plugin) {
        this.plugin = plugin;
        config = plugin.getLoadedConfig();
        teamManager = plugin.getTeamManager();
        gameManager = plugin.getGameManager();
    }

    /**
     * Join a team. Can also be used on other players (with the optional otherPlayer param, if the sender has the admin perm)
     *
     * @param stack       Command source stack
     * @param team        Team we want to join (should either be "RED" or "BLUE", caps insensitive)
     * @param otherPlayer Player we want to assign a team to (optional)
     */
    @Command("ctf join <team> [otherPlayer]")
    @CommandDescription("Join a team")
    public void join(@NonNull CommandSourceStack stack, @NonNull @Argument(value = "team", suggestions = "team") String team, @Argument("otherPlayer") Player otherPlayer) {
        CommandSender sender = stack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.PLAYER_ONLY)));
            return;
        }

        Player targetPlayer = player;
        if (otherPlayer != null && player.hasPermission("ctf.admin")) targetPlayer = otherPlayer;

        //If the player's already in a team, leave the team first
        Optional<Team> teamOpt = teamManager.getPlayerTeam(targetPlayer);
        if (teamOpt.isPresent()) teamOpt.get().removeMember(targetPlayer.getUniqueId());

        Optional<Team> targetTeamOpt = teamManager.getTeam(team);
        if (targetTeamOpt.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.NO_TEAM_FOUND)));
            return;
        }

        if (targetTeamOpt.get().getTeamSize() >= config.getTeamSize()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.TEAM_FULL)));
            return;
        }

        targetTeamOpt.get().addMember(targetPlayer.getUniqueId());
        targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.JOINED_TEAM).replace("{team}", targetTeamOpt.get().getDisplayName())));
        if(targetPlayer != player) player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.OTHER_JOINED)));
    }

    /**
     * Leaves the current team, or makes a player leave (same as join)
     *
     * @param stack       Command source stack
     * @param otherPlayer Player we want to force leave (optional)
     */
    @Command("ctf leave [otherPlayer]")
    @CommandDescription("Leave your current team")
    public void leave(@NonNull CommandSourceStack stack, @Argument(value = "otherPlayer", suggestions = "team") Player otherPlayer) {
        CommandSender sender = stack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.PLAYER_ONLY)));
            return;
        }

        Player targetPlayer = player;
        if (otherPlayer != null && player.hasPermission("ctf.admin")) targetPlayer = otherPlayer;

        //If the player's not in a team, warn
        Optional<Team> teamOpt = teamManager.getPlayerTeam(targetPlayer);
        if (teamOpt.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.NOT_IN_TEAM)));
            return;
        }

        teamOpt.get().removeMember(targetPlayer.getUniqueId());
        targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.LEFT_TEAM).replace("{team}", teamOpt.get().getDisplayName())));
        if(targetPlayer != player) player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.OTHER_LEFT)));
    }

    /**
     * Forcestarts the game (if not started already)
     *
     * @param stack Command source stack
     */
    @Command("ctf start")
    @CommandDescription("Start the game")
    @Permission("ctf.admin")
    public void start(@NonNull CommandSourceStack stack) {
        CommandSender sender = stack.getSender();
        if (!(gameManager.getGameState() instanceof LobbyGameState)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.GAME_ALREADY_STARTED)));
            return;
        }

        if(teamManager.getTeams().stream().anyMatch(t -> t.getFlagLocation() == null)){
            sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.SETUP_INCOMPLETE)));
            return;
        }

        gameManager.startGame();

        sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.GAME_STARTED)));
    }

    /**
     * Stops the game (if started)
     *
     * @param stack Command source stack
     */
    @Command("ctf stop")
    @CommandDescription("Stop the game")
    @Permission("ctf.admin")
    public void stop(@NonNull CommandSourceStack stack) {
        CommandSender sender = stack.getSender();
        if (!(gameManager.getGameState() instanceof InProgressGameState)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.GAME_NOT_STARTED)));
            return;
        }

        //Has to be in a sync task as we're adding entites in the end method of InProgressGameState
        //and commands are asynchronous
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, gameManager::stopGame);

        sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.GAME_STOPPED)));
    }

    /**
     * Sets the flag location for a team (also the base location, radius defined in config)
     *
     * @param stack Command source stack
     * @param team  Team we want to set the flag pos for
     */
    @Command("ctf setflag <team>")
    @CommandDescription("Set the flag location for a team")
    @Permission("ctf.admin")
    public void setFlag(@NonNull CommandSourceStack stack, @NonNull @Argument(value = "team", suggestions = "team") String team) {
        CommandSender sender = stack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.PLAYER_ONLY)));
            return;
        }

        Optional<Team> teamOpt = teamManager.getTeam(team);

        if (teamOpt.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.NO_TEAM_FOUND)));
            return;
        }

        teamOpt.get().setFlagLocation(player.getLocation().clone().toCenterLocation().setDirection(new Vector(0, 0, 0)));
        player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.FLAG_SET)
                .replace("{team}", teamOpt.get().getDisplayName())));
    }

    /**
     * Sets the score for either team
     *
     * @param stack Command source stack
     * @param team  Team we want to change the score for (should either be "RED" or "BLUE", caps insensitive)
     * @param score Score amount we want to set, can also be negative (if over the required amount makes an instant win)
     */
    @Command("ctf score <team> <score>")
    @CommandDescription("Set the score for a team")
    @Permission("ctf.admin")
    public void score(@NonNull CommandSourceStack stack, @NonNull @Argument(value = "team", suggestions = "team") String team, @Argument("score") int score) {
        CommandSender sender = stack.getSender();
        Optional<Team> teamOpt = teamManager.getTeam(team);

        if (teamOpt.isEmpty()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.NO_TEAM_FOUND)));
            return;
        }

        teamOpt.get().setScore(score);
        sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.SCORE_SET)
                .replace("{team}", teamOpt.get().getDisplayName())
                .replace("{score}", String.valueOf(score))));
    }

    @Suggestions("team")
    public List<String> teamSuggestions(@NonNull CommandContext<CommandSourceStack> context, @NonNull String input) {
        return teamManager.getTeams().stream()
                .map(Team::getName)
                .toList();
    }
}
