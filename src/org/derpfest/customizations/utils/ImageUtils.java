/*
 * Copyright (C) 2023-2024 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.derpfest.customizations.utils;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImageUtils {

    /**
     * Copies an image to shared storage and returns its absolute path.
     * Older copies with the same prefix are removed only after the new file is written.
     */
    public static String saveImageToInternalStorage(Context context, Uri imgUri, String featurePath, String filePrefix) {
        File outFile = null;
        try {
            Uri source = resolveSource(imgUri);
            String mime = context.getContentResolver().getType(source);
            File directory = new File("/sdcard/risingOS/" + featurePath);
            if (!directory.exists() && !directory.mkdirs()) {
                return null;
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            outFile = new File(directory, filePrefix + "_" + timeStamp + "." + extensionFor(mime));
            try (InputStream in = context.getContentResolver().openInputStream(source);
                    OutputStream out = new FileOutputStream(outFile)) {
                if (in == null) {
                    throw new FileNotFoundException("Unable to open " + source);
                }
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
            }
            deleteOlderCopies(directory, filePrefix, outFile.getName());
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            if (outFile != null && outFile.exists()) {
                outFile.delete();
            }
            return null;
        }
    }

    private static Uri resolveSource(Uri imgUri) throws Exception {
        if (!imgUri.toString().startsWith("content://com.google.android.apps.photos.contentprovider")) {
            return imgUri;
        }
        List<String> segments = imgUri.getPathSegments();
        if (segments.size() <= 2) {
            throw new FileNotFoundException("Failed to parse Google Photos content URI");
        }
        String mediaUriString = URLDecoder.decode(segments.get(2), StandardCharsets.UTF_8.name());
        return Uri.parse(mediaUriString);
    }

    private static String extensionFor(String mime) {
        if ("image/png".equals(mime)) {
            return "png";
        }
        if ("image/webp".equals(mime)) {
            return "webp";
        }
        if ("image/gif".equals(mime)) {
            return "gif";
        }
        return "jpg";
    }

    private static void deleteOlderCopies(File directory, String filePrefix, String keepName) {
        File[] files = directory.listFiles((dir, name) ->
                name.startsWith(filePrefix) && !name.equals(keepName));
        if (files == null) {
            return;
        }
        for (File file : files) {
            file.delete();
        }
    }
}
