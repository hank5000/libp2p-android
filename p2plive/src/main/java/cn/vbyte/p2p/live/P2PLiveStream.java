package cn.vbyte.p2p.live;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class P2PLiveStream {
    private static final String TAG = "cn.vbyte.p2p.live";
	
	public static final int START = 100;
	public static final int STOP = 101;
    public static final int EXIT = 102;
    public static final int DESTROY = 103;
	
    private static int SUCCESS = 0;
    private static String SDK_VERSION;

    public static abstract class EventListener {
        public abstract int onEvent(int Event);
    }

    private EventListener listener = null;
    private String flvFilePath;

    public P2PLiveStream(String appId, String appKey, String appSecretKey)
    		throws Exception {
    	if (init() != SUCCESS) {
    		throw new Exception("Can not use p2p!");
    	}
    	
    	this.setAppId(appId);
    	this.setAppKey(appKey);
    	this.setAppSecretKey(appSecretKey);
    }
    
    public void start(Context context, String channel, String resolution) {
        if (context == null) {
            Log.e(TAG, "Oh, context is null, please pass an valid context to start p2p!");
            return;
        }
        if (channel == null || channel.length() == 0) {
            Log.e(TAG, "Channel can not be null or empty, please init it first!");
            return;
        }
        if (resolution == null || resolution.length() == 0) {
            Log.e(TAG, "Resolution can not be null or empty, please init it first!");
            return;
        }
    	this.setChannel(channel);
    	this.setResolution(resolution);
        flvFilePath = context.getCacheDir().getAbsolutePath();
        flvFilePath += File.separator + channel;
        this.setFlvFilePath(flvFilePath);
        this.emit(START);
    }
    
    public void stop() {
    	this.emit(STOP);
    }

    public String getURI() {
        return flvFilePath;
    }

	public void onEvent(int event) {
        if (listener != null) {
            listener.onEvent(event);
        }
	}

    public void setEventListener(EventListener listener) {
        this.listener = listener;
    }

    public EventListener getEventListener() {
        return listener;
    }

	public static String getVersion() {
		if (SDK_VERSION == null) {
			SDK_VERSION = P2PLiveStream.version();
		}
		return SDK_VERSION;
	}
	
	private native void setChannel(String channel);
	private native void setResolution(String resolution);
	private native void setFlvFilePath(String flvFilePath);
	
	/**
     * Destroy这是一个耗时操作，因其会join event loop线程，
     * 请不要在主线程里面直接调用调用
     */
	public native int destroy();
	private native int init();
	
	private native void emit(int event);
	
	private native void setAppId(String appId);
	private native void setAppKey(String appKey);
	private native void setAppSecretKey(String appKey);
	
	public static native void enableDebug();
    public static native void disableDebug();
    private static native String version();

}
