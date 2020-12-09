/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.listeners;

import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.model.request.ReadyRequest;
import com.nickuc.login.addon.nLoginAddon;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import net.labymod.utils.Consumer;
import net.labymod.utils.ServerData;

@AllArgsConstructor
public class JoinEvent implements Consumer<ServerData> {

    private final nLoginAddon addon;

    @SneakyThrows
    @Override
    public void accept(ServerData serverData) {
        addon.getSession().join();
        Credentials credentials = addon.getCredentials();
        Credentials.User user = credentials.getUser();
        ReadyRequest ready = new ReadyRequest(credentials.getUuid(), addon.getSettings());
        addon.sendRequest(ready);
    }

}
