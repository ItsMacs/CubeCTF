package eu.macsworks.cubectf.utils;

import eu.macsworks.cubectf.CubeCTF;
import eu.macsworks.cubectf.utils.lang.Messages;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Getter @RequiredArgsConstructor
public class LoadedConfig {

    private final CubeCTF instance;

    private double baseRadiusSquared;
    private int capturePoints;
    private int winPoints;
    private long matchTimeMs;
    private long startTimeMs;
    private long endTimeMs;
    private int teamSize;

    @Getter(AccessLevel.NONE) private final Map<Messages, String> messages = new HashMap<>();

    public void loadConfig(){
        instance.saveDefaultConfig();

        baseRadiusSquared = Math.pow(instance.getConfig().getDouble("team-base-radius", 5), 2);

        capturePoints = instance.getConfig().getInt("capture-points", 1);
        winPoints = instance.getConfig().getInt("win-points", 3);
        matchTimeMs = instance.getConfig().getInt("match-time", 600) * 1000L;
        startTimeMs = instance.getConfig().getInt("start-time", 10) * 1000L;
        endTimeMs = instance.getConfig().getInt("end-time", 10) * 1000L;
        teamSize = instance.getConfig().getInt("team-size", 5);
    }

    public void loadMessages(){
        File langFile = new File(instance.getDataFolder(), "lang.yml");
        if(!langFile.exists()) instance.saveResource("lang.yml", false);

        YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
        langConfig.getKeys(false).forEach(key -> messages.put(Messages.valueOf(key.toUpperCase().replace("-", "_")), langConfig.getString(key)));
    }

    public String getMessage(Messages key){
        if(!messages.containsKey(key)) return "";

        return messages.get(key);
    }
}
