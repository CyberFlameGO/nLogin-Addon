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
public class SyncRequest implements Request {

    private static final int ID = 0x2;

    @Override
    public void write(JsonObject json) {
    }

    @Override
    public int getId() {
        return ID;
    }
}
