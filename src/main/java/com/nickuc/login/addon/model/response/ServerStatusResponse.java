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
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Getter
@ToString
public class ServerStatusResponse implements Response {

    private static final int ID = 0x2;

    public static final int LOGIN_SUCCESSFUL = 0;
    public static final int RSA_CHALLENGE_REJECTED = 1;
    public static final int SYNC_REQUEST_REJECTED = 2;
    public static final int CHECKSUM_REJECTED = 3;

    private int code;

    @Override
    public void read(JsonObject json) {
        code = json.has("code") ? json.get("code").getAsInt() : LOGIN_SUCCESSFUL;
    }

    @Override
    public int getId() {
        return ID;
    }
}
