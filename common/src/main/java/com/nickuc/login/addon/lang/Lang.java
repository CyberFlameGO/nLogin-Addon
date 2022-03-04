/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.lang;

import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.nLoginAddon;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Lang {

    private static final Map<String, String> LANG_MAP = new HashMap<>();
    @Getter
    @Setter
    private static Type lang = Type.EN_US;

    @AllArgsConstructor
    public enum Type {

        EN_US("en_US.lang", "EN_US"),
        PT_BR("pt_BR.lang", "PT_BR");

        private final String file;
        private final String locale;

        public static Type findByLocale(String locale) {
            for (Type type : values()) {
                String localeUpper = locale.toUpperCase();
                if (type.locale.equals(localeUpper) || localeUpper.startsWith(type.locale.substring(0, 2))) return type;
            }
            return EN_US;
        }

    }

    @AllArgsConstructor
    public enum Message {

        // settings
        ENABLED_NAME("settings.enabled.name"),
        DEBUG_NAME("settings.debug.name"),
        DEBUG_DESCRIPTION("settings.debug.description"),
        SAVE_LOGIN_NAME("settings.save-login.name"),
        SAVE_LOGIN_DESCRIPTION("settings.save-login.description"),
        SYNC_PASSWORDS_NAME("settings.sync-passwords.name"),
        SYNC_PASSWORDS_DESCRIPTION("settings.sync-passwords.description"),
        MASTER_PASSWORD_NAME("settings.master-password.name"),
        MASTER_PASSWORD_DESCRIPTION("settings.master-password.description"),
        SECURITY_WARNINGS_NAME("settings.security-warnings.name"),
        SECURITY_WARNINGS_DESCRIPTION("settings.security-warnings.description"),

        // in-game
        INVALID_SERVER_UUID("in-game.invalid-server-uuid"),
        INVALID_SERVER_PUBLIC_KEY("in-game.invalid-server-pbkey"),
        INVALID_SERVER_SIGNATURE("in-game.invalid-server-signature"),
        REGISTERING_A_PASSWORD("in-game.registering-a-password"),
        REGISTERING_A_PASSWORD2("in-game.registering-a-password2"),
        SYNC_FAILED_ENCRYPT("in-game.sync.failed-encrypt"),
        SYNC_REQUIRE_PASSWORD("in-game.sync.require-password"),
        SYNC_FAILED_DECRYPT("in-game.sync.failed-decrypt"),
        SYNC_FAILED_DECRYPT2("in-game.sync.failed-decrypt2"),
        SYNC_SYNCING("in-game.sync.syncing"),
        STATUS_MESSAGE2("in-game.status.message2"),
        STATUS_MESSAGE3("in-game.status.message3"),
        STATUS_MESSAGE4("in-game.status.message4"),
        STATUS_UNKNOWN("in-game.status.unknown"),
        UPDATE_AVAILABLE("in-game.update-available");

        private final String key;

        public String toText() {
            String prefixKey = lang.name().toLowerCase() + "_";
            return LANG_MAP.get(prefixKey + key);
        }

    }

    private static final String CHARACTERS = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr";
    private static final char COLOR_CODE = 167;
    private static final char ALT_COLOR_CHAR = '&';

    /**
     * This method was taken from Bukkit-API (class: org.bukkit.ChatColor)
     * <p>
     * Translates a string using an alternate color code character into a
     * string that uses the internal ChatColor.COLOR_CODE color code
     * character. The alternate color code character will only be replaced if
     * it is immediately followed by 0-9, A-F, a-f, K-O, k-o, R or r.
     *
     * @param textToTranslate Text containing the alternate color code character.
     * @return Text containing the Messages.COLOR_CODE color code character.
     */
    private static String translateAlternateColorCodes(String textToTranslate) {
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == ALT_COLOR_CHAR && CHARACTERS.indexOf(b[i + 1]) > -1) {
                b[i] = COLOR_CODE;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    public static void loadAll() {
        for (Type type : Type.values()) {
            System.out.println(Constants.PREFIX + "Loading the language " + type);
            try {
                String resourcePath = "/lang/" + type.file;
                @Cleanup InputStream inputStream = nLoginAddon.class.getResourceAsStream(resourcePath);
                if (inputStream == null) {
                    throw new RuntimeException("Resource " + resourcePath + " not found!");
                }

                @Cleanup InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Constants.UTF_8);
                @Cleanup BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                String keyPrefix = type.name().toLowerCase() + "_";
                while ((line = bufferedReader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (!line.contains("=")) continue;

                    String[] parts = line.split("=");
                    if (parts.length != 2) continue;

                    LANG_MAP.put(keyPrefix + parts[0].toLowerCase(), translateAlternateColorCodes(parts[1]).replace("\\n", "\n"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
