package io.github.namiuni.paperplugintemplate.common.command;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record UserCommander(Audience audience, PluginTemplateUser user) implements Commander, ForwardingAudience.Single {
}
