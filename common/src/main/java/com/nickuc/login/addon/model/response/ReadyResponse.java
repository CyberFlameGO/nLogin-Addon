/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.model.response;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.utils.crypt.RSA;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.annotation.Nullable;
import java.security.PublicKey;
import java.util.Base64;

@NoArgsConstructor
@Getter
@ToString
public class ReadyResponse implements Response {

    private static final int ID = 0x0;

    private String serverUuid;
    @Nullable
    private PublicKey publicKey;
    private byte[] signature;
    private int maxDataLength;
    private int status = -1;

    // client
    private boolean registered;
    private boolean requireSync;
    private String checksum;

    @Override
    public void read(JsonObject json) {
        serverUuid = json.get("uuid").getAsString();
        if (json.has("challenge")) {
            JsonObject challengeJson = json.get("challenge").getAsJsonObject();
            byte[] publicKeyBytes = Base64.getDecoder().decode(challengeJson.get("publicKey").getAsString());
            publicKey = RSA.getPublicKeyFromBytes(publicKeyBytes);
            signature = Base64.getDecoder().decode(challengeJson.get("signature").getAsString());
        }

        maxDataLength = json.get("maxDataLength").getAsInt();
        if (json.has("status")) {
            status = json.get("status").getAsInt();
        }

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
