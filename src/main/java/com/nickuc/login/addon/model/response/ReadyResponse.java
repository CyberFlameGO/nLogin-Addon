/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.model.response;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor @Getter @ToString
public class ReadyResponse implements Response {

    private static final int ID = 0x0;

    private String serverUuid;
    private int maxDataLength;

    // client
    private boolean registered;
    private boolean requireSync;
    private String checksum;

    @Override
    public void read(JsonObject json) {
        serverUuid = json.get("uuid").getAsString();
        maxDataLength = json.get("maxDataLength").getAsInt();

        JsonObject clientJson = json.getAsJsonObject("client");
        registered = clientJson.get("registered").getAsBoolean();
        if (registered) {
            requireSync = clientJson.get("requireSync").getAsBoolean();
            checksum = clientJson.get("checksum").getAsString();
        } else {
            requireSync = true;
        }
    }

    @Override
    public int getId() {
        return ID;
    }
}
