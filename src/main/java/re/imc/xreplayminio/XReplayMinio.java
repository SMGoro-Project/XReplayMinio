package re.imc.xreplayminio;

import de.musterbukkit.replaysystem.main.ReplayAPI;
import io.minio.MinioClient;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import re.imc.xreplayminio.command.ReplayLoadCommand;
import re.imc.xreplayminio.listener.ReplayListener;

import java.io.File;

public final class XReplayMinio extends JavaPlugin {

    private static XReplayMinio instance;

    private static MinioClient client;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;

        loadMinio();
        if (getConfig().getBoolean("use-default-world", false)) {
            ReplayAPI.setMapName(Bukkit.getWorlds().get(0).getName());
        }
        Bukkit.getPluginManager().registerEvents(new ReplayListener(), this);
        Bukkit.getPluginCommand("replayload").setExecutor(new ReplayLoadCommand());
        deleteDir(new File(XReplayMinio.getInstance().getConfig()
                    .getString("target-folder")));
    }

    public static void deleteDir(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles())
                deleteDir(f);
        }
        file.delete();
    }
    public void loadMinio() {
        ConfigurationSection section = getConfig().getConfigurationSection("minio");
        client = MinioClient.builder()
                .endpoint(section.getString("url"))
                .credentials(section.getString("accessKey"), section.getString("secretKey"))
                .build();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        ReplayAPI.saveReplay();
        try {
            Thread.sleep(1000 * 6);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        File folder = new File(XReplayMinio.getInstance().getConfig()
                .getString("target-folder"));
        File[] files = folder.listFiles();
        if (files != null && files.length > 0) {
            getLogger().info("Finally Replay");
            while (true) {
                try {
                    if (ReplayListener.uploadFile(folder.getPath(), files[0].getName(), true)) break;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                XReplayMinio.getInstance().getLogger().info("Compressing...");
            }
        }
    }

    public static XReplayMinio getInstance() {
        return instance;
    }

    public static MinioClient getClient() {
        return client;
    }
}
