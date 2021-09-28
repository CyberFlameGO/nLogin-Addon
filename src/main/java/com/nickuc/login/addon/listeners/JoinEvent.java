/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.listeners;

import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.model.Session;
import com.nickuc.login.addon.model.request.ReadyRequest;
import com.nickuc.login.addon.nLoginAddon;
import com.nickuc.login.addon.utils.SafeGenerator;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import net.labymod.main.LabyMod;
import net.labymod.utils.Consumer;
import net.labymod.utils.ServerData;

import java.util.Arrays;

@AllArgsConstructor
public class JoinEvent implements Consumer<ServerData> {

    private final nLoginAddon addon;

    @SneakyThrows
    @Override
    public void accept(ServerData serverData) {
        final Session session = addon.getSession();
        session.join();
        if (addon.getSettings().isEnabled()) {
            Credentials credentials = addon.getCredentials();
            final byte[] challenge = SafeGenerator.generateRSAChallenge();
            session.setRsaChallenge(challenge);

            final ReadyRequest ready = new ReadyRequest(credentials.getUuid(), challenge, addon.getSettings());
            Constants.EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    int wait = 3000;
                    int sleep = 100;
                    for (int i = 0; i < wait/sleep; i++) {
                        if (!session.isActive()) {
                            break;
                        }

                        if (LabyMod.getInstance().isInGame()) {
                            addon.sendRequest(ready);
                            addon.sendMessage("first pkt sent! " + ready.toString());
                            addon.sendMessage(Arrays.toString(challenge));
                            break;
                        }

                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }
            });
        }
    }

}
