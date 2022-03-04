/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.bootstrap;

import com.nickuc.login.addon.model.request.Request;

public interface Platform {

    boolean isEnabled();

    void saveConfig();

    String getPlayerName();

    boolean isConnected();

    void sendRequest(Request request);

    void sendMessage(String message);

    void sendChatPacket(String message);

    void sendNotification(String message);

}
