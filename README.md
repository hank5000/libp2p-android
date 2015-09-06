P2P直播Android-SDK
===

## 依赖安装

### gradle编译（推荐）

如果您的项目是一个使用gradle编译的AndroidStudio项目，那么集成是非常简单的。

- 首先，在buildscript的repositories里面加入mavenCenter，因为我们的库是公开在maven central的
```
buildscript {
    repositories {
        jcenter()
        mavenCentral()
        maven {
            url "https://oss.sonatype.org/content/repositories/snapshots/"
        }
    }
}
```
- 然后添加依赖，随后等gradle同步之后，即可使用该SDK的各种接口
```
dependencies {
    compile 'cn.vbyte.p2p:livestream:1.0.3-SNAPSHOT'  # 加入这一行即可
}
```

### 传统的Eclipse编译

如果您的项目是一个传统的Eclipse项目，那可能要稍微麻烦一点。
- 首先，下载libevent.so,libstun.so,libp2pstream.so，将其放在项目根目录的libs/armeabi-v7a/下面
- 然后，下载cn_vbyte_p2p-livestream.jar文件，放在libs/下面，并添加到编译依赖包里面
- 以上完成后，即可使用该SDK里面的API

## 文档

### 接口API

> P2PLiveStream.create("app id", "app kkey", "app secret key");

此接口传入您申请的appId，appKey，appSecretKey，来完成P2P模块的载入和初始化

> P2PLiveStream.dismiss();

此接口销毁P2P模块，这本身已经是一个异步操作，与create创建相对应，create应该在程序启动的时候，dismiss应该在程序退出的时候。

> P2PLiveStream.load(channel, resolution);

该接口载入频道为channel，分辨率为resolution的源，并使用P2P加速。该函数会返回一个URI，一般使用该URI可直接给播放器打开并播放之。

> P2PLiveStream.unload();

该接口与load相对应，应用同一时刻只能播放一个源，所以调用此函数会将上一个您加载的源关闭。该函数应该用在您想让播放器退出的时候。

> P2PLiveStream.setEventListener(listener);

该接口设置一个P2P模块给上层应用反馈事件的回调函数，其中listener是实现了EventListener接口的实例，EventListener定义了2个接口，如下：
```java
public interface EventListener {
    /**
     * P2PLiveStream模块内部事件的回调函数
     * @param code: 事件状态码
     * @param message: 事件说明
     * @return 返回值无意义
     */
    int onEvent(int code, String message);

    /**
     * P2PLiveStream模块内部错误的回调函数
     * @param code: 错误状态码
     * @param message: 事件说明
     * @return 返回值无意义
     */
    int onError(int code, String message);
}
```

> P2PLiveStream.version();  

 该接口获取P2P模块的版本号，返回一个一`v`开头的字符串，您可以看需使用。

> P2PLiveStream.enableDebug();  
> P2PLiveStream.disableDebug();

这2个接口是debug开关的接口，默认是打开的，在发布App时，应关闭debug。

### 事件

#### 正常事件

* **EVENT_CREATE** : 标志着P2P模块的创建成功
* **EVENT_START**: 标志着P2P成功加载频道
* **EVENT_STOP**: 表明P2P成功停止了上一个频道（上一个频道可能早被停止过了）
* **EVENT_EXIT**: 表明P2P模块收到了退出信号，即将退出
* **EVENT_DESTROY**: 标志着P2P模块成功销毁了自己
* **EVENT_STUN_SUCCESS**: 表明P2P模块成功获取到了自己的公网地址
* **EVENT_JOIN_SUCCESS**: 表明P2P模块在载入一个频道的过程中成功加入了P2P的大军
* **EVENT_HTBT_SUCCESS**: 表明此时当前程序实例没有掉队
* **EVENT_BYE_SUCCESS**: 表明当前程序实例要退出P2P了，这在播放器停止播放，程序调用unload之后会发生
* **EVENT_NEW_PARTNER**: 表明当前应用程序又获取了一个伙伴
* **EVENT_STREAM_READY**: 表明即将载入频道的数据流已经就绪，将会给播放器数据，在播放器有足够的缓冲后（这取决于播放器自己的设定），就会有画面呈现
* **EVENT_P2P_STABLE**: 表明当前程序实例的P2P效果很稳定
* **EVENT_BLOCK**: 表明在写数据时遇到了阻塞，这可能会造成播放器的卡顿
* **EVENT_REPORT**: 表明P2P模块将上传数据，要上传的数据在message里面，是一段json数据

**注意**: 请务必处理这些事件时不要执行耗时的操作，因为它跟Android ui主线程一样，如果耗时太久，将会阻止数据流的连续载入；如需要耗时的操作，请使用异步处理。

#### 异常和错误

* **ERROR_CONF_UNAVAILABLE**: 配置服务器不可用，将停止载入，不会播放！
* **ERROR_AUTH_FAILED**: 认证失败，此时您应确保您填入的app id，app key， app secret key都正确
* **ERROR_CONF_INVALID**: 配置不对，此时，应联系运营人员或者我们，及时修改
* **ERROR_CHANNEL_EMPTY**: 您在载入一个频道时没有传频道或者频道为空
* **ERROR_RESOLUTION_INVALID**: 该频道不存在这个分辨率，您填写的分辨率不合法或者超出的源本有的清晰度
* **ERROR_NO_SUCH_CHANNEL**: 不存在你想要播放的频道，请检查和确认你填写的频道是否正确，是否被下线等
* **ERROR_BAD_NETWORK**: 网络差，或者程序没有连接上网络，这个错误将会在P2P模块联网超时N次超时后抛出
* **ERROR_STUN_FAILED**: 获取自己的公网地址失败，此时应用程序将退化为和普通CDN一样拉去数据流，将没有P2P效果
* **ERROR_CDN_UNSTABLE**: 表明CDN不稳定，可能因网络造成，可能因源本身就不太稳定，P2P模块在连续N次获取数据失败后会抛出此错误，并停止加载，您的程序收到此错误后，可让用户刷新重试。
* **ERROR_JOIN_FAILED**: 加入P2P大军失败，后续会继续尝试
* **ERROR_HTBT_FAILED**: 表明应用程序已掉队，对P2P效果会减弱，并且可能会带来片刻的卡顿
* **ERROR_BYE_FAILED**: 退出P2P大军时失败，然而这不会影响当前应用程序从P2P大军中剔除
* **ERROR_REPORT_FAILED**: 应用程序上报统计数据失败
* **ERRPR_UNKNOWN_PACKET**: 收到一个未知类型的包，将忽略
* **ERROR_INVALID_PACKET**: 收到一个数据不一致的包，将忽略
* **ERROR_INTERNAL**: 内部错误
