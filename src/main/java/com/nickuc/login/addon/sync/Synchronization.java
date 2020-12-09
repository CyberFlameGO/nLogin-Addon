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
import net.labymod.main.LabyMod;

import java.security.GeneralSecurityException;

public class Synchronization {

    public static void sendUpdateRequest(final nLoginAddon addon, final Credentials.Server server) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(750);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!LabyMod.getInstance().isInGame()) return;

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
                    synchronized (Constants.LOCK) {
                        LabyMod.getInstance().displayMessageInChat("§4[" + Constants.DEFAULT_TITLE + "] §c" + Lang.Message.SYNC_FAILED_ENCRYPT.toText());
                        LabyMod.getInstance().notifyMessageRaw(Constants.DEFAULT_TITLE, Lang.Message.SYNC_FAILED_ENCRYPT.toText());
                    }
                }
            }
        };

        Constants.EXECUTOR_SERVICE.submit(runnable);
    }

}
