package re.imc.xreplayminio.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.musterbukkit.replaysystem.main.ReplayAPI;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import re.imc.xreplayminio.XReplayMinio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ReplayLoadCommand implements CommandExecutor {

    Cache<CommandSender, Boolean> cache = CacheBuilder.newBuilder().expireAfterWrite(3, TimeUnit.SECONDS).build();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if (cache.getIfPresent(commandSender) != null) {
            return false;
        }

        cache.put(commandSender, true);



        String id = strings[0] + ".XREPLAY.gz";


        String bucketName = XReplayMinio.getInstance().getConfig()
                .getString("bucket");


        XReplayMinio.getInstance().getLogger().info("start get");

        CompletableFuture.runAsync(() -> {
            String fileName = XReplayMinio.getInstance().getConfig()
                    .getString("path") + id;
            boolean found = false;
            try {
                XReplayMinio.getClient().statObject(StatObjectArgs.builder().bucket(bucketName)
                        .object(fileName).build());
                found = true;
            } catch (Exception e) {
            }


            File target = new File(XReplayMinio.getInstance().getConfig()
                    .getString("target-folder"));

            Path filePath = target.toPath().resolve(id);
            if (!found) {
                XReplayMinio.getInstance().getLogger().info("zip");
                fileName = XReplayMinio.getInstance().getConfig()
                        .getString("path") + id + ".zip";
                filePath = target.toPath().resolve(id + ".zip");
            }


            if (!target.exists()) {
                try {
                    Files.createDirectories(target.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }


            try (InputStream stream = XReplayMinio.getClient().getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build())) {
                Files.copy(stream, filePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
                     InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException |
                     XmlParserException e) {
                e.printStackTrace();
            }
        }).whenCompleteAsync((v, t) -> {
            if (t != null) {
                t.printStackTrace();
                commandSender.sendMessage("ERROR");
                return;
            }
            if (commandSender instanceof Player) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ReplayAPI.playReplayID(strings[0], (Player) commandSender);
                    }
                }.runTask(XReplayMinio.getInstance());
            }
        });


        return true;
    }
}
