package eu.macsworks.cubectf.game.states.impl;

import eu.macsworks.cubectf.CubeCTF;
import eu.macsworks.cubectf.game.states.GameState;
import eu.macsworks.cubectf.utils.lang.Messages;

public class StartingGameState extends GameState {

    public StartingGameState(CubeCTF plugin) {
        super(plugin, plugin.getLoadedConfig().getStartTimeMs());
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onTick() {
        //Manage the state running out of time by starting the game
        manageTimedState(config.getMessage(Messages.TIMEFUL_MESSAGE), "Game starting", () -> {
            gameManager.setGameState(new InProgressGameState(plugin));
        });
    }

    @Override
    public void onEnd() {

    }
}
