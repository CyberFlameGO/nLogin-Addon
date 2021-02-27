/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Constants {

    public static final Object LOCK = nLoginAddon.LOCK;

    public static final Gson GSON = new Gson();
    public static final Gson GSON_PRETTY = (new GsonBuilder()).setPrettyPrinting().create();

    public static final String DEFAULT_TITLE = "nLogin Addon";
    public static final String NLOGIN_SUBCHANNEL = "nlogin-addon";
    public static final String PREFIX = "[nLoginAddon] ";

    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(2);

    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    public static int DEFAULT_PASSWORD_LENGTH = 12;
    public static int USER_CRYPT_KEY_LENGTH = 192;

}
