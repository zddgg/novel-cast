package com.example.novelcastserver.utils;

import java.util.Date;
import java.util.UUID;

public class IdUtils {
    public static String getUUIDStr() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getSysUserId() {
        return "sys-" + new Date().getTime();
    }
}
