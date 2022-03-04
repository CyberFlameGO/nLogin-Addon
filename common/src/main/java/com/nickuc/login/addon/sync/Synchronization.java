/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.sync;

import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.bootstrap.Platform;
import com.nickuc.login.addon.lang.Lang;
import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.model.request.UpdateRequest;
import com.nickuc.login.addon.nLoginAddon;
import com.nickuc.login.addon.utils.crypt.AesGcm;
import com.nickuc.login.addon.utils.hash.Sha256;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.GeneralSecurityException;

@RequiredArgsConstructor
public class Synchronization {

    private final nLoginAddon addon;
    private final Platform platform;

    @SneakyThrows
    public void sendUpdateRequest(final Credentials.Server server) {
        Credentials credentials = addon.getCredentials();
        Credentials.User user = credentials.getUser(addon.getPlatform());
        String masterPassword = credentials.getMasterPassword();
        if (masterPassword.isEmpty()) {
            return;
        }

        try {
            String primaryCryptKey = user.getPrimaryCryptKey();
            String encryptedPrimaryCryptKey = AesGcm.encrypt(primaryCryptKey, masterPassword);
            String encryptedData = AesGcm.encrypt(server.toJson().toString(), primaryCryptKey);
            String checksum = Sha256.hash(Sha256.hash(masterPassword + primaryCryptKey));

            // require auth
            UpdateRequest updateRequest = new UpdateRequest(encryptedPrimaryCryptKey, encryptedData, checksum);
            platform.sendRequest(updateRequest);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            platform.sendMessage("§4[" + Constants.DEFAULT_TITLE + "] §c" + Lang.Message.SYNC_FAILED_ENCRYPT.toText());
            platform.sendNotification(Lang.Message.SYNC_FAILED_ENCRYPT.toText());

            if (addon.getSettings().isDebug()) {
                @Cleanup StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                platform.sendMessage("§eStacktrace:");
                platform.sendMessage(stringWriter.toString());
            }
        }
    }

}
