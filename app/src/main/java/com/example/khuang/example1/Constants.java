package com.example.khuang.example1;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by khuang on 3/15/2018.
 */

public final class Constants {

    private Constants() {
        // restrict instantiation
    }

    public static final String HOSTNAME = "tcp://130.58.167.93:1883";
    public static final int QOS = 2; // ensures delivery. slow.
    public static final byte[] QUERY = {0};
    public static final byte[] UNLOCK = {1};
    public static final byte[] LOCK = {2};

    public static final HashMap<Integer, String> locationMap = new HashMap<Integer, String>() {{
        put(1, "Mary Lyons 1");
    }};
    public static final Map<Integer, String> locations = Collections.unmodifiableMap(locationMap);
}
