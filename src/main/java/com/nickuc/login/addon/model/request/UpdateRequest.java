/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.model.request;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class UpdateRequest implements Request {

    private static final int ID = 0x1;

    private final String cryptKey;
    private final String data;
    private final String checksum;

    @Override
    public void write(JsonObject json) {
        String encrypted = cryptKey + ";" + data;
        json.addProperty("encrypted", encrypted);
        json.addProperty("checksum", checksum);
    }

    @Override
    public int getId() {
        return ID;
    }
}
