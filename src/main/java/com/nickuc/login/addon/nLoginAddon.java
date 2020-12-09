/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nickuc.login.addon.lang.Lang;
import com.nickuc.login.addon.listeners.JoinEvent;
import com.nickuc.login.addon.listeners.QuitEvent;
import com.nickuc.login.addon.listeners.ServerMessage;
import com.nickuc.login.addon.model.AddonSettings;
import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.model.Session;
import com.nickuc.login.addon.model.request.Request;
import com.nickuc.login.addon.model.response.Response;
import lombok.Cleanup;
import lombok.Getter;
import net.labymod.addon.AddonConfig;
import net.labymod.addon.AddonLoader;
import net.labymod.api.EventManager;
import net.labymod.api.LabyModAddon;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.StringElement;
import net.labymod.utils.Consumer;
import net.labymod.utils.Material;
import net.labymod.utils.manager.ConfigManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class nLoginAddon extends LabyModAddon {

    @Getter private final Session session = new Session();
    private File credentialsFile;
    @Getter private Credentials credentials;
    @Getter private AddonSettings settings;
    private boolean credentialsModified, settingsModified;

    @Override
    public void onEnable() {
        EventManager eventManager = getApi().getEventManager();
        eventManager.registerOnJoin(new JoinEvent(this));
        eventManager.registerOnQuit(new QuitEvent(this));
        eventManager.register(new ServerMessage(this));
    }

    private void onUnistall() {
        if (credentials != null) {
            File parent = credentialsFile.getParentFile();

            //if (parent.isDirectory() && parent.getName().contains("nlogin")) {
            if (parent.isDirectory() && parent.getName().equals("nLogin-Addon")) {
                File[] files = parent.listFiles();
                if (files == null) return;

                if (files.length > 0) {
                    for (File f : files) {
                        if (!f.getName().endsWith(".json")) continue;

                        if (!f.delete()) {
                            System.err.println(Constants.PREFIX + "Failed to delete '" + f.getPath() + "'.");
                        }
                    }
                }

                files = parent.listFiles();
                if (files == null) return;

                if (files.length == 0 && !parent.delete()) {
                    System.err.println(Constants.PREFIX + "Failed to delete directory '" + parent.getPath() + "'.");
                }
            }
        }
        System.out.println(Constants.PREFIX + "Addon successfully uninstalled.");
    }

    @Override
    public void loadConfig() {

        final JsonObject config = getConfig();
        settings = Constants.GSON_PRETTY.fromJson(config, AddonSettings.class);

        if (SystemUtils.IS_OS_WINDOWS) {
            credentialsFile = new File(System.getenv("APPDATA") + File.separator + "nlogin" + File.separator + "credentials.json");
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX) {
            credentialsFile = new File(System.getProperty("user.home"), ".nlogin" + File.separator + "credentials.json");
        } else {
            credentialsFile = new File(FileSystemView.getFileSystemView().getDefaultDirectory(), "nlogin" + File.separator + "credentials.json");
        }

        File parent = credentialsFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            try {
                Field f = LabyModAddon.class.getDeclaredField("configManager");
                f.setAccessible(true);
                ConfigManager<AddonConfig> configManager = (ConfigManager<AddonConfig>) f.get(this);
                credentialsFile = new File(configManager.getFile().getParentFile() + File.separator + "nLogin-Addon", "credentials.json");
                parent = credentialsFile.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    throw new SecurityException("Failed to create directory '" + parent.getPath() + "'!");
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                return;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            if (!credentialsFile.exists() && !credentialsFile.createNewFile()) {
                try {
                    Field f = LabyModAddon.class.getDeclaredField("configManager");
                    f.setAccessible(true);
                    ConfigManager<AddonConfig> configManager = (ConfigManager<AddonConfig>) f.get(this);
                    credentialsFile = new File(configManager.getFile().getParentFile() + File.separator + "nLogin-Addon", "credentials.json");
                    parent = credentialsFile.getParentFile();
                    if (parent.exists() || parent.mkdirs()) {
                        if (!credentialsFile.exists() && !credentialsFile.createNewFile()) {
                            throw new SecurityException("Failed to create file '" + credentialsFile.getPath() + "'!");
                        }
                    } else {
                        throw new SecurityException("Failed to create directory '" + parent.getPath() + "'!");
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    return;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return;
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

        String path = null;
        try {
            path = URLDecoder.decode(getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        final File addonFile = path == null ? null : new File(path);

        credentials = Credentials.fromJson(Constants.GSON.fromJson(content.isEmpty() ? "{}" : content, JsonObject.class));

        Timer timer = new Timer("nLoginAddon$Save");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                nLoginAddon addon = (nLoginAddon) AddonLoader.getAddonByUUID(nLoginAddon.this.about.uuid);
                if (addonFile != null && !addonFile.exists()) {
                    System.out.println(Constants.PREFIX + "Addon removed, performing unnistall tasks...");
                    onUnistall();
                    cancel();
                    return;
                }

                synchronized (Constants.LOCK) {
                    if (credentialsModified) {
                        System.out.println(Constants.PREFIX + "Saving credentials changes...");
                        try {
                            @Cleanup PrintWriter writter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(credentialsFile), Constants.UTF_8), true);
                            writter.print(Constants.GSON_PRETTY.toJson(credentials.toJson()));
                            writter.flush();
                            writter.close();
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
                        saveConfig();
                        nLoginAddon.this.settingsModified = false;
                    }
                }

                if (addon == null || !addon.about.loaded) {
                    cancel();
                }
            }
        }, 1000, 1000);

        Lang.loadAll();
    }

    @Override
    protected void fillSettings(final List<SettingsElement> settings) {

        final BooleanElement storePasswordElement = new BooleanElement(Lang.Message.SYNC_PASSWORDS_NAME.toText(), new ControlElement.IconData(Material.REDSTONE_COMPARATOR), new Consumer<Boolean>() {
            @Override
            public void accept(Boolean result) {
                nLoginAddon.this.settings.setSyncPasswords(result);
                markModified(false);
            }
        }, nLoginAddon.this.settings.isSyncPasswords());
        storePasswordElement.setDescriptionText(Lang.Message.SYNC_PASSWORDS_DESCRIPTION.toText());

        final StringElement masterPasswordElement = new StringElement(Lang.Message.MASTER_PASSWORD_NAME.toText(), new ControlElement.IconData(Material.PAPER), credentials.getMasterPassword(), new Consumer<String>() {
            @Override
            public void accept(String password) {
                credentials.setMasterPassword(password.trim());
                markModified(true);
            }
        });
        masterPasswordElement.setDescriptionText(Lang.Message.MASTER_PASSWORD_DESCRIPTION.toText());

        settings.add(storePasswordElement);
        settings.add(masterPasswordElement);
    }

    public void markModified(boolean credentials) {
        if (credentials) {
            credentialsModified = true;
        } else {
            settingsModified = true;
        }
    }

    public void sendRequest(Request request) {
        JsonObject json = new JsonObject();
        json.addProperty("id", request.getId());
        request.write(json);
        getApi().sendJsonMessageToServer(Constants.NLOGIN_SUBCHANNEL, json);
    }

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
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Failed to read " + clasz + ", content: " + json);
    }
}
