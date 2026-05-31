package io.github.namiuni.paperplugintemplate.common.command;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record SimpleCommander(Audience audience) implements Commander, ForwardingAudience.Single {
}
