package eu.macsworks.cubectf.game.teams;

import eu.macsworks.cubectf.CubeCTF;
import eu.macsworks.cubectf.game.GameManager;
import eu.macsworks.cubectf.utils.LoadedConfig;
import eu.macsworks.cubectf.utils.lang.Messages;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Data
public class Team {

    private final CubeCTF plugin;
    private final LoadedConfig config;
    private GameManager gameManager;
    private TeamManager teamManager;

    private Team(CubeCTF plugin, String name, String displayName, Material flagMaterial){
        this.plugin = plugin;
        this.config = plugin.getLoadedConfig();

        this.name = name;
        this.displayName = displayName;
        this.flagMaterial = flagMaterial;
    }

    public void init(){
        gameManager = plugin.getGameManager();
        teamManager = plugin.getTeamManager();
    }

    private final String name;
    private final String displayName;
    @Getter(AccessLevel.NONE) private final List<UUID> members = new ArrayList<>();
    private final Material flagMaterial;

    private int score = 0;

    private Location flagLocation;

    //Actual spawned flag - can be null if currently picked up.
    private @Nullable Interaction flagInteraction;

    private @Nullable Player holdingFlag;

    //Inventory of the player currently holding the flag - empty if nobody's holding it
    //holdingFlag's inventory becomes the flag when they have it in hand, as a balancing measure
    private List<ItemStack> holdingPlayerInventory = new ArrayList<>();

    /**
     * Gives the flag item to the player taking it, and destroys the flag block display
     * @param player Player taking the flag (ignored if in the flag's team)
     */
    public void takeFlag(Player player){
        if(isMember(player)) return;

        holdingFlag = player;
        holdingFlag.setGlowing(true);

        giveFlagToPlayer(player);

        gameManager.broadcastMessage(config.getMessage(Messages.FLAG_TAKEN)
                    .replace("{player}", player.getName())
                    .replace("{team}", displayName),
                Sound.ITEM_GOAT_HORN_SOUND_0);

        //Despawn the flag block
        if(flagLocation != null && flagLocation.getBlock().getType() == flagMaterial) {
            flagLocation.getBlock().setType(Material.AIR);
        }

        if(flagInteraction != null) {
            flagInteraction.remove();
            flagInteraction = null;
        }
    }

    /**
     * Check who is picking up the flag item (for destroying the actual flag blockdisplay, check {@link #takeFlag(Player)},
     * and replaces the flag block if a person from the flag's team took it.
     * @param player Player picking up the flag
     */
    public void pickupFlag(Player player){
        //The flag already doesn't exist - it was picked up by someone else
        //if that someone else is from this team, we need to return it to the location
        if(!isMember(player)) {
            //But this isn't from the team - they're stealing it
            takeFlag(player);
            return;
        }

        //That someone is from the team - respawn le flag
        respawnFlag(true, false);
    }

    /**
     * Checks and acts upon a complete capture of the flag (meaning the player holding this teams flag has reached
     * their own team base) and if so, updates scoring.
     * @return Whether the flag was captured successfully by another team.
     */
    public boolean checkFlagCapture(){
        if(holdingFlag == null) return false;

        Team playerTeam = teamManager.getPlayerTeam(holdingFlag).orElseThrow(() -> new IllegalStateException("Player holding flag is not in a team"));

        //Player still hasn't reached their base
        if(holdingFlag.getLocation().distanceSquared(playerTeam.getFlagLocation()) > config.getBaseRadiusSquared()) return false;

        flagCaptured();
        return true;
    }

    public void flagCaptured(){
        if(holdingFlag == null) return;

        Optional<Team> playerTeam = teamManager.getPlayerTeam(holdingFlag);
        if(playerTeam.isEmpty()) return;

        playerTeam.get().addScore(config.getCapturePoints());

        giveBackPlayerInventory(holdingFlag);

        respawnFlag(false, true);
    }


    /**
     * Drops the flag and cleans up the player holding it
     * @param player Player dropping the flag
     */
    public void dropFlag(Player player){
        if(holdingFlag != player) return;

        player.getInventory().clear();
        giveBackPlayerInventory(player);

        holdingFlag = null;
        player.setGlowing(false);

        //Drop the actual flag item, set it glowing, and give it the data in the NBT/PDC to identify it easily
        Item droppedFlag = player.getWorld().dropItem(player.getLocation(), makeFlagItem());
        droppedFlag.setUnlimitedLifetime(true);
        droppedFlag.setCanMobPickup(false);
        droppedFlag.setCanPlayerPickup(true);
        droppedFlag.setGlowing(true);
        droppedFlag.getPersistentDataContainer().set(GameManager.FLAG_ITEM_KEY, PersistentDataType.STRING, name);
    }

    /**
     * Respawns the flag at its spawn location, and broadcasts the correct message depending on the reason its getting respawned
     * @param recaptured Whether the flag is respawned as a result of a recapture (from the same team)
     * @param captured Same as above, but for a capture (from the other team)
     */
    public void respawnFlag(boolean recaptured, boolean captured){
        holdingFlag = null;

        flagLocation.getBlock().setType(flagMaterial);

        flagInteraction = flagLocation.getWorld().spawn(flagLocation.clone().subtract(0, 1, 0), Interaction.class, entity -> {
            entity.setInteractionWidth(2.5F);
            entity.setInteractionHeight(2.5f);
            entity.setResponsive(true);
            entity.setGravity(false);
            entity.setNoPhysics(true);
        });

        flagInteraction.getPersistentDataContainer().set(GameManager.FLAG_BLOCK_KEY, PersistentDataType.STRING, name);

        if(recaptured) gameManager.broadcastMessage(config.getMessage(Messages.FLAG_RECAPTURED)
                        .replace("{team}", displayName), Sound.ITEM_GOAT_HORN_SOUND_0);

        if(captured) gameManager.broadcastMessage(config.getMessage(Messages.FLAG_CAPTURED)
                .replace("{team}", displayName), Sound.ITEM_GOAT_HORN_SOUND_0);
    }

    /**
     * Set the player's hotbar to the flag - a bit nicer than having one random item.
     * The player previous inventory is stored in {@link #holdingPlayerInventory}
    */
    private void giveFlagToPlayer(Player player){
        holdingPlayerInventory.addAll(Arrays.asList(player.getInventory().getContents()));

        player.getInventory().clear();
        for(int i = 0; i < 9; i++) player.getInventory().setItem(i, makeFlagItem());
    }

    /**
     * Gives back the holding player's inventory
     * @param player Holding player
     */
    private void giveBackPlayerInventory(Player player){
        if(player != holdingFlag) return; //no stealing inventories

        //Remove the flag and give back the inv
        player.getInventory().remove(flagMaterial);
        player.getInventory().setContents(holdingPlayerInventory.toArray(new ItemStack[0]));

        holdingPlayerInventory.clear();
    }

    public void cleanup(){
        if(flagLocation != null && flagLocation.getWorld().getType(flagLocation) == flagMaterial) flagLocation.getWorld().setType(flagLocation, Material.AIR);
        if(flagInteraction != null) flagInteraction.remove();
        if(holdingFlag != null) holdingFlag.setGlowing(false);
        holdingPlayerInventory.clear();
        score = 0;

        flagInteraction = null;
        holdingFlag = null;

        getMembers().forEach(p -> p.getInventory().clear());
        members.clear();
    }

    private ItemStack makeFlagItem(){
        ItemStack item = new ItemStack(flagMaterial);

        item.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage().deserialize(config.getMessage(Messages.FLAG_NAME).replace("{team}", displayName)));
            meta.lore(Arrays.stream(config.getMessage(Messages.FLAG_LORE).split("\n")).map(MiniMessage.miniMessage()::deserialize).toList());
            meta.getPersistentDataContainer().set(GameManager.FLAG_ITEM_KEY, PersistentDataType.STRING, name);
        });

        return item;
    }

    public boolean isMember(Player player){
        return isMember(player.getUniqueId());
    }

    public boolean isMember(UUID uuid){
        return members.contains(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public List<Player> getMembers(){
        return members.stream().map(Bukkit::getPlayer).toList();
    }

    public void clearMembers(){
        members.clear();
    }

    public int getTeamSize(){
        return members.size();
    }

    public void addScore(int score){
        this.score += score;
    }

    public void removeScore(int score){
        this.score -= score;
    }

    public static Team of(CubeCTF plugin, String name, String displayName, Material flagMaterial){
        return new Team(plugin, name, displayName, flagMaterial);
    }

}
