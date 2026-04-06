package io.github.namiuni.paperplugintemplate.common;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record PluginMetadata(
        String name,
        String displayName,
        String namespace,
        String version
) {
}
