/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.listeners;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.handler.ResponseHandler;
import com.nickuc.login.addon.model.Session;
import com.nickuc.login.addon.model.response.ReadyResponse;
import com.nickuc.login.addon.model.response.ServerStatusResponse;
import com.nickuc.login.addon.model.response.SyncResponse;
import com.nickuc.login.addon.nLoginAddon;
import lombok.RequiredArgsConstructor;
import net.labymod.api.events.ServerMessageEvent;

@RequiredArgsConstructor
public class ServerMessage implements ServerMessageEvent {

    private final nLoginAddon addon;
    private long lastTimestamp;

    @Override
    public void onServerMessage(String subchannel, final JsonElement jsonElement) {
        if (subchannel.equals(Constants.NLOGIN_SUBCHANNEL)) { // is our message
            if (!addon.getSettings().isEnabled()) {
                return;
            }

            JsonObject json = jsonElement.getAsJsonObject();
            if (!json.has("id")) {
                return;
            }

            final int id = json.get("id").getAsInt();
            if (json.has("timestamp")) {
                long timestamp = json.get("timestamp").getAsLong();
                if (timestamp <= lastTimestamp) { // prevent duplicate packets
                    return;
                }
                lastTimestamp = timestamp;
            }

            Session session = addon.getSession();
            if (session.isActive()) {
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

}
