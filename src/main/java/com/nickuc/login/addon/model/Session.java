/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.model;

import lombok.Getter;
import lombok.Setter;

public class Session {

    @Getter private boolean active;
    @Getter @Setter private boolean requireSync;
    @Getter @Setter private Credentials.Server server;
    @Getter @Setter private String checksum;

    public void join() {
        active = true;
    }

    public void quit() {
        active = false;
        requireSync = false;
        server = null;
        checksum = null;
    }

}
