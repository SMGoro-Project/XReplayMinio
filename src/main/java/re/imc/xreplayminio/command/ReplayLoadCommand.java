package re.imc.xreplayminio.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.musterbukkit.replaysystem.main.ReplayAPI;
import io.minio.GetObjectArgs;
import io.minio.errors.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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

        CompletableFuture.runAsync(() -> {

            File target = new File(XReplayMinio.getInstance().getConfig()
                    .getString("target-folder"));

            if (!target.exists()) {
                try {
                    Files.createDirectories(target.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            Path filePath = target.toPath().resolve(id);

            String bucketName = XReplayMinio.getInstance().getConfig()
                    .getString("bucket");

            String fileName = XReplayMinio.getInstance().getConfig()
                    .getString("path") + id;


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
            if (commandSender instanceof Player) {
                ReplayAPI.playReplayID(strings[0], (Player) commandSender);
            }
        });


        return true;
    }
}
