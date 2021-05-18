/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.listeners;

import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.lang.Lang;
import com.nickuc.login.addon.model.Session;
import com.nickuc.login.addon.nLoginAddon;
import lombok.RequiredArgsConstructor;
import net.labymod.api.events.MessageReceiveEvent;
import net.labymod.main.LabyMod;

@RequiredArgsConstructor
public class MessageReceived implements MessageReceiveEvent {

    private final nLoginAddon addon;

    @Override
    public boolean onReceive(String formatted, String unformatted) {
        Session session = addon.getSession();
        if (!session.isUnsafeServerWarn() && unformatted.contains("/login ") || unformatted.contains("/logar ") || unformatted.contains("/register ") || unformatted.contains("/registrar ")) {
            Constants.EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000L);
                        synchronized (Constants.LOCK) {
                            Session session = addon.getSession();
                            if (!session.isUnsafeServerWarn() && session.isActive() && !session.isUsingNLogin()) {
                                LabyMod.getInstance().notifyMessageRaw(Constants.DEFAULT_TITLE, Lang.Message.STATUS_UNKNOWN.toText());
                                LabyMod.getInstance().displayMessageInChat(Lang.Message.STATUS_UNKNOWN.toText()); ;
                                session.setUnsafeServerWarn(true);
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return false;
    }

}
