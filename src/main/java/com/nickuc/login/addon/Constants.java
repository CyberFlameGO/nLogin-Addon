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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Constants {

    public static final Object LOCK = nLoginAddon.LOCK;

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    public static final Gson GSON_PRETTY = (new GsonBuilder()).disableHtmlEscaping().setPrettyPrinting().create();

    public static final String DEFAULT_TITLE = "nLogin Addon";
    public static final String NLOGIN_SUBCHANNEL = "nlogin-addon";
    public static final String PREFIX = "[nLoginAddon] ";
    public static final String VERSION = "1.4";

    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(2);

    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final Pattern UUID_PATTERN = Pattern.compile("\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b");

    public static final int DEFAULT_PASSWORD_LENGTH = 12;
    public static final int USER_CRYPT_KEY_LENGTH = 192;
    public static final int RSA_CHALLENGE_LENGTH = 4;

}
