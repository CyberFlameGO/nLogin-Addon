/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.handler;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.lang.Lang;
import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.model.Session;
import com.nickuc.login.addon.model.request.SyncRequest;
import com.nickuc.login.addon.model.response.LoginFinishResponse;
import com.nickuc.login.addon.model.response.ReadyResponse;
import com.nickuc.login.addon.model.response.SyncResponse;
import com.nickuc.login.addon.nLoginAddon;
import com.nickuc.login.addon.sync.Synchronization;
import com.nickuc.login.addon.utils.SafeGenerator;
import com.nickuc.login.addon.utils.crypt.AesGcm;
import com.nickuc.login.addon.utils.hash.Sha256;
import net.labymod.core.LabyModCore;
import net.labymod.main.LabyMod;

import java.security.GeneralSecurityException;

public class ResponseHandler {

    public static void handle0x0(final nLoginAddon addon, final ReadyResponse packet) {
        Constants.EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (Constants.LOCK) {
                    if (!LabyMod.getInstance().isInGame()) return;

                    final Credentials credentials = addon.getCredentials();
                    final Credentials.User user = credentials.getUser();
                    final Credentials.Server server;

                    Session session = addon.getSession();
                    String checksum = packet.getChecksum();
                    session.setChecksum(checksum);
                    String serverUuid = packet.getServerUuid();
                    session.setServerUuid(serverUuid);

                    String message;
                    boolean syncPasswords = addon.getSettings().isSyncPasswords();
                    if (packet.isRegistered()) {

                        server = user.getServer(serverUuid);
                        if (server == null) {
                            if (syncPasswords) {
                                if (!credentials.getMasterPassword().isEmpty()) {
                                    addon.sendRequest(new SyncRequest());
                                } else {
                                    LabyMod.getInstance().displayMessageInChat("§4[" + Constants.DEFAULT_TITLE + "] §c" + Lang.Message.SYNC_REQUIRE_PASSWORD.toText());
                                    LabyMod.getInstance().notifyMessageRaw(Constants.DEFAULT_TITLE, Lang.Message.SYNC_REQUIRE_PASSWORD.toText());
                                }
                            }
                            return;
                        }

                        message = "/login " + server.getPassword();
                    } else {
                        String securePassword = SafeGenerator.generatePassword();
                        server = user.updateServer(serverUuid, securePassword);
                        addon.markModified(true);
                        message = "/register " + securePassword + " " + securePassword;
                        LabyMod.getInstance().notifyMessageRaw(Constants.DEFAULT_TITLE, Lang.Message.REGISTERING_A_PASSWORD.toText());
                    }

                    session.setServer(server);

                    System.out.println(Constants.PREFIX + "Sending '" + message + "'...");
                    LabyModCore.getMinecraft().getPlayer().sendChatMessage(message);

                    if (syncPasswords) {
                        String masterPassword = credentials.getMasterPassword();
                        if (!masterPassword.isEmpty()) {
                            boolean requireSync = packet.isRequireSync();
                            if (!requireSync) {
                                requireSync = !Sha256.hash(Sha256.hash(masterPassword + user.getPrimaryCryptKey())).equals(checksum);
                            }

                            System.out.println("Sync required: " + requireSync);
                            session.setRequireSync(requireSync);
                        }
                    }
                }
            }
        });
    }

    public static void handle0x1(final nLoginAddon addon, final SyncResponse packet) {
        Constants.EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
                final Credentials credentials = addon.getCredentials();
                final Credentials.User user = credentials.getUser();

                String masterPassword = credentials.getMasterPassword();
                String cryptKey = user.getPrimaryCryptKey();
                String checksum = addon.getSession().getChecksum();
                if (!Sha256.hash(Sha256.hash(cryptKey + masterPassword)).equals(checksum)) {
                    boolean detected = false;
                    for (String ck : user.getCryptKeys()) {
                        if (ck.equals(cryptKey)) continue;

                        if (Sha256.hash(Sha256.hash(ck + masterPassword)).equals(checksum)) {
                            detected = true;
                            cryptKey = ck;
                            break;
                        }
                    }
                    if (!detected) {
                        try {
                            String responseCryptKey = packet.getCryptKey();
                            cryptKey = AesGcm.decrypt(responseCryptKey, masterPassword);
                        } catch (GeneralSecurityException e) {
                            e.printStackTrace();
                            synchronized (Constants.LOCK) {
                                LabyMod.getInstance().displayMessageInChat("§4[" + Constants.DEFAULT_TITLE + "] §c" + Lang.Message.SYNC_FAILED_DECRYPT.toText());
                                LabyMod.getInstance().notifyMessageRaw(Constants.DEFAULT_TITLE, Lang.Message.SYNC_FAILED_DECRYPT.toText());
                            }
                            return;
                        }
                    }
                }

                String encryptedData = packet.getData();
                String decryptedData;
                try {
                    decryptedData = AesGcm.decrypt(encryptedData, cryptKey);
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                    synchronized (Constants.LOCK) {
                        LabyMod.getInstance().displayMessageInChat("§4[" + Constants.DEFAULT_TITLE + "] §c" + Lang.Message.SYNC_FAILED_DECRYPT2.toText());
                        LabyMod.getInstance().notifyMessageRaw(Constants.DEFAULT_TITLE, Lang.Message.SYNC_FAILED_DECRYPT2.toText());
                    }
                    return;
                }

                JsonObject json = Constants.GSON.fromJson(decryptedData, JsonObject.class);
                Credentials.Server server = Credentials.Server.fromJson(json);
                if (server != null) {
                    addon.markModified(true);
                    Session session = addon.getSession();
                    session.setServer(server);
                    session.setRequireSync(true);
                    user.updateServer(server);
                    user.addCryptKey(cryptKey);

                    synchronized (Constants.LOCK) {
                        LabyMod.getInstance().notifyMessageRaw(Constants.DEFAULT_TITLE, Lang.Message.SYNC_SYNCING.toText());

                        String password = server.getPassword();
                        String message = "/login " + password;

                        System.out.println(Constants.PREFIX + "Sending '" + message + "'...");
                        LabyModCore.getMinecraft().getPlayer().sendChatMessage(message);
                    }
                }
            }
        });
    }

    public static void handle0x2(final nLoginAddon addon, final LoginFinishResponse packet) {
        Session session = addon.getSession();
        if (session.isActive()) {
            session.authenticate();

            Credentials.Server server = session.getServer();
            if (server != null) {
                if (session.isRequireSync()) {
                    Synchronization.sendUpdateRequest(addon, server);
                }
            } else {
                String serverUuid = session.getServerUuid();
                String tmpPassword = session.getTmpPassword();
                if (serverUuid != null && tmpPassword != null) {
                    Credentials credentials = addon.getCredentials();
                    Credentials.User user = credentials.getUser();
                    server = user.updateServer(serverUuid, tmpPassword);
                    addon.markModified(true);
                    LabyMod.getInstance().notifyMessageRaw(Constants.DEFAULT_TITLE, Lang.Message.REGISTERING_A_PASSWORD2.toText());
                    Synchronization.sendUpdateRequest(addon, server);
                }
            }
        }
    }

}
