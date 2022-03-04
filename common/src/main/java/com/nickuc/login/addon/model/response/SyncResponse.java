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

@NoArgsConstructor
@Getter
@ToString
public class SyncResponse implements Response {

    private static final int ID = 0x1;

    private String cryptKey;
    private String data;

    @Override
    public void read(JsonObject json) {
        String[] encryptedParts = json.get("encrypted").getAsString().split(";");
        cryptKey = encryptedParts[0];
        data = encryptedParts[1];
    }

    @Override
    public int getId() {
        return ID;
    }
}
