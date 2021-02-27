/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Session {

    private boolean active;
    @Setter private boolean requireSync;
    private boolean authenticated;
    @Setter private Credentials.Server server;
    @Setter private String serverUuid;
    @Setter private String checksum;
    @Setter private String tmpPassword;

    public void join() {
        active = true;
    }

    public void quit() {
        active = false;
        authenticated = false;
        requireSync = false;
        server = null;
        serverUuid = null;
        checksum = null;
        tmpPassword = null;
    }

    public void authenticate() {
        authenticated = true;
    }

}
