# 实现Shape形状编辑项功能
    原有的工具在颜色编辑的基础上将Android功能上的Shape形状编辑项功能实现到图标编辑器中
- 解析Shape文件夹，提取Shape中的path形状，和形状配对

    icon_shape_edit_shape_2和 shape2 对应
    icon_shape_edit_shape_square_round和squircle 对应
    这是一步性操作，保存path 和shape再web端显示使用
    ```xml
    <!-- icon_shape_edit_shape_2.xml -->
    <?xml version="1.0" encoding="utf-8"?>
    <vector xmlns:android="http://schemas.android.com/apk/res/android"
        android:width="48.0dip"
        android:height="48.0dip"
        android:viewportWidth="100.0"
        android:viewportHeight="100.0">
        <group>
            <path
                android:fillColor="@color/ic_shape_color5"
                android:pathData="M 3.89 3.42 C 20.29 6.08 36.56 9.62 52.97 12.06 C 67.72 9.45 82.34 6.10 97.09 3.44 C 94.71 17.48 92.05 31.47 89.54 45.48 C 89.30 46.86 89.23 48.28 89.45 49.67 C 91.99 65.30 94.68 80.91 97.08 96.57 C 82.01 93.86 67.09 90.20 52.00 87.66 C 35.93 90.38 19.96 93.80 3.90 96.59 C 6.86 81.02 10.50 65.58 13.35 50.01 C 10.59 34.42 6.87 18.98 3.89 3.42 Z"/>
                <!-- 提取上面这个path的pathData -->
            <path android:fillColor="@color/icon_shape_edit_check_select"
                android:pathData="M50, 54 L36, 42 l-4,5
                                    L50,64 72,34 l-5, -4 L50,54z" />
        </group>
    </vector>
    ```

    ```java
        public static final String ICON_INTERNAL_SQUARE = "square";
        public static final String ICON_INTERNAL_SQUARE_SMALL_CORNER = "square_small_corner";
        public static final String ICON_INTERNAL_CIRCLE = "circle";
        public static final String ICON_INTERNAL_SQUIRCLE = "squircle";
        public static final String ICON_INTERNAL_ROUND_SQUARE = "round_square";
        public static final String ICON_INTERNAL_IOS_SQUARE = "ios_roundsq";
        public static final String ICON_INTERNAL_TEARDROP = "teardrop";
        public static final String ICON_INTERNAL_HEXAGON = "hexagon";
        public static final String ICON_INTERNAL_AMBER = "amber";
        public static final String ICON_INTERNAL_STAMP = "stamp";
        public static final String ICON_INTERNAL_OCTAGON = "octagon";
        public static final String ICON_INTERNAL_LEMON = "lemon";
        public static final String ICON_INTERNAL_HIVE = "hive";
        public static final String ICON_INTERNAL_ROUND_PENTAGON = "round_pentagon";
        public static final String ICON_INTERNAL_ROUND_RECTANGLE = "round_rectangle";
        public static final String ICON_INTERNAL_HEART = "heart";
        public static final String ICON_INTERNAL_STAR = "star";
        public static final String ICON_INTERNAL_SHAPE1 = "shape1";
        public static final String ICON_INTERNAL_SHAPE2 = "shape2";
        public static final String ICON_INTERNAL_SHAPE3 = "shape3";
        public static final String ICON_INTERNAL_SHAPE4 = "shape4";
        public static final String ICON_INTERNAL_SHAPE5 = "shape5";
        public static final String ICON_INTERNAL_SHAPE6 = "shape6";
        public static final String ICON_INTERNAL_SHAPE7 = "shape7";
        public static final String ICON_INTERNAL_SHAPE8 = "shape8";
        public static final String ICON_INTERNAL_SHAPE9 = "shape9";
        public static final String ICON_INTERNAL_SHAPE10 = "shape10";
        public static final String ICON_INTERNAL_SHAPE11 = "shape11";
        public static final String ICON_INTERNAL_SHAPE12 = "shape12";
        public static final String ICON_INTERNAL_SHAPE13 = "shape13";
        public static final String ICON_INTERNAL_SHAPE14 = "shape14";
        public static final String ICON_INTERNAL_SHAPE15 = "shape15";

        public static final String ICON_INTERNAL_BUTTERFLY = "butterfly";
        public static final String ICON_INTERNAL_CLOUD = "cloud";
        public static final String ICON_INTERNAL_FIRE = "fire";
        public static final String ICON_INTERNAL_FRIED_EGG = "fried_egg";
        public static final String ICON_INTERNAL_MILK = "milk";
        public static final String ICON_INTERNAL_MANGO = "mango";
        public static final String ICON_INTERNAL_SUGAR = "sugar";
        public static final String ICON_INTERNAL_TOAST = "toast";
        public static final String ICON_INTERNAL_STAR_12 = "star_12";
        public static final String ICON_INTERNAL_FOUR_LEAF = "four_leaf";

    shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_squircle), R.drawable.icon_shape_edit_shape_square_round, PreferenceUtil.ICON_INTERNAL_SQUIRCLE));
        shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_rounded_square), R.drawable.icon_shape_edit_shape_round_square, PreferenceUtil.ICON_INTERNAL_ROUND_SQUARE));
        shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_square_small_corners), R.drawable.icon_shape_edit_square_small_round, PreferenceUtil.ICON_INTERNAL_SQUARE_SMALL_CORNER));
        shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_round), R.drawable.icon_shape_edit_shape_circle, PreferenceUtil.ICON_INTERNAL_CIRCLE));
        shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_heart), R.drawable.icon_shape_edit_shape_heart, PreferenceUtil.ICON_INTERNAL_HEART));
        shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_teardrop), R.drawable.icon_shape_edit_shape_teardrop, PreferenceUtil.ICON_INTERNAL_TEARDROP));

        if(ICON_SHAPE_DISPLAY_MODE_1 == mode){
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_hexagon), R.drawable.icon_shape_edit_shape_hexagon, PreferenceUtil.ICON_INTERNAL_HEXAGON));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_4), R.drawable.icon_shape_edit_shape_4, PreferenceUtil.ICON_INTERNAL_SHAPE4));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_amber), R.drawable.icon_shape_edit_shape_amber, PreferenceUtil.ICON_INTERNAL_AMBER));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_stamp), R.drawable.icon_shape_edit_shape_stamp, PreferenceUtil.ICON_INTERNAL_STAMP));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_octagon), R.drawable.icon_shape_edit_shape_octagon, PreferenceUtil.ICON_INTERNAL_OCTAGON));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_lemon), R.drawable.icon_shape_edit_shape_lemon, PreferenceUtil.ICON_INTERNAL_LEMON));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_hive), R.drawable.icon_shape_edit_shape_hive, PreferenceUtil.ICON_INTERNAL_HIVE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_round_pentagon), R.drawable.icon_shape_edit_shape_round_pentagon, PreferenceUtil.ICON_INTERNAL_ROUND_PENTAGON));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_round_rectangle), R.drawable.icon_shape_edit_shape_round_rectangle, PreferenceUtil.ICON_INTERNAL_ROUND_RECTANGLE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_square), R.drawable.icon_shape_edit_shape_square, PreferenceUtil.ICON_INTERNAL_SQUARE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_star), R.drawable.icon_shape_edit_shape_star, PreferenceUtil.ICON_INTERNAL_STAR));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_1), R.drawable.icon_shape_edit_shape_1, PreferenceUtil.ICON_INTERNAL_SHAPE1));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_2), R.drawable.icon_shape_edit_shape_2, PreferenceUtil.ICON_INTERNAL_SHAPE2));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_3), R.drawable.icon_shape_edit_shape_3, PreferenceUtil.ICON_INTERNAL_SHAPE3));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_5), R.drawable.icon_shape_edit_shape_5, PreferenceUtil.ICON_INTERNAL_SHAPE5));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_6), R.drawable.icon_shape_edit_shape_6, PreferenceUtil.ICON_INTERNAL_SHAPE6));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_7), R.drawable.icon_shape_edit_shape_7, PreferenceUtil.ICON_INTERNAL_SHAPE7));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_8), R.drawable.icon_shape_edit_shape_8, PreferenceUtil.ICON_INTERNAL_SHAPE8));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_9), R.drawable.icon_shape_edit_shape_9, PreferenceUtil.ICON_INTERNAL_SHAPE9));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_10), R.drawable.icon_shape_edit_shape_10, PreferenceUtil.ICON_INTERNAL_SHAPE10));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_11), R.drawable.icon_shape_edit_shape_11, PreferenceUtil.ICON_INTERNAL_SHAPE11));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_12), R.drawable.icon_shape_edit_shape_12, PreferenceUtil.ICON_INTERNAL_SHAPE12));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_13), R.drawable.icon_shape_edit_shape_13, PreferenceUtil.ICON_INTERNAL_SHAPE13));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_14), R.drawable.icon_shape_edit_shape_14, PreferenceUtil.ICON_INTERNAL_SHAPE14));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_15), R.drawable.icon_shape_edit_shape_15, PreferenceUtil.ICON_INTERNAL_SHAPE15));
        }else if(ICON_SHAPE_DISPLAY_MODE_2 == mode){

            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_butterfly), R.drawable.icon_shape_edit_shape_butterfly, PreferenceUtil.ICON_INTERNAL_BUTTERFLY));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_cloud), R.drawable.icon_shape_edit_shape_cloud, PreferenceUtil.ICON_INTERNAL_CLOUD));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_fire), R.drawable.icon_shape_edit_shape_fire, PreferenceUtil.ICON_INTERNAL_FIRE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_fried_egg), R.drawable.icon_shape_edit_shape_fried_egg, PreferenceUtil.ICON_INTERNAL_FRIED_EGG));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_milk), R.drawable.icon_shape_edit_shape_milk, PreferenceUtil.ICON_INTERNAL_MILK));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_mango), R.drawable.icon_shape_edit_shape_mango, PreferenceUtil.ICON_INTERNAL_MANGO));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_sugar), R.drawable.icon_shape_edit_shape_sugar, PreferenceUtil.ICON_INTERNAL_SUGAR));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_toast), R.drawable.icon_shape_edit_shape_toast, PreferenceUtil.ICON_INTERNAL_TOAST));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_octagon), R.drawable.icon_shape_edit_shape_octagon, PreferenceUtil.ICON_INTERNAL_OCTAGON));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_lemon), R.drawable.icon_shape_edit_shape_lemon, PreferenceUtil.ICON_INTERNAL_LEMON));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_hive), R.drawable.icon_shape_edit_shape_hive, PreferenceUtil.ICON_INTERNAL_HIVE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_round_rectangle), R.drawable.icon_shape_edit_shape_round_rectangle, PreferenceUtil.ICON_INTERNAL_ROUND_RECTANGLE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_round_pentagon), R.drawable.icon_shape_edit_shape_round_pentagon, PreferenceUtil.ICON_INTERNAL_ROUND_PENTAGON));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_2), R.drawable.icon_shape_edit_shape_2, PreferenceUtil.ICON_INTERNAL_SHAPE2));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_square), R.drawable.icon_shape_edit_shape_square, PreferenceUtil.ICON_INTERNAL_SQUARE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_3), R.drawable.icon_shape_edit_shape_3, PreferenceUtil.ICON_INTERNAL_SHAPE3));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_6), R.drawable.icon_shape_edit_shape_6, PreferenceUtil.ICON_INTERNAL_SHAPE6));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_5), R.drawable.icon_shape_edit_shape_5, PreferenceUtil.ICON_INTERNAL_SHAPE5));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_8), R.drawable.icon_shape_edit_shape_8, PreferenceUtil.ICON_INTERNAL_SHAPE8));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_9), R.drawable.icon_shape_edit_shape_9, PreferenceUtil.ICON_INTERNAL_SHAPE9));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_7), R.drawable.icon_shape_edit_shape_7, PreferenceUtil.ICON_INTERNAL_SHAPE7));
        }else if(ICON_SHAPE_DISPLAY_MODE_3 == mode){
            shapeBeans.clear();
            shapeBeans.add(new IconShapeOption("Default", R.drawable.ic_color_none_empty, PreferenceUtil.ICON_INTERNAL_NONE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_squircle), R.drawable.icon_shape_edit_shape_square_round, PreferenceUtil.ICON_INTERNAL_SQUIRCLE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_rounded_square), R.drawable.icon_shape_edit_shape_round_square, PreferenceUtil.ICON_INTERNAL_ROUND_SQUARE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_square_small_corners), R.drawable.icon_shape_edit_square_small_round, PreferenceUtil.ICON_INTERNAL_SQUARE_SMALL_CORNER));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_round), R.drawable.icon_shape_edit_shape_circle, PreferenceUtil.ICON_INTERNAL_CIRCLE));

            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_square), R.drawable.icon_shape_edit_shape_square, PreferenceUtil.ICON_INTERNAL_SQUARE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_teardrop), R.drawable.icon_shape_edit_shape_teardrop, PreferenceUtil.ICON_INTERNAL_TEARDROP));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_hexagon), R.drawable.icon_shape_edit_shape_hexagon, PreferenceUtil.ICON_INTERNAL_HEXAGON));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_4), R.drawable.icon_shape_edit_shape_4, PreferenceUtil.ICON_INTERNAL_SHAPE4));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_stamp), R.drawable.icon_shape_edit_shape_stamp, PreferenceUtil.ICON_INTERNAL_STAMP));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_lemon), R.drawable.icon_shape_edit_shape_lemon, PreferenceUtil.ICON_INTERNAL_LEMON));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_hive), R.drawable.icon_shape_edit_shape_hive, PreferenceUtil.ICON_INTERNAL_HIVE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_round_rectangle), R.drawable.icon_shape_edit_shape_round_rectangle, PreferenceUtil.ICON_INTERNAL_ROUND_RECTANGLE));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_heart), R.drawable.icon_shape_edit_shape_heart, PreferenceUtil.ICON_INTERNAL_HEART));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_star), R.drawable.icon_shape_edit_shape_star, PreferenceUtil.ICON_INTERNAL_STAR));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_3), R.drawable.icon_shape_edit_shape_3, PreferenceUtil.ICON_INTERNAL_SHAPE3));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_5), R.drawable.icon_shape_edit_shape_5, PreferenceUtil.ICON_INTERNAL_SHAPE5));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_7), R.drawable.icon_shape_edit_shape_7, PreferenceUtil.ICON_INTERNAL_SHAPE7));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_8), R.drawable.icon_shape_edit_shape_8, PreferenceUtil.ICON_INTERNAL_SHAPE8));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_12), R.drawable.icon_shape_edit_shape_12, PreferenceUtil.ICON_INTERNAL_SHAPE12));
            shapeBeans.add(new IconShapeOption(resources.getString(R.string.icon_shape_15), R.drawable.icon_shape_edit_shape_15, PreferenceUtil.ICON_INTERNAL_SHAPE15));


        }
    ```

- 实现path 生成作为图标的背景预览，替代原来的默认图标背景icon_back
- 根据生成的path和shape的数据，在shape编辑功能tab上显示，用户可以多选选择不同的shape，随机在图标上显示
    - shape选项显示4列多行
    - 每个shape选项都有一个图标（通过path显示）和一个名称
    - 图标显示在选项的上面
    - 名称显示在选项的下面
    - 用户可以点击选项来选择shape
- 没有选择shape时，默认显示默认图标背景icon_back
- 保存用户选择的shape到json中，json增加字段 icon_shape，保存对应是shape(squircle;shape2...)，分号隔开多个shape
