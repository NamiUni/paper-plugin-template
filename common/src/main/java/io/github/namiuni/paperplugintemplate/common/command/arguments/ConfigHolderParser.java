package io.github.namiuni.paperplugintemplate.common.command.arguments;

import io.github.namiuni.paperplugintemplate.common.command.CommandSource;
import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.ConfigHolder;
import io.leangen.geantyref.TypeToken;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ConfigHolderParser implements
        ArgumentParser.FutureArgumentParser<CommandSource, ConfigHolder<?>>,
        SuggestionProvider<CommandSource>,
        ParserDescriptor<CommandSource, ConfigHolder<?>> {

    private final Set<ConfigHolder<?>> configurations;

    @Inject
    public ConfigHolderParser(final Set<ConfigHolder<?>> configurations) {
        this.configurations = configurations;
    }

    @Override
    public CompletableFuture<ArgumentParseResult<ConfigHolder<?>>> parseFuture(
            final CommandContext<CommandSource> commandContext,
            final CommandInput commandInput
    ) {
        final String input = commandInput.readString();

        final Optional<ConfigHolder<?>> target = this.configurations.stream()
                .filter(config -> config.configName().equalsIgnoreCase(input))
                .findFirst();
        return CompletableFuture.completedFuture(
                target.<ArgumentParseResult<ConfigHolder<?>>>map(ArgumentParseResult::success)
                        .orElseGet(() -> ArgumentParseResult.failure(new IllegalArgumentException("Configuration not found: " + input)))
        );
    }

    @Override
    public CompletableFuture<Iterable<Suggestion>> suggestionsFuture(
            final CommandContext<CommandSource> context,
            final CommandInput input
    ) {
        return CompletableFuture.completedFuture(
                this.configurations.stream()
                        .map(ConfigHolder::configName)
                        .map(Suggestion::suggestion)
                        .toList()
        );
    }

    @Override
    public ArgumentParser<CommandSource, ConfigHolder<?>> parser() {
        return this;
    }

    @Override
    public TypeToken<ConfigHolder<?>> valueType() {
        return new TypeToken<>() {
        };
    }
}
