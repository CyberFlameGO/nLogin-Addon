/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.bootstrap;

import net.labymod.api.LabyModAddon;
import net.labymod.main.LabyMod;
import net.labymod.main.Source;
import net.labymod.settings.elements.SettingsElement;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LMAddon extends LabyModAddon {

    private static final String[] BOOTSTRAP_CLASSES = {
        "com.nickuc.login.addon.bootstrap.versions.LMAddon18",
        "com.nickuc.login.addon.bootstrap.versions.LMAddon112",
        "com.nickuc.login.addon.bootstrap.versions.LMAddon116",
    };

    private LabyModBootstrap bootstrap;

    private void loadBootstrap(int index) {
        try {
            Class<?> bootstrapClass = Class.forName(BOOTSTRAP_CLASSES[index]);
            Constructor<?> bootstrapConstructor = bootstrapClass.getConstructor(LMAddon.class);
            bootstrap = (LabyModBootstrap) bootstrapConstructor.newInstance(this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        if (bootstrap == null) {
            String mcVersion = Source.ABOUT_MC_VERSION;
            if (mcVersion.startsWith("1.16")) {
                loadBootstrap(2);
            } else if (mcVersion.startsWith("1.12")) {
                loadBootstrap(1);
            } else if (mcVersion.startsWith("1.8")) {
                loadBootstrap(0);
            } else {
                System.err.println("Versão não suportada! " + mcVersion);
                Timer timer = new Timer("nLoginAddon$VersionWarning");
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        LabyMod.getInstance().notifyMessageRaw("nLogin Addon", "Versão não suportada! (" + mcVersion + ")");
                    }
                }, 60L * 1000L, 5L * 60L * 1000L);
            }
        }
        if (bootstrap != null) {
            bootstrap.onEnable();
        }
    }

    @Override
    public void loadConfig() {
        if (bootstrap != null) {
            bootstrap.loadConfig();
        }
    }

    @Override
    protected void fillSettings(List<SettingsElement> list) {
        if (bootstrap != null) {
            bootstrap.fillSettings(list);
        }
    }

}
