/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class Http {

    private static final String USER_AGENT = "nLogin-Addon Installer (+https://github.com/nickuc/nLogin-Addon)";
    private static final int TIMEOUT = 15000;

    private static void buildHttp(HttpURLConnection http) throws IllegalArgumentException {
        http.setInstanceFollowRedirects(false);
        http.setRequestProperty("User-Agent", USER_AGENT);
        http.setConnectTimeout(TIMEOUT);
        http.setReadTimeout(TIMEOUT * 2);
    }

    public static void download(String url, File... output) throws IOException {
        HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
        buildHttp(http);

        boolean redirect;
        do {
            int status = http.getResponseCode();
            redirect = status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER;

            if (redirect) {
                String newUrl = http.getHeaderField("Location");
                http = (HttpURLConnection) new URL(newUrl).openConnection();
                buildHttp(http);
            }

        } while (redirect);

        for (File f : output) {
            if (f.exists()) {
                f.delete();
            }
        }

        try (BufferedInputStream bin = new BufferedInputStream(http.getInputStream())) {
            FileOutputStream[] fosArray = new FileOutputStream[output.length];
            BufferedOutputStream[] boutArray = new BufferedOutputStream[output.length];
            try {
                int bufSize = 8192;
                for (int i = 0; i < output.length; i++) {
                    boutArray[i] = new BufferedOutputStream(fosArray[i] = new FileOutputStream(output[i]), bufSize);
                }

                byte[] buf = new byte[bufSize];
                int cp;
                while ((cp = bin.read(buf, 0, buf.length)) >= 0) {
                    for (BufferedOutputStream bout : boutArray) {
                        bout.write(buf, 0, cp);
                    }
                }
            } finally {
                for (int i = 0; i < output.length; i++) {
                    BufferedOutputStream bout = boutArray[i];
                    if (bout != null) {
                        bout.close();
                    }
                    FileOutputStream fos = fosArray[i];
                    if (fos != null) {
                        fos.close();
                    }
                }
            }
        }
    }

}
