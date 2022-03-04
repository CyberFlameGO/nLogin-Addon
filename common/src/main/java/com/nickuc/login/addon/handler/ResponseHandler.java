/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.handler;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.bootstrap.Platform;
import com.nickuc.login.addon.lang.Lang;
import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.model.Session;
import com.nickuc.login.addon.model.request.SyncRequest;
import com.nickuc.login.addon.model.response.ReadyResponse;
import com.nickuc.login.addon.model.response.ServerStatusResponse;
import com.nickuc.login.addon.model.response.SyncResponse;
import com.nickuc.login.addon.nLoginAddon;
import com.nickuc.login.addon.sync.Synchronization;
import com.nickuc.login.addon.utils.SafeGenerator;
import com.nickuc.login.addon.utils.crypt.AesGcm;
import com.nickuc.login.addon.utils.crypt.RSA;
import com.nickuc.login.addon.utils.hash.Sha256;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Arrays;

public class ResponseHandler {

    private final nLoginAddon addon;
    private final Platform platform;
    private final Synchronization synchronization;

    public ResponseHandler(nLoginAddon addon, Platform platform) {
        this.addon = addon;
        this.platform = platform;
        this.synchronization = new Synchronization(addon, platform);
    }

    public void handle0x0(final ReadyResponse packet) {
        synchronized (Constants.LOCK) {
            if (!platform.isConnected()) {
                return;
            }

            String serverUuid = packet.getServerUuid();
            if (serverUuid.isEmpty() || !Constants.UUID_PATTERN.matcher(serverUuid).matches()) {
                platform.sendNotification(Lang.Message.INVALID_SERVER_UUID.toText());
                return;
            }

            final Credentials credentials = addon.getCredentials();
            final Credentials.User user = credentials.getUser(addon.getPlatform());
            final Credentials.Server server;

            Session session = addon.getSession();
            session.setServerUuid(serverUuid);

            String checksum = packet.getChecksum();
            session.setChecksum(checksum);

            PublicKey serverPublicKey = packet.getPublicKey();
            session.setServerPublicKey(serverPublicKey);

            byte[] serverSignature = packet.getSignature();
            session.setServerSignature(serverSignature);

            // server status message
            boolean statusSent = false;
            switch (packet.getStatus()) {
                case 2:
                    platform.sendNotification(Lang.Message.STATUS_MESSAGE2.toText());
                    statusSent = true;
                    break;
                case 3:
                    platform.sendNotification(Lang.Message.STATUS_MESSAGE3.toText());
                    statusSent = true;
                    break;
                case 4:
                    platform.sendNotification(Lang.Message.STATUS_MESSAGE4.toText());
                    statusSent = true;
                    break;
            }

            String message;
            boolean syncPasswords = addon.getSettings().isSyncPasswords();
            if (packet.isRegistered()) {

                server = user.getServer(serverUuid);
                if (server == null) {
                    if (syncPasswords) {
                        if (!credentials.getMasterPassword().isEmpty()) {
                            platform.sendRequest(new SyncRequest());
                        } else {
                            platform.sendMessage("§4[" + Constants.DEFAULT_TITLE + "] §c" + Lang.Message.SYNC_REQUIRE_PASSWORD.toText());
                            if (!statusSent) {
                                platform.sendNotification(Lang.Message.SYNC_REQUIRE_PASSWORD.toText());
                            }
                        }
                    }
                    return;
                }

                // signature check
                PublicKey publicKey = server.getPublicKey();
                byte[] clientRsaChallenge = session.getRsaChallenge();
                if (publicKey != null) {
                    if (clientRsaChallenge == null) {
                        System.err.println(Constants.PREFIX + "The server did not send the challenge, but its public key was previously registered.");
                        platform.sendNotification(Lang.Message.INVALID_SERVER_SIGNATURE.toText());
                        if (addon.getSettings().isDebug()) {
                            platform.sendMessage("§3The server did not send the challenge, but its public key was previously registered.");
                        }
                        return;
                    }

                    if (serverPublicKey == null) {
                        System.err.println(Constants.PREFIX + "The server did not send its public key.");
                        platform.sendNotification(Lang.Message.INVALID_SERVER_SIGNATURE.toText());
                        if (addon.getSettings().isDebug()) {
                            platform.sendMessage("§3The server did not send its public key.");
                        }
                        return;
                    }

                    if (!Arrays.equals(publicKey.getEncoded(), serverPublicKey.getEncoded())) {
                        System.err.println(Constants.PREFIX + "The public key of the remote server is not the same as the stored one.");
                        platform.sendNotification(Lang.Message.INVALID_SERVER_SIGNATURE.toText());
                        if (addon.getSettings().isDebug()) {
                            platform.sendMessage("§3The public key of the remote server is not the same as the stored one.");
                        }
                        return;
                    }

                    byte[] decrypt = RSA.decrypt(publicKey, serverSignature);
                    if (decrypt == null) {
                        System.err.println(Constants.PREFIX + "RSA challenge verification failed: the remote server appears not to have this key pair.");
                        platform.sendNotification(Lang.Message.INVALID_SERVER_SIGNATURE.toText());
                        if (addon.getSettings().isDebug()) {
                            platform.sendMessage("§3RSA challenge verification failed: the remote server appears not to have this key pair.");
                        }
                        return;
                    }

                    if (!Arrays.equals(decrypt, clientRsaChallenge)) {
                        System.err.println(Constants.PREFIX + "The bytes of the challenge are not the same as in cryptography.");
                        platform.sendNotification(Lang.Message.INVALID_SERVER_SIGNATURE.toText());
                        if (addon.getSettings().isDebug()) {
                            platform.sendMessage("§3The bytes of the challenge are not the same as in cryptography.");
                            platform.sendMessage("§3" + Arrays.toString(decrypt));
                            platform.sendMessage("§3" + Arrays.toString(clientRsaChallenge));
                        }
                        return;
                    }
                }

                message = "/login " + server.getPassword();
            } else {
                String securePassword = SafeGenerator.generatePassword();
                server = user.updateServer(serverUuid, serverPublicKey, securePassword);
                addon.markModified(true);
                message = "/register " + securePassword + " " + securePassword;
                if (!statusSent) {
                    platform.sendNotification(Lang.Message.REGISTERING_A_PASSWORD.toText());
                }
            }

            session.setServer(server);

            System.out.println(Constants.PREFIX + "Sending '" + message + "'...");
            if (addon.getSettings().isDebug()) {
                platform.sendMessage("§3Sending '" + message + "'...");
            }
            platform.sendChatPacket(message);

            if (syncPasswords) {
                String masterPassword = credentials.getMasterPassword();
                if (!masterPassword.isEmpty()) {
                    boolean requireSync = packet.isRequireSync();
                    if (!requireSync) {
                        requireSync = !Sha256.hash(Sha256.hash(masterPassword + user.getPrimaryCryptKey())).equals(checksum);
                    }

                    System.out.println(Constants.PREFIX + "Sync required: " + requireSync);
                    session.setRequireSync(requireSync);
                }
            }
        }
    }

    public void handle0x1(final SyncResponse packet) {
        final Credentials credentials = addon.getCredentials();
        final Credentials.User user = credentials.getUser(addon.getPlatform());

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
                    platform.sendMessage("§4[" + Constants.DEFAULT_TITLE + "] §c" + Lang.Message.SYNC_FAILED_DECRYPT.toText());
                    platform.sendNotification(Lang.Message.SYNC_FAILED_DECRYPT.toText());
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
            platform.sendMessage("§4[" + Constants.DEFAULT_TITLE + "] §c" + Lang.Message.SYNC_FAILED_DECRYPT2.toText());
            platform.sendNotification(Lang.Message.SYNC_FAILED_DECRYPT2.toText());
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
                platform.sendNotification(Lang.Message.SYNC_SYNCING.toText());

                String password = server.getPassword();
                String message = "/login " + password;

                System.out.println(Constants.PREFIX + "Sending '" + message + "'...");
                platform.sendChatPacket(message);
            }
        }
    }

    public void handle0x2(final ServerStatusResponse packet) {
        switch (packet.getCode()) {
            case ServerStatusResponse.LOGIN_SUCCESSFUL: {
                Session session = addon.getSession();
                if (session.isActive()) {
                    session.authenticate();

                    Credentials.Server server = session.getServer();
                    if (server != null) {
                        if (session.isRequireSync()) {
                            try {
                                Thread.sleep(750);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            synchronization.sendUpdateRequest(server);
                        }
                    } else {
                        String serverUuid = session.getServerUuid();
                        PublicKey serverPublicKey = session.getServerPublicKey();
                        String tmpPassword = session.getTmpPassword();
                        if (serverUuid != null && tmpPassword != null) {
                            Platform platform = addon.getPlatform();
                            Credentials credentials = addon.getCredentials();

                            Credentials.User user = credentials.getUser(platform);
                            server = user.updateServer(serverUuid, serverPublicKey, tmpPassword);
                            addon.markModified(true);
                            platform.sendNotification(Lang.Message.REGISTERING_A_PASSWORD2.toText());
                            synchronization.sendUpdateRequest(server);
                        }
                    }
                }
                break;
            }
            case ServerStatusResponse.RSA_CHALLENGE_REJECTED: {
                System.err.println(Constants.PREFIX + "RSA challenge rejected.");
                if (addon.getSettings().isDebug()) {
                    platform.sendMessage("§3RSA challenge rejected.");
                }
                break;
            }
            case ServerStatusResponse.SYNC_REQUEST_REJECTED: {
                System.err.println(Constants.PREFIX + "Sync request rejected.");
                if (addon.getSettings().isDebug()) {
                    platform.sendMessage("§3Sync request rejected.");
                }
                break;
            }
            case ServerStatusResponse.CHECKSUM_REJECTED: {
                System.err.println(Constants.PREFIX + "Checksum rejected.");
                if (addon.getSettings().isDebug()) {
                    platform.sendMessage("§3Checksum rejected.");
                }
                break;
            }
        }
    }

}
