package eu.macsworks.cubectf.listeners;

import eu.macsworks.cubectf.CubeCTF;
import eu.macsworks.cubectf.game.GameManager;
import eu.macsworks.cubectf.game.teams.Team;
import eu.macsworks.cubectf.game.teams.TeamManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.persistence.PersistentDataType;

@RequiredArgsConstructor
public class PickupFlagBlockListener implements Listener {

    private final TeamManager teamManager;

    @EventHandler
    public void onClickOnFlagBlock(PlayerInteractAtEntityEvent event){
        //Check if this is the flag
        if(!event.getRightClicked().getPersistentDataContainer().has(GameManager.FLAG_BLOCK_KEY)) return;
        event.setCancelled(true);

        //Can't be null
        String teamID = event.getRightClicked().getPersistentDataContainer().get(GameManager.FLAG_BLOCK_KEY, PersistentDataType.STRING);

        Team team = teamManager.getTeam(teamID).orElseThrow(() -> new IllegalArgumentException("Team with ID " + teamID + " doesn't exist but is in a PDC"));
        team.takeFlag(event.getPlayer());
    }

    @EventHandler
    public void onLClickOnFlagBlock(EntityDamageByEntityEvent event){
        //Check if this is the flag
        if(!event.getEntity().getPersistentDataContainer().has(GameManager.FLAG_BLOCK_KEY)) return;
        if(!(event.getDamager() instanceof Player player)) return;
        event.setCancelled(true);

        //Can't be null
        String teamID = event.getEntity().getPersistentDataContainer().get(GameManager.FLAG_BLOCK_KEY, PersistentDataType.STRING);

        Team team = teamManager.getTeam(teamID).orElseThrow(() -> new IllegalArgumentException("Team with ID " + teamID + " doesn't exist but is in a PDC"));
        team.takeFlag(player);
    }
}
