package com.android.customization.model.iconpack;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.customization.model.CustomizationManager;
import com.android.customization.model.color.ColorCustomOption;
import com.android.customization.model.color.ColorOption;
import com.android.customization.model.themedicon.ThemedIconSwitchProvider;
import com.android.customization.model.themedicon.ThemedIconUtils;
import com.android.wallpaper.BuildConfig;
import com.android.wallpaper.util.ColorUtils;
import com.android.wallpaper.util.IconAdapterBean;
import com.android.wallpaper.util.IconAdapterTag;
import com.android.wallpaper.util.IconColorOptionBean;
import com.android.wallpaper.util.IconPackConfig;
import com.android.wallpaper.util.IconPackPrefUtils;
import com.extra.iconshape.PreferenceUtil;
import com.google.gson.Gson;
import com.lib.request.PrefUtils;
import com.lib.request.Request;
import com.lib.request.interceptor.DecryptInterceptor;
import com.liblauncher.prefs.PrefHelper;
import com.liblauncher.util.CollectionUtils;
import com.liblauncher.util.FileUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * IconPack管理器
 * 负责加载、保存和管理IconPack颜色方案
 */
public class IconPackManager implements CustomizationManager<IconPackOption> {

    private static final String TAG = "IconPackManager";
    public static final String PREF_NAME = IconPackPrefUtils.ICON_PACK_PREF_NAME;
    public static final String PREF_KEY_SELECTED = "selected_icon_pack";
    public static final String PREF_KEY_SELECTED_PREVIEW = "selected_icon_pack_preview";
    public static String REQUEST_URL = "https://res.appser.top/color-iconpack/";
    public static String REQUEST_URL_PREVIEW = "https://nati.oss-cn-hangzhou.aliyuncs.com/apk_logo_xct/server_resource_update/test/icon_color_adapter.json";

    private static final String COLOR_ICONPACK_NAME = "color_iconpack.json";
    public static String TYPE = "cmn_tiptop";

    // 优化加载相关常量
    private static final String ZIP_CACHE_DIR = "icon_pack_colors";
    private boolean mUseOptimizedLoading = true;

    private static IconPackManager sInstance;
    private static final ExecutorService sExecutorService = Executors.newSingleThreadExecutor();

    private final Context mContext;
    private final SharedPreferences mPreferences;
    private final OkHttpClient mOkHttpClient;
    private final Gson mGson;
    private final Handler mMainHandler;

    private IconColorOptionBean mSelectedOption;
    //预览时选择的iconpack
    private IconColorOptionBean mPreviewSelectedOption;
    //设置为预览模式，保存预览的cfg，使用预览colorOption
    private boolean mPreviewState = false;
    /**
     * 获取单例实例
     */
    public static IconPackManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (IconPackManager.class) {
                if (sInstance == null) {
                    sInstance = new IconPackManager(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private IconPackManager(Context context) {
        mContext = context;
        mPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mOkHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new DecryptInterceptor())
                .build();
        mGson = new Gson();
        mMainHandler = new Handler(Looper.getMainLooper());

        // 加载保存的选项
        loadSelectedOption();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void apply(IconPackOption option, Callback callback) {
        ArrayList<IconPackOption> options = new ArrayList<>();
        options.add(option);
        apply(options, callback);
    }

    @Override
    public void apply(ArrayList<IconPackOption> options, Callback callback) {
        if (options == null || options.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Options cannot be null or empty"));
            }
            return;
        }

        sExecutorService.submit(() -> {
            try {
                IconPackOption option = options.get(0);

                // 保存选中的选项
                if (option instanceof EmptyIconPackOption) {
                    // 清除选择
                    boolean flag = false;
                    if(mPreviewState){
                        mPreviewSelectedOption = null;
                        mPreferences.edit().remove(PREF_KEY_SELECTED_PREVIEW).commit();
                        FileUtils.INSTANCE.deleteFile(PrefUtils.Companion.getFileInRes(mContext,COLOR_ICONPACK_CACHE_PREVIEW));
                        flag = ThemedIconSwitchProvider.getInstance(mContext).setThemedIconEnabledPreview(false);
                    }else{
                        mSelectedOption = null;
                        mPreferences.edit().remove(PREF_KEY_SELECTED).commit();
                        FileUtils.INSTANCE.deleteFile(PrefUtils.Companion.getFileInRes(mContext,COLOR_ICONPACK_CACHE));
                        ThemedIconSwitchProvider.getInstance(mContext).setThemedIconEnabled(false);
                        flag = true;
                    }
                    if(callback != null){
                        boolean finalFlag = flag;
                        mMainHandler.postDelayed(() -> {
                            if (finalFlag) {
                                callback.onSuccess();
                            } else {
                                callback.onError(null);
                            }
                        }, 500);
                    }
                } else {

                    if(mPreviewState){
                        ThemedIconSwitchProvider.getInstance(mContext).setThemedIconEnabledPreview(true);
                    }else{
                        ThemedIconSwitchProvider.getInstance(mContext).setThemedIconEnabled(true);
                    }
                    if(mPreviewState){
                        mPreviewSelectedOption = option.getBean();
                        String json = mGson.toJson(mPreviewSelectedOption);
                        FileUtils.INSTANCE.writeFile(PrefUtils.Companion.getFileInRes(mContext,
                                COLOR_ICONPACK_CACHE_PREVIEW), json);

                        mPreferences.edit().putString(PREF_KEY_SELECTED_PREVIEW, json).commit();
                    }else{
                        mSelectedOption = option.getBean();
                        String json = mGson.toJson(mSelectedOption);
                        //保存对应的shape
                        String shape =  mSelectedOption.getIcon_shape();
                        if(TextUtils.isEmpty(shape)){
                            shape = PreferenceUtil.ICON_INTERNAL_SQUIRCLE;
                        }
                        PreferenceUtil.setIconShapeString(mContext, shape);
                        mPreferences.edit().putString(PREF_KEY_SELECTED, json).commit();
                        FileUtils.INSTANCE.writeFile(PrefUtils.Companion.getFileInRes(mContext,
                                COLOR_ICONPACK_CACHE), json);
                    }
                }

                // 回调成功
                mMainHandler.postDelayed(() -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                }, 500);
            } catch (Exception e) {
                Log.e(TAG, "Error applying icon pack option", e);
                mMainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            }
        });
    }

    @Override
    public void fetchOptions(OptionsFetchedListener<IconPackOption> callback, boolean reload) {
        Log.i(TAG, "fetchOptions: reload=" + reload + ", useOptimized=" + mUseOptimizedLoading);
        sExecutorService.submit(() -> {
            try {
                if(BuildConfig.DEBUG){

//                    String cacheJson = PrefUtils.Companion.getCacheDataSource(mContext, COLOR_ICONPACK_NAME);
//                    if(!TextUtils.isEmpty(cacheJson)){
//                        try {
//                            Log.i(TAG, "fetchOptions: cache " + cacheJson);
//                            parseAndCallback(cacheJson, callback);
//                        } catch (Exception e) {
//                        }
//                    }
                    // 构建网络请求
                    okhttp3.Request request = new okhttp3.Request.Builder()
//                        .url(BuildConfig.DEBUG ? REQUEST_URL : REQUEST_URL_PREVIEW)
                            .url(REQUEST_URL_PREVIEW)
                            .build();

                    // 异步请求
                    mOkHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                        @Override
                        public void onFailure(okhttp3.Call call, IOException e) {
                            Log.e(TAG, "Network request failed, using default data", e);
                            // 网络失败，使用本地默认数据
                            loadDefaultData(callback);
                        }

                        @Override
                        public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                            if (!response.isSuccessful()) {
                                Log.e(TAG, "Network response not successful: " + response.code());
                                loadDefaultData(callback);
                                return;
                            }

                            try {
                                String json = response.body().string();
                                PrefUtils.Companion.saveCacheData(mContext, COLOR_ICONPACK_NAME, json);

                                // 尝试优化加载
                                if (mUseOptimizedLoading) {
                                    try {
                                        com.android.wallpaper.util.IconAdapterBeanV2 testBean =
                                                mGson.fromJson(json, com.android.wallpaper.util.IconAdapterBeanV2.class);
                                        if (testBean != null && testBean.getZipUrl() != null && !testBean.getZipUrl().isEmpty()) {
                                            Log.i(TAG, "Detected optimized JSON format, using optimized loading");
                                            parseOptimizedJson(mContext, json, callback);
                                            return;
                                        }
                                    } catch (Exception e) {
                                        Log.d(TAG, "Not optimized JSON format, using standard loading");
                                    }
                                }

                                // 使用标准加载
                                parseAndCallback(json, callback);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing JSON", e);
                                loadDefaultData(callback);
                            }
                        }
                    });
                }else{
                    Log.i(TAG, "fetchOptions: " + TYPE );
                    Request.Companion.reqDataDisRectResponse(
                            mContext,
                            REQUEST_URL,
                            COLOR_ICONPACK_NAME,
                            TYPE,
                            new Request.Companion.NewCallback() {
                                @Override
                                public void onFail() {
                                    Log.e(TAG, "Network request failed, using default data");
                                    // 网络失败，使用本地默认数据
                                    loadDefaultData(callback);
                                }

                                @Override
                                public void onResponse(@NotNull String string) {
                                    try {
                                        String json = string;
                                        PrefUtils.Companion.saveCacheData(mContext, COLOR_ICONPACK_NAME, json);

                                        // 尝试优化加载
                                        if (mUseOptimizedLoading) {
                                            try {
                                                com.android.wallpaper.util.IconAdapterBeanV2 testBean =
                                                        mGson.fromJson(json, com.android.wallpaper.util.IconAdapterBeanV2.class);
                                                if (testBean != null && testBean.getZipUrl() != null && !testBean.getZipUrl().isEmpty()) {
                                                    Log.i(TAG, "Detected optimized JSON format, using optimized loading");
                                                    parseOptimizedJson(mContext, json, callback);
                                                    return;
                                                }
                                            } catch (Exception e) {
                                                Log.d(TAG, "Not optimized JSON format, using standard loading");
                                            }
                                        }

                                        // 使用标准加载
                                        parseAndCallback(json, callback);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error parsing JSON", e);
                                        loadDefaultData(callback);
                                    }
                                }
                            }

                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching options", e);
                loadDefaultData(callback);
            }
        });
    }

    private IconAdapterBean iconAdapterBean;
    /**
     * 解析JSON并回调
     */
    private void parseAndCallback(String json, OptionsFetchedListener<IconPackOption> callback) {
        Log.i(TAG, "parseAndCallback: ");
        try {
            IconAdapterBean bean = mGson.fromJson(json, IconAdapterBean.class);
            if(iconAdapterBean != null && bean != null){
                //版本相同，不刷新
                if(bean.getVersion() == iconAdapterBean.getVersion()){
                    return;
                }
            }
            if (bean == null || bean.getTags() == null || bean.getTags().isEmpty()) {
                Log.e(TAG, "Parsed bean is invalid");
                loadDefaultData(callback);
                return;
            }

            List<IconPackOption> allOptions = new ArrayList<>();

            // 转换为IconPackOption列表
            for (IconAdapterTag tag : bean.getTags()) {
                if (tag.getColorOptions() != null) {
                    for (IconColorOptionBean colorBean : tag.getColorOptions()) {
                        Log.i(TAG, "parseAndCallback: iconpack=" +colorBean.getIconpack_name());
                        if(!colorBean.getIcon_colors().isEmpty()){
                            Log.i(TAG, "parseAndCallback: iconpack=" +colorBean.getIconpack_name());
                            Log.i(TAG, "parseAndCallback: bean=method" + colorBean.getIcon_colors().get(0).getPre_method());
                        }
                        for (ColorCustomOption customOption : colorBean.getIcon_colors()) {
                            //根据color_generate，生成新的背景，前景色
                            transformCustomColorType(colorBean, customOption);
                            //转换HEX颜色
                            transformColorFromHex(customOption.getForegroundColors());
                            transformColorFromHex(customOption.getBackgroundColors());
                            List<ColorCustomOption> colorCustomOptions =
                                    IconPackConfig.Companion.getMoreColorInfo(customOption, colorBean.getPre_method());
                            colorCustomOptions.remove(customOption);
                            List<ColorOption> childs = customOption.getColorOptionsChilds();
                            if (childs == null) {
                                childs = new ArrayList<>();
                            }
                            childs.addAll(colorCustomOptions);
                            customOption.setColorOptionsChilds(childs);
                        }
                        allOptions.add(new IconPackOption(colorBean, tag.getName()));
                    }
                }
            }

            // 在第一个位置插入EmptyIconPackOption
            if (!allOptions.isEmpty()) {
                allOptions.add(0, new EmptyIconPackOption());
            }

            // 回调
            mMainHandler.post(() -> {
                if (callback != null) {
                    callback.onOptionsLoaded(allOptions);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in parseAndCallback", e);
            loadDefaultData(callback);
        }
    }
    public static void transformCustomColorType(IconColorOptionBean bean, ColorCustomOption customOption){
        List<ColorCustomOption.ColorCustomInfo> fgs = customOption.getForegroundColors();
        List<ColorCustomOption.ColorCustomInfo> bgs = customOption.getBackgroundColors();
        boolean change = false;
        if(CollectionUtils.isNotEmpty(fgs)){
            ArrayList<ColorCustomOption.ColorCustomInfo> newColors = makeColors(fgs.get(0));
            if(CollectionUtils.isNotEmpty(newColors)){
                fgs.clear();
                fgs.addAll(newColors);
                change = true;
            }
        }
        if(CollectionUtils.isNotEmpty(bgs)){
            ArrayList<ColorCustomOption.ColorCustomInfo> newColors = makeColors(bgs.get(0));
            if(CollectionUtils.isNotEmpty(newColors)){
                bgs.clear();
                bgs.addAll(newColors);
                change = true;
            }
        }
        if(change){
            customOption.setPre_method(IconPackConfig.METHOD_GENERATE);
            bean.setPre_method(IconPackConfig.METHOD_GENERATE);
        }
    }
    private static ArrayList<ColorCustomOption.ColorCustomInfo> makeColors(ColorCustomOption.ColorCustomInfo color){
        ArrayList<ColorCustomOption.ColorCustomInfo> newColors = new ArrayList<>();
        String [] types =  color.getColorTypes();
        if(types == null || types.length == 0 ){
            return newColors;
        }
        String type = color.getColorTypes()[0];
        int defaultColor = color.getColors()[0];
        try {
            defaultColor = Color.parseColor(color.getColorHex()[0]);
        } catch (Exception e) {
        }
        float[] hsv = new float[3];
        Color.colorToHSV(defaultColor, hsv);
        switch (type){
            case ColorCustomOption.ColorCustomInfo.COLOR_TYPE_GENERATE_NEAR_CONTRAST -> {
                float offset = 25f;
                if(Math.abs(hsv[0] - 120) < 25){
                    offset = 40f;
                }
                boolean flag = new Random().nextFloat() - 0.5f > 0;
                float[] newHsv = new float[3];
                newHsv[0] = (hsv[0] + (flag ? offset : - offset)) % 360;
                newHsv[1] = hsv[1];
                newHsv[2] = hsv[2];
                int nearColor = Color.HSVToColor(newHsv);
                newHsv[0] = (hsv[0] + 180) % 360;
                int contrast = Color.HSVToColor(newHsv);
                newColors.add(new ColorCustomOption.ColorCustomInfo(ColorCustomOption.ColorCustomInfo.COLOR_TYPE_NORMAL, new int[]{defaultColor},
                        new String[]{"#"+Integer.toHexString(defaultColor)}));
                newColors.add(new ColorCustomOption.ColorCustomInfo(ColorCustomOption.ColorCustomInfo.COLOR_TYPE_NORMAL, new int[]{nearColor},
                        new String[]{"#"+Integer.toHexString(nearColor)}));
                newColors.add(new ColorCustomOption.ColorCustomInfo(ColorCustomOption.ColorCustomInfo.COLOR_TYPE_NORMAL, new int[]{contrast},
                        new String[]{"#"+Integer.toHexString(contrast)}));
                Log.i(TAG, "makeColors: " + type + " " + Integer.toHexString(defaultColor) + " " +
                        Integer.toHexString(nearColor) + " " + Integer.toHexString(contrast));
            }
            case ColorCustomOption.ColorCustomInfo.COLOR_TYPE_GENERATE_SPLIT_3 -> {
                float[] newHsv = new float[3];
                newColors.add(new ColorCustomOption.ColorCustomInfo(ColorCustomOption.ColorCustomInfo.COLOR_TYPE_NORMAL,
                        new int[]{defaultColor}, new String[]{"#"+Integer.toHexString(defaultColor)}));
                for(int i=0;i<2;i++){
                    newHsv[0] = (hsv[0] + 360 / (2+1) * (i+1)) % 360;
                    newHsv[1] = hsv[1];
                    newHsv[2] = hsv[2];
                    int nearColor = Color.HSVToColor(newHsv);
                    newColors.add(new ColorCustomOption.ColorCustomInfo(ColorCustomOption.ColorCustomInfo.COLOR_TYPE_NORMAL,
                            new int[]{nearColor}, new String[]{"#"+Integer.toHexString(nearColor)}));
                    Log.i(TAG, "makeColors: " + type + " " + Integer.toHexString(defaultColor) + " " +
                            Integer.toHexString(nearColor));
                }

            }
            case ColorCustomOption.ColorCustomInfo.COLOR_TYPE_GENERATE_SPLIT_8 -> {
                float[] newHsv = new float[3];
                newColors.add(new ColorCustomOption.ColorCustomInfo(ColorCustomOption.ColorCustomInfo.COLOR_TYPE_NORMAL, new int[]{defaultColor}, new String[]{"#"+Integer.toHexString(defaultColor)}));
                for(int i=0;i<7;i++){
                    newHsv[0] = (hsv[0] + 360 / (7+1) * (i+1)) % 360;
                    newHsv[1] = hsv[1];
                    newHsv[2] = hsv[2];
                    int nearColor = Color.HSVToColor(newHsv);
                    newColors.add(new ColorCustomOption.ColorCustomInfo(ColorCustomOption.ColorCustomInfo.COLOR_TYPE_NORMAL,
                            new int[]{nearColor}, new String[]{"#"+Integer.toHexString(nearColor)}));
                    Log.i(TAG, "makeColors: " + type + " " + Integer.toHexString(defaultColor) + " " +
                            Integer.toHexString(nearColor));
                }
            }
            default->{
            }
        }
        return newColors;
    }
    //转换颜色HEX成int
    public static void transformColorFromHex(List<ColorCustomOption.ColorCustomInfo> colors){
        if(colors != null){
            for(ColorCustomOption.ColorCustomInfo info:colors){
                String[] hexs = info.getColorHex();
                if(info.getGradient() != null){
                    info.setPositions(info.getGradient().getPositions());
                    info.setXOffset(info.getGradient().getXOffset());
                    info.setYOffset(info.getGradient().getYOffset());
                    info.setRadial(info.getGradient().getRadial());
                    info.setAngle(info.getGradient().getAngle());
                }
                if(hexs != null && hexs.length > 0){
                    int[] colorInts = new int[hexs.length];
                    for(int i=0;i<hexs.length;i++){
                        String hex = hexs[i];
                        try {
                            colorInts[i] = Color.parseColor(hex);
                            if(colorInts[i] == 16777215){
                                colorInts[i] = Color.TRANSPARENT;
                            }
                        } catch (Exception e) {
                            colorInts[0] = Color.WHITE;
                        }
                    }
                    info.setColors(colorInts);
                }
            }
        }
    }

    /**
     * 加载本地默认数据
     */
    private void loadDefaultData(OptionsFetchedListener<IconPackOption> callback) {
        Log.i(TAG, "loadDefaultData: ", new Throwable());
        sExecutorService.submit(() -> {
            try {
                // 从assets读取默认JSON
                String json = loadJsonFromAssets("icon_pack_default.json");
                if (!TextUtils.isEmpty(json)) {
                    parseAndCallback(json, callback);
                } else {
                    // 如果assets也没有，返回空列表
                    mMainHandler.post(() -> {
                        if (callback != null) {
                            List<IconPackOption> emptyList = new ArrayList<>();
                            emptyList.add(new EmptyIconPackOption());
                            callback.onOptionsLoaded(emptyList);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading default data", e);
                mMainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            }
        });
    }

    /**
     * 从assets加载JSON文件
     */
    private String loadJsonFromAssets(String fileName) {
        try {
            java.io.InputStream inputStream = mContext.getAssets().open(fileName);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            inputStream.close();
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error loading JSON from assets: " + fileName, e);
            return null;
        }
    }

    public static final String COLOR_ICONPACK_CACHE = "color_iconPack_select";
    public static final String COLOR_ICONPACK_CACHE_PREVIEW = "color_iconPack_select_preview";
    /**
     * 加载保存的选项
     */
    private void loadSelectedOption() {
        String json = mPreferences.getString(PREF_KEY_SELECTED, null);
        if (!TextUtils.isEmpty(json)) {
            try {
                mSelectedOption = mGson.fromJson(json, IconColorOptionBean.class);
                Log.i(TAG, "loadSelectedOption: "+mSelectedOption.getIcon_colors().get(0).getColorOptionsChilds());
            } catch (Exception e) {
                Log.e(TAG, "Error loading selected option", e);
                mSelectedOption = null;
            }
        }
    }

    /**
     * 获取当前选中的选项
     */
    @Nullable
    public IconColorOptionBean getSelectedOption() {
        return mSelectedOption;
    }
    /**
     * 获取当前选中的选项
     */
    @Nullable
    public IconColorOptionBean getSelectedOptionPreview() {
        return mPreviewSelectedOption;
    }

    /**
     * 判断是否有选中的选项
     */
    public boolean hasSelectedOption() {
        if(mPreviewState){
            return mPreviewSelectedOption != null;
        }else{
            return mSelectedOption != null;
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        mSelectedOption = null;
        mPreferences.edit().clear().apply();
    }

    public void setPreviewState(boolean flag){
        mPreviewState = flag;
        if(flag){
            //复制原先
            sExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    mPreviewSelectedOption = mSelectedOption;
                }
            });
        }
    }

    public boolean isPreviewState(){
        return mPreviewState;
    }

    /**
     * 检查ZIP是否已解压
     */
    private boolean isZipExtracted(File zipDir) {
        if (!zipDir.exists() || !zipDir.isDirectory()) {
            return false;
        }
        // 检查目录中是否有.json文件
        File[] files = zipDir.listFiles((dir, name) -> name.endsWith(".json"));
        return files != null && files.length > 0;
    }

    /**
     * 解析优化版JSON
     */
    private void parseOptimizedJson(Context context, String json,
                                     OptionsFetchedListener<IconPackOption> callback) {
        try {
            com.android.wallpaper.util.IconAdapterBeanV2 bean =
                    mGson.fromJson(json, com.android.wallpaper.util.IconAdapterBeanV2.class);

            if (bean == null || bean.getTags() == null || bean.getZipUrl() == null || bean.getZipUrl().isEmpty()) {
                Log.w(TAG, "Invalid optimized JSON, falling back to full JSON");
                fallbackToFullJson(context, callback);
                return;
            }

            // 检查版本
            int localVersion = PrefUtils.Companion.getVersion(context, ZIP_CACHE_DIR);
            Log.i(TAG, "Optimized JSON version: " + bean.getVersion() + ", local version: " + localVersion);
            File zipDir = new File(PrefUtils.Companion.getFileInRes(context, ZIP_CACHE_DIR), "v" + localVersion);
            if (bean.getVersion() > localVersion || !zipDir.exists()) {
                // 需要下载新的ZIP包
                Log.i(TAG, "New version available, downloading ZIP...");
                downloadAndExtractZip(context, bean.getZipUrl(), bean.getVersion(),
                        new DownloadCallback() {
                            @Override
                            public void onSuccess() {
                                PrefUtils.Companion.setVersion(context, ZIP_CACHE_DIR, bean.getVersion());
                                buildIconPackOptions(context, bean, callback);
                            }

                            @Override
                            public void onError() {
                                // 下载失败，降级到完整JSON
                                Log.w(TAG, "ZIP download failed, falling back to full JSON");
                                fallbackToFullJson(context, callback);
                            }
                        });
            } else {
                // 使用本地ZIP数据
                Log.i(TAG, "Using local ZIP data");
                buildIconPackOptions(context, bean, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing optimized json", e);
            fallbackToFullJson(context, callback);
        }
    }

    /**
     * 降级到完整JSON加载
     */
    private void fallbackToFullJson(Context context,
                                     OptionsFetchedListener<IconPackOption> callback) {
        Log.w(TAG, "Falling back to full JSON loading");
        mUseOptimizedLoading = false;

        // 请求完整JSON（使用现有的REQUEST_URL_PREVIEW）
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(REQUEST_URL_PREVIEW)
                .build();

        mOkHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "Fallback request failed", e);
                loadDefaultData(callback);
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "Fallback request unsuccessful: " + response.code());
                    loadDefaultData(callback);
                    return;
                }

                try {
                    String json = response.body().string();
                    Log.i(TAG, "Fallback JSON loaded, parsing...");
                    parseAndCallback(json, callback); // 使用现有方法
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing fallback JSON", e);
                    loadDefaultData(callback);
                }
            }
        });
    }

    /**
     * 构建IconPackOption列表
     */
    private void buildIconPackOptions(Context context, com.android.wallpaper.util.IconAdapterBeanV2 bean,
                                       OptionsFetchedListener<IconPackOption> callback) {
        sExecutorService.execute(() -> {
            try {
                List<IconPackOption> allOptions = new ArrayList<>();

                for (com.android.wallpaper.util.IconAdapterTagLite tag : bean.getTags()) {
                    for (com.android.wallpaper.util.IconColorOptionBeanLite lite : tag.getColorOptions()) {
                        // 从本地加载完整数据
                        IconColorOptionBean fullBean = loadIconColorsFromLocal(context, lite, bean.getVersion());

                        if (fullBean == null) {
                            // 加载失败，降级到完整JSON
                            Log.w(TAG, "Failed to load icon colors for id: " + lite.getId() + ", falling back");
                            fallbackToFullJson(context, callback);
                            return;
                        }

                        // 处理颜色扩展（与现有逻辑一致）
                        for (ColorCustomOption customOption : fullBean.getIcon_colors()) {
                            //转换HEX颜色
                            //根据color_generate，生成新的背景，前景色
                            transformCustomColorType(fullBean, customOption);
                            transformColorFromHex(customOption.getForegroundColors());
                            transformColorFromHex(customOption.getBackgroundColors());
                            List<ColorCustomOption> colorCustomOptions =
                                    IconPackConfig.Companion.getMoreColorInfo(customOption, fullBean.getPre_method());
                            colorCustomOptions.remove(customOption);
                            List<ColorOption> childs = customOption.getColorOptionsChilds();
                            if (childs == null) {
                                childs = new ArrayList<>();
                            }
                            childs.addAll(colorCustomOptions);
                            customOption.setColorOptionsChilds(childs);
                        }

                        allOptions.add(new IconPackOption(fullBean, tag.getName()));
                    }
                }

                // 插入EmptyIconPackOption
                if (!allOptions.isEmpty()) {
                    allOptions.add(0, new EmptyIconPackOption());
                }

                Log.i(TAG, "Built " + allOptions.size() + " icon pack options from optimized data");

                // 回调到主线程
                mMainHandler.post(() -> {
                    if (callback != null) {
                        callback.onOptionsLoaded(allOptions);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error building icon pack options", e);
                fallbackToFullJson(context, callback);
            }
        });
    }

    /**
     * 从本地加载IconColorOptionBean的完整数据
     */
    private IconColorOptionBean loadIconColorsFromLocal(Context context,
                                                         com.android.wallpaper.util.IconColorOptionBeanLite lite,
                                                         int version) {
        File zipDir = new File(PrefUtils.Companion.getFileInRes(context, ZIP_CACHE_DIR), "v" + version);
        File subJsonFile = new File(new File(zipDir, "icon_colors"), lite.getId() + ".json");

        if (!subJsonFile.exists()) {
            Log.w(TAG, "Sub JSON file not found for id: " + lite.getId());
            return null;
        }

        try {
            String json = PrefUtils.Companion.readFile(subJsonFile);
            com.android.wallpaper.util.IconColorSubJson subJson =
                    mGson.fromJson(json, com.android.wallpaper.util.IconColorSubJson.class);

            if (subJson == null || subJson.getIcon_colors() == null) {
                Log.w(TAG, "Invalid sub JSON for id: " + lite.getId());
                return null;
            }

            // 合并数据：创建完整的IconColorOptionBean
            IconColorOptionBean fullBean = new IconColorOptionBean();
            fullBean.setId(lite.getId());
            if(!TextUtils.isEmpty(lite.getIconpack_name())){
                fullBean.setIconpack_name(lite.getIconpack_name());
            }
            if(!TextUtils.isEmpty(subJson.getIconpack_name())){
                fullBean.setIconpack_name(subJson.getIconpack_name());
            }
            if(!TextUtils.isEmpty(lite.getPre_method())){
                fullBean.setPre_method(lite.getPre_method());
            }
            if(!TextUtils.isEmpty(lite.getIcon_shape())){
                fullBean.setIcon_shape(lite.getIcon_shape());
            }
            fullBean.setIcon_colors(subJson.getIcon_colors());

            Log.i(TAG, "Loaded sub JSON for id: " + lite.getId() + ", colors count: " +
                    subJson.getIcon_colors().size());

            return fullBean;
        } catch (Exception e) {
            Log.e(TAG, "Error loading sub json for id: " + lite.getId(), e);
            return null;
        }
    }

    /**
     * 下载并解压ZIP包
     */
    private void downloadAndExtractZip(Context context, String zipUrl, int version,
                                       DownloadCallback callback) {
        File zipDir = new File(PrefUtils.Companion.getFileInRes(context, ZIP_CACHE_DIR), "v" + version);
        File zipFile = new File(zipDir, "icon_colors");

        // 检查是否已下载并解压
        if (zipDir.exists() && isZipExtracted(zipDir)) {
            Log.i(TAG, "ZIP already extracted for version: " + version);
            callback.onSuccess();
            return;
        }

        Log.i(TAG, "Downloading ZIP from: " + zipUrl + ", version: " + version);

        // 确保目录存在
        if (!zipDir.exists()) {
            zipDir.mkdirs();
        }

        // 下载ZIP
        Request.Companion.downloadRes(context, zipUrl, zipFile, new Request.DownloadCallback() {
            @Override
            public void onSuc() {
                Log.i(TAG, "ZIP download success, extracting...");
                try {
//                    // 解压ZIP
//                    PrefUtils.Companion.unZip(zipFile);
                    // 删除ZIP文件，节省空间
                    zipFile.delete();
                    Log.i(TAG, "ZIP extracted successfully");

                    // 清理旧版本缓存
                    cleanOldZipCache(context, version);

                    callback.onSuccess();
                } catch (Exception e) {
                    Log.e(TAG, "Error extracting ZIP", e);
                    callback.onError();
                }
            }

            @Override
            public void onError() {
                Log.e(TAG, "ZIP download failed");
                callback.onError();
            }

            @Override
            public void onProgress(long transPortedBytes, long totalBytes) {
                // 可选：显示下载进度
                if (totalBytes > 0) {
                    int progress = (int) ((transPortedBytes * 100) / totalBytes);
                    Log.d(TAG, "Download progress: " + progress + "%");
                }
            }
        });
    }

    /**
     * 清理旧版本的ZIP缓存
     */
    private void cleanOldZipCache(Context context, int currentVersion) {
        File zipCacheRoot = PrefUtils.Companion.getFileInRes(context, ZIP_CACHE_DIR);
        if (!zipCacheRoot.exists()) {
            return;
        }

        File[] versionDirs = zipCacheRoot.listFiles();
        if (versionDirs == null) {
            return;
        }

        for (File dir : versionDirs) {
            if (dir.isDirectory() && dir.getName().startsWith("v")) {
                try {
                    int version = Integer.parseInt(dir.getName().substring(1));
                    if (version < currentVersion) {
                        FileUtils.INSTANCE.deleteFile(dir);
                        Log.i(TAG, "Cleaned old zip cache: " + dir.getName());
                    }
                } catch (NumberFormatException e) {
                    // 忽略无效的目录名
                    Log.w(TAG, "Invalid version directory name: " + dir.getName());
                }
            }
        }
    }

    /**
     * 下载回调接口
     */
    private interface DownloadCallback {
        void onSuccess();
        void onError();
    }
}
