package eu.macsworks.cubectf.game.states.impl;

import eu.macsworks.cubectf.CubeCTF;
import eu.macsworks.cubectf.game.states.GameState;
import eu.macsworks.cubectf.utils.lang.Messages;
import org.bukkit.Sound;

public class LobbyGameState extends GameState {

    public LobbyGameState(CubeCTF plugin) {
        super(plugin, Long.MAX_VALUE); //doesn't matter, timer isn't used and isn't a constraint anywhere. nothing happens if the time elapses
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onTick() {
        //If an automatic game start shall ever be required, it should be added through this method
    }

    @Override
    public void onEnd() {
        gameManager.broadcastMessage(config.getMessage(Messages.GAME_STARTING), Sound.BLOCK_NOTE_BLOCK_CHIME);
    }
}
