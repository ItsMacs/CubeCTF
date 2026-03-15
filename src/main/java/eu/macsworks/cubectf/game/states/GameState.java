package eu.macsworks.cubectf.game.states;

import eu.macsworks.cubectf.CubeCTF;
import eu.macsworks.cubectf.game.GameManager;
import eu.macsworks.cubectf.game.teams.TeamManager;
import eu.macsworks.cubectf.utils.DurationUtil;
import eu.macsworks.cubectf.utils.LoadedConfig;
import eu.macsworks.cubectf.utils.lang.Messages;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Stateful game state object
 */

@Getter
public abstract class GameState {

    protected final CubeCTF plugin;
    protected final LoadedConfig config;
    protected final GameManager gameManager;
    protected final TeamManager teamManager;

    public GameState(CubeCTF plugin, long stateDuration) {
        this.plugin = plugin;
        this.stateDuration = stateDuration;

        config = plugin.getLoadedConfig();
        gameManager = plugin.getGameManager();
        teamManager = plugin.getTeamManager();
    }

    private final long startTime = System.currentTimeMillis();
    private final long stateDuration;

    /**
     * @return Returns the elapsed time from the start of this state (in milliseconds)
     */
    public long elapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Manages automatically a timed state (state that has a set amount of time before ending),
     * providing automatically the message of "X time remaining before Y happens" and running {@code action} when time's up.
     *
     * @param messageTimeRemaining Message to send to indicate time is running out (sent at -3,-2-,-1 seconds and 50%)
     * @param actionWhenTimesUp    Message to indicate what's gonna happen (example: "game ends" or "game starts")
     * @param action               Code to run when time's up
     */
    protected void manageTimedState(String messageTimeRemaining, String actionWhenTimesUp, Runnable action) {
        if (elapsedTime() >= stateDuration) {
            action.run();
            return;
        }

        long timeRemaining = stateDuration - elapsedTime();
        long previousTimeRemaining = timeRemaining + 1000;

        if (previousTimeRemaining > stateDuration / 2 && timeRemaining <= stateDuration / 2) {
            gameManager.broadcastMessage(messageTimeRemaining.replace("{time}", DurationUtil.formatDurationWithWords(timeRemaining)).replace("{action}", actionWhenTimesUp), null);
            return;
        }

        if (previousTimeRemaining > 3000 && timeRemaining <= 3000
                || previousTimeRemaining > 2000 && timeRemaining <= 2000
                || previousTimeRemaining > 1000 && timeRemaining <= 1000) {
            gameManager.broadcastMessage(messageTimeRemaining.replace("{time}", DurationUtil.formatDurationWithWords(timeRemaining + 1000)).replace("{action}", actionWhenTimesUp), null);
            return;
        }
    }

    /**
     * Displays the timer action bar for all players ingame
     */
    protected void showTimerActionBar() {
        String timer = DurationUtil.formatDuration(stateDuration - (stateDuration - elapsedTime()));

        gameManager.getPlayers().forEach(player -> player.sendActionBar(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.ACTIONBAR).replace("{timer}", timer))));
    }

    public abstract void onStart();

    public abstract void onTick();

    public abstract void onEnd();
}
