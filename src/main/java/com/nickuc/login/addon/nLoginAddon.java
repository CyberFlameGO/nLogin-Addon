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
import com.nickuc.login.addon.listeners.MessageReceived;
import com.nickuc.login.addon.listeners.QuitEvent;
import com.nickuc.login.addon.listeners.ServerMessage;
import com.nickuc.login.addon.model.AddonSettings;
import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.model.Session;
import com.nickuc.login.addon.model.request.Request;
import com.nickuc.login.addon.model.response.Response;
import com.nickuc.login.addon.updater.Updater;
import lombok.Cleanup;
import lombok.Getter;
import net.labymod.addon.AddonConfig;
import net.labymod.api.EventManager;
import net.labymod.api.LabyModAddon;
import net.labymod.gui.elements.DropDownMenu;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.*;
import net.labymod.utils.Consumer;
import net.labymod.utils.Material;
import net.labymod.utils.manager.ConfigManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class nLoginAddon extends LabyModAddon {

    public static final Object LOCK = new Object();

    @Getter
    private final Session session = new Session();
    private File credentialsFile;
    @Getter
    private Credentials credentials;
    @Getter
    private AddonSettings settings;
    private boolean credentialsModified, settingsModified;

    @Override
    public void onEnable() {
        EventManager eventManager = getApi().getEventManager();
        eventManager.registerOnJoin(new JoinEvent(this));
        eventManager.registerOnQuit(new QuitEvent(this));
        eventManager.register(new ServerMessage(this));
        eventManager.register(new MessageReceived(this));
        Updater.checkForUpdates(Constants.VERSION);
        if (Updater.isUpdateAvailable()) {
            Timer timer = new Timer("nLoginAddon$UpdateWarning");
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    synchronized (Constants.LOCK) {
                        if (LabyMod.getInstance().isInGame()) {
                            LabyMod.getInstance().notifyMessageRaw(Constants.DEFAULT_TITLE, String.format(Lang.Message.UPDATE_AVAILABLE.toText(), "v" + Constants.VERSION, Updater.getNewerVersion()));
                        }
                    }
                }
            }, TimeUnit.SECONDS.toMillis(30), TimeUnit.MINUTES.toMillis(30));
        }
    }

    @Override
    public void loadConfig() {
        final JsonObject config = getConfig();
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
                        saveConfig();
                        nLoginAddon.this.settingsModified = false;
                    }
                }

                if (!nLoginAddon.this.about.loaded) {
                    cancel();
                }
            }
        }, TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(1));

        Lang.loadAll();
        Lang.setLang(Lang.Type.findByLocale(settings.getLanguage()));
    }

    @Override
    protected void fillSettings(final List<SettingsElement> settings) {

        final BooleanElement enabledElement = new BooleanElement(Lang.Message.ENABLED_NAME.toText(), new ControlElement.IconData(Material.LEVER), new Consumer<Boolean>() {
            @Override
            public void accept(Boolean result) {
                nLoginAddon.this.settings.setEnabled(result);
                markModified(false);
            }
        }, nLoginAddon.this.settings.isEnabled());

        final BooleanElement securityWarningsElement = new BooleanElement(Lang.Message.SECURITY_WARNINGS_NAME.toText(), new ControlElement.IconData(Material.REDSTONE_COMPARATOR), new Consumer<Boolean>() {
            @Override
            public void accept(Boolean result) {
                nLoginAddon.this.settings.setSecurityWarnings(result);
                markModified(false);
            }
        }, nLoginAddon.this.settings.isSecurityWarnings());
        securityWarningsElement.setDescriptionText(Lang.Message.SECURITY_WARNINGS_DESCRIPTION.toText());

        final BooleanElement saveLoginElement = new BooleanElement(Lang.Message.SAVE_LOGIN_NAME.toText(), new ControlElement.IconData(Material.REDSTONE_COMPARATOR), new Consumer<Boolean>() {
            @Override
            public void accept(Boolean result) {
                nLoginAddon.this.settings.setSaveLogin(result);
                markModified(false);
            }
        }, nLoginAddon.this.settings.isSaveLogin());
        saveLoginElement.setDescriptionText(Lang.Message.SAVE_LOGIN_DESCRIPTION.toText());

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

        Lang.Type[] langTypes0 = Lang.Type.values();
        String[] langTypes = new String[langTypes0.length];
        for (int i = 0; i < langTypes0.length; i++) {
            langTypes[i] = langTypes0[i].name();
        }

        final DropDownMenu<String> langSelectorDropDownMenu = new DropDownMenu<String>("Language", 0, 0, 0, 0).fill(langTypes);
        DropDownElement<String> langSelectorDropDown = new DropDownElement<String>("Language", langSelectorDropDownMenu);
        langSelectorDropDownMenu.setSelected(nLoginAddon.this.settings.getLanguage());

        langSelectorDropDown.setChangeListener(new Consumer<String>() {
            @Override
            public void accept(String type) {
                nLoginAddon.this.settings.setLanguage(type);
                markModified(false);
            }
        });

        /*
        langSelectorDropDownMenu.setEntryDrawer(new DropDownMenu.DropDownEntryDrawer() {
            @Override
            public void draw(Object object, int x, int y, String trimmedEntry) {
                String entry = object.toString().toLowerCase();
                LabyMod.getInstance().getDrawUtils().drawString(LanguageManager.translate(entry), x, y);
            }
        });
         */

        settings.add(langSelectorDropDown);
        settings.add(enabledElement);
        settings.add(securityWarningsElement);
        settings.add(saveLoginElement);
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
