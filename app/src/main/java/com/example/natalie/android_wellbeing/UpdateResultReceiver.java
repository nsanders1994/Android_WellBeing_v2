package com.example.natalie.android_wellbeing;

/**
 * Created by Natalie on 4/18/2015.
 */

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public class UpdateResultReceiver extends ResultReceiver {
    /**
     * Handles the communication between the receiver in the start screen activity and the update service
    **/

    private Receiver mReceiver;

    public UpdateResultReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }
}
