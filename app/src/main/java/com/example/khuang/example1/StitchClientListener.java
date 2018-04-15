package com.example.khuang.example1;

import com.mongodb.stitch.android.StitchClient;

/**
 * Created by khuang on 4/12/2018.
 */

// interface that activities inherit when they need a stitchclient
public interface StitchClientListener {
    void onReady(StitchClient stitchClient);
}
