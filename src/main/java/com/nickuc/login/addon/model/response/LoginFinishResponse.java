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
public class LoginFinishResponse implements Response {

    private static final int ID = 0x2;

    @Override
    public void read(JsonObject json) {
    }

    @Override
    public int getId() {
        return ID;
    }
}
