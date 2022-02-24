/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.utils;

import net.labymod.utils.Material;

import java.util.Arrays;

public class MaterialUtils {

    public static final Material LEVER = getMaterial("LEVER");
    public static final Material COMMAND_BLOCK = getMaterial("COMMAND");
    public static final Material REDSTONE_COMPARATOR = getMaterial("COMPARATOR", "REDSTONE_COMPARATOR");
    public static final Material PAPER = getMaterial("PAPER");

    public static Material getMaterial(String... names) {
        Material material = null;
        for (String materialName : names) {
            try {
                material = Material.valueOf(materialName);
                break;
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (material == null) {
            throw new RuntimeException("Could not find material: " + Arrays.toString(names));
        }
        return material;
    }

}
