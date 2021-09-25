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
import com.nickuc.login.addon.model.response.ServerStatusResponse;
import com.nickuc.login.addon.model.response.ReadyResponse;
import com.nickuc.login.addon.model.response.SyncResponse;
import com.nickuc.login.addon.nLoginAddon;
import lombok.AllArgsConstructor;
import net.labymod.api.events.ServerMessageEvent;

@AllArgsConstructor
public class ServerMessage implements ServerMessageEvent {

    private final nLoginAddon addon;

    @Override
    public void onServerMessage(String subchannel, JsonElement jsonElement) {
        if (subchannel.equals(Constants.NLOGIN_SUBCHANNEL)) { // is our message
            if (!addon.getSettings().isEnabled()) return;

            JsonObject json = jsonElement.getAsJsonObject();
            if (!json.has("id")) return;

            int id = json.get("id").getAsInt();

            Session session = addon.getSession();
            if (session.isActive()) {
                session.setUsingNLogin(true);
            }

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
    }

}
