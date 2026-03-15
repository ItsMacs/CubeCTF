package eu.macsworks.cubectf.game;

import eu.macsworks.cubectf.CubeCTF;
import eu.macsworks.cubectf.game.states.GameState;
import eu.macsworks.cubectf.game.states.impl.EndingGameState;
import eu.macsworks.cubectf.game.states.impl.LobbyGameState;
import eu.macsworks.cubectf.game.states.impl.StartingGameState;
import eu.macsworks.cubectf.game.teams.Team;
import eu.macsworks.cubectf.game.teams.TeamManager;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class GameManager {

    public static final NamespacedKey FLAG_ITEM_KEY = new NamespacedKey("cubectf", "flag_item");
    public static final NamespacedKey FLAG_BLOCK_KEY = new NamespacedKey("cubectf", "flag_block");

    private final CubeCTF plugin;
    private TeamManager teamManager;

    @Getter @Setter
    private BossBar scoreBossBar;

    @Getter
    private GameState gameState;


    public GameManager(CubeCTF plugin) {
        this.plugin = plugin;
    }

    public void init(){
        gameState = new LobbyGameState(plugin);
        teamManager = plugin.getTeamManager();
    }

    public void startGame() {
        setGameState(new StartingGameState(plugin));
    }

    public void stopGame() {
        setGameState(new EndingGameState(plugin));
    }

    /**
     * Called every 20 ticks (1 second), ticks the game based upon the current {@link #gameState}
     */
    public void tick(){
        gameState.onTick();
    }

    /**
     * Sets the new game state, ending the previous one and starting the new one
     * @param state New game state
     */
    public void setGameState(GameState state){
        gameState.onEnd();

        gameState = state;

        gameState.onStart();
    }

    public void broadcastMessage(String message, @Nullable Sound soundToPlay){
        Component component = MiniMessage.miniMessage().deserialize(message);

        getPlayers().forEach(p -> {
            p.sendMessage(component);
            if(soundToPlay != null) p.playSound(p.getLocation(), soundToPlay, 1, 1);
        });
    }

    /**
     * Returns a list of all players in the game. Important not to use Bukkit#getOnlinePlayers() as some players might not be playing.
     */
    public List<Player> getPlayers(){
        return teamManager.getTeams().stream().map(Team::getMembers).flatMap(Collection::stream).toList();
    }

}
