/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 * Contributors []
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.namiuni.paperplugintemplate.common.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.namiuni.paperplugintemplate.common.command.commands.CommandFactory;
import java.util.Set;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.component.CommandComponent;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@NullMarked
@ExtendWith(MockitoExtension.class)
class CommandRegistrarTest {

    @Mock
    @SuppressWarnings("rawtypes")
    private CommandManager rawManager;

    @SuppressWarnings("unchecked")
    private CommandManager<CommandSource> manager() {
        return this.rawManager;
    }

    private CommandFactory factory1;
    private CommandFactory factory2;

    @SuppressWarnings("unchecked")
    private Command<CommandSource> newCommand(final String rootName) {
        final Command<CommandSource> command = mock(Command.class);
        final CommandComponent<CommandSource> root = mock(CommandComponent.class);
        when(command.rootComponent()).thenReturn(root);
        when(root.name()).thenReturn(rootName);
        return command;
    }

    // ── registerCommands: factory delegation ──────────────────────────────────

    @Test
    void registerCommandsInvokesCreateCommandOnSingleFactory() {
        final Command<CommandSource> command = this.newCommand("test");
        this.factory1 = mock(CommandFactory.class);
        when(this.factory1.createCommand()).thenReturn(command);

        new CommandRegistrar(this.manager(), Set.of(this.factory1), mock(ComponentLogger.class))
                .registerCommands();

        verify(this.factory1).createCommand();
    }

    @Test
    void registerCommandsInvokesCreateCommandOnAllFactories() {
        final Command<CommandSource> c1 = this.newCommand("cmd1");
        final Command<CommandSource> c2 = this.newCommand("cmd2");
        this.factory1 = mock(CommandFactory.class);
        this.factory2 = mock(CommandFactory.class);
        when(this.factory1.createCommand()).thenReturn(c1);
        when(this.factory2.createCommand()).thenReturn(c2);

        new CommandRegistrar(this.manager(), Set.of(this.factory1, this.factory2), mock(ComponentLogger.class))
                .registerCommands();

        verify(this.factory1).createCommand();
        verify(this.factory2).createCommand();
    }

    // ── registerCommands: manager registration ────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void registerCommandsPassesCommandToManager() {
        final Command<CommandSource> command = this.newCommand("reload");
        this.factory1 = mock(CommandFactory.class);
        when(this.factory1.createCommand()).thenReturn(command);

        new CommandRegistrar(this.manager(), Set.of(this.factory1), mock(ComponentLogger.class))
                .registerCommands();

        verify(this.rawManager).command(command);
    }

    @Test
    @SuppressWarnings("unchecked")
    void registerCommandsPassesAllCommandsToManager() {
        final Command<CommandSource> c1 = this.newCommand("cmd1");
        final Command<CommandSource> c2 = this.newCommand("cmd2");
        this.factory1 = mock(CommandFactory.class);
        this.factory2 = mock(CommandFactory.class);
        when(this.factory1.createCommand()).thenReturn(c1);
        when(this.factory2.createCommand()).thenReturn(c2);

        new CommandRegistrar(this.manager(), Set.of(this.factory1, this.factory2), mock(ComponentLogger.class))
                .registerCommands();

        verify(this.rawManager).command(c1);
        verify(this.rawManager).command(c2);
    }

    // ── registerCommands: empty set ───────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void registerCommandsWithEmptySetDoesNotCallManager() {
        new CommandRegistrar(this.manager(), Set.of(), mock(ComponentLogger.class))
                .registerCommands();

        verify(this.rawManager, never()).command((Command<CommandSource>) any());
    }
}
