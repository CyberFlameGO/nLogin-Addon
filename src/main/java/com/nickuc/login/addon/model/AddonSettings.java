/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.model;

import com.google.gson.JsonObject;
import com.nickuc.login.addon.lang.Lang;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Field;

@NoArgsConstructor(access = AccessLevel.PRIVATE) @Getter @Setter
public class AddonSettings {

    private boolean enabled = true;
    private boolean syncPasswords = true;
    private boolean saveLogin = true;
    private String language = Lang.Type.EN_US.name();

    public void toJson(JsonObject json) throws IllegalAccessException {
        json.entrySet().clear();
        Field[] fields = AddonSettings.class.getDeclaredFields();
        for (Field field : fields) {
            String key = field.getName();
            Object value = field.get(this);
            Class<?> type = field.getType();
            if (type.isAssignableFrom(boolean.class)) {
                json.addProperty(key, (Boolean) value);
            } else if (type.isAssignableFrom(String.class)) {
                json.addProperty(key, String.valueOf(value));
            }
        }
    }

}
