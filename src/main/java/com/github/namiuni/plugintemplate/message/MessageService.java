/*
 * plugin-template
 *
 * Copyright (c) 2024. Namiu (Unitarou)
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

package com.github.namiuni.plugintemplate.message;

import net.kyori.adventure.audience.Audience;
import net.kyori.moonshine.annotation.Message;

public interface MessageService {

    /*
     * ☆彡━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━☆彡
     *  ✧･ﾟ: *✧･ﾟ:* ✨✨✨ Reload ✨✨✨ *:･ﾟ✧*:･ﾟ✧
     * ☆彡━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━☆彡
     */

    @Message("plugin_template.command.config.reload")
    void configReloadSuccess(final Audience audience);
}
