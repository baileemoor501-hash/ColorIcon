package com.android.wallpaper.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.Log
import android.util.Xml
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.graphics.toColor
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withScale
import androidx.palette.graphics.Palette
import com.android.customization.model.ResourceConstants
import com.android.customization.model.color.ColorCustomOption
import com.android.customization.model.color.ColorCustomOption.ColorCustomInfo
import com.android.customization.model.color.ColorOptionsProvider
import com.android.customization.model.decorator.IconDecOption
import com.android.customization.model.decorator.IconDecProvider
import com.android.customization.model.decorator.IconMarkBean
import com.android.customization.model.iconback.IconBackOption
import com.android.customization.model.iconback.IconBackProvider
import com.android.customization.model.iconpack.IconPackManager
import com.android.customization.model.mask.IconMaskOption
import com.android.customization.model.mask.IconMaskProvider
import com.android.customization.model.shadow.IconShadowOption
import com.android.customization.model.shadow.IconShadowProvider
import com.android.customization.model.shape.IconShapeProvider
import com.android.customization.model.stroke.IconStrokeOption
import com.android.customization.model.stroke.IconStrokeProvider
import com.android.systemui.monet.ColorScheme
import com.android.systemui.monet.Style
import com.android.wallpaper.BuildConfig
import com.android.wallpaper.R
import com.android.wallpaper.config.CalendarCfg
import com.extra.iconshape.AdaptiveIconShape
import com.extra.iconshape.IconShapeHelper
import com.extra.iconshape.PreferenceUtil
import com.extra.iconshape.graphics.AdaptiveIconDrawableCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.lib.request.PrefUtils
import com.lib.request.Request
import com.lib.request.Request.Companion.getBitmapByPreview
import com.liblauncher.ShortcutInfo
import com.liblauncher.compat.LauncherActivityInfoCompat
import com.liblauncher.compat.LauncherAppsCompat
import com.liblauncher.compat.UserHandleCompat
import com.liblauncher.prefs.PrefHelper
import com.liblauncher.util.CollectionUtils
import com.liblauncher.util.ScreenUtils.dp
import com.liblauncher.util.Utilities
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * 前景图处理结果数据类
 * 封装已上色的前景图Bitmap列表和相关颜色配置信息
 */
data class ProcessedForeground(
    @JvmField
    val foregroundBitmaps: MutableList<Bitmap>,
    @JvmField
    val fgColors: MutableList<ColorCustomInfo>,
    @JvmField
    val bgColors: MutableList<ColorCustomInfo>,
    @JvmField
    val internalMono: Boolean,
    @JvmField
    val useSourceColor: Boolean
)

class IconPackConfig(var context: Context) {

  
    // IconColorOptionBean配置
    @JvmField
    var mIconColorOptionBean: IconColorOptionBean? = null

    //装饰
    var iconDecOptionList: ArrayList<IconDecOption>? = null
    //底座
    var iconBackOptions: ArrayList<IconBackOption>? = null
    //遮罩
    var iconMaskOption: IconMaskOption? = null
    //描边
    var iconStrokeOption: IconStrokeOption?=null
    //形状
    var iconShapeGroup = ArrayList<AdaptiveIconShape>()
    var iconColorOptions : List<ColorCustomOption>? = null
    //阴影
    var iconShadowOption : IconShadowOption?= null
    var icon_pack_name = ""
    lateinit var drawableMap : HashMap<String, List<String>>
    var iconBgDrawable = mutableListOf<Drawable>()
    var iconMaskDrawable:Drawable?=null
    //前景图路径
    var iconForegroundFileName = ""

    /**
     * 从IconColorOptionBean同步配置到现有变量
     */
    private fun syncFromOptionBean(bean: IconColorOptionBean) {
        // 1. 同步底座
        bean.icon_backs?.let{
            iconBackOptions = bean.icon_backs.toCollection(ArrayList())
        }


        // 2. 同步装饰
        bean.icon_decs?.let{
            iconDecOptionList = bean.icon_decs.toCollection(ArrayList())
        }


        // 3. 同步遮罩
        iconMaskOption = bean.icon_mask

        // 4. 同步描边
        iconStrokeOption = bean.icon_stroke

        // 5. 同步阴影
        iconShadowOption = bean.icon_shadow
        Log.i(TAG, "testModeGetIcon: icon_shadow2 = ${bean.icon_shadow} $this")
        // 6. 同步形状（String转ArrayList<AdaptiveIconShape>）
        iconShapeGroup.clear()
        if (!bean.icon_shape.isNullOrEmpty()) {
            bean.icon_shape.split(";").forEach { shapeStr ->
                PreferenceUtil.shapeStrToShape(shapeStr)?.let { shape ->
                    if (shape != AdaptiveIconShape.sNone) {
                        iconShapeGroup.add(shape)
                    }
                }
            }
        }
        if (iconShapeGroup.isEmpty()) {
            iconShapeGroup.add(AdaptiveIconShape.SQUIRCLE)
        }
        //前景图指向url链接，判断是否有
        if(bean.iconpack_url.isNotEmpty()){
            val split = bean.iconpack_url.split("/")
            if(split.isNotEmpty()){
                val fileName = split.last().replace(".zip", "")
                if(fileName.isNotEmpty()){
                    iconForegroundFileName = fileName
                }
            }
        }

        // 7. 同步颜色（直接保存List，不做随机选择）
        val tempList = mutableListOf<ColorCustomOption>()
        Log.i(TAG, "syncFromOptionBean: ${bean.icon_shape}")
        bean.icon_colors?.forEach { it->
            tempList += getMoreColorInfo(it, bean.pre_method)
        }
        iconColorOptions = tempList.toMutableList()

        // 8. 同步iconpack名称
        icon_pack_name = bean.iconpack_name

        Log.i(TAG, "syncFromOptionBean: 成功同步Bean配置 ${bean.iconpack_name}")
        updateDrawableMap()
    }
    /**
     * 将旧配置数据迁移到IconColorOptionBean
     */
    private fun migrateOldDataToBean(context: Context) {
        // 如果Bean已存在，无需迁移
        if (mIconColorOptionBean != null) return

        // 如果旧配置为空，无需迁移
        if (iconBackOptions.isNullOrEmpty() && iconDecOptionList.isNullOrEmpty()) {
            Log.i(TAG, "migrateOldDataToBean: 无旧配置需要迁移")
            return
        }

        try {
            // 获取当前的shape字符串
            val shapeStr = IconShapeProvider.getIconShapeGroup(context)

            // 创建IconColorOptionBean实例
            val bean = IconColorOptionBean(
                version = 1,
                icon_backs = iconBackOptions?.toMutableList() ?: mutableListOf(),
                icon_decs = iconDecOptionList?.toMutableList() ?: mutableListOf(),
                icon_mask = iconMaskOption,
                icon_stroke = iconStrokeOption ?: IconStrokeOption(),
                icon_shadow = iconShadowOption ?: IconShadowOption(),
                icon_shape = shapeStr,
                iconpack_name = icon_pack_name,
                icon_colors = mutableListOf()  // 旧数据无icon_colors
            )

            // 保存Bean到IconPackManager
            val iconPackManager = IconPackManager.getInstance(context)
            val option = com.android.customization.model.iconpack.IconPackOption(bean, icon_pack_name)
            iconPackManager.apply(option, object : com.android.customization.model.CustomizationManager.Callback {
                override fun onSuccess() {
                    Log.i(TAG, "migrateOldDataToBean: 迁移成功")
                }
                override fun onError(throwable: Throwable?) {
                    Log.e(TAG, "migrateOldDataToBean: 迁移失败", throwable)
                }
            })
            mIconColorOptionBean = bean
        } catch (e: Exception) {
            Log.e(TAG, "migrateOldDataToBean: 迁移异常", e)
        }
    }



    /**
     * 刷新IconPack配置
     */
    fun updateIconPackConfig(context: Context, preview: Boolean){
        // 优先从IconPackManager加载Bean
        try {
            val iconPackManager = IconPackManager.getInstance(context)
            val bean = if(preview) iconPackManager.selectedOptionPreview else iconPackManager.selectedOption
            if (bean != null) {
                mIconColorOptionBean = bean
                syncFromOptionBean(bean)
                if(iconShapeGroup.isEmpty()){
                    var shapeStr = IconShapeProvider.getIconShapeGroup(context);
                    Log.i(TAG, "updateIconPackConfig:shape= $shapeStr")
                    shapeStr = PreferenceUtil.ICON_INTERNAL_SQUIRCLE
                    shapeStr.split(";").forEach {
                        val shape = PreferenceUtil.shapeStrToShape(it)
                        if(shape != null && shape != AdaptiveIconShape.sNone){
                            iconShapeGroup.add(shape)
                        }
                    }
                }
                if(iconShapeGroup.isEmpty()){
                    iconShapeGroup.add(AdaptiveIconShape.SQUIRCLE)
                }
                Log.i(TAG, "updateIconPackConfig: 成功从Bean加载配置")
                return  // Bean加载成功，直接返回
            } else {
                Log.w(TAG, "updateIconPackConfig: Bean为null，使用旧逻辑加载")
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateIconPackConfig: Bean加载失败", e)
        }

        // Fallback: 使用旧逻辑从SharedPreference加载
        val iconBack = PrefHelper.with(context).getStringCustomDefault(
            IconPackPrefUtils.ICON_PACK_PREF_NAME, if(preview) ResourceConstants.ICON_BACK_PREVIEW else  ResourceConstants.ICON_BACK, "{}")
        iconBackOptions = IconBackProvider.generaBackFromJson(iconBack)
        val decorator = PrefHelper.with(context).getStringCustomDefault(
            IconPackPrefUtils.ICON_PACK_PREF_NAME, if(preview) ResourceConstants.ICON_DECORATOR_PREVIEW else ResourceConstants.ICON_DECORATOR, "{}")
        iconDecOptionList = IconDecProvider.generaDecFromJson(decorator)
        val iconMask = PrefHelper.with(context).getStringCustomDefault(
            IconPackPrefUtils.ICON_PACK_PREF_NAME, ResourceConstants.ICON_MASK_PREVIEW, "{}")
        iconMaskOption = IconMaskProvider.generaMaskFromJson(context, iconMask)
        val iconStroke = PrefHelper.with(context).getStringCustomDefault(
            PrefHelper.getDefName(context),
            if (preview) ResourceConstants.ICON_STROKE_PREVIEW else ResourceConstants.ICON_STROKE,
            context.getString(R.string.icon_shape_stroke_cfg_def)
        )
        iconStrokeOption = IconStrokeProvider.generaStrokeFromJSON(iconStroke)
        val iconShadow = PrefHelper.with(context).getStringCustomDefault(
            IconPackPrefUtils.ICON_PACK_PREF_NAME, ResourceConstants.ICON_SHADOW_PREVIEW, "{}")
        iconShadowOption = IconShadowProvider.generaShadowFromJSON(iconShadow)
//        iconShapeGroup.clear()
//        var shapeStr = IconShapeProvider.getIconShapeGroup(context);
//        //TODO:固定为三星形状
//        shapeStr = PreferenceUtil.ICON_INTERNAL_SQUIRCLE
//        Log.i(TAG, "updateIconPackConfig: $shapeStr")
//        shapeStr.split(";").forEach {
//            val shape = PreferenceUtil.shapeStrToShape(it)
//            if(shape != null && shape != AdaptiveIconShape.sNone){
//                iconShapeGroup.add(shape)
//            }
//        }
        if(iconShapeGroup.size == 0){
            iconShapeGroup.add(AdaptiveIconShape.SQUIRCLE)
        }
//        iconColorOptions = ColorManager.getInstance(context).colorOptionMap
//        iconForegroundFileName = PrefHelper.with(context).getStringCustomDefault(
//            IconPackPrefUtils.ICON_PACK_PREF_NAME, ResourceConstants.ICON_FOREGROUND_RESOURCE_NAME, "")

        // 尝试迁移旧数据到Bean
        migrateOldDataToBean(context)
    }

    fun getPrefs(context: Context): SharedPreferences? {
        return context.getSharedPreferences(
            context.packageName + ".prefs",
            Context.MODE_PRIVATE
        )
    }

    var iconColorBean: IconColorPackBean?= null
    //从主题zip包读取json配置
    fun readJsonToIconColorBean(file: File){
        val jsonStr = file.readText(Charsets.UTF_8)
        Log.i(TAG, "readJsonToIconColorBean: 加载json文件")
        //TODO:暂时屏蔽
        runCatching {
            val bean = Gson().fromJson(jsonStr, IconColorPackBean::class.java)
            val json = JSONObject(jsonStr)
            val pre_method = json.has("pre_method")
            val color = JSONObject(jsonStr).optJSONArray("icon_colors")
            //没有pre_method表示旧版本的json数据
            Log.i(TAG, "readJsonToIconColorBean: $pre_method")
            if(color != null && !pre_method){
                Log.i(TAG, "readJsonToIconColorBean: 旧的json配置 super上的 ${color}")
                bean.icon_colors.clear()
                // 关键：使用GsonBuilder配置，禁用对空集合的特殊处理
                val gson = GsonBuilder()
                    .serializeNulls()  // 序列化空值
                    .disableHtmlEscaping()
                    .create()
                for(index in 0 until color.length()){
                    val c = color.optJSONObject(index)
                    val t = c.optString("bg")
                    val m = c.optString("fg")
                    val tJsonArray = JSONArray(t)
                    val mJsonAdapter = JSONArray(m)
                    // 解析时使用正确的TypeToken
                    val type = object : TypeToken<ArrayList<ColorCustomInfo>>() {}.type
                    val cc = gson.fromJson<ArrayList<ColorCustomInfo>>(t, type)
                    cc.forEachIndexed { i, ccc ->
                        var d = tJsonArray.optJSONObject(i).optJSONArray("colors")
                        d?.let{
                            val cccc = arrayListOf<Int>()
                            for(s in 0 until d.length()){
                                cccc.add(d.optInt(s))
                            }
                            ccc.colors = cccc.toIntArray()
                        }
                        d = tJsonArray.optJSONObject(i).optJSONArray("colorTypes")
                        d?.let{
                            val cccc = arrayListOf<String>()
                            for(s in 0 until d.length()){
                                cccc.add(d.optString(s))
                            }
                            ccc.colorTypes = cccc.toTypedArray()
                        }
                        d = tJsonArray.optJSONObject(i).optJSONArray("positions")
                        d?.let{
                            val cccc = arrayListOf<Float>()
                            for(s in 0 until d.length()){
                                cccc.add(d.optDouble(s).toFloat())
                            }
                            ccc.positions = cccc.toFloatArray()
                        }
                    }
                    val cc1 = gson.fromJson<ArrayList<ColorCustomInfo>>(m,
                        object:TypeToken<ArrayList<ColorCustomInfo>>(){}.type)
                    cc1.forEachIndexed { i, ccc ->
                        Log.i(TAG, "exportToString: ccc")
                        var d = mJsonAdapter.optJSONObject(i).optJSONArray("colors")
                        Log.i(TAG, "exportToString:ddddd $d")
                        d?.let{
                            val cccc = arrayListOf<Int>()
                            for(s in 0 until d.length()){
                                cccc.add(d.optInt(s))
                            }
                            ccc.colors = cccc.toIntArray()
                        }
                        d = mJsonAdapter.optJSONObject(i).optJSONArray("colorTypes")
                        d?.let{
                            val cccc = arrayListOf<String>()
                            for(s in 0 until d.length()){
                                cccc.add(d.optString(s))
                            }
                            ccc.colorTypes = cccc.toTypedArray()
                        }
                        d = mJsonAdapter.optJSONObject(i).optJSONArray("positions")
                        d?.let{
                            val cccc = arrayListOf<Float>()
                            for(s in 0 until d.length()){
                                cccc.add(d.optDouble(s).toFloat())
                            }
                            ccc.positions = cccc.toFloatArray()
                        }
                    }
                    bean.icon_colors.add(ColorSubScheme().apply{
                        bgColors.addAll(cc);
                        fgColors.addAll(cc1);
                    })
                }
            }
            if(bean.icon_colors.isNotEmpty() &&bean.icon_colors[0].fgColors.isNotEmpty()){
                iconColorBean = bean
            }
        }
        if(iconColorBean == null){
            runCatching {
                Log.i(TAG, "readJsonToIconColorBean:加载新的json文件")
               Gson().fromJson(jsonStr, IconColorOptionBean::class.java)?.let{

                    it.icon_colors.forEach { customOption->
                        //根据color_generate，生成新的背景，前景色
                        IconPackManager.transformCustomColorType(it, customOption)
                        //转换HEX颜色
                        IconPackManager.transformColorFromHex(customOption.foregroundColors)
                        IconPackManager.transformColorFromHex(customOption.backgroundColors)
                        val colorCustomOptions =
                            getMoreColorInfo(customOption, it.pre_method)
                        colorCustomOptions.remove(customOption)
                        var childs = customOption.colorOptionsChilds
                        if (childs == null) {
                            childs = mutableListOf()
                        }
                        childs.addAll(colorCustomOptions)
                        customOption.colorOptionsChilds = childs
                    }


                    mIconColorOptionBean = it

                    syncFromOptionBean(it)
                    if(iconShapeGroup.isEmpty()){
                        var shapeStr = IconShapeProvider.getIconShapeGroup(context);
                        Log.i(TAG, "readJsonToIconColorBean:shape= $shapeStr")
                        shapeStr = PreferenceUtil.ICON_INTERNAL_SQUIRCLE
                        shapeStr.split(";").forEach {
                            val shape = PreferenceUtil.shapeStrToShape(it)
                            if(shape != null && shape != AdaptiveIconShape.sNone){
                                iconShapeGroup.add(shape)
                            }
                        }
                    }
                    if(iconShapeGroup.isEmpty()){
                        iconShapeGroup.add(AdaptiveIconShape.SQUIRCLE)
                    }
                    Log.i(TAG, "readJsonToIconColorBean: 成功从新json加载配置")
                }
            }
        }


    }

    fun readJsonToIconColorBean(jsonStr:String){

        if(iconColorBean == null){
            runCatching {
                Log.i(TAG, "readJsonToIconColorBean:加载新的json文件")
                Gson().fromJson(jsonStr, IconColorOptionBean::class.java)?.let{

                    it.icon_colors.forEach { customOption->
                        //根据color_generate，生成新的背景，前景色
                        IconPackManager.transformCustomColorType(it, customOption)
                        //转换HEX颜色
                        IconPackManager.transformColorFromHex(customOption.foregroundColors)
                        IconPackManager.transformColorFromHex(customOption.backgroundColors)
                        val colorCustomOptions =
                            getMoreColorInfo(customOption, it.pre_method)
                        colorCustomOptions.remove(customOption)
                        var childs = customOption.colorOptionsChilds
                        if (childs == null) {
                            childs = mutableListOf()
                        }
                        childs.addAll(colorCustomOptions)
                        customOption.colorOptionsChilds = childs
                    }


                    mIconColorOptionBean = it

                    syncFromOptionBean(it)
                    if(iconShapeGroup.isEmpty()){
                        var shapeStr = IconShapeProvider.getIconShapeGroup(context);
                        Log.i(TAG, "readJsonToIconColorBean:shape= $shapeStr")
                        shapeStr = PreferenceUtil.ICON_INTERNAL_SQUIRCLE
                        shapeStr.split(";").forEach {
                            val shape = PreferenceUtil.shapeStrToShape(it)
                            if(shape != null && shape != AdaptiveIconShape.sNone){
                                iconShapeGroup.add(shape)
                            }
                        }
                    }
                    if(iconShapeGroup.isEmpty()){
                        iconShapeGroup.add(AdaptiveIconShape.SQUIRCLE)
                    }
                    Log.i(TAG, "readJsonToIconColorBean: 成功从新json加载配置")
                }
            }.onFailure {
                Log.i(TAG, "readJsonToIconColorBean:fail ", it)
            }
        }
    }
    /**
     * 导出配置数据到json文件中
     */
    fun exportToString(context:Context):String{
        val json = JSONObject()
        val iconBack = PrefHelper.with(context).getStringCustomDefault(
            context.packageName + ".prefs", ResourceConstants.ICON_BACK_PREVIEW, "{}")
        val decorator = PrefHelper.with(context).getStringCustomDefault(
            context.packageName + ".prefs", ResourceConstants.ICON_DECORATOR_PREVIEW, "{}")
        val iconMask = PrefHelper.with(context).getStringCustomDefault(
            context.packageName + ".prefs", ResourceConstants.ICON_MASK_PREVIEW, "{}")
        val iconStroke = PrefHelper.with(context).getStringCustomDefault(
            PrefHelper.getDefName(context), ResourceConstants.ICON_STROKE_PREVIEW, "{}")
        val iconShadow = PrefHelper.with(context).getStringCustomDefault(
            context.packageName + ".prefs", ResourceConstants.ICON_SHADOW_PREVIEW, "{}")
        val shapeStr = PrefHelper.with(context).getStringCustomDefault(
            PrefHelper.getDefName(context), ResourceConstants.ICON_SHAPE_PREVIEW, "")
        val colorOptionsStr = PrefHelper.with(context).getStringCustomDefault(
            context.packageName + ".prefs", ResourceConstants.THEME_CUSTOMIZATION_OVERLAY_PACKAGES_PREVIEW, "")
        val icon_colors = JSONArray()
        Log.i(TAG, "exportToString: json=${json.toString(2)}")
        val icon_colorSchemes = ArrayList<ColorSubScheme>()
        if(colorOptionsStr.isNotEmpty()){
            runCatching {
                val colorJson = JSONArray(colorOptionsStr)
                for(i in 0 until colorJson.length()){
                    val j = colorJson.optJSONObject(i)
                    if(j.optString(ColorOptionsProvider.OVERLAY_COLOR_SOURCE)
                        == ColorOptionsProvider.COLOR_SOURCE_CUSTOM){
                        val t = ColorSubScheme().apply{
                            val type = object : TypeToken<ArrayList<ColorCustomInfo>>() {}.type
                            val bs = Gson().fromJson<ArrayList<ColorCustomInfo>>(
                                j.optString("bg"), type)
                            bgColors.addAll(bs)
                            val fs = Gson().fromJson<ArrayList<ColorCustomInfo>>(
                                j.optString("fg"), type)
                            fgColors.addAll(fs)
                        }
                        icon_colorSchemes.add(t)
                        icon_colors.put(j)
                    }
                }
            }.onFailure {
                Log.i(TAG, "exportToString: ", it)
            }
        }
        runCatching {
            json.put("version", 1)
            json.put("icon_backs", JSONArray(iconBack))
            json.put("icon_decs", JSONArray(decorator))
            json.put("icon_mask", JSONObject(iconMask))
            json.put("icon_stroke", JSONObject(iconStroke))
            json.put("icon_shadow", JSONObject(iconShadow))
            json.put("icon_shape", shapeStr)
            json.put("icon_colors", icon_colors)
//        json.put(ResourceConstants.ICON_FOREGROUND_RESOURCE_NAME, iconForegroundFileName)
            val jsonStr = json.toString(2)
            Log.i(TAG, "exportToString: ${json.toString(2)}")
            val bean = Gson().fromJson(json.toString(), IconColorPackBean::class.java)
            val color = JSONObject(jsonStr).optJSONArray("icon_colors")
            if(color != null){
                bean.icon_colors.clear()
                // 关键：使用GsonBuilder配置，禁用对空集合的特殊处理
                val gson = GsonBuilder()
                    .serializeNulls()  // 序列化空值
                    .disableHtmlEscaping()
                    .create()
                for(index in 0 until color.length()){
                    val c = color.optJSONObject(index)
                    val t = c.optString("bg")
                    val m = c.optString("fg")
                    val tJsonArray = JSONArray(t)
                    val mJsonAdapter = JSONArray(m)
                    // 解析时使用正确的TypeToken
                    val type = object : TypeToken<ArrayList<ColorCustomInfo>>() {}.type
                    val cc = gson.fromJson<ArrayList<ColorCustomInfo>>(t, type)
                    cc.forEachIndexed { i, ccc ->
                        var d = tJsonArray.optJSONObject(i).optJSONArray("colors")
                        d?.let{
                            val cccc = arrayListOf<Int>()
                            for(s in 0 until d.length()){
                                cccc.add(d.optInt(s))
                            }
                            ccc.colors = cccc.toIntArray()
                        }
                        d = tJsonArray.optJSONObject(i).optJSONArray("colorTypes")
                        d?.let{
                            val cccc = arrayListOf<String>()
                            for(s in 0 until d.length()){
                                cccc.add(d.optString(s))
                            }
                            ccc.colorTypes = cccc.toTypedArray()
                        }
                        d = tJsonArray.optJSONObject(i).optJSONArray("positions")
                        d?.let{
                            val cccc = arrayListOf<Float>()
                            for(s in 0 until d.length()){
                                cccc.add(d.optDouble(s).toFloat())
                            }
                            ccc.positions = cccc.toFloatArray()
                        }
                    }
                    val cc1 = gson.fromJson<ArrayList<ColorCustomInfo>>(m,
                        object:TypeToken<ArrayList<ColorCustomInfo>>(){}.type)
                    cc1.forEachIndexed { i, ccc ->
                        var d = mJsonAdapter.optJSONObject(i).optJSONArray("colors")
                        d?.let{
                            val cccc = arrayListOf<Int>()
                            for(s in 0 until d.length()){
                                cccc.add(d.optInt(s))
                            }
                            ccc.colors = cccc.toIntArray()
                        }
                        d = mJsonAdapter.optJSONObject(i).optJSONArray("colorTypes")
                        d?.let{
                            val cccc = arrayListOf<String>()
                            for(s in 0 until d.length()){
                                cccc.add(d.optString(s))
                            }
                            ccc.colorTypes = cccc.toTypedArray()
                        }
                        d = mJsonAdapter.optJSONObject(i).optJSONArray("positions")
                        d?.let{
                            val cccc = arrayListOf<Float>()
                            for(s in 0 until d.length()){
                                cccc.add(d.optDouble(s).toFloat())
                            }
                            ccc.positions = cccc.toFloatArray()
                        }
                    }
                    bean.icon_colors.add(ColorSubScheme().apply{
                        bgColors.addAll(cc);
                        fgColors.addAll(cc1);
                    })
                }
            }
            Log.i(TAG, "exportToString: bean=$bean")
        }.onFailure {
        }

        return json.toString()
    }

    /**
     * 应用导出的配置文件
     */
    fun restoreFromString(context:Context, str:String){
        val json = JSONObject(str)
        val iconBack = json.optJSONArray(ResourceConstants.ICON_BACK_PREVIEW)
        PrefHelper.with(context).putStringCommit(
            IconPackPrefUtils.ICON_PACK_PREF_NAME, ResourceConstants.ICON_BACK_PREVIEW, iconBack.toString())

        val decorator = json.optJSONArray(ResourceConstants.ICON_DECORATOR_PREVIEW)
        PrefHelper.with(context).putStringCommit(
            IconPackPrefUtils.ICON_PACK_PREF_NAME, ResourceConstants.ICON_DECORATOR_PREVIEW, decorator.toString())

        val iconMask = json.optJSONObject(ResourceConstants.ICON_MASK_PREVIEW)
        PrefHelper.with(context).putStringCommit(
            IconPackPrefUtils.ICON_PACK_PREF_NAME, ResourceConstants.ICON_MASK_PREVIEW, iconMask.toString())

        val iconStroke = json.optString(ResourceConstants.ICON_STROKE_PREVIEW)
        PrefHelper.with(context).putStringCommit(PrefHelper.getDefName(context), ResourceConstants.ICON_STROKE_PREVIEW, iconStroke)

        val iconShadow =  json.optString(ResourceConstants.ICON_SHADOW_PREVIEW)
        PrefHelper.with(context).putStringCommit(
            IconPackPrefUtils.ICON_PACK_PREF_NAME, ResourceConstants.ICON_SHADOW_PREVIEW, iconShadow)

        val shapeStr = json.optString(PreferenceUtil.SETTINGS_UI_THEME_INTERNAL_ICON_SHAPE_GROUP)
        IconShapeProvider.setIconShapeGroup(context, shapeStr)

//        val colorOptionsStr = json.optJSONObject(ResourceConstants.ICON_COLOR_OPTIONS)
//        PrefHelper.with(context).putStringCommit(
//            IconPackPrefUtils.ICON_PACK_PREF_NAME, ResourceConstants.ICON_COLOR_OPTIONS, colorOptionsStr.toString())
//
//        val iconForegroundFileName = json.optString(ResourceConstants.ICON_FOREGROUND_RESOURCE_NAME)
//        PrefHelper.with(context).putStringCommit(
//            IconPackPrefUtils.ICON_PACK_PREF_NAME, ResourceConstants.ICON_FOREGROUND_RESOURCE_NAME, iconForegroundFileName)
        updateIconPackConfig(context, false)
    }

    class IconShapeConfig {
        var iconBelow : BitmapDrawable ?= null
        var iconBelowScale :BitmapDrawable ?= null
        var adaptiveScale = 1f
    }
    companion object {
        private const val TAG = "IconPackConfig"
        const val ICON_PACK_NAME = "icon_pack_name"
        const val INTERNAL_ICON_PACK_1 = "interval_icon_pack_1"
        const val INTERNAL_ICON_PACK_2 = "interval_icon_pack_2"
        const val INTERNAL_ICON_PACK_3 = "interval_icon_pack_3"
        const val INTERNAL_ICON_PACK_4 = "interval_icon_pack_4"
        const val INTERNAL_ICON_PACK_5 = "interval_icon_pack_5"
        const val INTERNAL_ICON_PACK_6 = "interval_icon_pack_6"
        const val INTERNAL_ICON_PACK_7 = "interval_icon_pack_7"
        const val INTERNAL_ICON_PACK_8 = "new droid 16 线面"
        const val INTERNAL_ICON_PACK_9 = "pi图标分层（带背景）"
        const val INTERNAL_ICON_PACK_ROSE_NO_DEC = "rose无点缀"

        const val ROLE_SURFACE = "surface"
        const val ROLE_PRIMARY = "primary"
        const val ROLE_ON_SURFACE = "on_surface"
        const val ROLE_ON_PRIMARY = "on_primary"
        const val ROLE_SECONDARY = "secondary"
        const val ROLE_SECONDARY_1 = "secondary_1"
        const val ROLE_SECONDARY_2 = "secondary_2"
        const val ROLE_SECONDARY_3 = "secondary_3"
        const val ROLE_SECONDARY_4 = "secondary_4"
        const val ROLE_SECONDARY_5 = "secondary_5"
        const val ROLE_SECONDARY_6 = "secondary_6"
        const val ROLE_SECONDARY_7 = "secondary_7"
        const val ROLE_ON_SECONDARY = "on_secondary"
        //如果dark SurfaceView,调浅颜色， 否则secondary
        const val ROLE_LIGHT_SURFACE_OR_SECONDARY = "light_surface_or_secondary"
        const val ROLE_VARIANT = "variant"

        /**
         * 根据前景图/背景图， 颜色角色，返回对应不同的颜色值，没有则用默认颜色
         */
        fun getColorByRole(colorPair:Pair<MutableList<ColorCustomInfo>,
                MutableList<ColorCustomInfo>>?,
                           role:String, def:Int):Int{
            colorPair?.let{
                val scheme = ColorScheme(colorPair.first[0].colors[0], false)
                val ret = when(role){
                    ROLE_PRIMARY -> colorPair.first[0].colors[0]
                    ROLE_SURFACE -> {
                        if(colorPair.second.isNotEmpty() && colorPair.second[0].colors[0] != Color.TRANSPARENT){
                            colorPair.second[0].colors[0]
                        }else{
                            scheme.neutral1[9]
                        }
                    }
                    ROLE_LIGHT_SURFACE_OR_SECONDARY ->{
                        val surface = if(colorPair.second.isNotEmpty() && colorPair.second[0].colors[0] != Color.TRANSPARENT){
                            colorPair.second[0].colors[0]
                        }else{
                            scheme.neutral1[9]
                        }
                        val hsb = floatArrayOf(0f,0f,0f)
                        ColorUtils.colorToHSL(surface, hsb)
                        if(hsb[2] < 0.1f){
                            hsb[2] += 0.3f
                            ColorUtils.HSLToColor(hsb)
                        }else{
                            if(colorPair.first.size > 1){
                                colorPair.first[1].colors[0]
                            }else{
                                colorPair.first[0].colors[0]
                            }
                        }
                    }
                    ROLE_SECONDARY -> {
                        if(colorPair.first.size > 1){
                            colorPair.first[1].colors[0]
                        }else{
                            colorPair.first[0].colors[0]
                        }
                    }
                    ROLE_SECONDARY_1 -> {
                        if(colorPair.first.size > 2){
                            colorPair.first[2].colors[0]
                        }else{
                           Color.WHITE
                        }
                    }
                    ROLE_SECONDARY_2 -> {
                        if(colorPair.first.size > 3){
                            colorPair.first[3].colors[0]
                        }else{
                            colorPair.first[0].colors[0]
                        }
                    }
                    ROLE_SECONDARY_3 -> {
                        if(colorPair.first.size > 4){
                            colorPair.first[4].colors[0]
                        }else{
                            colorPair.first[0].colors[0]
                        }
                    }
                    ROLE_SECONDARY_4 -> {
                        if(colorPair.first.size > 5){
                            colorPair.first[5].colors[0]
                        }else{
                            colorPair.first[0].colors[0]
                        }
                    }
                    ROLE_SECONDARY_5 -> {
                        if(colorPair.first.size > 6){
                            colorPair.first[6].colors[0]
                        }else{
                            colorPair.first[0].colors[0]
                        }
                    }
                    ROLE_SECONDARY_6 -> {
                        if(colorPair.first.size > 7){
                            colorPair.first[7].colors[0]
                        }else{
                            colorPair.first[0].colors[0]
                        }
                    }
                    ROLE_SECONDARY_7 -> {
                        if(colorPair.first.size > 8){
                            colorPair.first[8].colors[0]
                        }else{
                            colorPair.first[0].colors[0]
                        }
                    }

                    ROLE_ON_PRIMARY -> {
//                        val fg = if(colorPair.first.size > 2){
//                            colorPair.first[2].colors[0]
//                        }else{
//                            scheme.accent1[10]
//                        }
                        val fg = Color.WHITE
                        val bg = colorPair.first[0].colors[0]
                        adjustColorContrast(bg, fg)
                    }
                    ROLE_ON_SECONDARY -> {
//                        val fg = if(colorPair.first.size > 1){
//                            ColorScheme(colorPair.first[1].colors[0], false).accent1[8]
//                        }else{
//                            scheme.accent3[8]
//                        }
                        val fg = Color.WHITE
                        val bg = if(colorPair.first.size > 1){
                            colorPair.first[1].colors[0]
                        }else{
                            colorPair.first[0].colors[0]
                        }
                        adjustColorContrast(bg, fg)
                    }
                    ROLE_ON_SURFACE ->{
                        val surfaceColor = if(colorPair.second.isNotEmpty() && colorPair.second[0].colors[0] != Color.TRANSPARENT){
                            colorPair.second[0].colors[0]
                        }else{
                            scheme.neutral1[9]
                        }
//                        var onSurface = if(colorPair.second.isNotEmpty() && colorPair.second[0].colors[0] != Color.TRANSPARENT){
//                            ColorScheme(colorPair.second[0].colors[0], false).accent1[9]
//                        }else{
//                            colorPair.first[0].colors[0]
//                        }
                        val onSurface = Color.WHITE
                        adjustColorContrast(surfaceColor, onSurface)

                    }
                    else -> def
                }
                return ret
            }
            return def
        }
        private fun adjustColorContrast(bgColor:Int, fgColor:Int):Int{
            var ret = fgColor
            if(bgColor.alpha == 255){
                var contrast = ColorUtils.calculateContrast(fgColor, bgColor)
                if(contrast < 2){
                    ret = if(ColorUtils.calculateContrast(Color.WHITE, bgColor) >
                        ColorUtils.calculateContrast(Color.BLACK, bgColor)) Color.WHITE else Color.BLACK
                }
            }
            return ret
        }
        
        
        val sCanvas = Canvas()
        val tempPathDrawableMap = HashMap<Path, IconShapeConfig>()
        val maskOutRect = arrayOf(Rect(), Rect())
        init {
            sCanvas.drawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        }
        fun setDefaultColorIconPack(context:Context, json:String){
            val mPreferences = context.getSharedPreferences(IconPackManager.PREF_NAME, Context.MODE_PRIVATE);
            Log.i(TAG, "setDefaultColorIconPack: " + json)
            mPreferences.edit(commit = true) {
                putString(IconPackManager.PREF_KEY_SELECTED, json)
            }
        }
        //获取Mono Bitmap
        private fun getDrawable(context:Context, info: LauncherActivityInfoCompat, iconDpi:Int):Drawable{
            if (Utilities.ATLEAST_T){
                try {
                    PreferenceUtil.getIconShape(context)
                    val iconId: Int = info.applicationInfo.icon
                    val resources = context.packageManager.getResourcesForApplication(info.applicationInfo.packageName)
                    val drawableCompat = AdaptiveIconDrawableCompat()
                    val xmlPullParser: XmlPullParser = resources.getXml(iconId)
                    val attrs = Xml.asAttributeSet(xmlPullParser)
                    drawableCompat.inflate(resources, xmlPullParser, attrs, null)
                    return drawableCompat
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (Utilities.ATLEAST_OREO) {
                try {
                    PreferenceUtil.getIconShape(context)
                    val iconId: Int = info.applicationInfo.icon
                    val resources = context.packageManager.getResourcesForApplication(info.applicationInfo.packageName)
                    val drawableCompat = AdaptiveIconDrawableCompat()
                    val xmlPullParser: XmlPullParser = resources.getXml(iconId)
                    val attrs = Xml.asAttributeSet(xmlPullParser)
                    drawableCompat.inflate(resources, xmlPullParser, attrs, null)
                    return drawableCompat
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return info.getIcon(context, iconDpi)
        }

        fun getPreviewInfoList(context: Context, cns :ArrayList<ComponentName>, callback:(ArrayList<ShortcutInfo>)->Unit):ArrayList<ShortcutInfo> {
            val infoList = ArrayList<ShortcutInfo>()
            val appsCompat = LauncherAppsCompat.getInstance(context)
            cns.forEach { cn ->
                val activityLists = appsCompat.getActivityList(cn.packageName, UserHandleCompat.myUserHandle())

                val launcherActivity = activityLists.filter { activity -> cn == activity.componentName }[0]
                val drawable = getDrawable(context, launcherActivity, context.resources.configuration.densityDpi)

                val info = ShortcutInfo(launcherActivity.label.toString(),
                    Utilities.createIconBitmap(drawable, 1f, context),
                    launcherActivity.componentName.packageName,
                    UserHandleCompat.myUserHandle().user,
                    Intent().setComponent(launcherActivity.componentName)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    launcherActivity.componentName)
                info.sourceDrawable = drawable
                info.realmono = drawable is AdaptiveIconDrawableCompat
                infoList.add(info)

            }
            //少于4个，补充
            val activityLists = appsCompat.getActivityList(null, UserHandleCompat.myUserHandle())
            activityLists.forEach { launcherActivity ->
                if(!cns.contains(launcherActivity.componentName)){
                    val drawable = getDrawable(context, launcherActivity, context.resources.configuration.densityDpi)
                    val info = ShortcutInfo(launcherActivity.label.toString(),
                        Utilities.createIconBitmap(drawable, 1f, context),
                        launcherActivity.componentName.packageName,
                        UserHandleCompat.myUserHandle().user,
                        Intent().setComponent(launcherActivity.componentName)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        launcherActivity.componentName)
                    info.sourceDrawable = drawable
                    infoList.add(info)
                    if(infoList.size == 4){
                        callback(infoList)
                    }
                }
            }
            return infoList
        }
        fun getShader(colorOption: ColorCustomInfo, rect:Rect):Shader?{
            when(colorOption.type){
                ColorCustomInfo.LINE_GRADIENT -> {
                    val shader = LinearGradient(
                        0f,
                        0f,
                        0f,
                        rect.height().toFloat(),
                        colorOption.colors,
                        colorOption.positions,
                        Shader.TileMode.CLAMP
                    )
                    val matrix = Matrix()
                    shader.getLocalMatrix(matrix)
                    matrix.postRotate(colorOption.angle.toFloat(), rect.width()/2f, rect.height()/2f)
                    shader.setLocalMatrix(matrix)
                    return shader
                }
                ColorCustomInfo.RADIAL_GRADIENT -> {
                    return RadialGradient(
                        rect.width()  * colorOption.xOffset, rect.height() * colorOption.yOffset,
                        Math.max(0.1f, colorOption.radial * rect.width()/2), colorOption.colors, null, Shader.TileMode.CLAMP
                    )
                }
            }
            return null
        }

        fun createWallpaperAdapterBitmap(context:Context, source:Bitmap, monoBitmap: MutableList<Bitmap>,
                                         internalMono:Boolean, iconShapeHelper: IconShapeHelper,
                                         colorSource:Int, wallpaperColor:Int,
                                         fgColor: MutableList<ColorCustomInfo>, bgColor:ColorCustomInfo,
                                         path: Path,
                                         iconBack:IconBackOption?,
                                         maskOption:IconMaskOption?,
                                         decoration:IconDecOption?,
                                         iconStrokeOption: IconStrokeOption?,
                                         iconShadowOption: IconShadowOption,
                                         iconBackDrawable:Drawable? = null,
                                         iconMaskDrawable:Drawable? = null) : Bitmap{
            var sourceBitmap = source
            val iconNormalizer = IconNormalizer.getInstance(context)
            //装饰
            var decDisplay = 3
            val dec = decoration?.let { dec ->
                val index = dec.index
                val data = dec.iconMarkBean.data
                val previewUrl = dec.iconMarkBean.previewUrl
                val bitmap = if (index in data.indices) {
                    getBitmapByPreview(context, data[index], previewUrl)
                } else {
                    getBitmapByPreview(context, data.random(), previewUrl)
                }
                if(previewUrl.contains("themed_icon")
                    && !previewUrl.contains("xmodelosdec")){
                    decDisplay = -1
                }else{
                    decDisplay = dec.iconMarkBean.display_mode
                }
                bitmap?.toDrawable(context.resources)
            }
            //遮罩
            var iconMask: Drawable ?= null
            maskOption?.let {
                runCatching {
                    iconMask = context.resources.getDrawable(it.drawableId)
                }
            }
            //shape形状
            var iconShapeConfig = tempPathDrawableMap[path]
            if(iconShapeConfig == null){
                iconShapeConfig = IconShapeConfig()
                tempPathDrawableMap[path] = iconShapeConfig
            }
            if(iconShapeConfig.adaptiveScale < 0){
                synchronized(sCanvas){
                    val tempBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                    sCanvas.setBitmap(tempBitmap)
                    val paint = Paint(
                        Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or
                                Paint.FILTER_BITMAP_FLAG
                    )
                    sCanvas.drawPath(path, paint)
                    iconShapeConfig.adaptiveScale = iconNormalizer.getScale(tempBitmap.toDrawable(context.resources),
                        null, null, null )
                }
            }
            //没有前景图
            if(monoBitmap.isEmpty()) {
                //有底座
                if (iconBack != null && iconBack.iconMarkBean.data.isNotEmpty()) {
                    val back = getBitmapByPreview(
                        context,
                        if (iconBack.index < 0 || iconBack.index >= iconBack.iconMarkBean.data.size)
                            iconBack.iconMarkBean.data.random() else iconBack.iconMarkBean.data[iconBack.index],
                        iconBack.iconMarkBean.previewUrl
                    )
                        ?: return sourceBitmap
                    var mask = Request.Companion.getBitmapByPreview(
                        context,
                        iconBack.iconMarkBean.mask,
                        iconBack.iconMarkBean.previewUrl
                    )
                    var maskScale = iconBack.maskScale
                    var maskOffsetX = 0f
                    var maskOffsetY = 0f
                    if (iconMask != null && maskOption != null) {
                        mask = (iconMask as BitmapDrawable).bitmap
                        maskOffsetX = maskOption.offsetX
                        maskOffsetY = maskOption.offsetY
                        maskScale = maskOption.scale
                    }
                    val newMask =
                        Bitmap.createBitmap(mask!!.width, mask.height, Bitmap.Config.ARGB_8888)
                    synchronized(sCanvas) {
                        sCanvas.setBitmap(newMask)
                        sCanvas.save()
                        //缩小
                        if (maskScale < 1) {
                            sCanvas.drawColor(Color.BLACK)
                            val paint1 = Paint()
                            paint1.color = Color.BLACK
                            paint1.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))
                            val rect = Rect(0, 0, newMask.width, newMask.height)
                            val offset = ((1 - maskScale) * newMask.width).toInt() + 1
                            rect.inset(offset, offset)
                            sCanvas.drawRect(rect, paint1)
                        }
                        val matrix = Matrix()
                        val maxOffset = mask!!.width / 2
                        matrix.setScale(
                            maskScale,
                            maskScale,
                            (mask!!.width / 2).toFloat(),
                            (mask!!.height / 2).toFloat()
                        )
                        matrix.postTranslate(maskOffsetX * maxOffset, maskOffsetY * maxOffset)
                        sCanvas.drawBitmap(mask!!, matrix, null)
                        sCanvas.restore()
                        mask = newMask
                    }
                    val isMask = isS8RuleAndScaleIcon(sourceBitmap)
                    if (Utilities.ATLEAST_LOLLIPOP_MR1) {
                        //延伸处理
                        val temp = IconShapeHelper.extendBitmap(sourceBitmap)
                        temp?.let {
                            isMask[1] = 1f
                            isMask[0] = 1f
                            sourceBitmap = temp
                        }

                    }
                    val ret = mergeBitmap(
                        context, back, sourceBitmap, null,
                        if (isMask[0] != 1f && isMask[2] != 1f) null else mask, 1f
                    )
                    if (dec != null && ret != null) {
                        synchronized(sCanvas) {
                            val canvas = sCanvas
                            canvas.setBitmap(ret)
                            val matrix = Matrix()
                            matrix.setScale(decoration.decScale, decoration.decScale)
                            val maxOffset = ret.width / 2
                            dec.bounds = adjustDecDisplay(
                                Rect(
                                    0,
                                    0,
                                    ret.width,
                                    ret.height
                                ), decDisplay
                            )
                            canvas.save()
                            canvas.scale(decoration.decScale, decoration.decScale)
                            canvas.translate(
                                maxOffset * decoration.offsetX,
                                maxOffset * decoration.offsetY
                            )
                            dec.draw(canvas)
                            canvas.restore()
                        }
                    }
                    return ret!!
                }
                //没有底座
                val bounds = Rect(0, 0, sourceBitmap.width, sourceBitmap.height)
                val isMask = isS8RuleAndScaleIcon(sourceBitmap)
                if (Utilities.ATLEAST_LOLLIPOP_MR1) {
                    //延伸处理
                    val temp = IconShapeHelper.extendBitmap(sourceBitmap)
                    temp?.let {
                        isMask[1] = 1f
                        sourceBitmap = temp
                    }

                }
                synchronized(sCanvas) {
                    val canvas = sCanvas
                    val drawable = sourceBitmap.toDrawable(context.resources)
                    drawable.bounds = bounds
                    //生成Shape 底座
                    if (iconBackDrawable != null && iconMaskDrawable != null) {
                        val ret = mergeBitmap(
                            context, (iconBackDrawable as BitmapDrawable).bitmap,
                            sourceBitmap,
                            null, (iconMaskDrawable as BitmapDrawable).bitmap, 1f
                        )
                        if (ret != null) {
                            return ret;
                        }
                    }
                    if (iconShapeConfig.iconBelow == null) {
                        val temp = Bitmap.createBitmap(
                            bounds.width(),
                            bounds.height(),
                            Bitmap.Config.ARGB_8888
                        )
                        sCanvas.setBitmap(temp)
                        val matrix = Matrix()
                        matrix.setScale(bounds.width() / 100f, bounds.height() / 100f)
                        matrix.postScale(
                            iconShapeConfig.adaptiveScale,
                            iconShapeConfig.adaptiveScale,
                            (bounds.width() / 2).toFloat(),
                            (bounds.height() / 2).toFloat()
                        )
                        val newPath = Path()
                        path.transform(matrix, newPath)
                        val paint = Paint(
                            Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or
                                    Paint.FILTER_BITMAP_FLAG
                        )
                        if (Color.alpha(bgColor.colors[0]) == 0) {
                            bgColor.colors[0] = 0xFFFFFFFF.toInt()
                        }
                        val shader = getShader(bgColor, bounds);
                        if (shader == null) {
                            paint.color = bgColor.colors[0]
                        } else {
                            paint.shader = shader
                        }
                        sCanvas.drawPath(newPath, paint)
                        iconShapeConfig.iconBelow = BitmapDrawable(temp)
                        //                    sWallpaperAdapterBackgroundIconBelow = new BitmapDrawable(temp);
                        val scale = Bitmap.createBitmap(
                            bounds.width(),
                            bounds.height(),
                            Bitmap.Config.ARGB_8888
                        )
                        sCanvas.setBitmap(scale)
                        matrix.postScale(
                            0.95f,
                            0.95f,
                            (bounds.width() / 2).toFloat(),
                            (bounds.height() / 2).toFloat()
                        )
                        path.transform(matrix, newPath)
                        sCanvas.drawPath(newPath, paint)
                        paint.shader = null
                        //                    sWallpaperAdapterBackgroundIconBelowScale = new BitmapDrawable(scale);
                        iconShapeConfig.iconBelowScale = BitmapDrawable(scale)
                    }
                    return if (isMask[0] < 0.83f) {
                        val scale = 0.75f
                        val retBitmap = Bitmap.createBitmap(
                            bounds.width(),
                            bounds.height(),
                            Bitmap.Config.ARGB_8888
                        )
                        canvas.setBitmap(retBitmap)
                        canvas.save()
                        canvas.scale(
                            scale,
                            scale,
                            (bounds.width() / 2).toFloat(),
                            (bounds.height() / 2).toFloat()
                        )
                        drawable.draw(canvas)
                        canvas.restore()
                        //如果第三方的没有前景图，不用彩虹描边
                        if (iconStrokeOption?.strokeShape == true && iconStrokeOption.strokeColor != 0xFF123456.toInt()) {
                            iconShapeConfig.iconBelowScale?.let {
                                it.bounds = bounds
                                it.paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OVER))
                                it.draw(sCanvas)
                            }
                            iconShapeConfig.iconBelow?.let {
                                it.bounds = bounds
                                it.paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OVER))
                                it.paint.alpha = 255
                                with(it.paint) {
                                    if (iconStrokeOption.autoFitColorOption && fgColor.size > 0) {
                                        if (fgColor.size > 1) {
                                            colorFilter = PorterDuffColorFilter(
                                                fgColor[1].colors[0],
                                                PorterDuff.Mode.SRC_IN
                                            )
                                        } else {
                                            colorFilter = PorterDuffColorFilter(
                                                fgColor[0].colors[0],
                                                PorterDuff.Mode.SRC_IN
                                            )
                                        }
                                    } else if (iconStrokeOption.autoFitColor) {
                                        colorFilter = PorterDuffColorFilter(
                                            bgColor.colors[0],
                                            PorterDuff.Mode.SRC_IN
                                        )
                                    } else if (iconStrokeOption.strokeColor == 0xFF123456.toInt()) {

                                    } else {
                                        colorFilter = PorterDuffColorFilter(
                                            iconStrokeOption.strokeColor,
                                            PorterDuff.Mode.SRC_IN
                                        )
                                    }
                                }
                                it.draw(sCanvas)
                                it.paint.colorFilter = null
                                it.paint.shader = null
                                it.paint.xfermode = null
                            }
                        } else {
                            iconShapeConfig.iconBelow?.let { bitmapBelow ->
                                bitmapBelow.bounds = bounds
                                bitmapBelow.paint.setXfermode(
                                    PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
                                )
                                bitmapBelow.draw(canvas)
                            }
                        }


                        if (dec != null) {
                            val matrix = Matrix()
                            matrix.setScale(decoration.decScale, decoration.decScale)
                            val maxOffset = bounds.width() / 2
                            dec.bounds = adjustDecDisplay(bounds, decDisplay)
                            canvas.save()
                            canvas.scale(decoration.decScale, decoration.decScale)
                            canvas.translate(
                                maxOffset * decoration.offsetX,
                                maxOffset * decoration.offsetY
                            )
                            dec.draw(canvas)
                            canvas.restore()
                        }
                        retBitmap
                    } else {
                        val scale = isMask[1]
                        val retBitmap = createBitmap(bounds.width(), bounds.height() )
                        canvas.setBitmap(retBitmap)
                        canvas.save()
                        if (scale != 1.0f) {
                            canvas.scale(
                                scale * iconShapeConfig.adaptiveScale,
                                scale * iconShapeConfig.adaptiveScale,
                                (bounds.width() / 2).toFloat(),
                                (bounds.height() / 2).toFloat()
                            )
                        }
                        drawable.draw(canvas)
                        canvas.restore()
                        if (iconStrokeOption?.strokeShape == true && iconStrokeOption.strokeColor != 0xFF123456.toInt()) {
                            iconStrokeOption.let { stroke ->
                                iconShapeConfig.iconBelowScale?.let {
                                    it.bounds = bounds
                                    it.paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                                    it.draw(sCanvas)
                                    it.paint.colorFilter = null
                                }
                                iconShapeConfig.iconBelow?.let {
                                    it.bounds = bounds
                                    it.paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
                                    it.paint.alpha = 255
                                    if (iconStrokeOption.autoFitColorOption && fgColor.isNotEmpty()) {
                                        var filterColor = fgColor[0].colors[0]
                                        if (fgColor.size > 1) {
                                            filterColor = fgColor[1].colors[0]
                                        }
                                        filterColor = Color.argb(
                                            188,
                                            Color.red(filterColor),
                                            Color.green(filterColor),
                                            Color.blue(filterColor)
                                        )
                                        it.paint.colorFilter = PorterDuffColorFilter(
                                            filterColor,
                                            PorterDuff.Mode.SRC_IN
                                        )
                                    } else if (iconStrokeOption.autoFitColor) {
                                        it.paint.colorFilter = PorterDuffColorFilter(
                                            bgColor.colors[0],
                                            PorterDuff.Mode.SRC_IN
                                        )
                                    } else if (iconStrokeOption.strokeColor == 0xFF123456.toInt()) {
//                                            it.paint.shader =
//                                                ComposeShader(SweepGradient(bounds.width() /2f, bounds.height() /2f,
//                                                intArrayOf(0xFFFF0000.toInt(), 0xFFFF9900.toInt(), 0xFFCCFF00.toInt(), 0xFF5EFF00.toInt(),
//                                                    0xFF00FF3C.toInt(), 0xFF00FFD4.toInt(), 0xFF0090FF.toInt(),0xFF0055FF.toInt(), 0xFFA100FF.toInt(),
//                                                    0xFFFF00C3.toInt(), 0xFFFF0000.toInt()),
//                                                floatArrayOf(0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f)),
//                                                    BitmapShader(iconShapeConfig.iconBelow!!.bitmap,
//                                                        Shader.TileMode.CLAMP, Shader.TileMode.CLAMP), PorterDuff.Mode.DST_IN)
//                                            sCanvas.drawPaint(it.paint)
//                                            it.paint.shader = null
//                                            it.paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                                        //第三方的没有前景图，不用彩虹描边
                                    } else {
                                        it.paint.colorFilter = PorterDuffColorFilter(
                                            iconStrokeOption.strokeColor,
                                            PorterDuff.Mode.SRC_IN
                                        )
                                    }
                                    it.draw(sCanvas)
                                    it.paint.colorFilter = null
                                    it.paint.shader = null
                                    it.paint.xfermode = null
                                }
                            }
                        } else {
                            iconShapeConfig.iconBelow?.let {
                                it.bounds = bounds
                                it.paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_IN))
                                it.draw(sCanvas)
                                it.paint.colorFilter = null
                            }
                        }


                        if (dec != null) {
                            val matrix = Matrix()
                            matrix.setScale(decoration.decScale, decoration.decScale)
                            val maxOffset = bounds.width() / 2
                            dec.bounds = adjustDecDisplay(bounds, decDisplay)
                            canvas.save()
                            canvas.scale(decoration.decScale, decoration.decScale)
                            canvas.translate(
                                maxOffset * decoration.offsetX,
                                maxOffset * decoration.offsetY
                            )
                            dec.draw(canvas)
                            canvas.restore()
                        }
                        retBitmap
                    }
                }

            }

            val bounds = Rect(0, 0, sourceBitmap.width, sourceBitmap.height)
            val iconBackBitmap = createBitmap(sourceBitmap.width, sourceBitmap.height)
            var backOutShadow :Bitmap?=null
            var backInShadow :Bitmap?=null
            var logoOutBitmap :Bitmap?=null
            var logoInBitmap :Bitmap?=null
            var logoLongBitmap :Bitmap?=null
            iconBackBitmap.let{
                sCanvas.setBitmap(it)
                var matrix = Matrix()
                matrix.setScale(bounds.width() / 100f, bounds.height() / 100f)
                if(iconShapeConfig.adaptiveScale != 1f){
                    matrix.postScale(
                        iconShapeConfig.adaptiveScale,
                        iconShapeConfig.adaptiveScale, (bounds.width() / 2).toFloat(), (bounds.height() / 2).toFloat()
                    )
                }
                val newPath = Path()
                path.transform(matrix, newPath)
                val paint = Paint(
                    Paint.ANTI_ALIAS_FLAG
                )
                paint.color = Color.WHITE
                sCanvas.drawPath(newPath, paint)
            }

            //背景外阴影
            if (iconShadowOption.bgOutShadow.enable) {
                backOutShadow = createBitmap(sourceBitmap.width, sourceBitmap.height)
                sCanvas.setBitmap(backOutShadow)
                val paint = Paint(
                    Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or
                            Paint.FILTER_BITMAP_FLAG
                )
                paint.setMaskFilter(BlurMaskFilter(iconShadowOption.bgOutShadow.radius.toFloat(), BlurMaskFilter.Blur.NORMAL))
                paint.color = iconShadowOption.bgOutShadow.color
                sCanvas.drawBitmap(
                    iconBackBitmap.extractAlpha(),
                    iconShadowOption.bgOutShadow.offsetX.toFloat(),
                    iconShadowOption.bgOutShadow.offsetY.toFloat(),
                    paint
                )
                paint.setMaskFilter(null)
            }
            if (iconShadowOption.bgInShadow.enable) {
                val paint = Paint(
                    Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or
                            Paint.FILTER_BITMAP_FLAG
                )
                backInShadow = createBitmap(iconBackBitmap.width, iconBackBitmap.height)
                sCanvas.setBitmap(backInShadow)
                sCanvas.drawColor(Color.BLACK)
                paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OUT))
                sCanvas.drawBitmap(iconBackBitmap, 0f, 0f, paint)
                paint.setXfermode(null)
                backInShadow = backInShadow.extractAlpha()
                val temp = createBitmap(iconBackBitmap.width, iconBackBitmap.height)
                sCanvas.setBitmap(temp)
                paint.setMaskFilter(BlurMaskFilter(iconShadowOption.bgInShadow.radius.toFloat(), BlurMaskFilter.Blur.SOLID))
                paint.color = iconShadowOption.bgInShadow.color
                //偏移值
                sCanvas.drawBitmap(backInShadow, iconShadowOption.bgInShadow.offsetX.toFloat(),
                    iconShadowOption.bgInShadow.offsetY.toFloat(), paint)
                paint.setMaskFilter(null)
                val t = createBitmap(iconBackBitmap.width, iconBackBitmap.height)
                sCanvas.setBitmap(t)
                sCanvas.drawBitmap(iconBackBitmap, 0f, 0f, null)
                paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
                sCanvas.drawBitmap(temp, 0f, 0f, paint)
                paint.setXfermode(null)
                backInShadow = t
            }
            //整体的mono，做阴影处理
            val allMono = createBitmap(bounds.width(), bounds.height())
            val logoBitmap = createBitmap(bounds.width(), bounds.height())
            sCanvas.setBitmap(allMono)
            val tempPaint = Paint(
                Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or
                        Paint.FILTER_BITMAP_FLAG
            )
            monoBitmap.forEach {
                sCanvas.drawBitmap(it, 0f, 0f, tempPaint)
            }
            sCanvas.setBitmap(logoBitmap)
            sCanvas.drawColor(Color.BLACK)
            tempPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            sCanvas.drawBitmap(allMono, 0f, 0f, tempPaint)
            sCanvas.drawPaint(tempPaint)
            tempPaint.xfermode = null
            logoOutBitmap = allMono.extractAlpha().takeIf { iconShadowOption.logoOutShadow.enable }
            if(iconShadowOption.logoLongShadow.enable){
                logoLongBitmap = getLongShadow(allMono, iconShadowOption.logoLongShadow.offsetX,
                    iconShadowOption.logoLongShadow.radius, iconShadowOption.logoLongShadow.offsetY)
            }
            if(iconShadowOption.logoInShadow.enable){
                logoInBitmap = createBitmap(logoBitmap.width, logoBitmap.height)
                sCanvas.setBitmap(logoInBitmap)
                sCanvas.drawColor(Color.BLACK)
                tempPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                monoBitmap.forEach {
                    sCanvas.drawBitmap(it, 0f, 0f, tempPaint)
                }
                logoInBitmap = logoInBitmap.extractAlpha()
                val temp = createBitmap(sourceBitmap.width, sourceBitmap.height)
                sCanvas.setBitmap(temp)
                tempPaint.xfermode = null
                tempPaint.setMaskFilter(BlurMaskFilter(iconShadowOption.logoInShadow.radius.toFloat(),
                    BlurMaskFilter.Blur.NORMAL))
                tempPaint.color = iconShadowOption.logoInShadow.color
                //偏移值
                sCanvas.drawBitmap(logoInBitmap, iconShadowOption.logoInShadow.offsetX.toFloat(),
                    iconShadowOption.logoInShadow.offsetY.toFloat(), tempPaint)
                tempPaint.maskFilter = null
                val t = createBitmap(sourceBitmap.width, sourceBitmap.height)
                sCanvas.setBitmap(t)
                sCanvas.drawBitmap(logoBitmap, 0f, 0f, null)
                tempPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                sCanvas.drawBitmap(temp, 0f, 0f, tempPaint)
                tempPaint.xfermode = null
                logoInBitmap = t
            }


            var ret = createBitmap(bounds.width(), bounds.height())
            synchronized(sCanvas) {
                sCanvas.setBitmap(ret)
                //背景外阴影
                if (backOutShadow != null) {
                    sCanvas.drawBitmap(backOutShadow, 0f, 0f, null)
                }
                var matrix = Matrix()
                matrix.setScale(bounds.width() / 100f, bounds.height() / 100f)
                matrix.postScale(
                    iconShapeConfig.adaptiveScale,
                    iconShapeConfig.adaptiveScale, (bounds.width() / 2).toFloat(), (bounds.height() / 2).toFloat()
                )
                val newPath = Path()
                path.transform(matrix, newPath)
                val paint =
                    Paint(
                        Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or
                                Paint.FILTER_BITMAP_FLAG
                    )
                //描边处理
                if(iconStrokeOption?.strokeShape == true){
                    if(iconStrokeOption.strokeColor == 0xFF123456.toInt()){
                        paint.shader = ComposeShader(SweepGradient(bounds.width() /2f, bounds.height() /2f,
                        intArrayOf(0xFFFF0000.toInt(), 0xFFFF9900.toInt(), 0xFFCCFF00.toInt(), 0xFF5EFF00.toInt(),
                            0xFF00FF3C.toInt(), 0xFF00FFD4.toInt(), 0xFF0090FF.toInt(),0xFF0055FF.toInt(), 0xFFA100FF.toInt(),
                            0xFFFF00C3.toInt(), 0xFFFF0000.toInt()),
                        floatArrayOf(0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f)),
                        BitmapShader(iconBackBitmap,
                            Shader.TileMode.CLAMP, Shader.TileMode.CLAMP), PorterDuff.Mode.DST_IN)
                        sCanvas.drawPaint(paint)
                        paint.shader = null
                        paint.alpha = 255
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                    }else{
                        with(paint){
                            alpha = 255
                            color = if(iconStrokeOption.autoFitColorOption && fgColor.size > 0){
                                if(fgColor.size > 1) fgColor[1].colors[0] else fgColor[0].colors[0]
                            }else if(iconStrokeOption.autoFitColor){
                                bgColor.colors[0]
                            }else {
                                iconStrokeOption.strokeColor
                            }
                        }
                        paint.alpha = 128
                    }
                    paint.shader = null
                    sCanvas.drawPath(newPath, paint)
                    paint.xfermode = null
                    paint.alpha = 255
                    val shader = getShader(bgColor, bounds)
                    if(shader == null){
                        paint.color = bgColor.colors[0]
                    }else{
                        paint.shader = shader
                    }
                    matrix.reset()
                    matrix.postScale(
                        0.95f,
                        0.95f,
                        (bounds.width() / 2).toFloat(),
                        (bounds.height() / 2).toFloat()
                    )
                    newPath.transform(matrix)
                    sCanvas.drawPath(newPath, paint)
                }else{
                    val shader = getShader(bgColor, bounds);
                    if(shader == null){
                        paint.color = bgColor.colors[0]
                    }else{
                        paint.shader = shader
                    }
                    sCanvas.drawPath(newPath, paint)
                }
                //Logo 外阴影
                logoOutBitmap?.let{
                    paint.shader = null
                    paint.maskFilter = BlurMaskFilter(iconShadowOption.logoOutShadow.radius.toFloat(),
                        BlurMaskFilter.Blur.NORMAL)
                    paint.color = iconShadowOption.logoOutShadow.color
                    val tempDpi = sCanvas.density
                    sCanvas.density = logoOutBitmap.density
                    sCanvas.drawBitmap(logoOutBitmap, iconShadowOption.logoOutShadow.offsetX.toFloat(),
                        iconShadowOption.logoOutShadow.offsetY.toFloat(), paint)
                    paint.alpha = 255
                    sCanvas.density = tempDpi
                }
                //背景内阴影
                backInShadow?.let{ sCanvas.drawBitmap(it, 0f, 0f, paint)}
                //Logo 长阴影
                logoLongBitmap?.let{
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                    sCanvas.drawBitmap(logoLongBitmap, 0f, 0f, paint);
                    paint.xfermode = null
                }
                paint.shader = null
                paint.alpha = 255
                for(index in 0 until monoBitmap.size){
                    val colorIndex = index % fgColor.size
                    if(colorIndex < fgColor.size){
                        paint.shader = null
                        val fgShader = getShader(fgColor[colorIndex], bounds)
                        Log.i(TAG, "createWallpaperAdapterBitmap: $index   ${String.format("#%08x", fgColor[colorIndex].colors[0])}")
                        if(internalMono){
                            //内部的mono，且是Source颜色，不上色，直接绘制
                            sCanvas.drawBitmap(monoBitmap[index], null, bounds, paint)
                        }else  if(fgShader == null){
                            if(fgColor[colorIndex].type == ColorCustomInfo.COLOR_TYPE_NORMAL_GRAY){
                                //灰阶上色
                                paint.color = fgColor[colorIndex].colors[0]
                                val red = fgColor[colorIndex].colors[0].red/255f
                                val green = fgColor[colorIndex].colors[0].green/255f
                                val blue = fgColor[colorIndex].colors[0].blue/255f
                                val fiter = floatArrayOf(red, 0f, 0f, 0f, 0f,
                                    0f, green, 0f, 0f, 0f,
                                    0f, 0f, blue, 0f, 0f,
                                    0f, 0f, 0f, 1f, 0f)
                                val colorMatrix = ColorMatrix(fiter)
                                paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                            }else{
                                paint.colorFilter = PorterDuffColorFilter(fgColor[colorIndex].colors[0], PorterDuff.Mode.SRC_IN)
                            }
                            sCanvas.drawBitmap(monoBitmap[index], null, bounds, paint)
                            paint.colorFilter = null
                        }else{
                            paint.shader = ComposeShader(fgShader,
                                BitmapShader(monoBitmap[index],
                                    Shader.TileMode.CLAMP, Shader.TileMode.CLAMP),
                                PorterDuff.Mode.DST_IN)
                            sCanvas.drawRect(bounds, paint)
                        }
                    }
                }
                paint.shader = null
                if(bgColor.colors[0].alpha != 0){
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                    sCanvas.drawBitmap(iconBackBitmap, null, bounds, paint)
                    paint.xfermode = null
                }
                //Logo内阴影
                logoInBitmap?.let{
                    sCanvas.drawBitmap(logoInBitmap, 0F, 0F, paint)
                }
                //底座盖版处理
                if (iconBack != null && iconBack.iconMarkBean.data.size > 0) {
//                    Drawable backDrawable = context.getResources().getDrawable(
//                            backOption.backIds[new Random().nextInt(backOption.backIds.length)]);
//                    Bitmap back = ((BitmapDrawable)backDrawable).getBitmap();
                    val back = getBitmapByPreview(
                        context,
                        if (iconBack.index < 0 || iconBack.index >= iconBack.iconMarkBean.data.size)
                            iconBack.iconMarkBean.data.random() else iconBack.iconMarkBean.data[iconBack.index],
                        iconBack.iconMarkBean.previewUrl
                    ) ?: return sourceBitmap
                    var mask =
                        getBitmapByPreview(
                            context,
                            iconBack.iconMarkBean.mask,
                            iconBack.iconMarkBean.previewUrl
                        )
                    var maskScale = iconBack.maskScale
                    var maskOffsetX = 0f
                    var maskOffsetY = 0f
                    //有自选遮罩，加上遮罩处理
                    if (iconMask != null) {
                        mask = (iconMask as BitmapDrawable).bitmap
                        maskScale = maskOption!!.scale
                        maskOffsetX = maskOption.offsetX
                        maskOffsetY = maskOption.offsetY
                    }
                    val newMask = createBitmap(mask!!.width, mask!!.height)
                    synchronized(sCanvas) {
                        sCanvas.setBitmap(newMask)
                        sCanvas.save()
                        matrix = Matrix()
                        val maxOffset = mask!!.width / 2
                        //缩小
                        if (maskScale < 1) {
                            sCanvas.drawColor(Color.BLACK)
                            val paint1 = Paint()
                            paint1.color = Color.BLACK
                            paint1.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))
                            val rect = Rect(0, 0, newMask.width, newMask.height)
                            val offset = ((1 - maskScale) * newMask.width).toInt() + 1
                            rect.inset(offset, offset)
                            sCanvas.drawRect(rect, paint1)
                        }
                        matrix.setScale(
                            maskScale,
                            maskScale,
                            (mask!!.width / 2).toFloat(),
                            (mask!!.height / 2).toFloat()
                        )
                        matrix.postTranslate(maskOffsetX * maxOffset, maskOffsetY * maxOffset)
                        sCanvas.drawBitmap(mask!!, matrix, null)
                        sCanvas.restore()
                        mask = newMask
                    }
                    ret = mergeBitmap(context, back, ret, null, mask, 1f)!!
                    if (dec != null) {
                        synchronized(sCanvas) {
                            val canvas = sCanvas
                            canvas.setBitmap(ret)
                            matrix = Matrix()
                            matrix.setScale(decoration.decScale, decoration.decScale)
                            val maxOffset = ret.width / 2
                            dec.bounds = adjustDecDisplay(Rect(
                                0,
                                0,
                                ret.width,
                                ret.height
                            ), decDisplay)
                            canvas.withScale(decoration.decScale, decoration.decScale) {
                                translate(
                                    maxOffset * decoration.offsetX,
                                    maxOffset * decoration.offsetY
                                )
                                dec.draw(this)
                            }
                        }
                    }
                    return ret
                }
                if (dec != null) {
                    val maxOffset = bounds.width() / 2
                    dec.bounds = adjustDecDisplay(bounds, decDisplay)
                    sCanvas.save()
                    sCanvas.scale(decoration.decScale, decoration.decScale)
                    sCanvas.translate(maxOffset * decoration.offsetX, maxOffset * decoration.offsetY)
                    dec.draw(sCanvas)
                    sCanvas.restore()
                }
                return ret
            }

        }

        private fun adjustDecDisplay(rect:Rect, displayMode: Int):Rect{
            val newRect = Rect(rect)
            val width = rect.width()
            if(width > 0){
                val size = (width * 0.42f).toInt()
                if(displayMode == IconMarkBean.DISPLAY_LEFT_TOP){
                    newRect.set(0, 0, size, size)
                }else if(displayMode == IconMarkBean.DISPLAY_RIGHT_TOP){
                    newRect.set(width - size, 0, width, size)
                }else if(displayMode == IconMarkBean.DISPLAY_LEFT_BOTTOM){
                    newRect.set(0, width - size, size, width)
                }else if(displayMode == IconMarkBean.DISPLAY_RIGHT_BOTTOM){
                    newRect.set(width - size, width - size, width, width)
                }
            }
            return newRect
        }

        /**
         * S8 主题 是否是规则图形、是否圆形图标 和获取图标的缩放比例
         * @param bitmap
         * @return
         */
        fun isS8RuleAndScaleIcon(bitmap: Bitmap?): FloatArray {
            val ret = floatArrayOf(0f, 0f, 0f)
            var colCount = 0
            var rowCount = 0
            var topHasNoColor = 0.0
            var SlashHasNoColor = 0.0
            var leftBottomNoColor = 0.0
            if (bitmap == null || bitmap.isRecycled) {
                return ret
            }
            val height = bitmap.height
            val width = bitmap.width
            var topBreak = false
            var slashBreak = false
            var leftBottomBreak = false
            for (i in 0 until width) {
                val color = bitmap.getPixel(i, height / 2)
                if (Color.TRANSPARENT != color && Color.alpha(color) >= 200) {
                    rowCount++
                }
                var colColor = bitmap.getPixel(width / 2, i)
                if (Color.TRANSPARENT != colColor && Color.alpha(colColor) >= 200) {
                    colCount++
                }
                if (i < width / 2) {
                    if (!topBreak && Color.TRANSPARENT == Color.alpha(colColor)) {
                        topHasNoColor++
                    } else {
                        topBreak = true
                    }
                }
                colColor = bitmap.getPixel(i, i)
                if (i < width / 2) {
                    //左上角斜线
                    if (!slashBreak && Color.TRANSPARENT == Color.alpha(colColor)) {
                        SlashHasNoColor = i / Math.cos(Math.PI / 4)
                    } else {
                        slashBreak = true
                    }
                    //左下角斜线
                    colColor = bitmap.getPixel(i, height - i - 1)
                    if (!leftBottomBreak && Color.TRANSPARENT == Color.alpha(colColor)) {
                        leftBottomNoColor = i / Math.cos(Math.PI / 4)
                    } else {
                        leftBottomBreak = true
                    }
                }
            }
            val SlashLong = width / Math.cos(Math.PI / 4)
            //圆形图标
            if (abs((width / 2 - topHasNoColor) - (SlashLong / 2 - SlashHasNoColor)) < 5
                && abs((width / 2 - topHasNoColor) - (SlashLong / 2 - leftBottomNoColor)) < 5) {
                ret[2] = 1f
            } else {
                ret[2] = -1f
            }
            val scaleX = rowCount * 1.0f / width
            val scaleY = colCount * 1.0f / height
            //部分不规则图标，识别优化
            if (abs(scaleX - scaleY) < 0.01f
                && (SlashLong / 2 - SlashHasNoColor - (width / 2 - topHasNoColor).toInt()).toInt() > 4
                && Math.min(scaleX, scaleY) > 0.66f) {
                ret[0] = 1f
            } else {
                if (ret[2] > 0 && Math.abs(scaleX - scaleY) < 0.01f && Math.min(scaleX,scaleY) > 0.45f) {
                    ret[0] = 1f
                } else {
                    ret[0] = -1f
                }
            }
            val scale = Math.min(colCount, rowCount) * 1.0f / height
            ret[1] = 1.0f / scale
            //圆形图标，在缩放的基础上加上0.1f
            if (ret[2] == 1f) {
                ret[1] = ret[1] + 0.2f
            }
            return ret
        }

        /**
         * 获取图标的长阴影
         * @param logo
         * @param angle
         * @param radius
         * @param opacity
         * @return
         */
        private fun getLongShadow(logo: Bitmap, angle: Int, radius: Int, opacity: Int): Bitmap {
            synchronized(sCanvas) {
                val ret = Bitmap.createBitmap(
                    (logo.width * 1.5).toInt(),
                    logo.height,
                    Config.ALPHA_8
                )
                sCanvas.setBitmap(ret)
                sCanvas.setDensity(logo.density)
                sCanvas.save()
                val paint =
                    Paint(
                        Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or
                                Paint.FILTER_BITMAP_FLAG
                    )
                sCanvas.rotate(
                    -angle.toFloat(),
                    (logo.width / 2).toFloat(),
                    (logo.height / 2).toFloat()
                )
                sCanvas.drawBitmap(logo, 0f, 0f, paint)
                sCanvas.restore()
                val bytes = ByteArray(ret.width * ret.height)
                val buffer = ByteBuffer.wrap(bytes)
                buffer.rewind()
                ret.copyPixelsToBuffer(buffer)
                val bottomY = ret.height
                val rightX = ret.width
                var index = 0
                val lineBuffer = ByteArray(rightX)
                for (i in lineBuffer.indices) {
                    lineBuffer[i] = 0xFF.toByte()
                }
                var startX = rightX
                try {
                    for (y in 0 until bottomY) {
                        for (x in 0 until rightX) {
                            val lineIndex = index + x
                            if (bytes[lineIndex].toInt() and 0xFF > 40) {
                                startX = Math.min(startX, x)
                                //有透明度，后续全部改为黑色
                                buffer.position(lineIndex + 1)
                                buffer.put(lineBuffer, 0, rightX - x - 1)
                                break
                            }
                        }
                        index += rightX
                    }
                } catch (e: Exception) {
                }
                try {
                    buffer.rewind()
                    ret.copyPixelsFromBuffer(buffer)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                val shadow = Bitmap.createBitmap(
                    (logo.width * 1.5).toInt(),
                    logo.height,
                    Config.ARGB_8888
                )
                sCanvas.setBitmap(shadow)
                val colors = intArrayOf(-0x1000000, 0x00000000)
                colors[0] = opacity shl 24
                val newRadius = ((50 + radius) * 1.0f / 192 * logo.width).toInt()
                val gradient = LinearGradient(
                    startX.toFloat(),
                    0f,
                    (startX + newRadius).toFloat(),
                    0f,
                    colors,
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.setXfermode(null)
                paint.setShader(gradient)
                sCanvas.drawRect(0f, 0f, rightX.toFloat(), rightX.toFloat(), paint)
                paint.setShader(null)
                val t = Bitmap.createBitmap(logo.width, logo.height, Config.ARGB_8888)
                val matrix = Matrix()
                matrix.postRotate(
                    angle.toFloat(),
                    (logo.width / 2).toFloat(),
                    (logo.height / 2).toFloat()
                )
                sCanvas.setBitmap(t)
                paint.setXfermode(null)
                sCanvas.drawBitmap(ret, matrix, paint)
                paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_IN))
                sCanvas.drawBitmap(shadow, matrix, paint)
                return t
            }
        }
        /**
         * 合并图片
         *
         * @param belowBmp
         * @param aboveBmp
         * 合成后是否马上回收原来的两张图片
         * @return
         */
        fun mergeBitmap(
            context:Context,
            belowBmp: Bitmap?,
            sourceBmp: Bitmap?,
            aboveBmp: Bitmap?,
            maskBmp: Bitmap?,
            scale: Float
        ): Bitmap? {
            var scale = scale
            val sourceScale = scale
            if (belowBmp == null || belowBmp.isRecycled) {
                return aboveBmp
            }
            if (sourceBmp == null || sourceBmp.isRecycled) {
                return null
            }
            if (aboveBmp != null && aboveBmp.isRecycled) {
                return null
            }
            val desW = sourceBmp.width
            val desH = sourceBmp.height
            val output = Bitmap.createBitmap(desW, desH, Bitmap.Config.ARGB_8888)
            //sourceBitmap 的外圈到maskBmp的外圈的缩放值
            if (maskOutRect[0].width() == 0 && maskBmp != null) {
                //maskBmp的外圈
                val temp = Bitmap.createBitmap(maskBmp.width, maskBmp.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(temp)
                canvas.drawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawColor(Color.WHITE)
                canvas.density = maskBmp.density
                val paint = Paint()
                paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OUT))
                canvas.drawBitmap(maskBmp, 0f, 0f, paint)
                val rect = IconNormalizer.getInstance(context).getMinBound(temp)
                maskOutRect[0] = rect[0]
                // 部分sourceBitmap有阴影，稍微调大最后的缩放值，保证source不被裁剪成不规则
                maskOutRect[0].top -= 1
                maskOutRect[0].left -= 1
                maskOutRect[0].right += 1
                maskOutRect[0].bottom += 1
            }
            if (maskOutRect[0].width() != 0 && maskBmp != null) {
                //sourceBitmap 的外圈
                val iconNormalizer = IconNormalizer.getInstance(context)
                val rect = iconNormalizer.getMinBound(sourceBmp)
                //新的缩放值
                scale = (maskOutRect[0].height() * 1.0f / maskBmp.width
                            / (rect[0].width() * 1.0f /
                            if (iconNormalizer.getmMaxSize() < sourceBmp.width) iconNormalizer.getmMaxSize() else sourceBmp.width))
            }
            val canvas = Canvas(output)
            canvas.drawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val paint = Paint()
            val belowW = belowBmp.width
            val belowH = belowBmp.height
            val sourceW = (sourceBmp.width * scale).toInt()
            val sourceH = (sourceBmp.height * scale).toInt()
            val belowSrcRect = Rect( 0, 0, belowW, belowH)
            val belowDesRect = Rect(0, 0, desW, desH)
            val midSrcRect = Rect(0, 0, sourceBmp.width, sourceBmp.height)
            val scaleW = (sourceBmp.width * sourceScale).toInt()
            val scaleH = (sourceBmp.height * sourceScale).toInt()
            //遮罩裁剪后，缩放画到底座上的Rect 一般为0.8f
            val secondRect =
                Rect((desW - scaleW) / 2, (desH - scaleH) / 2, (desW + scaleW) / 2, (desH + scaleW) / 2)
            var centerPointOffsetX = 0
            var centerPointOffsetY = 0
            //遮罩的中心点可变化，对应调整原始缩放的中心点
            if (maskBmp != null) {
                centerPointOffsetY = (desH * ((maskOutRect[0]
                    .height() / 2 + maskOutRect[0].top) * 1.0f / maskBmp.height - 0.5f)).toInt()
                centerPointOffsetX = (desW * ((maskOutRect[0]
                    .width() / 2 + maskOutRect[0].left) * 1.0f / maskBmp.width - 0.5f)).toInt()
                Log.i("测试", "中心点位置变化 mergeBitmap: $centerPointOffsetX $centerPointOffsetY")
            }
            val midDesRect = Rect(
                (desW - sourceW) / 2 + centerPointOffsetX, (desH - sourceH) / 2 + centerPointOffsetY,
                (desW + sourceW) / 2 + centerPointOffsetX, (desH + sourceH) / 2 + centerPointOffsetY
            )
            canvas.drawBitmap(belowBmp, belowSrcRect, belowDesRect, paint)
            val mergeMask = mergeMask(sourceBmp, maskBmp, midDesRect)
            if (maskBmp != null) {
                canvas.drawBitmap(mergeMask!!, midSrcRect, secondRect, paint)
            } else {
                canvas.drawBitmap(mergeMask!!, midSrcRect, midSrcRect, paint)
            }
            if (aboveBmp != null) {
                val aboveW = aboveBmp.width
                val aboveH = aboveBmp.height
                canvas.drawBitmap(aboveBmp, Rect(0, 0, aboveW, aboveH), belowDesRect, paint)
            }
            if (!sourceBmp.isRecycled) {
                sourceBmp.recycle()
            }
            return output
        }
        private fun mergeMask(sourceBmp: Bitmap?, maskBmp: Bitmap?, midDesRect: Rect): Bitmap? {
            if (null != sourceBmp && maskBmp != null && !maskBmp.isRecycled) {
                val desW = sourceBmp.width
                val desH = sourceBmp.height
                val output = Bitmap.createBitmap(desW, desH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                canvas.drawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                val paint = Paint()
                val maskW = maskBmp.width
                val maskH = maskBmp.height
                val srcRect = Rect(0, 0, desW, desH)
                canvas.drawBitmap(sourceBmp, srcRect, midDesRect, paint)
                val maskRect = Rect(0, 0, maskW, maskH)
                output.setHasAlpha(true)
                paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OUT))
                canvas.drawBitmap(maskBmp, maskRect, Rect(0, 0, desW, desH), paint)
                //        		if (!sourceBmp.isRecycled()) {
//                    sourceBmp.recycle();
//                }
                return output
            }
            return sourceBmp
        }


        //保存第一个前景色不变，其他，随机
        @JvmField
        val METHOD_KEEP_FIRST = "keep_first"
        //保持最后一个前景色不变，其他随机
        @JvmField
        val METHOD_KEEP_LAST = "keep_last"
        //没有背景，其他前景色随机
        @JvmField
        val METHOD_NO_BG = "no_bg"
        //根据背景色和前景色随机匹配，对比度大于1.3
        @JvmField
        val METHOD_GENERATE = "generate"
        //前景色两两结合，背景色随机
        val METHOD_COMMBINE_TWO = "combine_fg_two"
        fun getMoreColorInfo( seedColor: ColorCustomOption, method:String): MutableList<ColorCustomOption>{
            val list = mutableListOf<ColorCustomOption>()
            Log.i(TAG, "getMoreColorInfo: method ${seedColor.pre_method}")
            val ret = when(method){
                METHOD_KEEP_FIRST ->{
                    generateColorGroupRandom(seedColor.backgroundColors,
                        seedColor.foregroundColors, true)
                }
                METHOD_NO_BG ->{
                    generateColorGroupNotBg(seedColor.backgroundColors,
                        seedColor.foregroundColors, keepLast = false)
                }
                METHOD_KEEP_LAST ->{
                    generateColorGroupNotBg(seedColor.backgroundColors,
                        seedColor.foregroundColors, keepLast = true)
                }
                METHOD_GENERATE ->{
                    generateColorGroupRandom(seedColor.backgroundColors,
                        seedColor.foregroundColors)
                }
                METHOD_COMMBINE_TWO ->{
                    combineTwoFg(seedColor.backgroundColors, seedColor.foregroundColors)
                }
                else ->{
                    if(seedColor.backgroundColors.size > 1){
                        randomBgOnly(seedColor.backgroundColors, seedColor.foregroundColors)
                    }else{
                        seedColor
                    }
                }
            }
            list += ret
            ret.colorOptionsChilds.forEach { it ->
                if(it is ColorCustomOption ){
                    list += it
                }
            }
            return list
        }

        fun randomBgOnly(bgColorsInt: MutableList<ColorCustomInfo>,
                          fgColorsInt: MutableList<ColorCustomInfo>): ColorCustomOption{
            val colorOptions = ArrayList<ColorCustomOption>()
            bgColorsInt.forEach { it ->
                var constantColors = fgColorsInt.toMutableList()
                colorOptions.add(ColorCustomOption(constantColors.toMutableList(),
                    mutableListOf(it), ""))
            }
            colorOptions[0].colorOptionsChilds.addAll(colorOptions.subList(1, colorOptions.size))
            return colorOptions[0]
        }

        fun combineTwoFg(bgColorsInt: MutableList<ColorCustomInfo>,
                         fgColorsInt: MutableList<ColorCustomInfo>): ColorCustomOption{
            val colorOptions = ArrayList<ColorCustomOption>()
            bgColorsInt.forEach { it ->
                var constantColors = fgColorsInt.toMutableList()
                val newFgs = constantColors.chunked(2).filter { it.size == 2 }
                newFgs.forEach { fgs ->
                    colorOptions.add(ColorCustomOption(fgs.toMutableList(),
                        mutableListOf(it), ""))
                }
            }
            colorOptions[0].colorOptionsChilds.addAll(colorOptions.subList(1, colorOptions.size))
            return colorOptions[0]
        }

        /**
         * 根据多个前景色，背景色，对比生成多套颜色组
         *
         * keepFirst 保持第一个颜色固定
         */
        fun generateColorGroupRandom(bgColorsInt: MutableList<ColorCustomInfo>,
                                     fgColorsInt: MutableList<ColorCustomInfo>,
                                     keepFirst:Boolean = false,
                                     keepLast:Boolean = false): ColorCustomOption{
            Log.i(TAG, "loadCustomColorsGroup generateColorGroupRandom: start $bgColorsInt")
            var fgColors = arrayListOf<ColorCustomOption.ColorCustomInfo>()
            var bgColors = arrayListOf<ColorCustomOption.ColorCustomInfo>()
            val colorOptions = ArrayList<ColorCustomOption>()
            val t = bgColorsInt.map { it ->
                var constantColors = fgColorsInt.toMutableList()
                if(keepLast){
                    constantColors.remove(constantColors[constantColors.lastIndex])
                    constantColors.remove(it)
                }else if(keepFirst){
                    constantColors.remove(constantColors[0])
                    constantColors.remove(it)
                }else{
                    constantColors.remove(it)
                    constantColors = fgColorsInt.filter { fgColor ->
                        if(it.colors[0].alpha < 255){
                            true
                        }else{
                            ColorUtils.calculateContrast(fgColor.colors[0], it.colors[0]) > 1.2
                        }
                    }.toMutableList()
                }
                val list = randomPermutations(constantColors)
                if(keepFirst){
                    list.forEach { it ->
                        it.add(0, fgColorsInt[0])
                    }
                }
                if(keepLast){
                    list.forEach { it ->
                        it.add(fgColorsInt[0])
                    }
                }
                if(BuildConfig.DEBUG){
                    runCatching {
                        list.forEach { colors ->
                            Log.i(TAG, "loadCustomColorsGroup: bg=${Integer.toHexString(it.colors[0])} " +
                                    "fgs=[${colors.map { Integer.toHexString(it.colors[0]) }}]")
                        }
                    }
                }
                var count = 0
                val pair = Pair(it, list)
                pair.second.forEach { list ->
                    if(count < 10){
                        if(list.size >= 3 || list.size >= constantColors.size){
                            fgColors = arrayListOf()
                            bgColors = arrayListOf()
                            bgColors.add(pair.first)
                            list.forEach { color->
                                fgColors.add(color)
                            }
                            colorOptions.add(ColorCustomOption(fgColors, bgColors, "Sunshine"))
                        }
                    }
                    count ++
                }
            }

            colorOptions[0].colorOptionsChilds.addAll(colorOptions.subList(1, colorOptions.size))
//            if(colorOptions[0].colorOptionsChilds.isEmpty()){
//                colorOptions[0].colorOptionsChilds.add(colorOptions[0])
//            }
            return colorOptions[0]
        }

        /**
         * 计算 [minSize, maxSize] 范围内的全排列总数（避免生成超过总数的排列）
         * 公式：Σ (n! / (n - k)!) ，k 从 minSize 到 maxSize
         */
        private fun calculateMaxPermutations(n: Int, minK: Int, maxK: Int): Int {
            var total = 0
            for (k in minK..maxK) {
                // 计算 n!/(n-k)! （排列数）
                var perm = 1
                for (i in 0 until k) {
                    perm *= (n - i)
                    // 防止整数溢出（超过 Int 范围时直接返回 Int 最大值）
                    if (perm < 0) return Int.MAX_VALUE
                }
                total += perm
                if (total < 0) return Int.MAX_VALUE // 溢出保护
            }
            return total
        }
        /**
         * Fisher-Yates 洗牌算法（原地打乱列表，生成随机排列的最优方式）
         */
        private fun fisherYatesShuffle(list: MutableList<ColorCustomInfo>) {
            for (i in list.indices.reversed()) {
                val j = Random.nextInt(0, i + 1)
                // 交换元素
                val temp = list[i]
                list[i] = list[j]
                list[j] = temp
            }
        }
        fun randomPermutations(
            list: MutableList<ColorCustomInfo>,
            count: Int = 10,
            minSize: Int = 1,
            maxSize: Int = list.size
        ): MutableList<MutableList<ColorCustomInfo>> {
            // 1. 边界校验
            val validMin = minSize.coerceAtLeast(1)
            val validMax = maxSize.coerceAtMost(list.size)
            require(validMin <= validMax) { "minSize 不能大于 maxSize" }
            require(count >= 0) { "生成数量不能为负数" }
            if (count == 0 || list.isEmpty()) return mutableListOf()

            // 2. 计算全排列的最大可能数（避免生成超过总数的排列）
            val maxPossible = calculateMaxPermutations(list.size, validMin, validMax)
            val targetCount = count.coerceAtMost(maxPossible)
            if (targetCount == 0) return mutableListOf()

            // 3. 用集合去重（核心：保证排列唯一），键为排列的唯一标识（比如 id 拼接）
            val uniquePerms = hashSetOf<String>()
            val result = mutableListOf<MutableList<ColorCustomInfo>>()
            var countIndex = 0
            // 4. 循环生成随机排列，直到达到目标数量
            while (uniquePerms.size < targetCount) {
                // 复制源列表（避免修改原列表）
                val copy = list.toMutableList()
                // Fisher-Yates 洗牌（原地打乱，O(n) 最优效率）
                fisherYatesShuffle(copy)
                // 截取符合长度的子列表（支持 minSize/maxSize）
                val permLength = Random.nextInt(validMin, validMax + 1)
                val perm = copy.take(permLength).toMutableList()
                // 生成排列的唯一标识（用于去重，需保证 ColorCustomInfo 有唯一 id）
                val permKey = perm.joinToString(",") { it.hashCode().toString() }

                // 未重复则加入结果
                if (uniquePerms.add(permKey)) {
                    result.add(perm)
                }
                countIndex++
            }
            Log.i(TAG, "randomPermutations: 循环次数 $countIndex")

            return result
        }

        fun combinations(list: MutableList<ColorCustomInfo>, minSize: Int = 1, maxSize: Int = 3): MutableList<MutableList<ColorCustomInfo>> {
            val result = mutableListOf<MutableList<ColorCustomInfo>>()

            fun backtrack(start: Int, current: MutableList<ColorCustomInfo>) {
                if (current.size in minSize..maxSize) {
                    result.add(current.toMutableList()) // 添加当前组合的副本
                }
                if (current.size == maxSize) return // 达到最大长度，不再继续

                for (i in start until list.size) {
                    current.add(list[i])
                    backtrack(i + 1, current)
                    if(current.isNotEmpty()){
                        current.removeAt(current.lastIndex) // 回溯
                    }
                }
            }

            backtrack(0, mutableListOf())
            return result
        }

        //取随机多个前景色，背景色是透明的
        fun generateColorGroupNotBg(bgColorsInt: MutableList<ColorCustomInfo>,
                                    fgColorsInt:MutableList<ColorCustomInfo>,
                                    maxSize:Int = 4, keepLast:Boolean = false): ColorCustomOption{
            Log.i(TAG, "loadCustomColorsGroup generateColorGroupNotBg: start ${bgColorsInt}")
            val t = bgColorsInt.map { it ->
                var constantColors = fgColorsInt.toMutableList()
                var last: ColorCustomInfo? = null
                if(keepLast){
                    last = constantColors.removeAt(constantColors.lastIndex)
                }
                val list = combinations(constantColors, 3, maxSize)
                list.forEach { colors ->
                    if(keepLast){
                        colors.add(last!!)
                    }

                    runCatching {
                        Log.i(TAG, "loadCustomColorsGroup: bg=${Integer.toHexString(it.colors[0])} " +
                                "fgs=[${colors.map { Integer.toHexString(it.colors[0]) }}]")
                    }

                }
                Pair(it, list)
            }
            var fgColors = arrayListOf<ColorCustomOption.ColorCustomInfo>()
            var bgColors = arrayListOf<ColorCustomOption.ColorCustomInfo>()
            val colorOptions = ArrayList<ColorCustomOption>()
            t.forEach { pair ->

                pair.second.forEach { list ->
                    if(list.size >= 3 || list.size >= fgColorsInt.size){
                        fgColors = arrayListOf()
                        bgColors = arrayListOf()
                        bgColors.add(pair.first)
                        list.forEach { color->
                            fgColors.add(color)
                        }
                        colorOptions.add(ColorCustomOption(fgColors,
                            bgColors,
                            "Sunshine"))
                    }
                }
            }
            colorOptions[0].colorOptionsChilds.addAll(colorOptions.subList(1, colorOptions.size))
            return colorOptions[0]
        }
    }

    private fun parseFromXML(context :Context) {
        var id = when (icon_pack_name) {
            INTERNAL_ICON_PACK_1 -> R.xml.appfilter_android_wallpaper_iconpack_1
            INTERNAL_ICON_PACK_2 -> R.xml.appfilter_android_wallpaper_iconpack_2
            INTERNAL_ICON_PACK_3 -> R.xml.appfilter_android_wallpaper_iconpack_3
            INTERNAL_ICON_PACK_4 -> R.xml.appfilter_android_wallpaper_iconpack_4
            INTERNAL_ICON_PACK_5 -> R.xml.appfilter_android_wallpaper_iconpack_5
            INTERNAL_ICON_PACK_6 -> R.xml.appfilter_android_wallpaper_iconpack_6
            INTERNAL_ICON_PACK_7 -> R.xml.appfilter_android_wallpaper_iconpack_7
            INTERNAL_ICON_PACK_8 -> R.xml.appfilter_android_wallpaper_iconpack_8
            INTERNAL_ICON_PACK_9 -> R.xml.appfilter_android_wallpaper_iconpack_9
            INTERNAL_ICON_PACK_ROSE_NO_DEC -> R.xml.appfilter_android_wallpaper_iconpack_rose_on_dec
            "" -> R.xml.appfilter_android_wallpaper_iconpack
            else -> R.xml.appfilter_android_wallpaper_iconpack
        }
        //zip包前景图，获取前景图资源
        if(iconForegroundFileName.isNotEmpty()){
            id = R.xml.appfilter_android_wallpaper_default_iconpack
        }
        maskOutRect[0].setEmpty()
        maskOutRect[1].setEmpty()
        drawableMap = HashMap()
        iconBgDrawable.clear()
        iconMaskDrawable = null
        val mDrawableMap = drawableMap
        var parser = context.resources.getXml(id)
        try {
            parser.next()
            var eventType = parser.eventType

            // 循环，直到文档结尾
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                try {
                    val name = parser.name
                    if (eventType == XmlResourceParser.START_DOCUMENT) {
                    } else if (eventType == XmlResourceParser.START_TAG) {
                        if (null == name) {
                            eventType = parser.next()
                            continue
                        }
                        if (name == "item") {
                            val componentName = parser.getAttributeValue(
                                null,
                                "component"
                            )
                            val drawableName = parser.getAttributeValue(
                                null,
                                "drawable"
                            ).split(" ")

                            mDrawableMap.put(componentName, drawableName)
                        }else if(name == "iconback"){
                            var drawableName = parser.getAttributeValue(
                                null,
                                "img1"
                            )
                            runCatching {
                                val id = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
                                if(id > 0){
                                    iconBgDrawable.add(context.resources.getDrawable(id))
                                }
                            }
                            drawableName = parser.getAttributeValue(
                                null,
                                "img2"
                            )
                            runCatching {
                                val id = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
                                if(id > 0){
                                    iconBgDrawable.add(context.resources.getDrawable(id))
                                }
                            }
                            drawableName = parser.getAttributeValue(
                                null,
                                "img3"
                            )
                            runCatching {
                                val id = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
                                if(id > 0){
                                    iconBgDrawable.add(context.resources.getDrawable(id))
                                }
                            }
                            drawableName = parser.getAttributeValue(
                                null,
                                "img4"
                            )
                            runCatching {
                                val id = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
                                if(id > 0){
                                    iconBgDrawable.add(context.resources.getDrawable(id))
                                }
                            }
                        }else if(name == "iconmask"){
                            val drawableName = parser.getAttributeValue(
                                null,
                                "img1"
                            )
                            runCatching {
                                val id = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
                                if(id > 0){
                                    iconMaskDrawable = context.resources.getDrawable(id)
                                }
                            }
                        }
                    } else if (eventType == XmlResourceParser.END_TAG) {
                    } else if (eventType == XmlResourceParser.TEXT) {
                    }
                    eventType = parser.next()
                } catch (e: java.lang.Exception) {
                }
            }
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

//    fun getIconForegroundFileRoot(context: Context):String{
//        return File(File(context.getExternalFilesDir(null), IconPackSectionController.PACK_RESOURCE),
//            iconForegroundFileName).absolutePath
//    }
    /**
     * 获取iconpack的图标
     */
    fun getThemeIcon(
        resources: Resources, pkgName: String,
        drawableName: String,
    ): Drawable? {
        if (TextUtils.isEmpty(drawableName)) {
            return null
        }
        //前景图配置文件中的数据
        if(iconForegroundFileName.isNotEmpty()){
            val iconColorPackDir = PrefUtils.getFileInRes(context, IconPackThemeComposeUtils.ICON_ADAPTER_FILE_NAME)
            val file = File(iconColorPackDir, iconForegroundFileName)
            //有目录
            if(file.isDirectory){
                val drawable = Drawable.createFromPath(File(file, "$drawableName.png").absolutePath)
                return drawable
            }
        }
        val iconId = resources.getIdentifier(drawableName, "drawable", pkgName)
        return if (iconId != 0) {
            return resources.getDrawableForDensity(iconId, resources.displayMetrics.densityDpi)
        } else null
    }
    fun getDrawableByDrawableName(context: Context, drawableName: String?):Drawable?{
        drawableName?.let{
            context.resources.getIdentifier(drawableName, "drawable", context.packageName).takeIf {
                it != 0
            }?.let {
                return context.resources.getDrawableForDensity(it, context.resources.displayMetrics.densityDpi)
            }
        }
        return null
    }
    private fun checkColorTypeSourceCfg(context:Context, cn: ComponentName?, entryColor: Int,
                                        foregrounds: MutableList<ColorCustomInfo>,
                                        background:MutableList<ColorCustomInfo>, monoBitmaps: ArrayList<Bitmap>):Boolean{
        if(foregrounds.isEmpty() || background.isEmpty()){
            return false
        }
        var useSourceColor = ColorCustomInfo.COLOR_TYPE_SOURCE == foregrounds[0].type
        val scheme = ColorScheme(entryColor, false, Style.VIBRANT)
        //颜色使用主色时，优化前景色效果
        if (entryColor != 0 && entryColor != 0xff123456.toInt()
            && useSourceColor
            && !((cn?.packageName?.contains("calendar") == true
                    || cn?.packageName?.contains("gallery") == true
                    || context.packageName == cn?.packageName))) {
            val fgColors = foregrounds
            var bgColors = ArrayList<ColorCustomInfo>()
            var t = IntArray(2)
            t[1] = entryColor
            t[0] = scheme.accent1[3]
            t[0] = Color.argb(222, Color.red(t[0]), Color.green(t[0]), Color.blue(t[0]))
            t[1] = scheme.accentColor
            var hsb = ColorUtils.RGBtoHSB(t[0])
            t[0] = ColorUtils.HSBLightColor(hsb)
            hsb = ColorUtils.RGBtoHSB(t[1])
            t[1] = ColorUtils.HSBLightColor(hsb)
            Log.i(TAG, ("iconHandle: source_icon_new" + cn?.packageName + " " + Integer.toHexString(entryColor)
                    + " " + Integer.toHexString(t[0]) + " " + Integer.toHexString(t[1])))
            fgColors.clear()
            fgColors.add(ColorCustomInfo(ColorCustomInfo.LINE_GRADIENT, t, positions =  floatArrayOf(0f, 1f)))
            fgColors.add(ColorCustomInfo(ColorCustomInfo.COLOR_TYPE_NORMAL, intArrayOf(scheme.accent1[7])))

            //背景用浅色对比色
            if (TextUtils.equals(background[0].type, ColorCustomInfo.COLOR_TYPE_CONTRASTING_COLOR)) {
                t = IntArray(2)
                var bgColors = background
                bgColors.clear()
                t[0] = scheme.neutral1[1]
                t[1] = scheme.neutral1[2]
                fgColors[1].colors[0] = scheme.accent1[4]
                bgColors.add(ColorCustomInfo(ColorCustomInfo.LINE_GRADIENT,t,  positions =  floatArrayOf(0f, 1f)))
                t = fgColors[0].colors
                t[0] = entryColor
                 t[1] = scheme.accentColor
                if(monoBitmaps.isNotEmpty()){
                    var sourceColor = Palette.from(monoBitmaps[0]).generate().getDominantColor(Color.WHITE)
                    if(ColorUtils.calculateContrast(sourceColor, scheme.neutral1[1]) < 2){
                        useSourceColor = false
                        t = IntArray(2)
                        fgColors.clear()
                        t[0] = scheme.accent1[4]
                        t[1] = scheme.accentColor
                        fgColors.add(ColorCustomInfo(ColorCustomInfo.LINE_GRADIENT, t,  positions =  floatArrayOf(0f, 1f)))
                        fgColors.add(ColorCustomInfo(ColorCustomInfo.COLOR_TYPE_NORMAL, intArrayOf(scheme.accent1[7])))
//                        fgColors[1].colors[0] = scheme.accent1[4]
//                        t = fgColors[0].colors
//                        t[0] = entryColor
//                        t[1] = scheme.accentColor
                    }
                }
            } else {
                if (background[0].colors.isEmpty() || ColorUtils.calculateContrast(entryColor, background[0].colors[0]) < 4) {
                    var bgColors = background
                    bgColors.clear()
                    val distWhite = ColorUtils.calculateContrast(Color.WHITE, entryColor)
                    val distBlack = ColorUtils.calculateContrast(Color.BLACK, entryColor)
                    t = IntArray(1)
                    if (min(distWhite, distBlack) > 4) {
//                                t[0] = new Random().nextFloat() > 0.5f ? Color.WHITE : Color.BLACK;
//                                bgColors.add(new ColorCustomOption.ColorCustomInfo(ColorCustomOption.ColorCustomInfo.COLOR_TYPE_NORMAL,
//                                        t));
                        bgColors.add(ColorCustomInfo(ColorCustomInfo.LINE_GRADIENT,
                            intArrayOf(0xFF313131.toInt(), 0xFF141414.toInt()),  positions =  floatArrayOf(0f, 1f)))
                    } else {
                        t[0] = if (distWhite > distBlack) Color.WHITE else Color.BLACK
//                    if (distWhite > distBlack) {
//                        fgColors[0].colors[0] = entryColor
//                    }
                        bgColors.add(ColorCustomInfo(ColorCustomInfo.COLOR_TYPE_NORMAL, t))
                    }
                }
            }
        }else{
            if(TextUtils.equals(background[0].type, ColorCustomInfo.COLOR_TYPE_SOURCE)){
                //颜色使用主色时，优化前景色效果
                var sourceColor = entryColor
                val hsv = floatArrayOf(0f, 0f, 0f)
                Color.colorToHSV(sourceColor, hsv)
                //浅色，暗色主色调，使用随机的颜色
                if(hsv[2] < 0.25 || (hsv[2] > 0.9f && hsv[1] < 0.15f) || entryColor == 0 || entryColor == 0xff123456.toInt()){
                    sourceColor = intArrayOf(0xFFFF0A0A.toInt(), 0xFFFF7C00.toInt(), 0xFF05FF00.toInt(), 0xFF0094FF.toInt(),
                        0xFF3000FF.toInt(), 0xFFF700FF.toInt(), 0xFFFF0075.toInt()).random()
                }
                var bgColors = background
                bgColors.clear()
                bgColors.add(ColorCustomInfo(ColorCustomInfo.NONE_GRADIENT, intArrayOf(
                    sourceColor, ColorScheme(sourceColor, false).accentColor
                ),  positions =  floatArrayOf(0f, 1f)))
                foregrounds.shuffle()
            }else if (TextUtils.equals(background[0].type, ColorCustomInfo.COLOR_TYPE_CONTRASTING_COLOR)) {
                //背景用浅色对比色
                var t = IntArray(2)
                var bgColors = background
                bgColors.clear()
                t[0] = scheme.neutral1[1]
                t[1] = scheme.neutral1[2]

                bgColors.add(ColorCustomInfo(ColorCustomInfo.LINE_GRADIENT,t,  positions =  floatArrayOf(0f, 1f)))
                val fgColors = foregrounds
                if(useSourceColor && monoBitmaps.isNotEmpty()){
                    var sourceColor = Palette.from(monoBitmaps[0]).generate().getDominantColor(Color.WHITE)
                    if(ColorUtils.calculateContrast(sourceColor, t[0]) < 2){
                        useSourceColor = false
                        t = IntArray(2)
                        fgColors.clear()
                        t[0] = scheme.accent1[4]
                        t[1] = scheme.accentColor
                        fgColors.add(ColorCustomInfo(ColorCustomInfo.LINE_GRADIENT, t,  positions =  floatArrayOf(0f, 1f)))
                        fgColors.add(ColorCustomInfo(ColorCustomInfo.COLOR_TYPE_NORMAL, intArrayOf(scheme.accent1[7])))
                    }
                }

            }
        }
        return useSourceColor
    }

    /**
     * 获取Widget 配色
     */
    fun getWidgetAdapterColors(
        context: Context, fgColors: MutableList<ColorCustomInfo>,
        bgColors: MutableList<ColorCustomInfo>
    ): Pair<MutableList<ColorCustomInfo>, MutableList<ColorCustomInfo>> {
        // 优先从iconColorOptions获取颜色配置
        var actualFgColors = fgColors
        var actualBgColors = bgColors
        if (!iconColorOptions.isNullOrEmpty()) {
            try {
                val selectedColorOption = iconColorOptions!!.random()

                // 收集selectedColorOption和colorOptionsChilds中的所有ColorCustomOption
                val allColorOptions = mutableListOf(selectedColorOption)
                if (selectedColorOption.colorOptionsChilds.isNotEmpty()) {
                    selectedColorOption.colorOptionsChilds.forEach { child ->
                        allColorOptions.add(child as ColorCustomOption)
                    }
                }

                // 从所有ColorCustomOption中随机选择一个
                val randomColorOption = allColorOptions.random()

                // 从选中的ColorCustomOption中随机选择一个索引（fg和bg对应）
                if (randomColorOption.foregroundColors.isNotEmpty() &&
                    randomColorOption.backgroundColors.isNotEmpty()
                ) {
                    actualFgColors = ArrayList(randomColorOption.foregroundColors.toMutableList())
                    actualBgColors = ArrayList(randomColorOption.backgroundColors)
                    actualFgColors.forEach { colorCustomInfo ->
                        colorCustomInfo.type = colorCustomInfo.gradient.type
                    }
                    actualBgColors.forEach { colorCustomInfo ->
                        colorCustomInfo.type = colorCustomInfo.gradient.type
                    }
                }

                Log.d(TAG, "testModeGetIcon: 使用iconColorOptions中的颜色配置")
            } catch (e: Exception) {
                Log.w(TAG, "testModeGetIcon: 从iconColorOptions获取颜色失败，使用参数颜色", e)
            }
        }

        //前景图，取主色调作为前景图颜色
        // 颜色是Source，
        val foregrounds = actualFgColors.map { it -> it.deepCopy() }.toMutableList()
        val background = actualBgColors.map { it -> it.deepCopy() }.toMutableList()

        iconColorBean?.let {
            val colorScheme = it.icon_colors.random()
            foregrounds.clear()
            foregrounds.addAll(colorScheme.fgColors)
            if (foregrounds.size <= 3) {
                val color = foregrounds[0].colors[0]
                val t = IntArray(1)
                t[0] = ColorScheme(color, false).accent1[4]
                foregrounds.add(
                    ColorCustomInfo(
                        ColorCustomInfo.COLOR_TYPE_NORMAL,
                        t
                    )
                )
            }
            background.clear()
            background.addAll(colorScheme.bgColors)
        }
        return Pair(foregrounds, background)
    }

    fun processForegroundCompose(context: Context,
                                  sourceDrawable: Drawable,
                                  cn: ComponentName?,
                                  mono: Boolean,
                                  entryColor: Int,
                                  fgColors: ArrayList<ColorCustomInfo>,
                                  bgColors: ArrayList<ColorCustomInfo>
    ): ProcessedForeground?{
        val pForeground = processForegroundLayers(context, sourceDrawable,
            cn, mono, entryColor, fgColors, bgColors)
        if(pForeground.foregroundBitmaps.isNotEmpty()){
            val ret = createBitmap(pForeground.foregroundBitmaps[0].width,
                pForeground.foregroundBitmaps[0].height)
            val canvas = Canvas(ret)
            val monoBitmap = pForeground.foregroundBitmaps
            val fgColor = pForeground.fgColors
            val bounds = Rect(0, 0, ret.width, ret.height)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            for(index in 0 until monoBitmap.size){
                val colorIndex = index % fgColor.size
                if(colorIndex < fgColor.size){
                    paint.shader = null
                    val fgShader = getShader(fgColor[colorIndex], bounds)
                    if(fgShader == null){
                        if(fgColor[colorIndex].type == ColorCustomInfo.COLOR_TYPE_NORMAL_GRAY){
                            //灰阶上色
                            paint.color = fgColor[colorIndex].colors[0]
                            val red = fgColor[colorIndex].colors[0].red/255f
                            val green = fgColor[colorIndex].colors[0].green/255f
                            val blue = fgColor[colorIndex].colors[0].blue/255f
                            val fiter = floatArrayOf(red, 0f, 0f, 0f, 0f,
                                0f, green, 0f, 0f, 0f,
                                0f, 0f, blue, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f)
                            val colorMatrix = ColorMatrix(fiter)
                            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                        }else{
                            paint.colorFilter = PorterDuffColorFilter(fgColor[colorIndex].colors[0], PorterDuff.Mode.SRC_IN)
                        }
                        canvas.drawBitmap(monoBitmap[index], null, bounds, paint)
                        paint.colorFilter = null
                    }else{
                        paint.shader = ComposeShader(fgShader,
                            BitmapShader(monoBitmap[index],
                                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP),
                            PorterDuff.Mode.DST_IN)
                        canvas.drawRect(bounds, paint)
                    }
                }
            }
            return ProcessedForeground(mutableListOf(ret),
                pForeground.fgColors, pForeground.bgColors,
                pForeground.internalMono,
                pForeground.useSourceColor)
        }
        return null
    }
    fun getForegroundColor(
        fgColors: ArrayList<ColorCustomInfo>,
        bgColors: ArrayList<ColorCustomInfo>
    ): ProcessedForeground{
        // 优先从iconColorOptions获取颜色配置
        var actualFgColors = fgColors
        var actualBgColors = bgColors
        if (!iconColorOptions.isNullOrEmpty()) {
            try {
                val selectedColorOption = iconColorOptions!!.random()

                // 收集selectedColorOption和colorOptionsChilds中的所有ColorCustomOption
                val allColorOptions = mutableListOf(selectedColorOption)
                if (selectedColorOption.colorOptionsChilds.isNotEmpty()) {
                    selectedColorOption.colorOptionsChilds.forEach { child ->
                        allColorOptions.add(child as ColorCustomOption)
                    }
                }

                // 从所有ColorCustomOption中随机选择一个
                val randomColorOption = allColorOptions.random()

                // 从选中的ColorCustomOption中随机选择一个索引（fg和bg对应）
                if (randomColorOption.foregroundColors.isNotEmpty() &&
                    randomColorOption.backgroundColors.isNotEmpty()) {
                    actualFgColors = ArrayList(randomColorOption.foregroundColors.toMutableList())
                    actualBgColors = ArrayList(randomColorOption.backgroundColors)
                    actualFgColors.forEach { colorCustomInfo ->
                        colorCustomInfo.type = colorCustomInfo.gradient.type
                    }
                    actualBgColors.forEach { colorCustomInfo ->
                        colorCustomInfo.type = colorCustomInfo.gradient.type
                    }
                }

                Log.d(TAG, "processForegroundLayers: 使用iconColorOptions中的颜色配置")
            } catch (e: Exception) {
                Log.w(TAG, "processForegroundLayers: 从iconColorOptions获取颜色失败，使用参数颜色", e)
            }
        }

        //前景图，取主色调作为前景图颜色
        // 颜色是Source，
        val foregrounds = actualFgColors.map { it -> it.deepCopy() }.toMutableList()
        val background = actualBgColors.map { it -> it.deepCopy() }.toMutableList()
        return ProcessedForeground(mutableListOf(), foregrounds,
            background, false, false)
    }
    /**
     * 处理前景图层，返回已上色的前景图Bitmap列表和颜色配置
     * 此方法提取了testModeGetIcon中的前景图处理逻辑，用于自定义合成逻辑的前置操作
     *
     * @param context 上下文
     * @param sourceDrawable 源图标Drawable
     * @param cn ComponentName，用于从drawableMap获取对应的前景图资源
     * @param mono 是否为单色模式
     * @param entryColor 入口颜色
     * @param fgColors 前景色配置列表
     * @param bgColors 背景色配置列表
     * @return ProcessedForeground 包含已上色的前景图和颜色信息
     */
    fun processForegroundLayers(
        context: Context,
        sourceDrawable: Drawable,
        cn: ComponentName?,
        mono: Boolean,
        entryColor: Int,
        fgColors: ArrayList<ColorCustomInfo>,
        bgColors: ArrayList<ColorCustomInfo>
    ): ProcessedForeground {
        // 优先从iconColorOptions获取颜色配置
        var actualFgColors = fgColors
        var actualBgColors = bgColors
        if (!iconColorOptions.isNullOrEmpty()) {
            try {
                val selectedColorOption = iconColorOptions!!.random()

                // 收集selectedColorOption和colorOptionsChilds中的所有ColorCustomOption
                val allColorOptions = mutableListOf(selectedColorOption)
                if (selectedColorOption.colorOptionsChilds.isNotEmpty()) {
                    selectedColorOption.colorOptionsChilds.forEach { child ->
                        allColorOptions.add(child as ColorCustomOption)
                    }
                }

                // 从所有ColorCustomOption中随机选择一个
                val randomColorOption = allColorOptions.random()

                // 从选中的ColorCustomOption中随机选择一个索引（fg和bg对应）
                if (randomColorOption.foregroundColors.isNotEmpty() &&
                    randomColorOption.backgroundColors.isNotEmpty()) {
                    actualFgColors = ArrayList(randomColorOption.foregroundColors.toMutableList())
                    actualBgColors = ArrayList(randomColorOption.backgroundColors)
                    actualFgColors.forEach { colorCustomInfo ->
                        colorCustomInfo.type = colorCustomInfo.gradient.type
                    }
                    actualBgColors.forEach { colorCustomInfo ->
                        colorCustomInfo.type = colorCustomInfo.gradient.type
                    }
                }

//                Log.d(TAG, "processForegroundLayers: 使用iconColorOptions中的颜色配置")
            } catch (e: Exception) {
//                Log.w(TAG, "processForegroundLayers: 从iconColorOptions获取颜色失败，使用参数颜色", e)
            }
        }

        //前景图，取主色调作为前景图颜色
        // 颜色是Source，
        val foregrounds = actualFgColors.map { it -> it.deepCopy() }.toMutableList()
        val background = actualBgColors.map { it -> it.deepCopy() }.toMutableList()

        synchronized(this){
            var monoBitmap = ArrayList<Bitmap>()
            val sourceBitmap = Utilities.createIconBitmap(sourceDrawable, 1f, context)
            val drawableMap = drawableMap
            val drawableList = drawableMap[cn?.toString()]
            Log.i(TAG, "processForegroundLayers: drawableList = " + drawableList + cn)
            var realmono = mono
            if(!drawableList.isNullOrEmpty()){
                if(foregrounds.size > 1 && drawableList.size > 1
                    || icon_pack_name == INTERNAL_ICON_PACK_5
                    || (foregrounds.size == 1 && drawableList.size == 1)){

                    drawableList.forEach {
                        runCatching {
                            val tempMono = Utilities.createIconBitmap(
                                getThemeIcon(
                                    context.resources,
                                    context.packageName, it
                                ), 1f, context
                            )
                            monoBitmap.add(tempMono)
                        }
                    }
                    //加上日历图标的特殊处理
                    if(drawableList[0].contains("calendar")){
                        if(drawableList[0].contains("wp_theme_calendar")){
                            monoBitmap.clear()
                        }
                        with(getCalendarThemeIcon(context, sourceBitmap.height)){
                            if(CollectionUtils.isNotEmpty(this)){
                                monoBitmap.addAll(this)
                            }
                        }
                        if(icon_pack_name == INTERNAL_ICON_PACK_1 && monoBitmap.size > 3){
                            Collections.swap(monoBitmap, 2, monoBitmap.lastIndex - 1)
                        }
                    }
                }
                if((icon_pack_name == INTERNAL_ICON_PACK_1
                    || icon_pack_name == INTERNAL_ICON_PACK_4
                            || mIconColorOptionBean?.pre_method == METHOD_KEEP_LAST)
                   && monoBitmap.size > 1){
                    //前景色数量超过前景图数量，调换最后一个前景色到最后一个前景图的位置（最后一个前景色对应上最后一个前景色）
                    if(foregrounds.size > monoBitmap.size){
                        Collections.swap(foregrounds, foregrounds.lastIndex, monoBitmap.lastIndex)
                    }else if(foregrounds.size < monoBitmap.size){
                        //前景色少于前景图数量，在最后一个前景色前插入前景色
                        val fgSize = foregrounds.size-1
                        for(index in foregrounds.size until monoBitmap.size){
                            val color = ColorScheme(foregrounds[index -1].colors[0], false).accentColor
                            foregrounds.add(fgSize, ColorCustomInfo(ColorCustomInfo.COLOR_TYPE_NORMAL,  intArrayOf(color)))
                        }
                    }
                }
                if(monoBitmap.isNotEmpty()){
                    realmono = true
                }
            }
            if(monoBitmap.isEmpty() && mono){
                monoBitmap.add(sourceBitmap)
                // 取图标主色调
                if(entryColor != 0xFF123456.toInt() && entryColor != Color.TRANSPARENT){
                    if(background.isEmpty()
                        ||  background[0].colors[0].alpha == 0
                        || (background[0].colors[0].alpha == 255
                                && ColorUtils.calculateContrast(entryColor,
                            background[0].colors[0]) > 1.3f)){
                        foregrounds.clear()
                        foregrounds.add(ColorCustomInfo(ColorCustomInfo.COLOR_TYPE_NORMAL,
                            intArrayOf(entryColor)))
                    }
                }
            }
            if(monoBitmap.isEmpty()){
                if (sourceDrawable is AdaptiveIconDrawableCompat) {
                    val drawableCompat = sourceDrawable
                    drawableCompat.monochrome?.let{Utilities.createIconBitmap(
                        drawableCompat.monochrome, 1f, context)}?.let { monoBitmap.add(it) }
                    if(monoBitmap.isNotEmpty()){
                        realmono = true
                    }else {
                        drawableCompat.foreground?.let{Utilities.createIconBitmap(
                            drawableCompat.foreground, 1f, context)}?.let { monoBitmap.add(it) }
                    }
                }
            }

            var useSourceColor = checkColorTypeSourceCfg(context, cn, entryColor, foregrounds, background, monoBitmap)
            Log.i(TAG, "processForegroundLayers: ${cn?.packageName} drawableList.size${(drawableList?.size ?: 0)}" )
            //内置的mono，如果颜色是Source，就不上色处理
            var internalMono = useSourceColor && (cn?.packageName == context.packageName
                    || (drawableList?.size ?: 0) > 1)

            iconColorBean?.let{
                val colorScheme = it.icon_colors.random()
                foregrounds.clear()
                foregrounds.addAll(colorScheme.fgColors)
                if(foregrounds.size <= 3){
                    val color = foregrounds[0].colors[0]
                    val t = IntArray(1)
                    t[0] = ColorScheme(color, false).accent1[4]
                    foregrounds.add(
                        ColorCustomInfo(
                            ColorCustomInfo.COLOR_TYPE_NORMAL,
                            t
                        )
                    )
                }
                background.clear()
                background.addAll(colorScheme.bgColors)
            }

            while(foregrounds.size < monoBitmap.size){
                val color = foregrounds[foregrounds.lastIndex].colors[0]
                val t = IntArray(1)
                t[0] = ColorScheme(color, true).accentColor
                foregrounds.add(
                    ColorCustomInfo(
                        ColorCustomInfo.COLOR_TYPE_NORMAL,
                        t
                    )
                )
            }

            return ProcessedForeground(
                monoBitmap,
                ArrayList(foregrounds),
                ArrayList(background),
                internalMono,
                useSourceColor
            )
        }
    }

     fun testModeGetIcon(context:Context, sourceDrawable:Drawable, cn :ComponentName?,
                        iconShapeHelper: IconShapeHelper, mono: Boolean, entryColor:Int,
                         fgColors:ArrayList<ColorCustomInfo>,
                        bgColors:ArrayList<ColorCustomInfo>):Bitmap {

         // 使用新的processForegroundLayers方法处理前景图
         val processed = processForegroundLayers(context, sourceDrawable, cn, mono, entryColor, fgColors, bgColors)
         Log.i(TAG, "testModeGetIcon: cn=$cn fgsize=${processed.fgColors.size} bgsize=${processed.bgColors.size}")
         val monoBitmap = processed.foregroundBitmaps
         val foregrounds = processed.fgColors
         val background = processed.bgColors
         val internalMono = processed.internalMono

         synchronized(this){
             val sourceBitmap = Utilities.createIconBitmap(sourceDrawable, 1f, context)
             var sourceColor = Palette.from(sourceBitmap).generate().getDominantColor(Color.WHITE)

             val iconBackOption = iconBackOptions?.let { if (it.size > 0) it.random() else null }
             val iconMask = iconMaskOption
             var icondec = iconDecOptionList?.let { if (it.size > 0) it.random() else null }
             //装饰随机图标上每5个显示装饰
             if(Random.nextFloat() > 0.2f){
                 icondec = null
             }

             if((iconStrokeOption?.strokeType ?: 0) > 0){
                 if(monoBitmap.isEmpty()){
                    iconStrokeOption!!.strokeShape = true
                    if(iconStrokeOption!!.strokeType == IconStrokeOption.STROKE_TYPE_NO_MONO_AUTO_FIT_SOURCE_COLOR){
                        iconStrokeOption!!.strokeColor = sourceColor
                    }else if(iconStrokeOption!!.strokeType == IconStrokeOption.STROKE_TYPE_NO_MONO_AUTO_FIT_COLOR_OPTION){
                        iconStrokeOption!!.autoFitColorOption = true
                    }
                 }else{
                     iconStrokeOption!!.strokeShape = false
                 }
             }

             // iconColorBean处理iconShapeGroup
             iconColorBean?.let{
                 iconShapeGroup.clear()
                 it.icon_shape.split(";").forEach {
                     val shape = PreferenceUtil.shapeStrToShape(it)
                     if(shape != null && shape != AdaptiveIconShape.sNone){
                         iconShapeGroup.add(shape)
                     }
                 }
                 if(iconShapeGroup.isEmpty()){
                     iconShapeGroup.add(AdaptiveIconShape.SQUIRCLE)
                 }
                 Log.i(TAG, "testModeGetIcon: icon_shadow11 = ${it.icon_shadow}")
                 iconShadowOption = it.icon_shadow
             }
             if(iconShapeGroup.isEmpty()){
                 iconShapeGroup.add(AdaptiveIconShape.SQUIRCLE)
             }

             if(iconShadowOption == null){
                 iconShadowOption = IconShadowOption()
             }
             Log.i(TAG, "testModeGetIcon: colors.size=${foregrounds.size} monoBitmap.Size = ${monoBitmap.size} $cn")
             val mIcon = createWallpaperAdapterBitmap(
                 context,
                 sourceBitmap, monoBitmap, internalMono, iconShapeHelper,
                 sourceColor, Color.RED,
                 foregrounds,
                 background[0], iconShapeGroup.random().path,
                 iconBackOption,
                 iconMask, icondec,
                 iconStrokeOption,
                 iconShadowOption!!,
                 iconBgDrawable.randomOrNull(),
                 iconMaskDrawable = iconMaskDrawable,
             )
             return mIcon
         }
    }

    //加上主题上的日历图标处理(日历日期分层)
    private fun getCalendarThemeIcon(context: Context, size:Int): ArrayList<Bitmap> {
        val bitmaps = arrayListOf<Bitmap>()

        val calendarCfg = when (icon_pack_name) {
            INTERNAL_ICON_PACK_1 -> "ip_1_calendar.json"
            INTERNAL_ICON_PACK_2 -> "ip_2_calendar.json"
            INTERNAL_ICON_PACK_3 -> "ip_3_calendar.json"
            INTERNAL_ICON_PACK_4 -> "ip_4_calendar.json"
            INTERNAL_ICON_PACK_5 -> "ip_5_calendar.json"
            INTERNAL_ICON_PACK_6 -> "ip_6_calendar.json"
            INTERNAL_ICON_PACK_7 -> "ip_7_calendar.json"
            INTERNAL_ICON_PACK_9 -> "ip_9_calendar.json"
//            INTERNAL_ICON_PACK_8 -> "ip_7_calendar.json"
            else -> ""
        }
        var cfg: CalendarCfg?=null
        if(calendarCfg.isNotEmpty()){
            runCatching {
                val json = context.assets.open(calendarCfg).use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                cfg = Gson().fromJson(json, CalendarCfg::class.java)
            }
        }
        val copyCfg = cfg
        synchronized(sCanvas) {
            try {
                var bitmap = createBitmap(size, size)
                var temp = createBitmap(100.dp, 100.dp)
                var calendarView = LayoutInflater.from(context)
                        .inflate(R.layout.calendar_theme_adapter_1_layout, null)
                val calendar = Calendar.getInstance()
                val day = calendar[Calendar.DAY_OF_MONTH]
                var textView = calendarView.findViewById<TextView>(R.id.day_month)

                textView?.let{
                    it.text = "$day"
                    cfg?.day?.let{ day ->
                        runCatching {
                            it.setTextColor(day.textColor.toColorInt())
                        }
                        it.textSize = day.textSize.toFloat()
                        val lp = textView.layoutParams as ConstraintLayout.LayoutParams
                        lp.bottomMargin = 0
                        if (day.centerX) {
                            lp.horizontalBias = 0.5f
                        } else {
                            lp.horizontalBias = day.x
                        }
                        if (day.centerY) {
                            lp.verticalBias = 0.5f
                        } else {
                            lp.verticalBias = day.y
                        }
                    }
                }
                val canvas = sCanvas
                canvas.setBitmap(temp)
                calendarView.measure(
                    View.MeasureSpec.makeMeasureSpec(100.dp, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(100.dp, View.MeasureSpec.EXACTLY)
                )
                calendarView.layout(0, 0, calendarView.measuredWidth, calendarView.measuredHeight)
                calendarView.draw(canvas)
                canvas.setBitmap(bitmap)
                canvas.drawBitmap(temp, null, Rect(0, 0, size, size), null)
                if(copyCfg == null || copyCfg.day != null){
                    bitmaps.add(bitmap)
                }
                //星期
                if(copyCfg == null || copyCfg.week != null){
                    bitmap = createBitmap(size, size)
                    calendarView = LayoutInflater.from(context).inflate(R.layout.calendar_theme_adapter_2_layout, null)
                    temp = createBitmap(100.dp, 100.dp)
                    canvas.setBitmap(temp)
                    textView = calendarView.findViewById(R.id.day_week)
                    val simpleDateFormat = SimpleDateFormat("EEE", Locale.getDefault())
                    val week = simpleDateFormat.format(Date())
                    textView.text = week
                    textView?.let{
                        cfg?.week?.let{ week ->
                            runCatching {
                                it.setTextColor(week.textColor.toColorInt())
                            }
                            it.textSize = week.textSize.toFloat()
                            val lp = textView.layoutParams as ConstraintLayout.LayoutParams
                            lp.bottomMargin = 0
                            if (week.centerX) {
                                lp.horizontalBias = 0.5f
                            } else {
                                lp.horizontalBias = week.x
                            }
                            if (week.centerY) {
                                lp.verticalBias = 0.5f
                            } else {
                                lp.verticalBias = week.y
                            }
                        }
                    }
                    calendarView.measure(
                        View.MeasureSpec.makeMeasureSpec(100.dp, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(100.dp, View.MeasureSpec.EXACTLY)
                    )
                    calendarView.layout(0, 0, calendarView.measuredWidth, calendarView.measuredHeight)
                    calendarView.draw(canvas)
                    canvas.setBitmap(bitmap)
                    canvas.drawBitmap(temp, null, Rect(0, 0, size, size), null)
                    bitmaps.add(bitmap)
                }


            } catch (ignored: java.lang.Exception) {
            }
        }
        return bitmaps
    }


    init {
        drawableMap = HashMap()
        icon_pack_name = IconPackPrefUtils.getIconPackName(context)
        Log.i(TAG, "iconPack Name $icon_pack_name ")
        updateDrawableMap()
    }
    fun updateDrawableMap(){
        parseFromXML(context)
        Log.i(TAG, "updateDrawableMap: " + icon_pack_name)
        when(icon_pack_name){
            INTERNAL_ICON_PACK_1 -> {
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_theme").toString(),
                    arrayListOf("ip_1_desktop_theme_1", "ip_1_desktop_theme_2", "ip_1_desktop_theme_3", "ip_1_desktop_theme_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_desktop_theme").toString(),
                    arrayListOf("ip_1_desktop_theme_1", "ip_1_desktop_theme_2", "ip_1_desktop_theme_3", "ip_1_desktop_theme_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icons").toString(),
                    arrayListOf("ip_1_add_icon_1", "ip_1_add_icon_2", "ip_1_add_icon_3", "ip_1_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_add_icon").toString(),
                    arrayListOf("ip_1_add_icon_1", "ip_1_add_icon_2", "ip_1_add_icon_3", "ip_1_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "all_apps_button_icon").toString(),
                    arrayListOf("ip_1_allapps_1", "ip_1_allapps_2", "ip_1_allapps_3", "ip_1_allapps_4", "ip_1_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_allapps").toString(),
                    arrayListOf("ip_1_allapps_1", "ip_1_allapps_2", "ip_1_allapps_3", "ip_1_allapps_4", "ip_1_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "launcher_setting").toString(),
                    arrayListOf("ip_1_setting_1", "ip_1_setting_2", "ip_1_setting_3", "ip_1_setting_4", "ip_1_setting_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_launcher_setting").toString(),
                    arrayListOf("ip_1_setting_1", "ip_1_setting_2", "ip_1_setting_3", "ip_1_setting_4", "ip_1_setting_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_tool_box").toString(),
                    arrayListOf("ip_1_tool_box_tool_box_1", "ip_1_tool_box_tool_box_2", "ip_1_tool_box_tool_box_3", "ip_1_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_tool_box").toString(),
                    arrayListOf("ip_1_tool_box_tool_box_1", "ip_1_tool_box_tool_box_2", "ip_1_tool_box_tool_box_3", "ip_1_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_themed_icon").toString(),
                    arrayListOf("ip_1_ic_themed_icon_1", "ip_1_ic_themed_icon_2", "ip_1_ic_themed_icon_3", "ip_1_ic_themed_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_quick_search").toString(),
                    arrayListOf("ip_1_ic_quick_search_1", "ip_1_ic_quick_search_2", "ip_1_ic_quick_search_3", "ip_1_ic_quick_search_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_tool_box").toString(),
                    arrayListOf("ip_1_tool_box_tool_box_1", "ip_1_tool_box_tool_box_2", "ip_1_tool_box_tool_box_3", "ip_1_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_themed_icon").toString(),
                    arrayListOf("ip_1_ic_themed_icon_1", "ip_1_ic_themed_icon_2", "ip_1_ic_themed_icon_3", "ip_1_ic_themed_icon_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "quick_search").toString(),
                    arrayListOf("ip_1_quick_search")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icon").toString(),
                    arrayListOf("ip_1_ic_add_icon")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "setting").toString(),
                    arrayListOf("ip_1_setting_1", "ip_1_setting_2", "ip_1_setting_3", "ip_1_setting_4", "ip_1_setting_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_prime_guide").toString(),
                    arrayListOf("ip_1_theme_prime_guide_1", "ip_1_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_prime_guide").toString(),
                    arrayListOf("ip_1_theme_prime_guide_1", "ip_1_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_theme_gallery_1").toString(),
                    arrayListOf("ip_1_theme_gallery_1", "ip_1_theme_gallery_2", "ip_1_theme_gallery_3", "ip_1_theme_gallery_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_live_effect").toString(),
                    arrayListOf("ip_1_ic_live_effect_1", "ip_1_ic_live_effect_2", "ip_1_ic_live_effect_3", "ip_1_ic_live_effect_4", "ip_1_ic_live_effect_5")
                )


            }
            INTERNAL_ICON_PACK_2 -> {
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_theme").toString(),
                    arrayListOf("ip_2_desktop_theme_1", "ip_2_desktop_theme_2", "ip_2_desktop_theme_3", "ip_2_desktop_theme_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_desktop_theme").toString(),
                    arrayListOf("ip_2_desktop_theme_1", "ip_2_desktop_theme_2", "ip_2_desktop_theme_3", "ip_2_desktop_theme_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icons").toString(),
                    arrayListOf("ip_2_add_icon_1", "ip_2_add_icon_2", "ip_2_add_icon_3", "ip_2_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_add_icon").toString(),
                    arrayListOf("ip_2_add_icon_1", "ip_2_add_icon_2", "ip_2_add_icon_3", "ip_2_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "all_apps_button_icon").toString(),
                    arrayListOf("ip_2_allapps_1", "ip_2_allapps_2", "ip_2_allapps_3", "ip_2_allapps_4", "ip_2_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_allapps").toString(),
                    arrayListOf("ip_2_allapps_1", "ip_2_allapps_2", "ip_2_allapps_3", "ip_2_allapps_4", "ip_2_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "launcher_setting").toString(),
                    arrayListOf("ip_2_setting_1", "ip_2_setting_2", "ip_2_setting_3", "ip_2_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_launcher_setting").toString(),
                    arrayListOf("ip_2_setting_1", "ip_2_setting_2", "ip_2_setting_3", "ip_2_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_tool_box").toString(),
                    arrayListOf("ip_2_tool_box_tool_box_1", "ip_2_tool_box_tool_box_2", "ip_2_tool_box_tool_box_3", "ip_2_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_tool_box").toString(),
                    arrayListOf("ip_2_tool_box_tool_box_1", "ip_2_tool_box_tool_box_2", "ip_2_tool_box_tool_box_3", "ip_2_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_themed_icon").toString(),
                    arrayListOf("ip_2_ic_themed_icon_1", "ip_2_ic_themed_icon_2", "ip_2_ic_themed_icon_3", "ip_2_ic_themed_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_quick_search").toString(),
                    arrayListOf("ip_2_ic_quick_search_1", "ip_2_ic_quick_search_2", "ip_2_ic_quick_search_3", "ip_2_ic_quick_search_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_tool_box").toString(),
                    arrayListOf("ip_2_tool_box_tool_box_1", "ip_2_tool_box_tool_box_2", "ip_2_tool_box_tool_box_3", "ip_2_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_themed_icon").toString(),
                    arrayListOf("ip_2_ic_themed_icon_1", "ip_2_ic_themed_icon_2", "ip_2_ic_themed_icon_3", "ip_2_ic_themed_icon_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "quick_search").toString(),
                    arrayListOf("ip_2_quick_search")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icon").toString(),
                    arrayListOf("ip_2_ic_add_icon")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "setting").toString(),
                    arrayListOf("ip_2_setting_1", "ip_2_setting_2", "ip_2_setting_3", "ip_2_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_prime_guide").toString(),
                    arrayListOf("ip_2_theme_prime_guide_1", "ip_2_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_prime_guide").toString(),
                    arrayListOf("ip_2_theme_prime_guide_1", "ip_2_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_theme_gallery_1").toString(),
                    arrayListOf("ip_2_theme_gallery_1", "ip_2_theme_gallery_2", "ip_2_theme_gallery_3", "ip_2_theme_gallery_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_live_effect").toString(),
                    arrayListOf("ip_2_ic_live_effect_1", "ip_2_ic_live_effect_2", "ip_2_ic_live_effect_3", "ip_2_ic_live_effect_4", "ip_2_ic_live_effect_5")
                )
            }
            INTERNAL_ICON_PACK_3 -> {
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_theme").toString(),
                    arrayListOf("ip_3_desktop_theme_1", "ip_3_desktop_theme_2", "ip_3_desktop_theme_3", "ip_3_desktop_theme_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_desktop_theme").toString(),
                    arrayListOf("ip_3_desktop_theme_1", "ip_3_desktop_theme_2", "ip_3_desktop_theme_3", "ip_3_desktop_theme_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icons").toString(),
                    arrayListOf("ip_3_add_icon_1", "ip_3_add_icon_2", "ip_3_add_icon_3", "ip_3_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_add_icon").toString(),
                    arrayListOf("ip_3_add_icon_1", "ip_3_add_icon_2", "ip_3_add_icon_3", "ip_3_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "all_apps_button_icon").toString(),
                    arrayListOf("ip_3_allapps_1", "ip_3_allapps_2", "ip_3_allapps_3", "ip_3_allapps_4", "ip_3_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_allapps").toString(),
                    arrayListOf("ip_3_allapps_1", "ip_3_allapps_2", "ip_3_allapps_3", "ip_3_allapps_4", "ip_3_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "launcher_setting").toString(),
                    arrayListOf("ip_3_setting_1", "ip_3_setting_2", "ip_3_setting_3", "ip_3_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_launcher_setting").toString(),
                    arrayListOf("ip_3_setting_1", "ip_3_setting_2", "ip_3_setting_3", "ip_3_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_tool_box").toString(),
                    arrayListOf("ip_3_tool_box_tool_box_1", "ip_3_tool_box_tool_box_2", "ip_3_tool_box_tool_box_3", "ip_3_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_tool_box").toString(),
                    arrayListOf("ip_3_tool_box_tool_box_1", "ip_3_tool_box_tool_box_2", "ip_3_tool_box_tool_box_3", "ip_3_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_themed_icon").toString(),
                    arrayListOf("ip_3_ic_themed_icon_1", "ip_3_ic_themed_icon_2", "ip_3_ic_themed_icon_3", "ip_3_ic_themed_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_quick_search").toString(),
                    arrayListOf("ip_3_ic_quick_search_1", "ip_3_ic_quick_search_2", "ip_3_ic_quick_search_3", "ip_3_ic_quick_search_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_tool_box").toString(),
                    arrayListOf("ip_3_tool_box_tool_box_1", "ip_3_tool_box_tool_box_2", "ip_3_tool_box_tool_box_3", "ip_3_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_themed_icon").toString(),
                    arrayListOf("ip_3_ic_themed_icon_1", "ip_3_ic_themed_icon_2", "ip_3_ic_themed_icon_3", "ip_3_ic_themed_icon_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "quick_search").toString(),
                    arrayListOf("ip_3_quick_search")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icon").toString(),
                    arrayListOf("ip_3_ic_add_icon")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "setting").toString(),
                    arrayListOf("ip_3_setting_1", "ip_3_setting_2", "ip_3_setting_3", "ip_3_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_prime_guide").toString(),
                    arrayListOf("ip_3_theme_prime_guide_1", "ip_3_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_prime_guide").toString(),
                    arrayListOf("ip_3_theme_prime_guide_1", "ip_3_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_theme_gallery_1").toString(),
                    arrayListOf("ip_3_theme_gallery_1", "ip_3_theme_gallery_2", "ip_3_theme_gallery_3", "ip_3_theme_gallery_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_live_effect").toString(),
                    arrayListOf("ip_3_ic_live_effect_1", "ip_3_ic_live_effect_2", "ip_3_ic_live_effect_3", "ip_3_ic_live_effect_4", "ip_3_ic_live_effect_5")
                )
            }
            INTERNAL_ICON_PACK_4 -> {
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_theme").toString(),
                    arrayListOf("ip_4_desktop_theme_1", "ip_4_desktop_theme_2", "ip_4_desktop_theme_3", "ip_4_desktop_theme_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_desktop_theme").toString(),
                    arrayListOf("ip_4_desktop_theme_1", "ip_4_desktop_theme_2", "ip_4_desktop_theme_3", "ip_4_desktop_theme_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icons").toString(),
                    arrayListOf("ip_4_add_icon_1", "ip_4_add_icon_2", "ip_4_add_icon_3", "ip_4_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_add_icon").toString(),
                    arrayListOf("ip_4_add_icon_1", "ip_4_add_icon_2", "ip_4_add_icon_3", "ip_4_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "all_apps_button_icon").toString(),
                    arrayListOf("ip_4_allapps_1", "ip_4_allapps_2", "ip_4_allapps_3", "ip_4_allapps_4", "ip_4_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_allapps").toString(),
                    arrayListOf("ip_4_allapps_1", "ip_4_allapps_2", "ip_4_allapps_3", "ip_4_allapps_4", "ip_4_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "launcher_setting").toString(),
                    arrayListOf("ip_4_setting_1", "ip_4_setting_2", "ip_4_setting_3", "ip_4_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_launcher_setting").toString(),
                    arrayListOf("ip_4_setting_1", "ip_4_setting_2", "ip_4_setting_3", "ip_4_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_tool_box").toString(),
                    arrayListOf("ip_4_tool_box_tool_box_1", "ip_4_tool_box_tool_box_2", "ip_4_tool_box_tool_box_3", "ip_4_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_tool_box").toString(),
                    arrayListOf("ip_4_tool_box_tool_box_1", "ip_4_tool_box_tool_box_2", "ip_4_tool_box_tool_box_3", "ip_4_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_themed_icon").toString(),
                    arrayListOf("ip_4_ic_themed_icon_1", "ip_4_ic_themed_icon_2", "ip_4_ic_themed_icon_3", "ip_4_ic_themed_icon_4", "ip_4_ic_themed_icon_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_quick_search").toString(),
                    arrayListOf("ip_4_ic_quick_search_1", "ip_4_ic_quick_search_2", "ip_4_ic_quick_search_3", "ip_4_ic_quick_search_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_tool_box").toString(),
                    arrayListOf("ip_4_tool_box_tool_box_1", "ip_4_tool_box_tool_box_2", "ip_4_tool_box_tool_box_3", "ip_4_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_themed_icon").toString(),
                    arrayListOf("ip_4_ic_themed_icon_1", "ip_4_ic_themed_icon_2", "ip_4_ic_themed_icon_3", "ip_4_ic_themed_icon_4", "ip_4_ic_themed_icon_5")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "quick_search").toString(),
                    arrayListOf("ip_4_quick_search")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icon").toString(),
                    arrayListOf("ip_4_ic_add_icon")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "setting").toString(),
                    arrayListOf("ip_4_setting_1", "ip_4_setting_2", "ip_4_setting_3", "ip_4_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_prime_guide").toString(),
                    arrayListOf("ip_4_theme_prime_guide_1", "ip_4_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_prime_guide").toString(),
                    arrayListOf("ip_4_theme_prime_guide_1", "ip_4_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_theme_gallery_1").toString(),
                    arrayListOf("ip_4_theme_gallery_1", "ip_4_theme_gallery_2", "ip_4_theme_gallery_3", "ip_4_theme_gallery_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_live_effect").toString(),
                    arrayListOf("ip_4_ic_live_effect_1", "ip_4_ic_live_effect_2", "ip_4_ic_live_effect_3", "ip_4_ic_live_effect_4", "ip_4_ic_live_effect_5")
                )
            }
            INTERNAL_ICON_PACK_5 -> {
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_theme").toString(),
                    arrayListOf("ip_5_desktop_theme_1", "ip_5_desktop_theme_2", "ip_5_desktop_theme_3", "ip_5_desktop_theme_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_desktop_theme").toString(),
                    arrayListOf("ip_5_desktop_theme_1", "ip_5_desktop_theme_2", "ip_5_desktop_theme_3", "ip_5_desktop_theme_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icons").toString(),
                    arrayListOf("ip_5_add_icon_1", "ip_5_add_icon_2", "ip_5_add_icon_3", "ip_5_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_add_icon").toString(),
                    arrayListOf("ip_5_add_icon_1", "ip_5_add_icon_2", "ip_5_add_icon_3", "ip_5_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "all_apps_button_icon").toString(),
                    arrayListOf("ip_5_allapps_1", "ip_5_allapps_2", "ip_5_allapps_3", "ip_5_allapps_4", "ip_5_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_allapps").toString(),
                    arrayListOf("ip_5_allapps_1", "ip_5_allapps_2", "ip_5_allapps_3", "ip_5_allapps_4", "ip_5_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "launcher_setting").toString(),
                    arrayListOf("ip_5_setting_1", "ip_5_setting_2", "ip_5_setting_3", "ip_5_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_launcher_setting").toString(),
                    arrayListOf("ip_5_setting_1", "ip_5_setting_2", "ip_5_setting_3", "ip_5_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_tool_box").toString(),
                    arrayListOf("ip_5_tool_box_tool_box_1", "ip_5_tool_box_tool_box_2", "ip_5_tool_box_tool_box_3", "ip_5_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_tool_box").toString(),
                    arrayListOf("ip_5_tool_box_tool_box_1", "ip_5_tool_box_tool_box_2", "ip_5_tool_box_tool_box_3", "ip_5_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_themed_icon").toString(),
                    arrayListOf("ip_5_ic_themed_icon_1", "ip_5_ic_themed_icon_2", "ip_5_ic_themed_icon_3", "ip_5_ic_themed_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_quick_search").toString(),
                    arrayListOf("ip_5_ic_quick_search_1", "ip_5_ic_quick_search_2", "ip_5_ic_quick_search_3", "ip_5_ic_quick_search_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_tool_box").toString(),
                    arrayListOf("ip_5_tool_box_tool_box_1", "ip_5_tool_box_tool_box_2", "ip_5_tool_box_tool_box_3", "ip_5_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_themed_icon").toString(),
                    arrayListOf("ip_5_ic_themed_icon_1", "ip_5_ic_themed_icon_2", "ip_5_ic_themed_icon_3", "ip_5_ic_themed_icon_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "quick_search").toString(),
                    arrayListOf("ip_5_quick_search")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icon").toString(),
                    arrayListOf("ip_5_ic_add_icon")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "setting").toString(),
                    arrayListOf("ip_5_setting_1", "ip_5_setting_2", "ip_5_setting_3", "ip_5_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_prime_guide").toString(),
                    arrayListOf("ip_5_theme_prime_guide_1", "ip_5_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_prime_guide").toString(),
                    arrayListOf("ip_5_theme_prime_guide_1", "ip_5_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_theme_gallery_1").toString(),
                    arrayListOf("ip_5_theme_gallery_1", "ip_5_theme_gallery_2", "ip_5_theme_gallery_3", "ip_5_theme_gallery_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_live_effect").toString(),
                    arrayListOf("ip_5_ic_live_effect_1", "ip_5_ic_live_effect_2", "ip_5_ic_live_effect_3", "ip_5_ic_live_effect_4", "ip_5_ic_live_effect_5")
                )
            }
            INTERNAL_ICON_PACK_6 -> {
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_theme").toString(),
                    arrayListOf("ip_6_desktop_theme_1", "ip_6_desktop_theme_2", "ip_6_desktop_theme_3", "ip_6_desktop_theme_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_desktop_theme").toString(),
                    arrayListOf("ip_6_desktop_theme_1", "ip_6_desktop_theme_2", "ip_6_desktop_theme_3", "ip_6_desktop_theme_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icons").toString(),
                    arrayListOf("ip_6_add_icon_1", "ip_6_add_icon_2", "ip_6_add_icon_3", "ip_6_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_add_icon").toString(),
                    arrayListOf("ip_6_add_icon_1", "ip_6_add_icon_2", "ip_6_add_icon_3", "ip_6_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "all_apps_button_icon").toString(),
                    arrayListOf("ip_6_allapps_1", "ip_6_allapps_2", "ip_6_allapps_3", "ip_6_allapps_4", "ip_6_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_allapps").toString(),
                    arrayListOf("ip_6_allapps_1", "ip_6_allapps_2", "ip_6_allapps_3", "ip_6_allapps_4", "ip_6_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "launcher_setting").toString(),
                    arrayListOf("ip_6_setting_1", "ip_6_setting_2", "ip_6_setting_3", "ip_6_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_launcher_setting").toString(),
                    arrayListOf("ip_6_setting_1", "ip_6_setting_2", "ip_6_setting_3", "ip_6_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_tool_box").toString(),
                    arrayListOf("ip_6_tool_box_tool_box_1", "ip_6_tool_box_tool_box_2", "ip_6_tool_box_tool_box_3", "ip_6_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_tool_box").toString(),
                    arrayListOf("ip_6_tool_box_tool_box_1", "ip_6_tool_box_tool_box_2", "ip_6_tool_box_tool_box_3", "ip_6_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_themed_icon").toString(),
                    arrayListOf("ip_6_ic_themed_icon_1", "ip_6_ic_themed_icon_2", "ip_6_ic_themed_icon_3", "ip_6_ic_themed_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_quick_search").toString(),
                    arrayListOf("ip_6_ic_quick_search_1", "ip_6_ic_quick_search_2", "ip_6_ic_quick_search_3", "ip_6_ic_quick_search_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_tool_box").toString(),
                    arrayListOf("ip_6_tool_box_tool_box_1", "ip_6_tool_box_tool_box_2", "ip_6_tool_box_tool_box_3", "ip_6_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_themed_icon").toString(),
                    arrayListOf("ip_6_ic_themed_icon_1", "ip_6_ic_themed_icon_2", "ip_6_ic_themed_icon_3", "ip_6_ic_themed_icon_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "quick_search").toString(),
                    arrayListOf("ip_6_quick_search")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icon").toString(),
                    arrayListOf("ip_6_ic_add_icon")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "setting").toString(),
                    arrayListOf("ip_6_setting_1", "ip_6_setting_2", "ip_6_setting_3", "ip_6_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_prime_guide").toString(),
                    arrayListOf("ip_6_theme_prime_guide_1", "ip_6_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_prime_guide").toString(),
                    arrayListOf("ip_6_theme_prime_guide_1", "ip_6_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_theme_gallery_1").toString(),
                    arrayListOf("ip_6_theme_gallery_1", "ip_6_theme_gallery_2", "ip_6_theme_gallery_3", "ip_6_theme_gallery_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_live_effect").toString(),
                    arrayListOf("ip_6_ic_live_effect_1", "ip_6_ic_live_effect_2", "ip_6_ic_live_effect_3", "ip_6_ic_live_effect_4", "ip_6_ic_live_effect_5")
                )
            }
            INTERNAL_ICON_PACK_7 -> {
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_theme").toString(),
                    arrayListOf("ip_7_desktop_theme_1", "ip_7_desktop_theme_2", "ip_7_desktop_theme_3", "ip_7_desktop_theme_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_desktop_theme").toString(),
                    arrayListOf("ip_7_desktop_theme_1", "ip_7_desktop_theme_2", "ip_7_desktop_theme_3", "ip_7_desktop_theme_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icons").toString(),
                    arrayListOf("ip_7_add_icon_1", "ip_7_add_icon_2", "ip_7_add_icon_3", "ip_7_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_add_icon").toString(),
                    arrayListOf("ip_7_add_icon_1", "ip_7_add_icon_2", "ip_7_add_icon_3", "ip_7_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "all_apps_button_icon").toString(),
                    arrayListOf("ip_7_allapps_1", "ip_7_allapps_2", "ip_7_allapps_3", "ip_7_allapps_4", "ip_7_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_allapps").toString(),
                    arrayListOf("ip_7_allapps_1", "ip_7_allapps_2", "ip_7_allapps_3", "ip_7_allapps_4", "ip_7_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "launcher_setting").toString(),
                    arrayListOf("ip_7_setting_1", "ip_7_setting_2", "ip_7_setting_3", "ip_7_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_launcher_setting").toString(),
                    arrayListOf("ip_7_setting_1", "ip_7_setting_2", "ip_7_setting_3", "ip_7_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_tool_box").toString(),
                    arrayListOf("ip_7_tool_box_tool_box_1", "ip_7_tool_box_tool_box_2", "ip_7_tool_box_tool_box_3", "ip_7_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_tool_box").toString(),
                    arrayListOf("ip_7_tool_box_tool_box_1", "ip_7_tool_box_tool_box_2", "ip_7_tool_box_tool_box_3", "ip_7_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_themed_icon").toString(),
                    arrayListOf("ip_7_ic_themed_icon_1", "ip_7_ic_themed_icon_2", "ip_7_ic_themed_icon_3", "ip_7_ic_themed_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_quick_search").toString(),
                    arrayListOf("ip_7_ic_quick_search_1", "ip_7_ic_quick_search_2", "ip_7_ic_quick_search_3", "ip_7_ic_quick_search_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_tool_box").toString(),
                    arrayListOf("ip_7_tool_box_tool_box_1", "ip_7_tool_box_tool_box_2", "ip_7_tool_box_tool_box_3", "ip_7_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_themed_icon").toString(),
                    arrayListOf("ip_7_ic_themed_icon_1", "ip_7_ic_themed_icon_2", "ip_7_ic_themed_icon_3", "ip_7_ic_themed_icon_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "quick_search").toString(),
                    arrayListOf("ip_7_quick_search")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icon").toString(),
                    arrayListOf("ip_7_ic_add_icon")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "setting").toString(),
                    arrayListOf("ip_7_setting_1", "ip_7_setting_2", "ip_7_setting_3", "ip_7_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_prime_guide").toString(),
                    arrayListOf("ip_7_theme_prime_guide_1", "ip_7_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_prime_guide").toString(),
                    arrayListOf("ip_7_theme_prime_guide_1", "ip_7_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_theme_gallery_1").toString(),
                    arrayListOf("ip_7_theme_gallery_1", "ip_7_theme_gallery_2", "ip_7_theme_gallery_3", "ip_7_theme_gallery_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_live_effect").toString(),
                    arrayListOf("ip_7_ic_live_effect_1", "ip_7_ic_live_effect_2", "ip_7_ic_live_effect_3", "ip_7_ic_live_effect_4", "ip_7_ic_live_effect_5")
                )
            }
            INTERNAL_ICON_PACK_8 -> {
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_theme").toString(),
                    arrayListOf("ip_8_desktop_theme_1", "ip_8_desktop_theme_2", "ip_8_desktop_theme_3", "ip_8_desktop_theme_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_desktop_theme").toString(),
                    arrayListOf("ip_8_desktop_theme_1", "ip_8_desktop_theme_2", "ip_8_desktop_theme_3", "ip_8_desktop_theme_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icons").toString(),
                    arrayListOf("ip_8_add_icon_1", "ip_8_add_icon_2", "ip_8_add_icon_3", "ip_8_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_add_icon").toString(),
                    arrayListOf("ip_8_add_icon_1", "ip_8_add_icon_2", "ip_8_add_icon_3", "ip_8_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "all_apps_button_icon").toString(),
                    arrayListOf("ip_8_allapps_1", "ip_8_allapps_2", "ip_8_allapps_3", "ip_8_allapps_4", "ip_8_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_allapps").toString(),
                    arrayListOf("ip_8_allapps_1", "ip_8_allapps_2", "ip_8_allapps_3", "ip_8_allapps_4", "ip_8_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "launcher_setting").toString(),
                    arrayListOf("ip_8_setting_1", "ip_8_setting_2", "ip_8_setting_3", "ip_8_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_launcher_setting").toString(),
                    arrayListOf("ip_8_setting_1", "ip_8_setting_2", "ip_8_setting_3", "ip_8_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_tool_box").toString(),
                    arrayListOf("ip_8_tool_box_tool_box_1", "ip_8_tool_box_tool_box_2", "ip_8_tool_box_tool_box_3", "ip_8_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_tool_box").toString(),
                    arrayListOf("ip_8_tool_box_tool_box_1", "ip_8_tool_box_tool_box_2", "ip_8_tool_box_tool_box_3", "ip_8_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_themed_icon").toString(),
                    arrayListOf("ip_8_ic_themed_icon_1", "ip_8_ic_themed_icon_2", "ip_8_ic_themed_icon_3", "ip_8_ic_themed_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_quick_search").toString(),
                    arrayListOf("ip_8_ic_quick_search_1", "ip_8_ic_quick_search_2", "ip_8_ic_quick_search_3", "ip_8_ic_quick_search_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_tool_box").toString(),
                    arrayListOf("ip_8_tool_box_tool_box_1", "ip_8_tool_box_tool_box_2", "ip_8_tool_box_tool_box_3", "ip_8_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_themed_icon").toString(),
                    arrayListOf("ip_8_ic_themed_icon_1", "ip_8_ic_themed_icon_2", "ip_8_ic_themed_icon_3", "ip_8_ic_themed_icon_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "quick_search").toString(),
                    arrayListOf("ip_8_quick_search")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icon").toString(),
                    arrayListOf("ip_8_ic_add_icon")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "setting").toString(),
                    arrayListOf("ip_8_setting_1", "ip_8_setting_2", "ip_8_setting_3", "ip_8_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_prime_guide").toString(),
                    arrayListOf("ip_8_theme_prime_guide_1", "ip_8_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_prime_guide").toString(),
                    arrayListOf("ip_8_theme_prime_guide_1", "ip_8_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_theme_gallery_1").toString(),
                    arrayListOf("ip_8_theme_gallery_1", "ip_8_theme_gallery_2", "ip_8_theme_gallery_3", "ip_8_theme_gallery_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_live_effect").toString(),
                    arrayListOf("ip_8_ic_live_effect_1", "ip_8_ic_live_effect_2", "ip_8_ic_live_effect_3", "ip_8_ic_live_effect_4", "ip_8_ic_live_effect_5")
                )
            }
            INTERNAL_ICON_PACK_9 -> {
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_theme").toString(),
                    arrayListOf("ip_9_desktop_theme_1", "ip_9_desktop_theme_2", "ip_9_desktop_theme_3", "ip_9_desktop_theme_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_desktop_theme").toString(),
                    arrayListOf("ip_9_desktop_theme_1", "ip_9_desktop_theme_2", "ip_9_desktop_theme_3", "ip_9_desktop_theme_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icons").toString(),
                    arrayListOf("ip_9_add_icon_1", "ip_9_add_icon_2", "ip_9_add_icon_3", "ip_9_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_add_icon").toString(),
                    arrayListOf("ip_9_add_icon_1", "ip_9_add_icon_2", "ip_9_add_icon_3", "ip_9_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "all_apps_button_icon").toString(),
                    arrayListOf("ip_9_allapps_1", "ip_9_allapps_2", "ip_9_allapps_3", "ip_9_allapps_4", "ip_9_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_allapps").toString(),
                    arrayListOf("ip_9_allapps_1", "ip_9_allapps_2", "ip_9_allapps_3", "ip_9_allapps_4", "ip_9_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "launcher_setting").toString(),
                    arrayListOf("ip_9_setting_1", "ip_9_setting_2", "ip_9_setting_3", "ip_9_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_launcher_setting").toString(),
                    arrayListOf("ip_9_setting_1", "ip_9_setting_2", "ip_9_setting_3", "ip_9_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_tool_box").toString(),
                    arrayListOf("ip_9_tool_box_tool_box_1", "ip_9_tool_box_tool_box_2", "ip_9_tool_box_tool_box_3", "ip_9_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_tool_box").toString(),
                    arrayListOf("ip_9_tool_box_tool_box_1", "ip_9_tool_box_tool_box_2", "ip_9_tool_box_tool_box_3", "ip_9_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_themed_icon").toString(),
                    arrayListOf("ip_9_ic_themed_icon_1", "ip_9_ic_themed_icon_2", "ip_9_ic_themed_icon_3", "ip_9_ic_themed_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_quick_search").toString(),
                    arrayListOf("ip_9_ic_quick_search_1", "ip_9_ic_quick_search_2", "ip_9_ic_quick_search_3", "ip_9_ic_quick_search_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_tool_box").toString(),
                    arrayListOf("ip_9_tool_box_tool_box_1", "ip_9_tool_box_tool_box_2", "ip_9_tool_box_tool_box_3", "ip_9_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_themed_icon").toString(),
                    arrayListOf("ip_9_ic_themed_icon_1", "ip_9_ic_themed_icon_2", "ip_9_ic_themed_icon_3", "ip_9_ic_themed_icon_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "quick_search").toString(),
                    arrayListOf("ip_9_quick_search")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icon").toString(),
                    arrayListOf("ip_9_ic_add_icon")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "setting").toString(),
                    arrayListOf("ip_9_setting_1", "ip_9_setting_2", "ip_9_setting_3", "ip_9_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_prime_guide").toString(),
                    arrayListOf("ip_9_theme_prime_guide_1", "ip_9_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_prime_guide").toString(),
                    arrayListOf("ip_9_theme_prime_guide_1", "ip_9_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_theme_gallery_1").toString(),
                    arrayListOf("ip_9_theme_gallery_1", "ip_9_theme_gallery_2", "ip_9_theme_gallery_3", "ip_9_theme_gallery_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_live_effect").toString(),
                    arrayListOf("ip_9_ic_live_effect_1", "ip_9_ic_live_effect_2", "ip_9_ic_live_effect_3", "ip_9_ic_live_effect_4", "ip_9_ic_live_effect_5")
                )
            }
            INTERNAL_ICON_PACK_ROSE_NO_DEC -> {
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_theme").toString(),
                    arrayListOf("ip_1_wp_desktop_theme_1", "ip_1_wp_desktop_theme_2", "ip_1_wp_desktop_theme_3", "ip_1_wp_desktop_theme_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_desktop_theme").toString(),
                    arrayListOf("ip_1_wp_desktop_theme_1", "ip_1_wp_desktop_theme_2", "ip_1_wp_desktop_theme_3", "ip_1_wp_desktop_theme_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icons").toString(),
                    arrayListOf("ip_1_wp_add_icon_1", "ip_1_wp_add_icon_2", "ip_1_wp_add_icon_3", "ip_1_wp_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_add_icon").toString(),
                    arrayListOf("ip_1_wp_add_icon_1", "ip_1_wp_add_icon_2", "ip_1_wp_add_icon_3", "ip_1_wp_add_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "all_apps_button_icon").toString(),
                    arrayListOf("ip_1_wp_allapps_1", "ip_1_wp_allapps_2", "ip_1_wp_allapps_3", "ip_1_wp_allapps_4", "ip_1_wp_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_allapps").toString(),
                    arrayListOf("ip_1_wp_allapps_1", "ip_1_wp_allapps_2", "ip_1_wp_allapps_3", "ip_1_wp_allapps_4", "ip_1_wp_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "launcher_setting").toString(),
                    arrayListOf("ip_1_wp_setting_1", "ip_1_wp_setting_2", "ip_1_wp_setting_3", "ip_1_wp_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_launcher_setting").toString(),
                    arrayListOf("ip_1_wp_setting_1", "ip_1_wp_setting_2", "ip_1_wp_setting_3", "ip_1_wp_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_tool_box").toString(),
                    arrayListOf("ip_1_wp_tool_box_tool_box_1", "ip_1_wp_tool_box_tool_box_2", "ip_1_wp_tool_box_tool_box_3", "ip_1_wp_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_tool_box").toString(),
                    arrayListOf("ip_1_wp_tool_box_tool_box_1", "ip_1_wp_tool_box_tool_box_2", "ip_1_wp_tool_box_tool_box_3", "ip_1_wp_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_themed_icon").toString(),
                    arrayListOf("ip_1_wp_ic_themed_icon_1", "ip_1_wp_ic_themed_icon_2", "ip_1_wp_ic_themed_icon_3", "ip_1_wp_ic_themed_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_quick_search").toString(),
                    arrayListOf("ip_1_wp_ic_quick_search_1", "ip_1_wp_ic_quick_search_2", "ip_1_wp_ic_quick_search_3", "ip_1_wp_ic_quick_search_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_tool_box").toString(),
                    arrayListOf("ip_1_wp_tool_box_tool_box_1", "ip_1_wp_tool_box_tool_box_2", "ip_1_wp_tool_box_tool_box_3", "ip_1_wp_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_themed_icon").toString(),
                    arrayListOf("ip_1_wp_ic_themed_icon_1", "ip_1_wp_ic_themed_icon_2", "ip_1_wp_ic_themed_icon_3", "ip_1_wp_ic_themed_icon_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "quick_search").toString(),
                    arrayListOf("ip_1_wp_quick_search")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icon").toString(),
                    arrayListOf("ip_1_wp_ic_add_icon")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "setting").toString(),
                    arrayListOf("ip_1_wp_setting_1", "ip_1_wp_setting_2", "ip_1_wp_setting_3", "ip_1_wp_setting_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_prime_guide").toString(),
                    arrayListOf("ip_1_wp_theme_prime_guide_1", "ip_1_wp_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_prime_guide").toString(),
                    arrayListOf("ip_1_wp_theme_prime_guide_1", "ip_1_wp_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_theme_gallery_1").toString(),
                    arrayListOf("ip_1_wp_theme_gallery_1", "ip_1_wp_theme_gallery_2", "ip_1_wp_theme_gallery_3", "ip_1_wp_theme_gallery_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_live_effect").toString(),
                    arrayListOf("ip_1_wp_ic_live_effect_1", "ip_1_wp_ic_live_effect_2", "ip_1_wp_ic_live_effect_3", "ip_1_wp_ic_live_effect_4", "ip_1_wp_ic_live_effect_5")
                )
            }
            else ->{
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_theme").toString(),
                    arrayListOf("wp_desktop_theme_1", "wp_desktop_theme_2", "wp_desktop_theme_3", "wp_desktop_theme_4",
                        "wp_desktop_theme_5", "wp_desktop_theme_6", "wp_desktop_theme_7")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_desktop_theme").toString(),
                    arrayListOf("wp_desktop_theme_1", "wp_desktop_theme_2", "wp_desktop_theme_3", "wp_desktop_theme_4",
                        "wp_desktop_theme_5", "wp_desktop_theme_6", "wp_desktop_theme_7")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icons").toString(),
                    arrayListOf("wp_add_icon_1", "wp_add_icon_2", "wp_add_icon_3", "wp_add_icon_4", "wp_add_icon_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "all_apps_button_icon").toString(),
                    arrayListOf("wp_allapps_1", "wp_allapps_2", "wp_allapps_3", "wp_allapps_4", "wp_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_allapps").toString(),
                    arrayListOf("wp_allapps_1", "wp_allapps_2", "wp_allapps_3", "wp_allapps_4", "wp_allapps_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "launcher_setting").toString(),
                    arrayListOf("wp_setting_1", "wp_setting_2", "wp_setting_3", "wp_setting_4", "wp_setting_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_launcher_setting").toString(),
                    arrayListOf("wp_setting_1", "wp_setting_2", "wp_setting_3", "wp_setting_4", "wp_setting_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_tool_box").toString(),
                    arrayListOf("wp_tool_box_tool_box_1", "wp_tool_box_tool_box_2", "wp_tool_box_tool_box_3", "wp_tool_box_tool_box_4",
                        "wp_tool_box_tool_box_5",
                        "wp_tool_box_tool_box_6",
                        "wp_tool_box_tool_box_7")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "desktop_tool_box").toString(),
                    arrayListOf("wp_tool_box_tool_box_1", "wp_tool_box_tool_box_2", "wp_tool_box_tool_box_3", "wp_tool_box_tool_box_4",
                        "wp_tool_box_tool_box_5",
                        "wp_tool_box_tool_box_6",
                        "wp_tool_box_tool_box_7"
                    )
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_themed_icon").toString(),
                    arrayListOf("wp_ic_themed_icon_1", "wp_ic_themed_icon_2", "wp_ic_themed_icon_3", "wp_ic_themed_icon_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_quick_search").toString(),
                    arrayListOf("wp_ic_quick_search_1", "wp_ic_quick_search_2", "wp_ic_quick_search_3", "wp_ic_quick_search_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_tool_box").toString(),
                    arrayListOf("wp_tool_box_tool_box_1", "wp_tool_box_tool_box_2", "wp_tool_box_tool_box_3", "wp_tool_box_tool_box_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_themed_icon").toString(),
                    arrayListOf("wp_ic_themed_icon_1", "wp_ic_themed_icon_2", "wp_ic_themed_icon_3", "wp_ic_themed_icon_4")
                )

                drawableMap.put(
                    ComponentName(context.packageName, "quick_search").toString(),
                    arrayListOf("wp_quick_search")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_add_icon").toString(),
                    arrayListOf("wp_ic_add_icon")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_add_icon").toString(),
                    arrayListOf("wp_ic_add_icon_1", "wp_ic_add_icon_2", "wp_ic_add_icon_3", "wp_ic_add_icon_4", "wp_ic_add_icon_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "setting").toString(),
                    arrayListOf("wp_setting_1", "wp_setting_2", "wp_setting_3", "wp_setting_4", "wp_setting_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "ic_prime_guide").toString(),
                    arrayListOf("wp_theme_prime_guide_1", "wp_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_prime_guide").toString(),
                    arrayListOf("wp_theme_prime_guide_1", "wp_theme_prime_guide_2")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_theme_gallery_1").toString(),
                    arrayListOf("wp_theme_gallery_1", "wp_theme_gallery_2", "wp_theme_gallery_3", "wp_theme_gallery_4"
                        , "wp_theme_gallery_5", "wp_theme_gallery_6", "wp_theme_gallery_7")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_live_effect").toString(),
                    arrayListOf("wp_ic_live_effect_1", "wp_ic_live_effect_2", "wp_ic_live_effect_3", "wp_ic_live_effect_4", "wp_ic_live_effect_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_marquee_effect").toString(),
                    arrayListOf("wp_ic_marquee_effect_1", "wp_ic_marquee_effect_2", "wp_ic_marquee_effect_3", "wp_ic_marquee_effect_4")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_photo_effect").toString(),
                    arrayListOf("wp_ic_photo_effect_1", "wp_ic_photo_effect_2", "wp_ic_photo_effect_3", "wp_ic_photo_effect_4", "wp_ic_photo_effect_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_ic_live_wallpaper").toString(),
                    arrayListOf("wp_ic_live_wallpaper_1", "wp_ic_live_wallpaper_2", "wp_ic_live_wallpaper_3", "wp_ic_live_wallpaper_4", "wp_ic_live_wallpaper_5")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "wp_theme_wallpaper_wall").toString(),
                    arrayListOf("wp_theme_wallpaper_wall_1", "wp_theme_wallpaper_wall_2", "wp_theme_wallpaper_wall_3")
                )
                drawableMap.put(
                    ComponentName(context.packageName, "l_theme_wallpaper_wall").toString(),
                    arrayListOf("wp_theme_wallpaper_wall_1", "wp_theme_wallpaper_wall_2", "wp_theme_wallpaper_wall_3")
                )

            }
        }
        if(iconForegroundFileName.isNotEmpty()){
            drawableMap.put(
                ComponentName(context.packageName, "desktop_theme").toString(),
                arrayListOf("desktop_theme_1", "desktop_theme_2", "desktop_theme_3", "desktop_theme_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "wp_desktop_theme").toString(),
                arrayListOf("desktop_theme_1", "desktop_theme_2", "desktop_theme_3", "desktop_theme_4")
            )

            drawableMap.put(
                ComponentName(context.packageName, "ic_add_icons").toString(),
                arrayListOf("add_icon_1", "add_icon_2", "add_icon_3", "add_icon_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "wp_ic_add_icon").toString(),
                arrayListOf("add_icon_1", "add_icon_2", "add_icon_3", "add_icon_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "all_apps_button_icon").toString(),
                arrayListOf("allapps_1", "allapps_2", "allapps_3", "allapps_4", "allapps_5")
            )
            drawableMap.put(
                ComponentName(context.packageName, "wp_allapps").toString(),
                arrayListOf("allapps_1", "allapps_2", "allapps_3", "allapps_4", "allapps_5")
            )
            drawableMap.put(
                ComponentName(context.packageName, "launcher_setting").toString(),
                arrayListOf("setting_1", "setting_2", "setting_3", "setting_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "wp_launcher_setting").toString(),
                arrayListOf("setting_1", "setting_2", "setting_3", "setting_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "wp_ic_tool_box").toString(),
                arrayListOf("ic_tool_box_tool_box_1", "ic_tool_box_tool_box_2", "ic_tool_box_tool_box_3", "ic_tool_box_tool_box_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "desktop_tool_box").toString(),
                arrayListOf("ic_tool_box_tool_box_1", "ic_tool_box_tool_box_2", "ic_tool_box_tool_box_3", "ic_tool_box_tool_box_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "ic_themed_icon").toString(),
                arrayListOf("ic_themed_icon_1", "ic_themed_icon_2", "ic_themed_icon_3", "ic_themed_icon_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "wp_quick_search").toString(),
                arrayListOf("ic_quick_search_1", "ic_quick_search_2", "ic_quick_search_3", "ic_quick_search_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "ic_tool_box_tool_box").toString(),
                arrayListOf("ic_tool_box_tool_box_1", "ic_tool_box_tool_box_2", "ic_tool_box_tool_box_3", "ic_tool_box_tool_box_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "wp_ic_themed_icon").toString(),
                arrayListOf("ic_themed_icon_1", "ic_themed_icon_2", "ic_themed_icon_3", "ic_themed_icon_4")
            )

            drawableMap.put(
                ComponentName(context.packageName, "wp_quick_search").toString(),
                arrayListOf("l_quick_search_1", "l_quick_search_2", "l_quick_search_3", "l_quick_search_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "ic_add_icon").toString(),
                arrayListOf("add_icon_1", "add_icon_2", "add_icon_3", "add_icon_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "setting").toString(),
                arrayListOf("setting_1", "setting_2", "setting_3", "setting_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "ic_prime_guide").toString(),
                arrayListOf("l_theme_prime_guide_1", "l_theme_prime_guide_2")
            )
            drawableMap.put(
                ComponentName(context.packageName, "wp_ic_prime_guide").toString(),
                arrayListOf("l_theme_prime_guide_1", "l_theme_prime_guide_2")
            )
            drawableMap.put(
                ComponentName(context.packageName, "wp_theme_gallery").toString(),
                arrayListOf("l_theme_gallery_1", "l_theme_gallery_2", "l_theme_gallery_3", "l_theme_gallery_4")
            )
            drawableMap.put(
                ComponentName(context.packageName, "wp_ic_live_effect").toString(),
                arrayListOf("ic_live_effect_1", "ic_live_effect_2", "ic_live_effect_3", "ic_live_effect_4", "ic_live_effect_5")
            )
            drawableMap.put(
                ComponentName(context.packageName, "wp_theme_wallpaper_wall").toString(),
                arrayListOf("wp_theme_wallpaper_wall_1", "wp_theme_wallpaper_wall_2", "wp_theme_wallpaper_wall_3")
            )
            drawableMap.put(
                ComponentName(context.packageName, "l_theme_wallpaper_wall").toString(),
                arrayListOf("l_theme_wallpaper_wall_1", "l_theme_wallpaper_wall_2", "l_theme_wallpaper_wall_3")
            )
        }
    }
}