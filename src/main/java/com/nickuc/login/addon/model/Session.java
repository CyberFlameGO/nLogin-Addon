/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.model;

import lombok.Getter;
import lombok.Setter;

import java.security.PublicKey;

@Getter
public class Session {

    private boolean active;
    @Setter
    private boolean requireSync;
    @Setter
    private boolean usingNLogin;
    @Setter
    private boolean unsafeServerWarn;
    private boolean authenticated;
    @Setter
    private Credentials.Server server;
    @Setter
    private String serverUuid;
    @Setter
    private PublicKey serverPublicKey;
    @Setter
    private byte[] serverSignature;
    @Setter
    private byte[] rsaChallenge;
    @Setter
    private String checksum;
    @Setter
    private String tmpPassword;

    public void join() {
        quit();
        active = true;
    }

    public void quit() {
        active = false;
        authenticated = false;
        requireSync = false;
        usingNLogin = false;
        unsafeServerWarn = false;
        server = null;
        serverUuid = null;
        serverPublicKey = null;
        serverSignature = null;
        rsaChallenge = null;
        checksum = null;
        tmpPassword = null;
    }

    public void authenticate() {
        authenticated = true;
    }

}
