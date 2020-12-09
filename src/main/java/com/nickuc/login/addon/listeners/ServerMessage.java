/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.listeners;

import com.google.gson.JsonElement;
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
import lombok.AllArgsConstructor;
import net.labymod.api.events.ServerMessageEvent;
import net.labymod.core.LabyModCore;
import net.labymod.main.LabyMod;

import java.security.GeneralSecurityException;

@AllArgsConstructor
public class ServerMessage implements ServerMessageEvent {

    private final nLoginAddon addon;

    @Override
    public void onServerMessage(String subchannel, JsonElement jsonElement) {
        if (subchannel.equals(Constants.NLOGIN_SUBCHANNEL)) { // is our message
            JsonObject json = jsonElement.getAsJsonObject();
            if (!json.has("id")) return;

            int id = json.get("id").getAsInt();
            switch (id) {
                case 0x0:
                    final ReadyResponse readyResponse = addon.readResponse(jsonElement, ReadyResponse.class);
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
                                String checksum = readyResponse.getChecksum();
                                session.setChecksum(checksum);

                                String message;
                                String masterPassword = credentials.getMasterPassword();
                                boolean syncPasswords = addon.getSettings().isSyncPasswords();
                                if (readyResponse.isRegistered()) {

                                    server = user.getServer(readyResponse.getServerUuid());
                                    if (server == null) {
                                        if (syncPasswords) {
                                            if (!masterPassword.isEmpty()) {
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
                                    server = user.updateServer(readyResponse.getServerUuid(), securePassword);
                                    addon.markModified(true);
                                    message = "/register " + securePassword + " " + securePassword;
                                    LabyMod.getInstance().notifyMessageRaw(Constants.DEFAULT_TITLE, Lang.Message.REGISTERING_A_PASSWORD.toText());
                                }

                                session.setServer(server);

                                System.out.println(Constants.PREFIX + "Sending '" + message + "'...");
                                LabyModCore.getMinecraft().getPlayer().sendChatMessage(message);

                                if (syncPasswords && !masterPassword.isEmpty()) {
                                    boolean requireSync = readyResponse.isRequireSync();
                                    if (!requireSync) {
                                        requireSync = !Sha256.hash(Sha256.hash(masterPassword + user.getPrimaryCryptKey())).equals(checksum);
                                    }

                                    System.out.println("Sync required: " + requireSync);
                                    session.setRequireSync(requireSync);
                                }
                            }
                        }
                    });
                    break;

                case 0x1:
                    final SyncResponse syncResponse = addon.readResponse(jsonElement, SyncResponse.class);
                    Constants.EXECUTOR_SERVICE.submit(new Runnable() {
                        @Override
                        public void run() {
                            Credentials credentials = addon.getCredentials();
                            String username = LabyMod.getInstance().getPlayerName();
                            Credentials.User user = credentials.getUser(username);

                            String masterPassword = credentials.getMasterPassword();
                            String responseCryptKey = syncResponse.getCryptKey();
                            String cryptKey = user.getPrimaryCryptKey();

                            String checksum = addon.getSession().getChecksum();
                            if (!Sha256.hash(Sha256.hash(cryptKey + masterPassword)).equals(checksum)) {
                                boolean detected = false;
                                for (String ck : user.getCryptKeys()) {
                                    if (ck.equals(cryptKey)) continue;

                                    if (Sha256.hash(Sha256.hash(cryptKey + masterPassword)).equals(checksum)) {
                                        detected = true;
                                        cryptKey = ck;
                                        break;
                                    }
                                }
                                if (!detected) {
                                    try {
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

                            String encryptedData = syncResponse.getData();
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
                                addon.getSession().setRequireSync(true);
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
                    break;

                case 0x2:
                    final LoginFinishResponse loginFinishResponse = addon.readResponse(jsonElement, LoginFinishResponse.class);
                    Session session = addon.getSession();
                    if (session.isActive() && session.isRequireSync()) {
                        Synchronization.sendUpdateRequest(addon, session.getServer());
                    }
                    break;
            }
        }
    }
}
