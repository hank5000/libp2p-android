package cn.vbyte.p2p.live;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by passionzhang on 15/8/3.
 */
public class P2PLiveSteamHandler extends Handler {
    private static final String TAG = "cn.vbyte.cn.live";

    public static final int START = 100;
    public static final int STOP = 101;
    public static final int EXIT = 102;
    public static final int DESTROY = 103;

    @Override
    public void handleMessage(Message msg) {
        switch(msg.what) {
            case START:
                Log.i(TAG, "P2P live stream start work.");
                break;
            case STOP:
                Log.i(TAG, "P2P live stream stoped.");
                break;
            case EXIT:
                Log.i(TAG, "P2P live stream exit.");
                break;
            case DESTROY:
                Log.i(TAG, "P2P live stream destroyed.");
                break;
        }
    }
}
