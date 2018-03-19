package com.example.khuang.example1;

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
}
