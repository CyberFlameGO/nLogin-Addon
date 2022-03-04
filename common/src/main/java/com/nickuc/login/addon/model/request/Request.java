/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.model.request;

import com.google.gson.JsonObject;

public interface Request {

    void write(JsonObject json);

    int getId();

}
