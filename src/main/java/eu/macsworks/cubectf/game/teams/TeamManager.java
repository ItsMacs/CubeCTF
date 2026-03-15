package eu.macsworks.cubectf.game.teams;

import eu.macsworks.cubectf.CubeCTF;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamManager {

    private final Map<String, Team> teams = new HashMap<>();

    public TeamManager(CubeCTF plugin) {
        //Magic implementation, although in-scope as this is a simple and quick plugin
        //could be genericized with a teams.yml config file and serialization
        addTeam(Team.of(plugin, "red", "<red><bold>RED", Material.RED_WOOL));
        addTeam(Team.of(plugin, "blue", "<blue><bold>BLUE", Material.BLUE_WOOL));
    }

    public void init(){
        teams.values().forEach(Team::init);
    }

    public void addTeam(Team team){
        teams.put(team.getName(), team);
    }

    public void removeTeam(Team team){
        teams.remove(team.getName());
    }

    public Optional<Team> getTeam(String name) {
        return Optional.ofNullable(teams.get(name));
    }

    public Optional<Team> getPlayerTeam(Player player){
        return getPlayerTeam(player.getUniqueId());
    }

    public Optional<Team> getPlayerTeam(UUID uuid) {
        return teams.values().stream().filter(team -> team.isMember(uuid)).findFirst();
    }

    /**
     * @return Returns an immutable collection of all teams
     */
    public Collection<Team> getTeams(){
        return teams.values();
    }

}
