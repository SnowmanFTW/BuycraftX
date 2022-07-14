package net.buycraft.plugin.minestom;

import net.buycraft.plugin.BuyCraftAPI;
import net.buycraft.plugin.IBuycraftPlatform;
import net.buycraft.plugin.data.responses.ServerInformation;
import net.buycraft.plugin.execution.DuePlayerFetcher;
import net.buycraft.plugin.execution.ServerEventSenderTask;
import net.buycraft.plugin.execution.placeholder.NamePlaceholder;
import net.buycraft.plugin.execution.placeholder.PlaceholderManager;
import net.buycraft.plugin.execution.placeholder.XuidPlaceholder;
import net.buycraft.plugin.execution.strategy.CommandExecutor;
import net.buycraft.plugin.execution.strategy.PostCompletedCommandsTask;
import net.buycraft.plugin.execution.strategy.QueuedCommandExecutor;
import net.buycraft.plugin.minestom.command.*;
import net.buycraft.plugin.minestom.gui.CategoryViewGUI;
import net.buycraft.plugin.minestom.gui.ViewCategoriesGUI;
import net.buycraft.plugin.minestom.tasks.GUIUpdateTask;
import net.buycraft.plugin.minestom.util.GUIUtil;
import net.buycraft.plugin.minestom.util.VersionCheck;
import net.buycraft.plugin.shared.Setup;
import net.buycraft.plugin.shared.config.BuycraftConfiguration;
import net.buycraft.plugin.shared.config.BuycraftI18n;
import net.buycraft.plugin.shared.tasks.ListingUpdateTask;
import net.buycraft.plugin.shared.tasks.PlayerJoinCheckTask;
import net.buycraft.plugin.shared.util.AnalyticsSend;
import net.minestom.server.MinecraftServer;
import net.minestom.server.extensions.Extension;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;
import okhttp3.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class BuycraftPlugin extends Extension {
    private final PlaceholderManager placeholderManager = new PlaceholderManager();
    private final BuycraftConfiguration configuration = new BuycraftConfiguration();
    private final File folder = new File("extensions/Buycraft");

    private BuyCraftAPI apiClient;
    private IBuycraftPlatform platform;
    private BuycraftI18n i18n;
    private OkHttpClient httpClient;
    private ServerInformation serverInformation;
    private DuePlayerFetcher duePlayerFetcher;
    private PostCompletedCommandsTask completedCommandsTask;
    private CommandExecutor commandExecutor;
    private PlayerJoinCheckTask playerJoinCheckTask;
    private ServerEventSenderTask serverEventSenderTask;
    private ListingUpdateTask listingUpdateTask;
    private ViewCategoriesGUI viewCategoriesGUI;
    private CategoryViewGUI categoryViewGUI;

    @Override
    public void initialize() {
        GUIUtil.setPlugin(this);
        platform = new MinestomBuycraftPlatform(this);

        folder.mkdir();
        Path path = folder.toPath().resolve("config.properties");
        try {
            try {
                configuration.load(path);
            } catch (NoSuchFileException e) {
                // Save defaults
                configuration.fillDefaults();
                configuration.save(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }

        i18n = configuration.createI18n();
        httpClient = Setup.okhttp(new File("extensions/Buycraft", "cache"));

        final String serverKey = configuration.getServerKey();
        if (serverKey == null || serverKey.equals("INVALID")) {
            MinecraftServer.LOGGER.info("Looks like this is a fresh setup. Get started by using 'tebex secret <key>' in the console.");
        } else {
            MinecraftServer.LOGGER.info("Validating your server key...");
            final BuyCraftAPI client = BuyCraftAPI.create(configuration.getServerKey(), httpClient);
            try {
                updateInformation(client);
            } catch (IOException e) {
                MinecraftServer.LOGGER.error(String.format("We can't check if your server can connect to Tebex: %s", e.getMessage()));
            }
            apiClient = client;
        }

        if (configuration.isCheckForUpdates()) {
            final VersionCheck check = new VersionCheck(this, getOrigin().getVersion(), configuration.getServerKey());
            try {
                check.verify();
            } catch (IOException e) {
                MinecraftServer.LOGGER.error("Can't check for updates", e);
            }
        }

        // Initialize placeholders.
        placeholderManager.addPlaceholder(new NamePlaceholder());
        placeholderManager.addPlaceholder(new XuidPlaceholder());

        // Queueing tasks.
        platform.executeAsyncLater(duePlayerFetcher = new DuePlayerFetcher(platform, configuration.isVerbose()), 1, TimeUnit.SECONDS);
        completedCommandsTask = new PostCompletedCommandsTask(platform);
        commandExecutor = new QueuedCommandExecutor(platform, completedCommandsTask);
        MinecraftServer.getSchedulerManager().scheduleTask(completedCommandsTask, TaskSchedule.seconds(1), TaskSchedule.seconds(1));
        MinecraftServer.getSchedulerManager().scheduleTask((Runnable) commandExecutor, TaskSchedule.seconds(1), TaskSchedule.seconds(1));
        playerJoinCheckTask = new PlayerJoinCheckTask(platform);
        MinecraftServer.getSchedulerManager().scheduleTask(playerJoinCheckTask, TaskSchedule.tick(1), TaskSchedule.tick(1));
        serverEventSenderTask = new ServerEventSenderTask(platform, configuration.isVerbose());
        MinecraftServer.getSchedulerManager().scheduleTask(serverEventSenderTask, TaskSchedule.minutes(1), TaskSchedule.minutes(1));

        // Register listeners.
        new BuycraftListener(this);

        BuycraftCommand command = new BuycraftCommand(this);
        command.getSubcommandMap().put("forcecheck", new ForceCheckSubcommand(this));
        command.getSubcommandMap().put("secret", new SecretSubcommand(this));
        command.getSubcommandMap().put("info", new InformationSubcommand(this));
        command.getSubcommandMap().put("report", new ReportSubcommand(this));
        command.getSubcommandMap().put("coupon", new CouponSubcommand(this));
        MinecraftServer.getCommandManager().register(command);

        viewCategoriesGUI = new ViewCategoriesGUI(this);
        categoryViewGUI = new CategoryViewGUI(this);

        listingUpdateTask = new ListingUpdateTask(platform, () -> {
            MinecraftServer.getSchedulerManager().scheduleTask(new GUIUpdateTask(this), TaskSchedule.immediate(), TaskSchedule.stop());
        });

        if (apiClient != null) {
            MinecraftServer.LOGGER.info("Fetching all server packages...");
            try {
                // for a first synchronous run
                listingUpdateTask.run();

                // Update GUIs too.
                viewCategoriesGUI.update();
                categoryViewGUI.update();
            } catch (Exception e) {
                MinecraftServer.LOGGER.error("Unable to fetch server packages", e);
            }
        }
        MinecraftServer.getSchedulerManager().scheduleTask(listingUpdateTask, TaskSchedule.minutes(10), TaskSchedule.minutes(10), ExecutionType.ASYNC);

        if (serverInformation != null) {
            MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                try {
                    AnalyticsSend.postServerInformation(httpClient, serverKey, platform, false);
                } catch (IOException e) {
                    MinecraftServer.LOGGER.warn("Can't send analytics", e);
                }
            }, TaskSchedule.immediate(), TaskSchedule.hours(24), ExecutionType.ASYNC);
        }
    }

    @Override
    public void terminate() {
        if (completedCommandsTask != null) {
            completedCommandsTask.flush();
        }
    }

    public BuyCraftAPI getApiClient() {
        return this.apiClient;
    }

    public void updateInformation(BuyCraftAPI client) throws IOException {
        serverInformation = client.getServerInformation().execute().body();
    }

    public void saveConfiguration() throws IOException {
        Path configPath = folder.toPath().resolve("config.properties");
        configuration.save(configPath);
    }

    public PlaceholderManager getPlaceholderManager() {
        return this.placeholderManager;
    }

    public BuycraftConfiguration getConfiguration() {
        return this.configuration;
    }

    public void setApiClient(final BuyCraftAPI apiClient) {
        this.apiClient = apiClient;
    }

    public DuePlayerFetcher getDuePlayerFetcher() {
        return this.duePlayerFetcher;
    }

    public ServerInformation getServerInformation() {
        return this.serverInformation;
    }

    public OkHttpClient getHttpClient() {
        return this.httpClient;
    }

    public IBuycraftPlatform getPlatform() {
        return this.platform;
    }

    public ListingUpdateTask getListingUpdateTask() {
        return this.listingUpdateTask;
    }

    public ViewCategoriesGUI getViewCategoriesGUI() {
        return this.viewCategoriesGUI;
    }

    public net.buycraft.plugin.minestom.gui.CategoryViewGUI getCategoryViewGUI() {
        return this.categoryViewGUI;
    }

    public CommandExecutor getCommandExecutor() {
        return this.commandExecutor;
    }

    public BuycraftI18n getI18n() {
        return this.i18n;
    }

    public PlayerJoinCheckTask getPlayerJoinCheckTask() {
        return this.playerJoinCheckTask;
    }

    public ServerEventSenderTask getServerEventSenderTask() {
        return serverEventSenderTask;
    }

    public File getFolder(){
        return folder;
    }
}
