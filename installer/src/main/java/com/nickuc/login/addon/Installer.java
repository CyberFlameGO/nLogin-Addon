/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class Installer extends JPanel {

    private static final String DOWNLOAD_URL = "https://github.com/nickuc/nLogin-Addon/releases/latest/download/nLogin-Addon.jar";

    private static final String[] INSTALL_CONFIRMATION = new String[] {
            "Install confirmation",
            "Confirmação da instalação"
    };
    private static final String[] FAILED_TO_INSTALL = new String[] {
            "Failed to install",
            "Falha na instalação"
    };
    private static final String[] SUCCESSFUL_INSTALLATION = new String[] {
            "Successful installation",
            "Instalação bem-sucedida"
    };
    private static final String[] INSTALLATION_QUESTION = new String[] {
            "Do you want to install the nLogin Addon?",
            "Você quer instalar o nLogin Addon?"
    };
    private static final String[] INSTALLATION_ERR_MINECRAFT = new String[] {
            "Could not detect .minecraft.",
            "Não foi possível detectar a .minecraft."
    };
    private static final String[] INSTALLATION_ERR_LABYMOD = new String[] {
            "Could not detect the LabyMod's folder.",
            "Não foi possível detectar a pasta do LabyMod."
    };
    private static final String[] INSTALLATION_ERR_MKDIR = new String[] {
            "Could not create addon folders (%s).",
            "Não foi possível gerar as pastas do addon (%s)."
    };
    private static final String[] INSTALLATION_ERR_DEL = new String[] {
            "Could not delete old addon version (%s).",
            "Não foi possível deletar o jar do addon antigo (%s)."
    };
    private static final String[] DOWNLOAD_ERR = new String[] {
            "Could not download the addon file:\n%s",
            "Não foi possível baixar o arquivo do addon:\n%s"
    };
    private static final String[] INSTALLATION_ERR_WRITE = new String[] {
            "Could not write the addon file: insufficient permissions?",
            "Não foi possível escrever o arquivo do addon: permissões insuficientes?"
    };
    private static final String[] REINSTALLATION_SUCCESS = new String[] {
            "The nLogin Addon has been successfully reinstalled on your LabyMod.",
            "O nLogin Addon foi reinstalado com sucesso no LabyMod."
    };
    private static final String[] INSTALLATION_SUCCESS = new String[] {
            "The nLogin Addon has been successfully installed on your LabyMod.\nFor more information: https://www.nickuc.com/docs/nlogin-addon",
            "O nLogin Addon foi reinstalado com sucesso no seu LabyMod.\nPara mais informações: https://www.nickuc.com/docs/nlogin-addon"
    };

    private static boolean direct;

    public static void main(String[] args) {
        direct = args.length > 0 && args[0].equals("-direct");
        int lang = Language.get().id;
        if (direct || JOptionPane.showConfirmDialog(null, INSTALLATION_QUESTION[lang], INSTALL_CONFIRMATION[lang], JOptionPane.YES_NO_OPTION) == 0) {
            File mcDir = getMCDir();
            if (!mcDir.exists()) {
                sendMessage(INSTALLATION_ERR_MINECRAFT[lang], FAILED_TO_INSTALL[lang], true);
                System.exit(1);
                return;
            }

            File labyModFolder = new File(mcDir, "LabyMod");
            if (!labyModFolder.exists() || !labyModFolder.isDirectory()) {
                sendMessage(INSTALLATION_ERR_LABYMOD[lang], FAILED_TO_INSTALL[lang], true);
                System.exit(1);
                return;
            }

            File addonFile18 = mkdirsAndRemove("1.8", labyModFolder, lang);
            if (addonFile18 == null) {
                return;
            }

            File addonFile112 = mkdirsAndRemove("1.12", labyModFolder, lang);
            if (addonFile112 == null) {
                return;
            }

            try {
                Http.download(DOWNLOAD_URL, addonFile18, addonFile112);
                if (addonFile18.exists() && addonFile112.exists()) {
                    sendMessage(INSTALLATION_SUCCESS[lang], SUCCESSFUL_INSTALLATION[lang], false);
                    System.exit(0);
                } else {
                    sendMessage(INSTALLATION_ERR_WRITE[lang], FAILED_TO_INSTALL[lang], true);
                    System.exit(1);
                }
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage(String.format(DOWNLOAD_ERR[lang], e.getLocalizedMessage()), FAILED_TO_INSTALL[lang], true);
                System.exit(1);
            }
        }
    }

    private static File mkdirsAndRemove(String ver, File labyModFolder, int lang) {
        File addons = new File(labyModFolder, String.format("addons-%s", ver));
        if (!addons.exists() && !addons.mkdirs()) {
            sendMessage(String.format(INSTALLATION_ERR_MKDIR[lang], "1.8"), FAILED_TO_INSTALL[lang], true);
            System.exit(1);
            return null;
        }

        File addonFile = new File(addons, "nLogin-Addon.jar");
        if (addonFile.exists() && !addonFile.delete()) {
            sendMessage(String.format(INSTALLATION_ERR_DEL[lang], "1.8"), FAILED_TO_INSTALL[lang], true);
            System.exit(1);
            return null;
        }
        return addonFile;
    }

    private static void sendMessage(String message, String title, boolean err) {
        if (direct) {
            if (err) {
                System.err.println(message);
            } else {
                System.out.println(message);
            }
        } else {
            JOptionPane.showMessageDialog(null, message, title, err ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        }
        System.exit(err ? 1 : 0);
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

    public enum Language {

        EN("en", 0),
        PT("pt", 1);

        private final String lang;
        public final int id;

        Language(String lang, int id) {
            this.lang = lang;
            this.id = id;
        }

        public static Language get() {
            String locale = Locale.getDefault().toString();
            System.out.println("Detected Locale: " + locale);
            for (Language lang : values()) {
                if (locale.startsWith(lang.lang)) {
                    return lang;
                }
            }
            return EN;
        }
    }

}
