/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.model.request;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.model.AddonSettings;
import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.utils.hash.Sha256;
import lombok.AllArgsConstructor;
import lombok.ToString;
import sun.security.provider.MD5;

@AllArgsConstructor @ToString
public class ReadyRequest implements Request {

    private static final int ID = 0x0;

    private final String uuid;
    private final AddonSettings settings;

    @Override
    public void write(JsonObject json) {
        json.addProperty("uuid", uuid);
        JsonObject settings = Constants.GSON.fromJson(Constants.GSON.toJson(this.settings), JsonObject.class);
        json.add("settings", settings);
    }

    @Override
    public int getId() {
        return ID;
    }
}
