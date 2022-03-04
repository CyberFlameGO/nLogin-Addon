/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nickuc.login.addon.bootstrap.Platform;
import com.nickuc.login.addon.bootstrap.PlatformBootstrap;
import com.nickuc.login.addon.handler.EventHandler;
import com.nickuc.login.addon.lang.Lang;
import com.nickuc.login.addon.model.AddonSettings;
import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.model.Session;
import com.nickuc.login.addon.model.response.Response;
import com.nickuc.login.addon.updater.Updater;
import lombok.Cleanup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class nLoginAddon {

    public static final Object LOCK = new Object();

    private final Session session = new Session();
    @Getter
    private EventHandler eventHandler;
    private File credentialsFile;
    @Getter
    private Credentials credentials;
    @Getter
    private AddonSettings settings;
    private boolean credentialsModified, settingsModified;

    @Getter
    private final Platform platform;
    private final PlatformBootstrap platformBootstrap;

    public void onEnable() {
        Updater.checkForUpdates(Constants.VERSION);
        if (Updater.isUpdateAvailable()) {
            Timer timer = new Timer("nLoginAddon$UpdateWarning");
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    platform.sendNotification(String.format(Lang.Message.UPDATE_AVAILABLE.toText(), "v" + Constants.VERSION, Updater.getNewerVersion()));
                }
            }, TimeUnit.SECONDS.toMillis(30), TimeUnit.MINUTES.toMillis(30));
        }
        eventHandler = new EventHandler(this, platform);
        platformBootstrap.registerEvents();
    }

    public void loadConfig(final JsonObject config, File addonSettingsFolder) {
        settings = Constants.GSON_PRETTY.fromJson(config, AddonSettings.class);

        if (SystemUtils.IS_OS_WINDOWS) {
            credentialsFile = new File(System.getenv("APPDATA") + File.separator + "nLogin", "credentials.json");
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX) {
            credentialsFile = new File(System.getProperty("user.home"), ".nlogin" + File.separator + "credentials.json");
        } else {
            credentialsFile = new File(FileSystemView.getFileSystemView().getDefaultDirectory() + File.separator + "nLogin", "credentials.json");
        }

        File parent = credentialsFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            credentialsFile = new File(addonSettingsFolder, "credentials.json");
            parent = credentialsFile.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new SecurityException("Failed to create directory '" + parent.getPath() + "'!");
            }
        }

        try {
            if (!credentialsFile.exists() && !credentialsFile.createNewFile()) {
                credentialsFile = new File(addonSettingsFolder, "credentials.json");
                parent = credentialsFile.getParentFile();
                if (parent.exists() || parent.mkdirs()) {
                    if (!credentialsFile.exists() && !credentialsFile.createNewFile()) {
                        throw new SecurityException("Failed to create file '" + credentialsFile.getPath() + "'!");
                    }
                } else {
                    throw new SecurityException("Failed to create directory '" + parent.getPath() + "'!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        FileInputStream stream;
        try {
            stream = new FileInputStream(credentialsFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        String content;
        try {
            content = IOUtils.toString(stream, Constants.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        credentials = Credentials.fromJson(Constants.GSON.fromJson(content.isEmpty() ? "{}" : content, JsonObject.class));

        Timer timer = new Timer("nLoginAddon$Save");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (Constants.LOCK) {
                    if (credentialsModified) {
                        System.out.println(Constants.PREFIX + "Saving credentials changes...");
                        try {
                            @Cleanup PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(credentialsFile), Constants.UTF_8), true);
                            writer.print(Constants.GSON_PRETTY.toJson(credentials.toJson()));
                            writer.flush();
                            writer.close();
                        } catch (Exception var2) {
                            var2.printStackTrace();
                        }
                        nLoginAddon.this.credentialsModified = false;
                    }
                    if (settingsModified) {
                        try {
                            settings.toJson(config);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        Lang.setLang(Lang.Type.findByLocale(settings.getLanguage()));
                        platform.saveConfig();
                        nLoginAddon.this.settingsModified = false;
                    }
                }

                if (!platform.isEnabled()) {
                    cancel();
                }
            }
        }, TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(1));

        Lang.loadAll();
        Lang.setLang(Lang.Type.findByLocale(settings.getLanguage()));
    }

    public synchronized Session getSession() {
        return session;
    }

    public void markModified(boolean credentials) {
        if (credentials) {
            credentialsModified = true;
        } else {
            settingsModified = true;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T readResponse(JsonElement element, Class<T> clasz) {
        Class<?>[] interfaces = clasz.getInterfaces();
        if (interfaces.length == 0 || !interfaces[0].isAssignableFrom(Response.class)) {
            throw new IllegalArgumentException("Class must be assignable from Response!");
        }

        JsonObject json = element.getAsJsonObject();
        try {
            Response response = (Response) clasz.newInstance();
            response.read(json);
            return (T) response;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to read " + clasz + ", content: " + json, e);
        }
    }
}
