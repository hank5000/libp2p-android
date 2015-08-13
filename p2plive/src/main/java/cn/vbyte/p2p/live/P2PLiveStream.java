package cn.vbyte.p2p.live;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.io.File;

/**
 * 这是一个单例，里面只有静态方法是公开的，其他都是私有方法
 * 即保护该类的内容，也方便开发者调用
 */
public class P2PLiveStream {
    private static final String TAG = "cn.vbyte.p2p.live";

    // p2p启动成功
    public static final int EVENT_CREATE = 100;
    // 启动一个频道
	public static final int EVENT_START = 101;
    // 停止一个频道
	public static final int EVENT_STOP = 102;
    // 收到退出信号，开始清理资源
    public static final int EVENT_EXIT = 103;
    // 退出完毕
    public static final int EVENT_DESTROY = 104;
    // 此事件表明成功获取到自己的公网地址
    public static final int EVENT_STUN_SUCCESS = 105;
    // Tracker相关成功事件
    public static final int EVENT_JOIN_SUCCESS = 106;
    public static final int EVENT_HTBT_SUCCESS = 107;
    public static final int EVENT_BYE_SUCCESS = 108;
    // 此事件表明跟某个节点成功建立连接
    public static final int EVENT_NEW_PARTNER = 109;
    // 此事件表明当前播放频道的数据源已经写入，准备就绪，意味着即将开始播放
    public static final int EVENT_STEAM_READY = 110;
    // 此事件表明P2P进入一个稳定状态
    public static final int EVENT_P2P_STABLE = 111;
    // 此事件表明缓冲区遇到一个阻塞
    public static final int EVENT_BLOCK = 112;
    // 上报一个数据统计
    public static final int EVENT_REPORT = 113;

    // conf服务器不可用
    public static final int ERROR_CONF_UNAVAILABLE = 200;
    // 认证失败，请检查appId,appKey,appSecretKey设置是否正确
    public static final int ERROR_AUTH_FAILED = 201;
    // 远程配置不合法
    public static final int ERROR_CONF_INVALID = 202;
    // 频道为空
    public static final int ERROR_CHANNEL_EMPTY = 203;
    // 启动此频道失败
    public static final int ERROR_NO_SUCH_CHANNEL = 204;
    // 分辨率不合法
    public static final int ERROR_RESOLUTION_INVALID = 205;
    // 网络不好
    public static final int ERROR_BAD_NETWORK = 206;
    // 获取自己公网地址失败
    public static final int ERROR_STUN_FAILED = 207;
    // CDN服务不可用
    public static final int ERROR_CDN_UNAVAILABLE = 208;
    // Tracker服务不可用
    public static final int ERROR_TRACKER_UNAVAILABLE = 209;
    // Tracker相关错误选项
    public static final int ERROR_JOIN_FAILED = 210;
    public static final int ERROR_HTBT_FAILED = 211;
    public static final int ERROR_BYE_FAILED = 212;
    // 上报服务不可用
    public static final int ERROR_REPORT_UNAVAILABLE = 213;
    // 收到未知的包
    public static final int ERROR_UNKNOWN_PACKET = 214;
    // 包内容与签名不一致
    public static final int ERROR_INVALID_PACKET = 215;
    // 内部错误
    public static final int ERROR_INTERNAL = 216;

    // 持久保存SDK版本号
    private static String SDK_VERSION;
    // P2PLiveStream的唯一实例
    private static P2PLiveStream p2pLiveStream = null;

    /**
     * 新启动一个p2pLiveStream模块，注意四个参数绝对不能为null
     * @param context 上下文
     * @param appId 应用唯一表示
     * @param appKey 应用密钥
     * @param appSecretKey 应用加密混淆字段，可选
     */
    public static void create(Context context, String appId, String appKey, String appSecretKey) {
        try {
            if (context == null || appId == null || appKey == null || appSecretKey == null) {
                throw new NullPointerException("Context or appId or appKey or appSecretKey is null!");
            }
            P2PLiveStream.dismiss();
            p2pLiveStream = new P2PLiveStream(context, appId, appKey, appSecretKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置监听，可监听native应用向上汇报的各种事件
     * @param listener 监听器
     */
    public static void setEventListener(EventListener listener) {
        p2pLiveStream.listener = listener;
    }

    /**
     * 加载一个频道，并将数据缓冲到本地
     * @param channel 频道名称
     * @param resolution 分辨率
     * @return 本地文件系统中缓冲文件路径，是个URI
     */
    public static Uri loadStream(String channel, String resolution) {
        p2pLiveStream.start(channel, resolution);
        return p2pLiveStream.getURI();
    }

    public static void stop() {
        p2pLiveStream.emit(EVENT_STOP);
    }

    /**
     * 注销当前p2pLiveStream模块
     */
    public static void dismiss() {
        if (p2pLiveStream != null) {
            p2pLiveStream.terminate();
            p2pLiveStream = null;
            SDK_VERSION = null;
        }
    }

    /**
     * 获取native应用的版本号
     * @return
     */
    public static String getVersion() {
        if (SDK_VERSION == null) {
            SDK_VERSION = P2PLiveStream.version();
        }
        return SDK_VERSION;
    }

    /**
     * 打开调试模式，默认是关闭调试模式的
     */
    public static native void enableDebug();

    /**
     * 关闭调试模式，应用上线时应关闭调试模式
     */
    public static native void disableDebug();

    /**
     * 获取P2PLiveStream模块的版本号
     * @return P2PLiveStream模块的版本号
     */
    private static native String version();

    public interface EventListener {
        /**
         * P2PLiveStream模块内部事件的回调函数
         * @param Event: 事件类型
         * @param message: 事件说明
         * @return 返回值无意义
         */
        int onEvent(int Event, String message);
    }


    /*==============================*
     * 接下来是该类的具体封装，全是私有的
     *==============================*/
    static {
        try {
            System.loadLibrary("p2plivestream");
        } catch (Throwable t) {
            Log.w(TAG, "Unable to load the p2plivestream library: " + t);
        }
    }

    private Context context;
    private EventListener listener = null;
    private String tmpDir = null;
    private String flvFilePath;
    private Handler handler = new Handler();

    private P2PLiveStream(Context context, String appId, String appKey, String appSecretKey)
    		throws Exception {
        if (context == null || appId == null || appKey == null || appSecretKey == null) {
            throw new NullPointerException("Context or appId or appKey or appSecretKey can't be null when init p2p live stream!");
        }
    	if (init() != 0) {
    		throw new Exception("Can not use p2p!");
    	}

        this.context = context;
        tmpDir = context.getFilesDir().getAbsolutePath();
    	this.setAppId(appId);
    	this.setAppKey(appKey);
    	this.setAppSecretKey(appSecretKey);
    }

    /**
     * 开始启动一个源
     * @param channel 频道名称
     * @param resolution 清晰度
     */
    private void start(String channel, String resolution) {
        if (channel == null || channel.length() == 0) {
            Log.e(TAG, "Channel can not be null or empty, please init it first!");
            return;
        }
        if (resolution == null || resolution.length() == 0) {
            Log.w(TAG, "Resolution is empty, may use default resolution!");
            resolution = "";
        }
    	this.setChannel(channel);
    	this.setResolution(resolution);
        flvFilePath = tmpDir + File.separator + channel;
        this.setFlvFilePath(flvFilePath);
        this.emit(EVENT_START);
    }

    /**
     * 获取当前频道的统一资源定位符
     * @return 当前频道的统一资源定位符
     */
    private Uri getURI() {
        return Uri.parse("file://" + flvFilePath);
    }

    /**
     * native应用向上层应用的回调函数，其他地方不得以任何理由调用此函数
     * @param event native代码上报的事件表示，详细见上面定义的常量
     * @param message native代码上报事件的说明
     */
	private void onEvent(int event, String message) {
        if (listener != null) {
            listener.onEvent(event, message);
        }
	}

    /**
     * 终止P2PLiveStream Module，这是个阻塞操作，
     * 要等到程序通知彻底退出了，才是真正的退出
     */
    private void terminate() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                P2PLiveStream.this.destroy();
            }
        });
    }

    /**
     * 设置频道
     * @param channel
     */
	private native void setChannel(String channel);

    /**
     * 设置清晰度
     * @param resolution
     */
    private native void setResolution(String resolution);

    /**
     * 设置当前播放频道的临时缓冲文件位置
     * @param flvFilePath
     */
    private native void setFlvFilePath(String flvFilePath);

    /**
     * native应用销毁
     * @return 成功返回0，失败返回非0
     */
	private native int destroy();

    /**
     * native应用初始化
     * @return 成功返回0，失败返回非0
     */
    private native int init();

    /**
     * 向native应用发送消息通知
     * @param event
     */
	private native void emit(int event);

    /**
     * 设置appId
     * @param appId
     */
	private native void setAppId(String appId);

    /**
     * 设置appKey
     * @param appKey
     */
    private native void setAppKey(String appKey);

    /**
     * 设置appSecretKey
     * @param appKey
     */
    private native void setAppSecretKey(String appKey);

}
