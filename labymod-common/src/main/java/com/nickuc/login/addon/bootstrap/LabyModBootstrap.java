/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.bootstrap;

import net.labymod.settings.elements.SettingsElement;

import java.util.List;

public abstract class LabyModBootstrap implements Platform, PlatformBootstrap {

    private final LMAddon addonInstance;

    public LabyModBootstrap(LMAddon addonInstance) {
        this.addonInstance = addonInstance;
    }

    public LMAddon getAddonInstance() {
        return addonInstance;
    }

    public abstract void onEnable();

    public abstract void loadConfig();

    public abstract void fillSettings(List<SettingsElement> list);

    @Override
    public boolean isEnabled() {
        return addonInstance.about.loaded;
    }

    @Override
    public void saveConfig() {
        addonInstance.saveConfig();
    }

}
