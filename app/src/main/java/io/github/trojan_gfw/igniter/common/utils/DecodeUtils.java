package io.github.trojan_gfw.igniter.common.utils;

import android.text.TextUtils;
import android.util.Base64;

import java.nio.charset.StandardCharsets;

public class DecodeUtils {

    public static String decodeBase64(String rawStr) {
        if (TextUtils.isEmpty(rawStr)) {
            return rawStr;
        }
        try {
            byte[] decode = Base64.decode(rawStr, Base64.DEFAULT);
            return new String(decode, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
