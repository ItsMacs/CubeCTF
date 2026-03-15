package eu.macsworks.cubectf.game.states.impl;

import eu.macsworks.cubectf.CubeCTF;
import eu.macsworks.cubectf.game.states.GameState;
import eu.macsworks.cubectf.game.teams.Team;
import eu.macsworks.cubectf.utils.lang.Messages;

public class EndingGameState extends GameState {

    public EndingGameState(CubeCTF plugin) {
        super(plugin, plugin.getLoadedConfig().getEndTimeMs());
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onTick() {
        manageTimedState(config.getMessage(Messages.TIMEFUL_MESSAGE), "Game resetting", () -> {
            gameManager.setGameState(new LobbyGameState(plugin));
        });
    }

    @Override
    public void onEnd() {
        //Clean up the game
        teamManager.getTeams().forEach(Team::cleanup);
    }
}
