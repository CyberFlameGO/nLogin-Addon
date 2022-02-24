/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.event;

import com.nickuc.login.addon.nLoginAddon;

public interface LMEventService {

    static LMEventService getImplementation() {
        try {
            return new EventService18();
        } catch (Throwable e) {
            return new EventService116();
        }
    };

    void registerEvents(nLoginAddon addon) throws Exception;

}
