package com.example.khuang.example1;

/**
 * Created by khuang on 4/12/2018.
 */

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.StitchClient;
import com.mongodb.stitch.android.StitchClientFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

// Singleton class which manages a StitchClient and
// a list of the listeners that have yet to receive it.
class StitchClientManager {
    private static final String appId = "bikeshare_app-dmpwx";
    private static StitchClientManager _shared = null;

    private StitchClient stitchClient;
    private List<StitchClientListener> listeners = new ArrayList<>();

    // Must call this at least once with application context before
    // registering any listener. This can be done at application
    // launch in a subclass of Application, or in each Activity's
    // onCreate() before registering a listener. This can be safely
    // called more than once, but calls beyond the first have no effect.
    public synchronized static void initialize(Context ctx) {
        if(_shared == null) {
            _shared = new StitchClientManager(ctx);
        }
    }

    // Method that should be called in an Activity's onCreate() to register
    // the Activity with the globally managed StitchClient.
    // Will result in NullPointerException if initialize() was never called.
    public synchronized static void registerListener(StitchClientListener listener) {
        _shared.listeners.add(listener);

        if(_shared.stitchClient != null) {
            ListIterator<StitchClientListener> it = _shared.listeners.listIterator();
            while(it.hasNext()) {
                StitchClientListener nextListener = it.next();
                nextListener.onReady(_shared.stitchClient);
                it.remove();
            }
        }
    }

    private StitchClientManager(Context ctx) {
        StitchClientFactory.create(ctx, appId).addOnCompleteListener(new OnCompleteListener<StitchClient>() {
            @Override
            public void onComplete(@NonNull Task<StitchClient> task) {
                if (task.isSuccessful()) {
                    _shared.stitchClient = task.getResult();
                    ListIterator<StitchClientListener> it = _shared.listeners.listIterator();
                    while(it.hasNext()) {
                        StitchClientListener nextListener = it.next();
                        nextListener.onReady(_shared.stitchClient);
                        it.remove();
                    }
                } else {
                    Exception exception = task.getException();
                }
            }
        });
    }
}

