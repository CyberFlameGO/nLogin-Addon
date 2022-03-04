/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.updater.http;

import com.nickuc.login.addon.Constants;
import lombok.Cleanup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

@RequiredArgsConstructor
@Getter
@Accessors(chain = true, fluent = true)
public final class Http {

    private static final String USER_AGENT = "nLogin-Addon (+https://github.com/nickuc/nLogin-Addon)";
    private static final int TIMEOUT = 15000;

    private static void buildHttp(HttpURLConnection http, String requestMethod) throws IllegalArgumentException {
        http.setInstanceFollowRedirects(false);
        if (requestMethod != null && !requestMethod.isEmpty()) {
            try {
                http.setRequestMethod(requestMethod);
            } catch (ProtocolException e) {
                throw new IllegalArgumentException("Method '" + requestMethod + "' does not exists!", e);
            }
        }
        http.setRequestProperty("User-Agent", USER_AGENT);
        http.setConnectTimeout(TIMEOUT);
        http.setReadTimeout(TIMEOUT * 2);
    }

    public static String get(String url) throws IOException {
        String method = "GET";
        HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
        buildHttp(http, method);

        boolean redirect;
        do {
            int status = http.getResponseCode();
            redirect = status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER;

            if (redirect) {
                String newUrl = http.getHeaderField("Location");
                http = (HttpURLConnection) new URL(newUrl).openConnection();
                buildHttp(http, method);
            }

        } while (redirect);

        @Cleanup InputStream inputStream = http.getInputStream();
        @Cleanup BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Constants.UTF_8));
        StringBuilder response = new StringBuilder();
        int cp;
        while ((cp = bufferedReader.read()) != -1) {
            response.append((char) cp);
        }
        return response.toString();
    }

    public static void download(String url, File... output) throws IOException {
        String method = "GET";
        HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
        buildHttp(http, method);

        boolean redirect;
        do {
            int status = http.getResponseCode();
            redirect = status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER;

            if (redirect) {
                String newUrl = http.getHeaderField("Location");
                http = (HttpURLConnection) new URL(newUrl).openConnection();
                buildHttp(http, method);
            }

        } while (redirect);

        for (File f : output) {
            if (f.exists()) {
                f.delete();
            }
        }

        @Cleanup BufferedInputStream bin = new BufferedInputStream(http.getInputStream());
        FileOutputStream[] fosArray = new FileOutputStream[output.length];
        BufferedOutputStream[] boutArray = new BufferedOutputStream[output.length];
        try {
            for (int i = 0; i < output.length; i++) {
                boutArray[i] = new BufferedOutputStream(fosArray[i] = new FileOutputStream(output[i]), 1024);
            }

            byte[] data = new byte[1024];
            int cp;
            while ((cp = bin.read(data, 0, 1024)) >= 0) {
                for (BufferedOutputStream bout : boutArray) {
                    bout.write(data, 0, cp);
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
