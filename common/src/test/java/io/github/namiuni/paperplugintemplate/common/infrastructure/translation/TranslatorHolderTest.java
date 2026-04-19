/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 *                     Contributors []
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
package io.github.namiuni.paperplugintemplate.common.infrastructure.translation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.translation.Translator;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@NullMarked
@ExtendWith(MockitoExtension.class)
class TranslatorHolderTest {

    @Mock
    private TranslatorLoader translatorLoader;
    @Mock
    private GlobalTranslatorRegistry globalRegistry;

    private Translator firstTranslator;
    private Translator secondTranslator;
    private TranslatorHolder holder;

    @BeforeEach
    void setUp() {
        this.firstTranslator = mock(Translator.class);
        this.secondTranslator = mock(Translator.class);

        when(this.translatorLoader.loadTranslator())
                .thenReturn(this.firstTranslator)
                .thenReturn(this.secondTranslator);

        this.holder = new TranslatorHolder(
                this.translatorLoader,
                this.globalRegistry,
                mock(ComponentLogger.class)
        );
    }

    // ── construction ──────────────────────────────────────────────────────────

    @Test
    void constructionLoadsInitialTranslator() {
        verify(this.translatorLoader, times(1)).loadTranslator();
    }

    @Test
    void constructionRegistersInitialSourceWithGlobalRegistry() {
        verify(this.globalRegistry).addSource(this.firstTranslator);
    }

    @Test
    void getReturnsInitialTranslator() {
        assertSame(this.firstTranslator, this.holder.get());
    }

    @Test
    void getReturnsNonNull() {
        assertNotNull(this.holder.get());
    }

    // ── reload ────────────────────────────────────────────────────────────────

    @Test
    void reloadReturnsNewTranslator() {
        final Translator result = this.holder.reload();
        assertSame(this.secondTranslator, result);
    }

    @Test
    void reloadUpdatesGetResult() {
        this.holder.reload();
        assertSame(this.secondTranslator, this.holder.get());
    }

    @Test
    void reloadRemovesOldSourceBeforeAddingNew() {
        this.holder.reload();

        final InOrder order = inOrder(this.globalRegistry);
        order.verify(this.globalRegistry).removeSource(this.firstTranslator);
        order.verify(this.globalRegistry).addSource(this.secondTranslator);
    }

    @Test
    void reloadDoesNotRetainOldTranslator() {
        this.holder.reload();
        assertNotSame(this.firstTranslator, this.holder.get());
    }

    @Test
    void reloadCallsLoadTranslatorOnEachInvocation() {
        this.holder.reload();
        // 1 from construction + 1 from reload
        verify(this.translatorLoader, times(2)).loadTranslator();
    }

    @Test
    void multipleReloadsSwapSourcesCorrectly() {
        final Translator third = mock(Translator.class);
        when(this.translatorLoader.loadTranslator()).thenReturn(this.secondTranslator).thenReturn(third);

        this.holder.reload();
        this.holder.reload();

        final InOrder order = inOrder(this.globalRegistry);
        order.verify(this.globalRegistry).removeSource(this.firstTranslator);
        order.verify(this.globalRegistry).addSource(this.secondTranslator);
        order.verify(this.globalRegistry).removeSource(this.secondTranslator);
        order.verify(this.globalRegistry).addSource(third);
        assertSame(third, this.holder.get());
    }
}