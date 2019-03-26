package com.freak.commonappframework.webview;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.freak.commonappframework.R;
import com.freak.commonappframework.base.BaseAbstractSimpleActivity;
import com.freak.commonappframework.base.IActivityStatusBar;
import com.freak.commonappframework.utils.imagepick.BitmapUtil;
import com.freak.commonappframework.utils.imagepick.ICompressImageResponse;
import com.freak.commonappframework.utils.imagepick.ImageSelector;
import com.freak.commonappframework.utils.imagepick.PermissionUtils;
import com.freak.commonappframework.utils.imagepick.popup.PopupGetPictureView;
import com.freak.httphelper.ApiCallback;
import com.freak.httphelper.SubscriberCallBack;
import com.just.agentweb.AbsAgentWebSettings;
import com.just.agentweb.AgentWeb;
import com.just.agentweb.DefaultWebClient;
import com.just.agentweb.IAgentWebSettings;
import com.just.agentweb.LogUtils;
import com.just.agentweb.WebListenerManager;
import com.just.agentweb.download.AgentWebDownloader;
import com.just.agentweb.download.DefaultDownloadImpl;
import com.just.agentweb.download.DownloadListenerAdapter;
import com.just.agentweb.download.DownloadingService;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.freak.commonappframework.utils.imagepick.ImageSelector.CUT_PHOTO;
import static com.freak.commonappframework.utils.imagepick.ImageSelector.GET_PICTURE_SELECT_PHOTO;
import static com.freak.commonappframework.utils.imagepick.ImageSelector.GET_PICTURE_TAKE_PHOTO;


/**
 * @author freak
 * @date 2019/2/19
 */
public class WebViewActivity extends BaseAbstractSimpleActivity implements IActivityStatusBar {
    private String url = "";
    private AgentWeb mAgentWeb;
    private LinearLayout mLinearLayoutWebView;
    private DownloadingService mDownloadingService;
    private WebView mWebView;
    private long mTime;
    private LinearLayout mLinearLayoutSplash;
    private ValueCallback<Uri[]> mUploadCallbackAboveFive;
    private ValueCallback<Uri> mUploadMessage;
    private final int RESULT_CODE_IMAGE = 1005;
    private File userImgFile;
    /**
     * 请求开启定位
     */
    public static int REQUEST_CODE_ENABLE_LOCATION = 100;
    public static int REQUEST_CODE_ACCESS_LOCATION_PERMISSION = 101;
    private LocationWebChromeClient mWebChromeClient;

    public static void startAction(Context context, @NonNull String url) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_web_view;
    }

    @Override
    protected void initEventAndData() {
        mWebChromeClient = new LocationWebChromeClient();
        mAgentWeb = AgentWeb.with(this)
                .setAgentWebParent(mLinearLayoutWebView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                .useDefaultIndicator(-1, 3)
                .setAgentWebWebSettings(getSettings())
                .setWebViewClient(mWebViewClient)
                .setWebChromeClient(mWebChromeClient)
                .setSecurityType(AgentWeb.SecurityType.STRICT_CHECK)
                .setMainFrameErrorView(R.layout.agentweb_error_page, -1)
                .setOpenOtherPageWays(DefaultWebClient.OpenOtherPageWays.DISALLOW)
                .interceptUnkownUrl()
                .createAgentWeb()
                .ready()
                .go(url);
        mWebView = mAgentWeb.getWebCreator().getWebView();

        if (url.contains("<html>")) {
            mWebView.loadDataWithBaseURL(null, url.toString(), "text/html", "utf-8", null);
        }

        mAgentWeb.getWebCreator().getWebView().setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void initView() {
        url = getIntent().getStringExtra("url");
        mLinearLayoutWebView = findViewById(R.id.linear_layout_webview);
        new RxPermissions(this)
                .requestEach(Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE)
                .subscribe(new SubscriberCallBack<>(new ApiCallback<Permission>() {
                    @Override
                    public void onSuccess(Permission model) {
                        if (model.granted) {

                        } else {
                            Toast.makeText(WebViewActivity.this, "请允许打开需要的权限", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(String msg) {

                    }
                }));
    }

    /**
     * @return IAgentWebSettings
     */
    public IAgentWebSettings getSettings() {
        return new AbsAgentWebSettings() {
            private AgentWeb mAgentWeb;

            @Override
            protected void bindAgentWebSupport(AgentWeb agentWeb) {
                this.mAgentWeb = agentWeb;
            }

            /**
             * AgentWeb 4.0.0 内部删除了 DownloadListener 监听 ，以及相关API ，将 Download 部分完全抽离出来独立一个库，
             * 如果你需要使用 AgentWeb Download 部分 ， 请依赖上 compile 'com.just.agentweb:download:4.0.0 ，
             * 如果你需要监听下载结果，请自定义 AgentWebSetting ， New 出 DefaultDownloadImpl，传入DownloadListenerAdapter
             * 实现进度或者结果监听，例如下面这个例子，如果你不需要监听进度，或者下载结果，下面 setDownloader 的例子可以忽略。
             * @param webView
             * @param downloadListener
             * @return WebListenerManager
             */
            @Override
            public WebListenerManager setDownloader(WebView webView, DownloadListener downloadListener) {
                return super.setDownloader(webView,
                        DefaultDownloadImpl
                                .create((Activity) webView.getContext(),
                                        webView,
                                        mDownloadListenerAdapter,
                                        mDownloadListenerAdapter,
                                        this.mAgentWeb.getPermissionInterceptor()));
            }
        };
    }

    protected DownloadListenerAdapter mDownloadListenerAdapter = new DownloadListenerAdapter() {


        @Override
        public boolean onStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength, AgentWebDownloader.Extra extra) {
            extra.setOpenBreakPointDownload(true)
                    .setIcon(R.drawable.ic_file_download_black_24dp)
                    .setConnectTimeOut(6000)
                    .setBlockMaxTime(2000)
                    .setDownloadTimeOut(60L * 5L * 1000L)
                    .setAutoOpen(true)
                    .setForceDownload(false);
            return false;
        }


        @Override
        public void onBindService(String url, DownloadingService downloadingService) {
            super.onBindService(url, downloadingService);
            mDownloadingService = downloadingService;
            LogUtils.i("TAG", "onBindService:" + url + "  DownloadingService:" + downloadingService);
        }


        @Override
        public void onUnbindService(String url, DownloadingService downloadingService) {
            super.onUnbindService(url, downloadingService);
            mDownloadingService = null;
            LogUtils.i("TAG", "onUnbindService:" + url);
        }


        @Override
        public void onProgress(String url, long loaded, long length, long usedTime) {
            int mProgress = (int) ((loaded) / Float.valueOf(length) * 100);
            LogUtils.i("TAG", "onProgress:" + mProgress);
            super.onProgress(url, loaded, length, usedTime);
        }


        @Override
        public boolean onResult(String path, String url, Throwable throwable) {
            if (null == throwable) {
                //do you work
            } else {

            }
            return false;
        }
    };
    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }


        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Request request1 = new Request.Builder().url(request.getUrl().toString()).get().build();
            OkHttpClient okHttpClient = new OkHttpClient();
            Call mCall = okHttpClient.newCall(request1);
            mCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    com.freak.commonappframework.utils.LogUtils.d(response.headers().toString());
                    com.freak.commonappframework.utils.LogUtils.d("状态码--》"+response.code());
                    com.freak.commonappframework.utils.LogUtils.d(response.header("content-type"));
                    //拿到登陆后操作的某个网页的内容
//                    Request request2=new Request.Builder().url(response.).build();
//                    Response response2 = okHttpClient.newCall(redirectRequest).execute();
//                    String result = response2.body().string();
                }
            });
            return super.shouldInterceptRequest(view, request);
        }

        /**
         * 重定向
         * @param view
         * @param request
         * @return
         */
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            WebView.HitTestResult hitTestResult = view.getHitTestResult();
            int type = hitTestResult.getType();
            if (type != WebView.HitTestResult.UNKNOWN_TYPE) {
                //没有进行重定向操作
            } else {
                //进行了重定向操作
                view.loadUrl(request.getUrl().toString());
                return false;
            }
            return super.shouldOverrideUrlLoading(view, request);
        }
    };

    @Override
    public int getStatusBarColor() {
        return ContextCompat.getColor(this, R.color.colorPrimary);
    }

    @Override
    protected void onResume() {
        mAgentWeb.getWebLifeCycle().onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mAgentWeb.getWebLifeCycle().onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mAgentWeb.getWebLifeCycle().onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("TAG", "结果码-->" + resultCode + "\n请求码--》" + requestCode);
        if (resultCode == 0) {
            if (mUploadCallbackAboveFive != null) {
                mUploadCallbackAboveFive.onReceiveValue(null);
                mUploadCallbackAboveFive = null;
            }

            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
            }
            return;
        }
        switch (requestCode) {
            //拍照
            case GET_PICTURE_TAKE_PHOTO:
                userImgFile = ImageSelector.cutPicture(this, userImgFile);
                break;
            //选择照片
            case GET_PICTURE_SELECT_PHOTO:
                userImgFile = ImageSelector.getPhotoFromIntent(data, this);
                userImgFile = ImageSelector.cutPicture(this, userImgFile);
                break;
            //裁剪照片
            case CUT_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    compressImage(userImgFile);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 压缩图片
     *
     * @param file
     */
    public void compressImage(File file) {
        List<File> list = new ArrayList<>();
        list.add(file);
        BitmapUtil.compressFiles(list, new ICompressImageResponse() {
            @Override
            public void onSuccess(List<File> images) {
                File imgFile = images.get(0);
                Uri result = ImageSelector.toURI(WebViewActivity.this, imgFile);
                Log.e("TAG", "文件URI-->" + result);
                if (null != mUploadMessage && null == mUploadCallbackAboveFive) {
                    mUploadMessage.onReceiveValue(result);
                    mUploadMessage = null;
                }

                if (null == mUploadMessage && null != mUploadCallbackAboveFive) {
                    mUploadCallbackAboveFive.onReceiveValue(new Uri[]{result});
                    mUploadCallbackAboveFive = null;
                }
            }

            @Override
            public void onMarch() {

            }

            @Override
            public void onFail() {

            }

            @Override
            public void onFinish() {

            }
        });
    }

    /**
     * 显示图片选择器
     *
     * @param context
     * @param view
     */
    public void showSelector(final Context context, View view) {
        PopupGetPictureView popupGetPictureView = new PopupGetPictureView(context, new
                PopupGetPictureView.GetPicture() {
                    @Override
                    public void takePhoto(View v) {
                        if (PermissionUtils.checkTakePhotoPermission(context)) {
                            userImgFile = ImageSelector.takePicture(context, GET_PICTURE_TAKE_PHOTO);
                        }
                    }

                    @Override
                    public void selectPhoto(View v) {
                        if (PermissionUtils.checkAlbumStroagePermission(context)) {
                            ImageSelector.photoPick(context, GET_PICTURE_SELECT_PHOTO);
                        }
                    }

                    @Override
                    public void cancel(PopupWindow popupWindow) {
                        if (popupWindow.isShowing()) {
                            if (mUploadCallbackAboveFive != null) {
                                mUploadCallbackAboveFive.onReceiveValue(null);
                                mUploadCallbackAboveFive = null;
                            }

                            if (mUploadMessage != null) {
                                mUploadMessage.onReceiveValue(null);
                                mUploadMessage = null;
                            }
                            popupWindow.dismiss();
                        }
                    }
                });
        popupGetPictureView.showPop(view);
    }

    class LocationWebChromeClient extends WebChromeClient {


        private LocationWebChromeClientListener mLocationWebChromeClientListener;

        private GeolocationPermissions.Callback mGeolocationPermissionsCallback;
        private String mOrigin;

        private boolean mShowRequestPermissionRationale = false;

        public LocationWebChromeClient() {
            mLocationWebChromeClientListener = new LocationWebChromeClientListener() {
                @Override
                public boolean onReturnFromLocationSetting(int requestCode) {
                    if (requestCode == REQUEST_CODE_ENABLE_LOCATION) {
                        if (mGeolocationPermissionsCallback != null) {
                            if (isEnabledLocationFunction()) {
                                mGeolocationPermissionsCallback.invoke(mOrigin, true, true);
                            } else {
                                //显然，从设置界面回来还没有开启定位服务，肯定是要拒绝定位了
                                Toast.makeText(WebViewActivity.this, "您拒绝了定位请求", Toast.LENGTH_SHORT).show();
                                mGeolocationPermissionsCallback.invoke(mOrigin, false, false);
                            }
                        }
                        return true;
                    }
                    return false;
                }

                @Override
                public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
                    boolean pass = true;
                    for (Integer result : grantResults) {
                        if (result == PackageManager.PERMISSION_DENIED) {
                            pass = false;
                            break;
                        }
                    }
                    if (pass) {
                        onAccessLocationPermissionGranted();
                    } else {
                        onAccessLocationPermissionRejected();
                    }
                }

                public void onAccessLocationPermissionGranted() {
                    doJudgeLocationServiceEnabled();
                }

                public void onAccessLocationPermissionRejected() {
                    if (mShowRequestPermissionRationale) {
                        Toast.makeText(WebViewActivity.this, "您拒绝了定位请求", Toast.LENGTH_SHORT).show();
                        mGeolocationPermissionsCallback.invoke(mOrigin, false, false);
                    } else {
                        doRequestAppSetting();
                    }
                }
            };
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            //do you work
        }

        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType, String capture) {
            this.openFileChooser(uploadMsg);
        }

        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType) {
            this.openFileChooser(uploadMsg);
        }

        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            mUploadMessage = uploadMsg;
            showSelector(WebViewActivity.this, mWebView);
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            mUploadCallbackAboveFive = filePathCallback;
            showSelector(WebViewActivity.this, mWebView);
            return true;
        }

        private void doRequestAppSetting() {
            AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
            builder.setTitle("温馨提示");
            builder.setMessage(String.format("您禁止了应用获取当前位置的权限，是否前往开启？", mOrigin));
            builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent mIntent = new Intent();
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (Build.VERSION.SDK_INT >= 9) {
                        mIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                        mIntent.setData(Uri.fromParts("package", getPackageName(), null));
                    } else if (Build.VERSION.SDK_INT <= 8) {
                        mIntent.setAction(Intent.ACTION_VIEW);
                        mIntent.setClassName("com.android.settings", "com.android.setting.InstalledAppDetails");
                        mIntent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
                    }
                    startActivity(mIntent);
                }
            });
            builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mGeolocationPermissionsCallback.invoke(mOrigin, false, false);
                }
            });
            builder.create().show();
        }

        public LocationWebChromeClientListener getLocationWebChromeClientListener() {
            return mLocationWebChromeClientListener;
        }

        @Override
        public void onGeolocationPermissionsHidePrompt() {
            super.onGeolocationPermissionsHidePrompt();
        }


        @Override
        public void
        onGeolocationPermissionsShowPrompt(final String origin, final GeolocationPermissions.Callback geolocationPermissionsCallback) {
            this.mOrigin = origin;
            this.mGeolocationPermissionsCallback = geolocationPermissionsCallback;
            //是否拥有定位权限
            if (hasAccessLocationPermission()) {
                doJudgeLocationServiceEnabled();
            } else {
                //请求定位
                requestAccessLocationPermission();
            }
        }

        private void doJudgeLocationServiceEnabled() {
            //是否开启定位
            if (isEnabledLocationFunction()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
                builder.setTitle("温馨提示");
                builder.setMessage(String.format("网站%s，正在请求使用您当前的位置，是否许可？", mOrigin));
                builder.setPositiveButton("许可", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mGeolocationPermissionsCallback.invoke(mOrigin, true, true);
                    }
                });
                builder.setNegativeButton("不许可", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mGeolocationPermissionsCallback.invoke(mOrigin, false, false);
                    }
                });
                builder.create().show();
            } else {
                //请求开启定位功能
                requestEnableLocationFunction(mOrigin, mGeolocationPermissionsCallback);
            }
        }

        /**
         * 请求开启定位服务
         */
        private void requestEnableLocationFunction(final String origin, final GeolocationPermissions.Callback geolocationPermissionsCallback) {
            AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
            builder.setTitle("温馨提示");
            builder.setMessage(String.format("网站%s，正在请求使用您当前的位置，是否前往开启定位服务？", origin));
            builder.setPositiveButton("前往开启", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, REQUEST_CODE_ENABLE_LOCATION);
                }
            });
            builder.setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    geolocationPermissionsCallback.invoke(origin, false, false);
                }
            });
            builder.create().show();
        }

        private boolean isEnabledLocationFunction() {
            int locationMode = 0;
            String locationProviders;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    locationMode = Settings.Secure.getInt(WebViewActivity.this.getContentResolver(), Settings.Secure.LOCATION_MODE);
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }
                return locationMode != Settings.Secure.LOCATION_MODE_OFF;
            } else {
                locationProviders = Settings.Secure.getString(WebViewActivity.this.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                return !TextUtils.isEmpty(locationProviders);
            }
        }

        private boolean hasAccessLocationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
            } else {
                return ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
            }
        }

        private void requestAccessLocationPermission() {
            // 是否要显示问什么要获取权限的解释界面
            /**
             * 什么情况下 shouldShowRequestPermissionRationale会返回true？
             * - 首次请求权限，但是用户禁止了，但是没有勾选“禁止后不再询问”，这样，之后的请求都会返回true
             * 什么情况下，shouldShowRequestPermissionRationale会返回false？
             * - 首次请求权限或者请求权限时，用户勾选了“禁止后不再询问”，之后的请求都会返回false
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    //请求过定位权限，但是被用户拒绝了（但是没有勾选“禁止后不再询问”）
                    // 显示解释权限用途的界面，然后再继续请求权限
                    mShowRequestPermissionRationale = true;
                } else {
                    mShowRequestPermissionRationale = false;
                }
            } else {
                mShowRequestPermissionRationale = false;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
            builder.setTitle("温馨提示");
            builder.setMessage(String.format("网站%s，正在请求使用您当前的位置，是否许可应用获取当前位置权限？", mOrigin));
            builder.setPositiveButton(" 是 ", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_CODE_ACCESS_LOCATION_PERMISSION);
                    } else {
                        //额，版本低，正常情况下，安装默认许可，然鹅，国产ROM各种魔改，有阔轮提前实现了单独授权
                        doRequestAppSetting();
                    }
                }
            });
            builder.setNegativeButton(" 否 ", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mGeolocationPermissionsCallback.invoke(mOrigin, false, false);
                }
            });
            builder.create().show();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mWebChromeClient != null && mWebChromeClient.getLocationWebChromeClientListener() != null) {
            mWebChromeClient.getLocationWebChromeClientListener().onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    interface LocationWebChromeClientListener {

        /**
         * 用户从开启定位页面回来了
         */
        boolean onReturnFromLocationSetting(int requestCode);

        /**
         * 请求权限结果
         */
        void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);
    }
}
