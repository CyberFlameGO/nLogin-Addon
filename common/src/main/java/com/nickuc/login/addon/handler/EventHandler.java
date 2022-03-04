/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.bootstrap.Platform;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class EventHandler {

    private static final Set<String> AUTH_COMMANDS = new HashSet<>();

    static {
        AUTH_COMMANDS.addAll(Arrays.asList(
                "/login",
                "/logar",
                "/log",
                "/register",
                "/registrar",
                "/reg"
        ));
    }

    private final nLoginAddon addon;
    private final Platform platform;
    private final ResponseHandler responseHandler;

    public EventHandler(nLoginAddon addon, Platform platform) {
        this.addon = addon;
        this.platform = platform;
        this.responseHandler = new ResponseHandler(addon, platform);
    }

    public void handleJoin() {
        final Session session = addon.getSession();
        session.join();
        if (addon.getSettings().isEnabled()) {
            Credentials credentials = addon.getCredentials();
            final byte[] challenge = SafeGenerator.generateRSAChallenge();
            session.setRsaChallenge(challenge);

            final ReadyRequest ready = new ReadyRequest(credentials.getUuid(), challenge, addon.getSettings());
            Constants.EXECUTOR_SERVICE.submit(() -> {
                try {
                    Thread.sleep(1000L); // wait for 1s (prevent message from antibot system)
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }

                int wait = 3000;
                int sleep = 100;
                for (int i = 0; i < wait / sleep; i++) {
                    if (!session.isActive()) {
                        break;
                    }

                    if (platform.isConnected()) {
                        platform.sendRequest(ready);
                        break;
                    }

                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
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
                if (AUTH_COMMANDS.contains(command)) {
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
                session.setUnsafeServerWarn(true);
                Constants.EXECUTOR_SERVICE.submit(() -> {
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
                        if (platform.isConnected() && session.isActive() && !session.isUsingNLogin()) {
                            platform.sendNotification(Lang.Message.STATUS_UNKNOWN.toText());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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
            platform.sendMessage("§3Packet with id 0x" + Integer.toHexString(id) + " received.");
        }

        Constants.EXECUTOR_SERVICE.submit(() -> {
            switch (id) {
                case 0x0:
                    final ReadyResponse readyResponse = addon.readResponse(jsonElement, ReadyResponse.class);
                    responseHandler.handle0x0(readyResponse);
                    break;

                case 0x1:
                    final SyncResponse syncResponse = addon.readResponse(jsonElement, SyncResponse.class);
                    responseHandler.handle0x1(syncResponse);
                    break;

                case 0x2:
                    final ServerStatusResponse loginFinishResponse = addon.readResponse(jsonElement, ServerStatusResponse.class);
                    responseHandler.handle0x2(loginFinishResponse);
                    break;
            }
        });
    }

}
