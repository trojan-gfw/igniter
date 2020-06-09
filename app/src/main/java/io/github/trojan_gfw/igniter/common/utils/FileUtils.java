package io.github.trojan_gfw.igniter.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

    public static boolean copy(File src, File dest) {
        try (FileOutputStream fos = new FileOutputStream(dest);
             FileInputStream fis = new FileInputStream(src)) {
            byte[] buff = new byte[4096];
            int readBytes;
            while ((readBytes = fis.read(buff)) != -1) {
                fos.write(buff, 0, readBytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
