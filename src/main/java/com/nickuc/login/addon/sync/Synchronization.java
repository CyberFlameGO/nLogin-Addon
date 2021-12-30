/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.sync;

import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.lang.Lang;
import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.model.request.UpdateRequest;
import com.nickuc.login.addon.nLoginAddon;
import com.nickuc.login.addon.utils.crypt.AesGcm;
import com.nickuc.login.addon.utils.hash.Sha256;
import lombok.Cleanup;
import lombok.SneakyThrows;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.GeneralSecurityException;

public class Synchronization {

    @SneakyThrows
    public static void sendUpdateRequest(final nLoginAddon addon, final Credentials.Server server) {
        Credentials credentials = addon.getCredentials();
        Credentials.User user = credentials.getUser();
        String masterPassword = credentials.getMasterPassword();
        if (masterPassword.isEmpty()) return;

        try {
            String primaryCryptKey = user.getPrimaryCryptKey();
            String encryptedPrimaryCryptKey = AesGcm.encrypt(primaryCryptKey, masterPassword);
            String encryptedData = AesGcm.encrypt(server.toJson().toString(), primaryCryptKey);
            String checksum = Sha256.hash(Sha256.hash(masterPassword + primaryCryptKey));

            // require auth
            UpdateRequest updateRequest = new UpdateRequest(encryptedPrimaryCryptKey, encryptedData, checksum);
            addon.sendRequest(updateRequest);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            addon.sendMessage("§4[" + Constants.DEFAULT_TITLE + "] §c" + Lang.Message.SYNC_FAILED_ENCRYPT.toText());
            addon.sendNotification(Lang.Message.SYNC_FAILED_ENCRYPT.toText());

            if (addon.getSettings().isDebug()) {
                @Cleanup StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                addon.sendMessage("§eStacktrace:");
                addon.sendMessage(stringWriter.toString());
            }
        }
    }

}
