package com.example.khuang.example1;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by khuang on 3/15/2018.
 */

final class Constants {

    private Constants() {
        // restrict instantiation
    }

    static final String HOSTNAME = "tcp://130.58.167.93:1883";
    static final int QOS = 1;
    static final byte[] UNLOCK = {1};
    static final String EXTRA_MESSAGE = "com.example.bikeshare.MESSAGE";

    static final int RESERVE_BIKE_REQUEST = 0;
    static final int RETURN_BIKE_REQUEST = 1;

    private static final HashMap<Integer, String> locationMap = new HashMap<Integer, String>() {{
        put(1, "Mary Lyons 1");
    }};
    static final Map<Integer, String> locations = Collections.unmodifiableMap(locationMap);
    private static final HashMap<String, Integer> invLocationMap = new HashMap<String, Integer>() {{
        put("Mary Lyons 1", 1);
    }};
    static final Map<String, Integer> invLocations = Collections.unmodifiableMap(invLocationMap);
}
