package io.github.namiuni.paperplugintemplate.common;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import io.github.namiuni.paperplugintemplate.api.PluginTemplate;
import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUserService;
import io.github.namiuni.paperplugintemplate.common.command.commands.CommandFactory;
import io.github.namiuni.paperplugintemplate.common.command.commands.HelpCommand;
import io.github.namiuni.paperplugintemplate.common.command.commands.ReloadCommand;
import io.github.namiuni.paperplugintemplate.common.event.EventBus;
import io.github.namiuni.paperplugintemplate.common.event.SimpleEventBus;
import io.github.namiuni.paperplugintemplate.common.infrastructure.InfrastructureModule;
import io.github.namiuni.paperplugintemplate.common.user.UserServiceInternal;
import io.github.namiuni.paperplugintemplate.common.user.UserSessionHandler;
import io.github.namiuni.paperplugintemplate.common.user.UserStorageModule;
import java.nio.file.Path;
import java.time.Clock;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class CommonModule extends AbstractModule {

    private final Metadata metadata;
    private final ComponentLogger logger;
    private final Path dataDirectory;
    private final Path pluginResource;

    public CommonModule(
            final Metadata metadata,
            final ComponentLogger logger,
            final Path dataDirectory,
            final Path pluginResource
    ) {
        this.metadata = metadata;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.pluginResource = pluginResource;
    }

    @Override
    protected void configure() {
        this.bind(Metadata.class).toInstance(this.metadata);
        this.bind(Clock.class).toInstance(Clock.systemUTC());
        this.bind(EventBus.class).to(SimpleEventBus.class).in(Scopes.SINGLETON);
        this.bind(PluginTemplateUserService.class).to(UserServiceInternal.class).in(Scopes.SINGLETON);
        this.bind(PluginTemplate.class).to(PluginTemplateImpl.class).in(Scopes.SINGLETON);
        this.bind(UserSessionHandler.class).asEagerSingleton();
        this.bindCommands();

        this.install(new InfrastructureModule(this.logger, this.dataDirectory, this.pluginResource));
        this.install(new UserStorageModule());
    }

    private void bindCommands() {
        final Multibinder<CommandFactory> commands = Multibinder.newSetBinder(this.binder(), CommandFactory.class);
        commands.addBinding().to(ReloadCommand.class).in(Scopes.SINGLETON);
        commands.addBinding().to(HelpCommand.class).in(Scopes.SINGLETON);
    }
}
