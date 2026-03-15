package eu.macsworks.cubectf.listeners;

import eu.macsworks.cubectf.CubeCTF;
import eu.macsworks.cubectf.game.GameManager;
import eu.macsworks.cubectf.game.teams.TeamManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class DropFlagListener implements Listener {

    private final TeamManager teamManager;

    @EventHandler
    public void onFlagPlace(BlockPlaceEvent event){
        event.setCancelled(event.getItemInHand().getPersistentDataContainer().has(GameManager.FLAG_ITEM_KEY));
    }

    @EventHandler
    public void onFlagDrop(PlayerDropItemEvent event){
        //Are we dropping the flag? if so cancel it, once you get the flag you either die with it or finish the job
        event.setCancelled(event.getItemDrop().getItemStack().getPersistentDataContainer().has(GameManager.FLAG_ITEM_KEY));
    }

    @EventHandler
    public void onPlayerInventoryClick(InventoryClickEvent event){
        //Check if this player is holding any flag item from any team - quicker to do a boolean check then to check the hotbar
        teamManager.getTeams().forEach(team -> {
            if(team.getHoldingFlag() != event.getWhoClicked()) return;

            //This player is holding a flag - they're not allowed to click anything, or they might mess with the flag (accounted for, sure, but isn't a good look)
            //NOTE: If the player is in creative this check will be buggy, Minecraft doesn't handle creative well internally, can't do much about it (out of scope for the plugin)
            event.setCancelled(true);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        //Validation not needed - done inside Team#dropFlag
        teamManager.getTeams().forEach(team -> team.dropFlag(event.getPlayer()));

        //Player quit - remove them from the game
        teamManager.getPlayerTeam(event.getPlayer().getUniqueId()).ifPresent(team -> team.removeMember(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){
        teamManager.getTeams().forEach(team -> {
            if(team.getHoldingFlag() != event.getPlayer()) return;

            event.getDrops().clear();
        });

        teamManager.getTeams().forEach(team -> team.dropFlag(event.getPlayer()));
    }

}
