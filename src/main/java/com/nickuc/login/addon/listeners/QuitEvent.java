/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.listeners;

import com.nickuc.login.addon.nLoginAddon;
import lombok.AllArgsConstructor;
import net.labymod.utils.Consumer;
import net.labymod.utils.ServerData;

@AllArgsConstructor
public class QuitEvent implements Consumer<ServerData> {

    private final nLoginAddon addon;

    @Override
    public void accept(ServerData serverData) {
        addon.getSession().quit();
    }

}
