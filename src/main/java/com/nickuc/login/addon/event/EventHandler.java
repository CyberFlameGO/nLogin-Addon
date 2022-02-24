/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.handler.ResponseHandler;
import com.nickuc.login.addon.lang.Lang;
import com.nickuc.login.addon.model.AddonSettings;
import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.model.Session;
import com.nickuc.login.addon.model.request.ReadyRequest;
import com.nickuc.login.addon.model.response.ReadyResponse;
import com.nickuc.login.addon.model.response.ServerStatusResponse;
import com.nickuc.login.addon.model.response.SyncResponse;
import com.nickuc.login.addon.nLoginAddon;
import com.nickuc.login.addon.utils.SafeGenerator;
import lombok.RequiredArgsConstructor;
import net.labymod.main.LabyMod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class EventHandler {

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

    public void handleJoin() {
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

    public void handleQuit() {
        addon.getSession().quit();
    }

    public boolean handleChat(String message) {
        AddonSettings settings = addon.getSettings();
        if (settings.isEnabled() && settings.isSaveLogin() && !message.isEmpty() && message.charAt(0) == '/') {
            String[] parts = message.split(" ");
            if (parts.length > 1) {
                String command = parts[0].toLowerCase();

                // switch are not supported in java 1.6
                if (COMMANDS.contains(command)) {
                    String password = parts[1];
                    addon.getSession().setTmpPassword(password);
                }
            }
        }
        return false;
    }

    public void handleReceivedMessage(String rawMessage) {
        if (addon.getSettings().isEnabled()) {
            Session session = addon.getSession();
            if (!session.isUnsafeServerWarn() && (rawMessage.contains("/register ") || rawMessage.contains("/registrar "))) {
                Constants.EXECUTOR_SERVICE.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
                            Session session = addon.getSession();
                            if (!session.isUnsafeServerWarn() && session.isActive() && !session.isUsingNLogin()) {
                                addon.sendNotification(Lang.Message.STATUS_UNKNOWN.toText());
                                session.setUnsafeServerWarn(true);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    public void handleServerMessage(String subChannel, JsonElement jsonElement) {
        if (!subChannel.equals(Constants.NLOGIN_SUBCHANNEL)) { // is our message
            return;
        }

        if (!addon.getSettings().isEnabled()) {
            return;
        }

        JsonObject json = jsonElement.getAsJsonObject();
        if (!json.has("id")) {
            return;
        }

        final int id = json.get("id").getAsInt();

        Session session = addon.getSession();
        if (session.isActive()) {
            if (json.has("timestamp")) {
                long timestamp = json.get("timestamp").getAsLong();
                if (timestamp <= session.getLastTimestamp()) { // prevent duplicate packets
                    return;
                }
                session.setLastTimestamp(timestamp);
            }

            session.setUsingNLogin(true);
        }

        if (addon.getSettings().isDebug()) {
            addon.sendMessage("§3Packet with id 0x" + Integer.toHexString(id) + " received.");
        }

        Constants.EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
                switch (id) {
                    case 0x0:
                        final ReadyResponse readyResponse = addon.readResponse(jsonElement, ReadyResponse.class);
                        ResponseHandler.handle0x0(addon, readyResponse);
                        break;

                    case 0x1:
                        final SyncResponse syncResponse = addon.readResponse(jsonElement, SyncResponse.class);
                        ResponseHandler.handle0x1(addon, syncResponse);
                        break;

                    case 0x2:
                        final ServerStatusResponse loginFinishResponse = addon.readResponse(jsonElement, ServerStatusResponse.class);
                        ResponseHandler.handle0x2(addon, loginFinishResponse);
                        break;
                }
            }
        });
    }

}
