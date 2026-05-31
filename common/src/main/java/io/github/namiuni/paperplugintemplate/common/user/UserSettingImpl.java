package io.github.namiuni.paperplugintemplate.common.user;

import io.github.namiuni.paperplugintemplate.api.user.PluginTemplateUser;

public record UserSettingImpl() implements PluginTemplateUser.Setting {
    public static final UserSettingImpl DEFAULT = new UserSettingImpl();
}
