package com.example.plugin;

import com.android.build.api.transform.Transform;
import com.android.build.api.variant.VariantInfo;
import com.android.build.gradle.internal.pipeline.TransformTask;
import com.android.tools.build.jetifier.processor.transform.proguard.ProGuardTransformer;

import org.apache.commons.codec.digest.DigestUtils;
import org.gradle.api.Task;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static String capitalize(String self) {

        return self.length() == 0 ? "" : " " + Character.toUpperCase(self.charAt(0)) + self.subSequence(1, self.length());
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }

    public static byte[] readFile(File file) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            int len;
            byte[] buffer = new byte[1024];
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        byte[] bytes = bos.toByteArray();
        try {
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static void applyMapping(Task proguardTask, File mappingFile) {
        if (mappingFile.exists() && proguardTask != null) {
            TransformTask transformTask = (TransformTask) proguardTask;
            Transform transform = transformTask.getTransform();
            transform.applyToVariant((VariantInfo) mappingFile);
        }

    }

    public static boolean isAndroidClass(String filePath) {
        return filePath.startsWith("android") ||
                filePath.startsWith("androidx");
    }

    public static String hex(byte[] bytes) {

        return DigestUtils.md5Hex(bytes);
    }

    public static Map<String, String> readHex(File hexFile) {
        Map<String, String> hashMap = new HashMap<>();
        if (!hexFile.exists()) {
            return hashMap;
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(hexFile)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split(":");
                if (split != null && split.length == 2) {
                    hashMap.put(split[0], split[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return hashMap;
    }

    public static void writeHex(Map<String, String> hexs, File hexFile) {
        try {
            FileOutputStream fos = new FileOutputStream(hexFile);
            for (String key : hexs.keySet()) {
                String value = hexs.get(key);
                String line = key + ":" + value + "\n";
                fos.write(line.getBytes());
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
