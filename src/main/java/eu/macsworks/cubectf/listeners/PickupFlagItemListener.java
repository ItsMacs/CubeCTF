package eu.macsworks.cubectf.listeners;

import eu.macsworks.cubectf.CubeCTF;
import eu.macsworks.cubectf.game.GameManager;
import eu.macsworks.cubectf.game.teams.Team;
import eu.macsworks.cubectf.game.teams.TeamManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.persistence.PersistentDataType;

@RequiredArgsConstructor
public class PickupFlagItemListener implements Listener {

    private final TeamManager teamManager;

    @EventHandler
    public void onPickupFlag(PlayerAttemptPickupItemEvent event){
        //Not a flag, ignore
        if(!event.getItem().getPersistentDataContainer().has(GameManager.FLAG_ITEM_KEY)) return;
        event.setCancelled(true);

        //Remove the actual flag item, it'll be recreated IF the player can hold it
        event.getItem().remove();

        //Can't be null - this is only ever a string and we know the item has it already
        String teamID = event.getItem().getPersistentDataContainer().get(GameManager.FLAG_ITEM_KEY, PersistentDataType.STRING);

        Team team = teamManager.getTeam(teamID).orElseThrow(() -> new IllegalArgumentException("Team with ID " + teamID + " doesn't exist but is in a PDC"));
        team.pickupFlag(event.getPlayer());
    }

    @EventHandler
    public void onFlagItemDestroyed(ItemDespawnEvent event){
        //Avoid flag item despawn - shouldn't happen because of the pickup delay being int maxvalue
        event.setCancelled(event.getEntity().getPersistentDataContainer().has(GameManager.FLAG_ITEM_KEY));
    }

}
