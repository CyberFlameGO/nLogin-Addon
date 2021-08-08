/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.updater;

import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.updater.http.Http;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;

public class Updater {

    private static final String CHECK_URL = "https://api.github.com/repos/nickuc/nLogin-Addon/releases/latest";
    private static final String DOWNLOAD_URL = "https://github.com/nickuc/nLogin-Addon/releases/latest/download/nLogin-Addon.jar";
    @Getter
    private static boolean updateAvailable;
    @Getter
    private static String newerVersion;

    public static void checkForUpdates(final String currentVersion) {
        String tagName = null;
        try {
            String result = Http.get(CHECK_URL);

            // avoid use Google Gson to avoid problems with older versions.
            if (result.contains("\"tag_name\":\"")) {
                tagName = result.split("\"tag_name\":\"")[1];
                ;
                if (tagName.contains("\",")) {
                    tagName = tagName.split("\",")[0];
                }
            }

            if (tagName == null) {
                System.err.println(Constants.PREFIX + "Failed to find new updates: invalid response.");
            } else {
                newerVersion = tagName;
                if (updateAvailable = !("v" + currentVersion).equals(newerVersion)) {
                    System.out.println(Constants.PREFIX + "A new version of nLogin-Addon is available (" + currentVersion + " -> " + newerVersion + ").");

                    final File labyModFolder = getLMDir();
                    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            performUpdate(labyModFolder);
                        }
                    }));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(Constants.PREFIX + "Failed to find new updates.");
        }
    }

    private static void performUpdate(File labyModFolder) {
        if (updateAvailable) {
            try {
                File addonFile18tmp = getAddonTmpFile(labyModFolder, "1.8");
                File addonFile112tmp = getAddonTmpFile(labyModFolder, "1.12");
                Http.download(DOWNLOAD_URL, addonFile18tmp, addonFile112tmp);

                if (addonFile18tmp.length() > 0 && addonFile112tmp.length() > 0) {
                    File pluginFile = getJarFile();
                    pluginFile.deleteOnExit();

                    performUpdate0(labyModFolder, addonFile18tmp, "1.8");
                    performUpdate0(labyModFolder, addonFile112tmp, "1.12");
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println(Constants.PREFIX + "Failed to download new version.");
            }
        }
    }

    private static File getAddonTmpFile(File labyModFolder, String ver) {
        File addons = new File(labyModFolder, String.format("addons-%s", ver));
        File addonFile = new File(addons, "nLogin-Addon.tmp");
        int index = 0;
        while (true) {
            if (addonFile.exists() && !addonFile.delete()) {
                addonFile.deleteOnExit();
                addonFile = new File(addons, "nLogin-Addon-" + index++ + ".tmp");
            } else {
                break;
            }
        }
        return addonFile;
    }

    private static void performUpdate0(File labyModFolder, File tmpFile, String ver) {
        File addons = new File(labyModFolder, String.format("addons-%s", ver));
        File[] files = addons.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                if (name.contains("nLogin-Addon") && name.endsWith(".jar")) {
                    f.delete();
                }
            }
        }

        File addonFile = new File(addons, "nLogin-Addon.jar");
        int index = 0;
        while (true) {
            if (addonFile.exists() && !addonFile.delete()) {
                addonFile = new File(addons, "nLogin-Addon-" + index++ + ".jar");
            } else {
                break;
            }
        }
        tmpFile.renameTo(addonFile);
    }

    private static File getMCDir() {
        final String userHomeDir = System.getProperty("user.home", ".");
        final String osType = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        final String mcDir = ".minecraft";
        if (osType.contains("win") && System.getenv("APPDATA") != null) {
            return new File(System.getenv("APPDATA"), mcDir);
        }
        if (osType.contains("mac")) {
            return new File(new File(new File(userHomeDir, "Library"), "Application Support"), "minecraft");
        }
        return new File(userHomeDir, mcDir);
    }

    private static File getLMDir() {
        return new File(getMCDir(), "LabyMod");
    }

    private static File getJarFile() throws UnsupportedEncodingException {
        return new File(URLDecoder.decode(Updater.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath(), "UTF-8"));
    }

}
