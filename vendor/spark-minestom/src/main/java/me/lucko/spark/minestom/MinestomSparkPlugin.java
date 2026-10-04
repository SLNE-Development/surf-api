/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.minestom;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.stream.Stream;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.common.util.SparkThreadFactory;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionCallback;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MinestomSparkPlugin implements SparkPlugin {

  public static final String COMMAND_NAME = "spark";

  private static final String VERSION = readVersion();

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
      4,
      new SparkThreadFactory()
  );

  private final Logger logger = LoggerFactory.getLogger("spark");

  private final Path directory;
  private final PermissionChecker permissionChecker;

  private @Nullable SparkPlatform platform;
  private @Nullable MinestomSparkCommand command;
  private boolean disabled;

  public MinestomSparkPlugin(Path directory) {
    this(directory, PermissionChecker.DEFAULT);
  }

  public MinestomSparkPlugin(Path directory, PermissionChecker permissionChecker) {
    this.directory = requireNonNull(directory, "directory");
    this.permissionChecker = requireNonNull(permissionChecker, "permissionChecker");
  }

  /**
   * Enables spark and registers the {@code /spark} command.
   *
   * <p>Must be called after {@link MinecraftServer#init()}. </p>
   */
  public void enable() {
    if (this.platform != null) {
      throw new IllegalStateException("spark has already been enabled");
    }

    this.platform = new SparkPlatform(this);
    this.platform.enable();
    this.command = new MinestomSparkCommand(this.platform, this.permissionChecker);
    MinecraftServer.getCommandManager().register(this.command);
  }

  /**
   * Disables spark and unregisters the {@code /spark} command.
   */
  public void disable() {
    if (this.platform == null || this.disabled) {
      return;
    }
    this.disabled = true;

    assert this.command != null : "command should not be null if platform is not null";

    this.platform.disable();
    MinecraftServer.getCommandManager().unregister(this.command);
    this.scheduler.shutdown();
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public Path getPluginDirectory() {
    return this.directory;
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  public Stream<me.lucko.spark.common.command.sender.CommandSender> getCommandSenders() {
    return Stream.concat(
        MinecraftServer.getConnectionManager().getOnlinePlayers().stream(),
        Stream.of(MinecraftServer.getCommandManager().getConsoleSender())
    ).map(sender -> new MinestomCommandSender(sender, this.permissionChecker));
  }

  @Override
  public void executeAsync(Runnable task) {
    this.scheduler.execute(task);
  }

  @Override
  public void log(Level level, String msg) {
    if (level.intValue() >= Level.SEVERE.intValue()) {
      this.logger.error(msg);
    } else if (level.intValue() >= Level.WARNING.intValue()) {
      this.logger.warn(msg);
    } else {
      this.logger.info(msg);
    }
  }

  @Override
  public void log(Level level, String msg, Throwable throwable) {
    if (level.intValue() >= Level.SEVERE.intValue()) {
      this.logger.error(msg, throwable);
    } else if (level.intValue() >= Level.WARNING.intValue()) {
      this.logger.warn(msg, throwable);
    } else {
      this.logger.info(msg, throwable);
    }
  }

  @Override
  public PlatformInfo getPlatformInfo() {
    return new MinestomPlatformInfo();
  }

  @Override
  public PlayerPingProvider createPlayerPingProvider() {
    return new MinestomPlayerPingProvider();
  }

  @Override
  public TickReporter createTickReporter() {
    return new MinestomTickReporter();
  }

  @Override
  public TickHook createTickHook() {
    return new MinestomTickHook();
  }

  private static String readVersion() {
    try (final InputStream in =
        MinestomSparkPlugin.class.getResourceAsStream("spark-minestom.properties")) {
      if (in == null) {
        return "unknown";
      }

      final Properties properties = new Properties();
      properties.load(in);

      return properties.getProperty("version", "unknown");
    } catch (final IOException e) {
      return "unknown";
    }
  }

  private static final class MinestomSparkCommand extends Command implements CommandExecutor,
      SuggestionCallback {

    private final SparkPlatform platform;
    private final PermissionChecker permissionChecker;

    public MinestomSparkCommand(SparkPlatform platform, PermissionChecker permissionChecker) {
      requireNonNull(platform, "platform");
      requireNonNull(permissionChecker, "permissionChecker");

      super(COMMAND_NAME);
      this.platform = platform;
      this.permissionChecker = permissionChecker;

      final ArgumentStringArray arrayArgument = ArgumentType.StringArray("args");
      arrayArgument.setSuggestionCallback(this);

      addSyntax(this, arrayArgument);
      setDefaultExecutor((sender, _) -> platform.executeCommand(wrap(sender), new String[0]));
    }

    @Override
    public void apply(CommandSender sender, CommandContext context) {
      requireNonNull(sender, "sender");
      requireNonNull(context, "context");

      final String[] args = processArgs(context, false);
      if (args == null) {
        return;
      }

      this.platform.executeCommand(wrap(sender), args);
    }

    @Override
    public void apply(CommandSender sender, CommandContext context, Suggestion suggestion) {
      requireNonNull(sender, "sender");
      requireNonNull(context, "context");
      requireNonNull(suggestion, "suggestion");

      final String[] args = processArgs(context, true);
      if (args == null) {
        return;
      }

      final Iterable<String> suggestionEntries = this.platform.tabCompleteCommand(wrap(sender),
          args);
      for (final String suggestionEntry : suggestionEntries) {
        suggestion.addEntry(new SuggestionEntry(suggestionEntry));
      }
    }

    private MinestomCommandSender wrap(CommandSender sender) {
      return new MinestomCommandSender(sender, this.permissionChecker);
    }

    private static String @Nullable [] processArgs(CommandContext context, boolean tabComplete) {
      final String[] split = context.getInput().split(" ", tabComplete ? -1 : 0);
      if (split.length == 0 || !split[0].equals("/" + COMMAND_NAME)
          && !split[0].equals(COMMAND_NAME)) {
        return null;
      }

      return Arrays.copyOfRange(split, 1, split.length);
    }
  }
}
