/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.listeners;

import com.nickuc.login.addon.model.AddonSettings;
import com.nickuc.login.addon.nLoginAddon;
import lombok.RequiredArgsConstructor;
import net.labymod.api.events.MessageSendEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class ChatEvent implements MessageSendEvent {

    private static final Set<String> COMMANDS = new HashSet<String>();

    static {
        COMMANDS.addAll(Arrays.asList(
                "/login",
                "/logar",
                "/log",
                "/register",
                "/registrar",
                "/reg"
        ));
    }

    private final nLoginAddon addon;

    @Override
    public boolean onSend(String message) {
        AddonSettings settings = addon.getSettings();
        if (settings.isEnabled() && settings.isSaveLogin() && message.charAt(0) == '/') {
            String[] parts = message.split(" ");
            if (parts.length > 1) {
                String command = parts[0].toLowerCase();

                // switch are not supported in java 1.6
                if (COMMANDS.contains(command)) {
                    String password = parts[1];
                    addon.getSession().setTmpPassword(password);
                }

                /*
                switch (command) {
                    case "/login":
                    case "/logar":
                    case "/log":
                    case "/register":
                    case "/registrar":
                    case "/reg":
                        String password = parts[1];
                        addon.getSession().setTmpPassword(password);
                        break;
                }
                 */
            }
        }
        return false;
    }
}