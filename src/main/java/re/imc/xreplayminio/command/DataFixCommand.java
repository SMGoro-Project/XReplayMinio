package re.imc.xreplayminio.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import re.imc.xreplayextendapi.XReplayExtendAPI;
import re.imc.xreplayextendapi.data.model.ReplayIndex;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DataFixCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof ConsoleCommandSender) {
            CompletableFuture.runAsync(() -> {
                try {
                    List<ReplayIndex> replays = XReplayExtendAPI.getInstance().getReplayDataManager()
                            .getReplayIndexDao().queryForAll();
                    for (ReplayIndex index : replays) {
                        if (index.chunkLoc().startsWith("Worlds: ")) {
                            String[] data = index.chunkLoc().substring(8).split(";");
                            String newData = String.join(";", new HashSet<>(Arrays.asList(data))) + ";";
                            XReplayExtendAPI.getInstance()
                                    .getReplayDataManager()
                                    .getReplayIndexDao()
                                    .update(index.chunkLoc("Worlds: " + newData));
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }).whenCompleteAsync((v, t) -> {
                commandSender.sendMessage("DONE");
            });
        }
        return false;
    }
}
