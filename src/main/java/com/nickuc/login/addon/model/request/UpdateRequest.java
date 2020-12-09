/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.model.request;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.nLoginAddon;
import com.nickuc.login.addon.utils.crypt.AesGcm;
import com.nickuc.login.addon.utils.hash.Sha256;
import lombok.AllArgsConstructor;
import lombok.ToString;
import net.labymod.main.LabyMod;

import java.security.GeneralSecurityException;

@AllArgsConstructor @ToString
public class UpdateRequest implements Request {

    private static final int ID = 0x1;

    private final String cryptKey;
    private final String data;
    private final String checksum;

    @Override
    public void write(JsonObject json) {
        json.addProperty("encrypted", cryptKey + ";" + data);
        json.addProperty("checksum", checksum);
    }

    @Override
    public int getId() {
        return ID;
    }
}
