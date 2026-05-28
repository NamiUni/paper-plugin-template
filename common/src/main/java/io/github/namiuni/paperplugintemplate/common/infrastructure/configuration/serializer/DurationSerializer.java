package io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.serializer;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

@NullMarked
public final class DurationSerializer implements TypeSerializer<Duration> {

    public static final DurationSerializer INSTANCE = new DurationSerializer();

    private static final Pattern PATTERN =
            Pattern.compile("^(?:(?<d>\\d+)d)?(?:(?<h>\\d+)h)?(?:(?<m>\\d+)m)?(?:(?<s>\\d+)s)?(?:(?<ms>\\d+)ms)?$");

    private DurationSerializer() {
    }

    @Override
    public Duration deserialize(final Type type, final ConfigurationNode node) throws SerializationException {
        final String raw = node.getString();
        if (raw == null || raw.isBlank()) {
            throw new SerializationException(node, Duration.class, "Duration must not be blank");
        }

        String trimmed = raw.trim();
        if (Objects.equals("0", trimmed)) {
            return Duration.ZERO;
        }

        final boolean isNegative = trimmed.startsWith("-");
        if (isNegative) {
            trimmed = trimmed.substring(1);
        }

        final Matcher matcher = PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new SerializationException(
                    node,
                    Duration.class,
                    "Invalid duration: '%s'. Use units d/h/m/s/ms.".formatted(raw)
            );
        }

        try {
            Duration duration = Duration.ZERO;
            if (matcher.group("d") != null) {
                duration = duration.plusDays(Long.parseLong(matcher.group("d")));
            }
            if (matcher.group("h") != null) {
                duration = duration.plusHours(Long.parseLong(matcher.group("h")));
            }
            if (matcher.group("m") != null) {
                duration = duration.plusMinutes(Long.parseLong(matcher.group("m")));
            }
            if (matcher.group("s") != null) {
                duration = duration.plusSeconds(Long.parseLong(matcher.group("s")));
            }
            if (matcher.group("ms") != null) {
                duration = duration.plusMillis(Long.parseLong(matcher.group("ms")));
            }

            return isNegative ? duration.negated() : duration;

        } catch (final NumberFormatException _) {
            throw new SerializationException(node, Duration.class, "Duration value is too large and cannot be parsed: '%s'".formatted(raw));
        }
    }

    @Override
    public void serialize(final Type type, final @Nullable Duration duration, final ConfigurationNode node) throws SerializationException {
        if (duration == null) {
            node.set(null);
            return;
        }
        node.set(format(duration));
    }

    private static String format(final Duration duration) {
        if (duration.isZero()) {
            return "0s";
        }

        final StringBuilder sb = new StringBuilder();

        if (duration.isNegative()) {
            sb.append('-');
        }

        final Duration absDuration = duration.abs();

        final long d = absDuration.toDays();
        final long h = absDuration.toHoursPart();
        final long m = absDuration.toMinutesPart();
        final long s = absDuration.toSecondsPart();
        final long ms = absDuration.toMillisPart();

        if (d > 0) {
            sb.append(d).append('d');
        }
        if (h > 0) {
            sb.append(h).append('h');
        }
        if (m > 0) {
            sb.append(m).append('m');
        }
        if (s > 0) {
            sb.append(s).append('s');
        }
        if (ms > 0) {
            sb.append(ms).append("ms");
        }

        return sb.toString();
    }
}
