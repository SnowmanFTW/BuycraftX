package net.buycraft.plugin.minestom;

import net.buycraft.plugin.BuyCraftAPI;
import net.buycraft.plugin.IBuycraftPlatform;
import net.buycraft.plugin.data.QueuedPlayer;
import net.buycraft.plugin.data.responses.ServerInformation;
import net.buycraft.plugin.execution.placeholder.PlaceholderManager;
import net.buycraft.plugin.execution.strategy.CommandExecutor;
import net.buycraft.plugin.platform.PlatformInformation;
import net.buycraft.plugin.platform.PlatformType;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MinestomBuycraftPlatform implements IBuycraftPlatform {
    private final BuycraftPlugin plugin;

    public MinestomBuycraftPlatform(BuycraftPlugin plugin){
        this.plugin = plugin;
    }

    @Override
    public BuyCraftAPI getApiClient() {
        return plugin.getApiClient();
    }

    @Override
    public PlaceholderManager getPlaceholderManager() {
        return plugin.getPlaceholderManager();
    }

    @Override
    public void dispatchCommand(String command) {
        MinecraftServer.getCommandManager().executeServerCommand(command);
    }

    @Override
    public void executeAsync(Runnable runnable) {
        MinecraftServer.getSchedulerManager().scheduleTask(runnable, TaskSchedule.immediate(), TaskSchedule.stop(), ExecutionType.ASYNC);
    }

    @Override
    public void executeAsyncLater(Runnable runnable, long time, TimeUnit unit) {
        MinecraftServer.getSchedulerManager().scheduleTask(runnable, TaskSchedule.seconds(unit.toSeconds(time)), TaskSchedule.stop(), ExecutionType.ASYNC);
    }

    @Override
    public void executeBlocking(Runnable runnable) {
        MinecraftServer.getSchedulerManager().scheduleTask(runnable, TaskSchedule.immediate(), TaskSchedule.stop(), ExecutionType.SYNC);
    }

    @Override
    public void executeBlockingLater(Runnable runnable, long time, TimeUnit unit) {
        MinecraftServer.getSchedulerManager().scheduleTask(runnable, TaskSchedule.seconds(unit.toSeconds(time)), TaskSchedule.stop(), ExecutionType.SYNC);
    }

    @Override
    public boolean isPlayerOnline(QueuedPlayer player) {
        return MinecraftServer.getConnectionManager().findPlayer(player.getName()) != null;
    }

    @Override
    public int getFreeSlots(QueuedPlayer player) {
        Player player1 = MinecraftServer.getConnectionManager().findPlayer(player.getName());
        if (player1 != null) {
            int free = 0;
            for (int i = 0; i < player1.getInventory().getSize(); i++) {
                if (player1.getInventory().getItemStack(i).isAir()) {
                    free++;
                }
            }
            return free;
        }
        return -1;
    }

    @Override
    public void log(Level level, String message) {
        MinecraftServer.LOGGER.info(message);
    }

    @Override
    public void log(Level level, String message, Throwable throwable) {
        MinecraftServer.LOGGER.info(message, throwable);
    }

    @Override
    public CommandExecutor getExecutor() {
        return plugin.getCommandExecutor();
    }

    @Override
    public PlatformInformation getPlatformInformation() {
        return new PlatformInformation(PlatformType.MINESTOM, MinecraftServer.VERSION_NAME);
    }

    @Override
    public String getPluginVersion() {
        return plugin.getOrigin().getVersion();
    }

    @Override
    public ServerInformation getServerInformation() {
        return plugin.getServerInformation();
    }

}
