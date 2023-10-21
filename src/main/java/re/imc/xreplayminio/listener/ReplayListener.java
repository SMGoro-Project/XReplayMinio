package re.imc.xreplayminio.listener;

import de.musterbukkit.replaysystem.main.ReplaySaveEvent;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import re.imc.xreplayextendapi.spigot.events.ReplayDeleteEvent;
import re.imc.xreplayminio.XReplayMinio;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ReplayListener implements Listener {

    public static Map<String, String> lastFileSize = new ConcurrentHashMap<>();

    @EventHandler
    public void onReplaySave(ReplaySaveEvent event) {

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (uploadFile(event.getFilepath(), event.getReplayID(), false)) {
                        this.cancel();
                    } else {
                        XReplayMinio.getInstance().getLogger().info("Compressing...");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }.runTaskTimerAsynchronously(XReplayMinio.getInstance(), 20, 20);
    }

    @EventHandler
    public void onReplayDelete(ReplayDeleteEvent event) {
        String id = event.getReplayId() + ".XREPLAY.gz";

        String bucketName = XReplayMinio.getInstance().getConfig()
                .getString("bucket");

        String fileName = XReplayMinio.getInstance().getConfig()
                .getString("path") + id;
        XReplayMinio.getInstance().getLogger().info("deleted " + event.getReplayId());
        try {
            XReplayMinio.getClient().removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean uploadFile(String path, String replayID, boolean isFileName) throws InterruptedException {

        String id = isFileName ? replayID : replayID + ".XREPLAY.gz";


        File file = new File(path, id);
        if (!file.exists()) {
            id = isFileName ? replayID : replayID + ".XREPLAY.gz.zip";
            file = new File(path, id);
        }
        if (!file.exists()) {
            return false;
        }

        String bucketName = XReplayMinio.getInstance().getConfig()
                .getString("bucket");

        String fileName = XReplayMinio.getInstance().getConfig()
                .getString("path") + id;

        if (!isCompressionCompletion(file)) {
            return false;
        }

        try (InputStream inputStream = Files.newInputStream(file.toPath(), StandardOpenOption.READ)) {

            XReplayMinio.getClient().putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, inputStream.available(), -1)
                            .build()
            );

        } catch (IOException | ServerException | InsufficientDataException | ErrorResponseException |
                 NoSuchAlgorithmException | InvalidKeyException | InvalidResponseException | XmlParserException |
                 InternalException e) {
            throw new RuntimeException(e);
        }
        XReplayMinio.getInstance().getLogger().info("Replay:" + replayID);
        file.delete();
        return true;
    }

    public static boolean isCompressionCompletion(File file) {

        String name = file.getName();
        try {

            String hash  = md5HashCode32(file);
            if (lastFileSize.containsKey(name)) {
                if (!Objects.equals(lastFileSize.get(name), hash)) {
                    return false;
                } else {
                    lastFileSize.remove(name);
                    return true;
                }
            } else {
                lastFileSize.put(file.getName(), hash);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().startsWith("/replay load")) {
            event.setMessage(event.getMessage().replace("replay load", "replayload"));
        }

    }



    public static String md5HashCode32(File file) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        return md5HashCode32(fis);
    }

    public static String md5HashCode32(InputStream fis) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer, 0, 1024)) != -1) {
                md.update(buffer, 0, length);
            }
            fis.close();

            byte[] md5Bytes  = md.digest();
            StringBuilder hexValue = getStringBuilder(md5Bytes);
            return hexValue.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @NotNull
    private static StringBuilder getStringBuilder(byte[] md5Bytes) {
        StringBuilder hexValue = new StringBuilder();
        for (byte md5Byte : md5Bytes) {
            int val = ((int) md5Byte) & 0xff;//解释参见最下方
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue;
    }


}
