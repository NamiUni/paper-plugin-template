package io.github.namiuni.paperplugintemplate.common.command;

import io.github.namiuni.paperplugintemplate.common.command.commands.CommandFactory;
import jakarta.inject.Inject;
import java.util.Set;
import org.incendo.cloud.CommandManager;
import org.jspecify.annotations.NullMarked;

// TODO: Javadoc
@NullMarked
public final class CommandRegistrar {

    private final CommandManager<CommandSource> commandManager;
    private final Set<CommandFactory> commandFactories;

    @Inject
    private CommandRegistrar(
            final CommandManager<CommandSource> commandManager,
            final Set<CommandFactory> commandFactories
    ) {
        this.commandManager = commandManager;
        this.commandFactories = commandFactories;
    }

    // TODO: Javadoc
    public void registerCommands() {
        this.commandFactories.forEach(factory -> this.commandManager.command(factory.command()));
    }
}
