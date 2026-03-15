package eu.macsworks.cubectf.game.states.impl;

import eu.macsworks.cubectf.CubeCTF;
import eu.macsworks.cubectf.game.GameManager;
import eu.macsworks.cubectf.game.states.GameState;
import eu.macsworks.cubectf.game.teams.Team;
import eu.macsworks.cubectf.utils.lang.Messages;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.*;

public class InProgressGameState extends GameState {

    private final GameManager gameManager = plugin.getGameManager();

    public InProgressGameState(CubeCTF plugin) {
        super(plugin, plugin.getLoadedConfig().getMatchTimeMs());
    }

    @Override
    public void onStart() {
        gameManager.setScoreBossBar(BossBar.bossBar(Component.text("Score"), 1F, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS));
        gameManager.broadcastMessage(config.getMessage(Messages.GAME_STARTING), Sound.BLOCK_NOTE_BLOCK_CHIME);
        
        //Teleport all players to their team's flag position
        teamManager.getTeams().forEach(team -> {
            team.respawnFlag(false, false);

            Location spawnLocation = team.getFlagLocation().clone().add(0, 1, 0);
            team.getMembers().forEach(member -> {
                member.teleport(spawnLocation);
                gameManager.getScoreBossBar().addViewer(member);
            });
        });
    }

    @Override
    public void onTick() {
        //Manage the game time out
        manageTimedState(config.getMessage(Messages.TIMEFUL_MESSAGE), "Game ending", () -> {
            gameManager.setGameState(new EndingGameState(plugin));
        });

        //Make each team check their flag status
        teamManager.getTeams().forEach(Team::checkFlagCapture);

        if(teamManager.getTeams().stream().anyMatch(t -> t.getScore() >= config.getWinPoints())){
            gameManager.setGameState(new EndingGameState(plugin));
            return;
        }

        int bluePoints = teamManager.getTeam("blue").orElseThrow().getScore();
        int redPoints = teamManager.getTeam("red").orElseThrow().getScore();

        gameManager.getScoreBossBar().name(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.BOSSBAR)
                        .replace("{blue_points}", String.valueOf(bluePoints))
                        .replace("{red_points}", String.valueOf(redPoints))));

        showTimerActionBar();
    }

    @Override
    public void onEnd() {
        //Decide a winner and display the correct end message
        Team winner = decideWinner();
        
        String resultStr = elapsedTime() >= getStateDuration() ? config.getMessage(Messages.RESULT_TIME_UP) : config.getMessage(Messages.RESULT_TEAM_WON);
        gameManager.broadcastMessage(config.getMessage(Messages.GAME_ENDED).replace("{result}", resultStr).replace("{team}", winner.getDisplayName()), Sound.BLOCK_NOTE_BLOCK_BASS);

        gameManager.getPlayers().forEach(p -> gameManager.getScoreBossBar().removeViewer(p));

        gameManager.setScoreBossBar(null);
        SplittableRandom random = new SplittableRandom();

        //Spawn some fireworks for looks
        winner.getMembers().forEach(p -> {
            Location loc = p.getLocation();
            Firework firework = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
            FireworkMeta meta = firework.getFireworkMeta();

            meta.addEffect(FireworkEffect.builder()
                    .withColor(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)))
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .trail(true)
                    .build());

            meta.setPower(1);
            firework.setFireworkMeta(meta);
            firework.setShotAtAngle(false);
            firework.setShooter(p);
        });
    }

    /**
     * Decide what team won the game, checking which team has the most points. If all teams have the same points,
     * a random one is chosen (otherwise it'll always be the red one, it's not very fair)
     * @return
     */
    private Team decideWinner() {
        Collection<Team> teams = teamManager.getTeams();
        List<Team> teamsWithSamePoints = new ArrayList<>();
        
        int maxPoints = 0;
        for(Team team : teams) {
            if(team.getScore() < maxPoints) continue;
            
            if(team.getScore() == maxPoints){
                teamsWithSamePoints.add(team);
                continue;
            }
            
            //At this point this team's score is higher than the max points. Update the list.
            maxPoints = team.getScore();
            teamsWithSamePoints.clear();
            teamsWithSamePoints.add(team);
        }
        
        //We have a list with all the teams that have the same point amount. Randomize a winner.
        return teamsWithSamePoints.get(new Random().nextInt(teamsWithSamePoints.size()));
    }
}
