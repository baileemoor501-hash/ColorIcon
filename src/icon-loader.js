// 图标资源加载器
// 负责加载和管理图标分层图片资源

(function (global) {
  // 图标文件列表（硬编码，第一版）
  const ICON_FILES = [
    'allapps_1.png', 'allapps_2.png', 'allapps_3.png',
    'ic_tool_box_tool_box_1.png', 'ic_tool_box_tool_box_2.png', 'ic_tool_box_tool_box_3.png',
    'l_theme_browser_1.png', 'l_theme_browser_2.png', 'l_theme_browser_3.png',
    'l_theme_calculator_1.png', 'l_theme_calculator_2.png', 'l_theme_calculator_3.png',
    'l_theme_calendar_1.png', 'l_theme_calendar_2.png', 'l_theme_calendar_3.png',
    'l_theme_camera_1.png', 'l_theme_camera_2.png', 'l_theme_camera_3.png',
    'l_theme_clock_1.png', 'l_theme_clock_2.png', 'l_theme_clock_3.png',
    'l_theme_contacts_1.png', 'l_theme_contacts_2.png', 'l_theme_contacts_3.png',
    'l_theme_downloads_1.png', 'l_theme_downloads_2.png', 'l_theme_downloads_3.png',
    'l_theme_email_1.png', 'l_theme_email_2.png', 'l_theme_email_3.png',
    'l_theme_gallery_1.png', 'l_theme_gallery_2.png', 'l_theme_gallery_3.png',
    'l_theme_phone_1.png', 'l_theme_phone_2.png', 'l_theme_phone_3.png',
    'l_theme_settings_1.png', 'l_theme_settings_2.png',
    'desk_theme_1.png', 'desk_theme_2.png', 'desk_theme_3.png',
    'l_settings_1.png', 'l_settings_2.png',  'l_settings_3.png',
    'l_theme_sms_1.png', 'l_theme_sms_2.png', 'l_theme_sms_3.png'
  ];

  const BACKGROUND_FILE = 'icon_base.png';
  const ICON_FOLDER = '图标分层';

  class IconLoader {
    constructor() {
      this.imageCache = new Map(); // 图片缓存
      this.icons = []; // 解析后的图标数据
    }

    /**
     * 解析文件名，提取图标名称和分层序号
     * @param {string} filename - 文件名（如 "allapps_1.png"）
     * @returns {object|null} - { name, layer, variant } 或 null
     */
    parseIconFile(filename) {
      // 移除扩展名
      const nameWithoutExt = filename.replace(/\.png$/i, '');

      // 匹配模式：{name}_{layer} 或 {name}_{layer}-{variant}
      const match = nameWithoutExt.match(/^(.+?)_(\d+)(?:-(\d+))?$/);

      if (!match) return null;

      return {
        name: match[1],
        layer: parseInt(match[2], 10),
        variant: match[3] ? parseInt(match[3], 10) : 0
      };
    }

    /**
     * 异步加载单个图片
     * @param {string} url - 图片URL
     * @returns {Promise<Image>}
     */
    loadImage(url) {
      // 检查缓存
      if (this.imageCache.has(url)) {
        return Promise.resolve(this.imageCache.get(url));
      }

      return new Promise((resolve, reject) => {
        const img = new Image();
        img.onload = () => {
          this.imageCache.set(url, img);
          resolve(img);
        };
        img.onerror = () => reject(new Error(`Failed to load image: ${url}`));
        img.src = url;
      });
    }

    /**
     * 加载图标包（所有图标资源）
     * @param {string} folderPath - 图标文件夹路径（相对路径）
     * @returns {Promise<Array>} - 图标数据数组
     */
    async loadIconPack(folderPath = ICON_FOLDER) {
      try {
        // 1. 加载背景图
        const bgUrl = `${folderPath}/${BACKGROUND_FILE}`;
        const bgImage = await this.loadImage(bgUrl);

        // 2. 解析文件列表
        const parsedFiles = ICON_FILES.map(filename => {
          const parsed = this.parseIconFile(filename);
          if (!parsed) return null;
          return {
            ...parsed,
            filename,
            url: `${folderPath}/${filename}`
          };
        }).filter(Boolean);

        // 3. 按图标名称分组
        const iconGroups = new Map();
        for (const file of parsedFiles) {
          const key = file.variant > 0 ? `${file.name}-${file.variant}` : file.name;
          if (!iconGroups.has(key)) {
            iconGroups.set(key, {
              name: key,
              displayName: file.name,
              variant: file.variant,
              layers: []
            });
          }
          iconGroups.get(key).layers.push(file);
        }

        // 4. 排序分层并加载图片
        const icons = [];
        for (const [key, iconData] of iconGroups) {
          // 按layer序号排序
          iconData.layers.sort((a, b) => a.layer - b.layer);

          // 加载所有分层图片
          const layerImages = await Promise.all(
            iconData.layers.map(async (layer) => ({
              index: layer.layer,
              url: layer.url,
              image: await this.loadImage(layer.url)
            }))
          );

          icons.push({
            name: iconData.name,
            displayName: iconData.displayName,
            variant: iconData.variant,
            layers: layerImages,
            background: {
              url: bgUrl,
              image: bgImage
            }
          });
        }

        // 5. 按名称排序
        icons.sort((a, b) => a.name.localeCompare(b.name));

        this.icons = icons;
        return icons;
      } catch (error) {
        console.error('Failed to load icon pack:', error);
        throw error;
      }
    }

    /**
     * 获取已加载的图标列表
     * @returns {Array}
     */
    getIcons() {
      return this.icons;
    }

    /**
     * 根据名称获取图标
     * @param {string} name - 图标名称
     * @returns {object|null}
     */
    getIconByName(name) {
      return this.icons.find(icon => icon.name === name) || null;
    }

    /**
     * 从图标包对象加载自定义图标包
     * @param {object} iconPackData - 图标包数据 { icons: [{name, layers: [Blob]}], background: Blob }
     * @returns {Promise<Array>} - 图标数据数组
     */
    async loadIconPackFromData(iconPackData) {
      try {
        console.log('Loading icon pack from data:', iconPackData);

        // 兼容旧格式：{ foregroundLayers: [], background }
        let iconsData = iconPackData.icons;
        if (!iconsData && iconPackData.foregroundLayers) {
          // 旧格式：将所有前景图作为一个图标
          iconsData = [{
            name: iconPackData.name || '自定义图标',
            layers: iconPackData.foregroundLayers
          }];
        }

        // 检查 iconsData 是否有效
        if (!iconsData || !Array.isArray(iconsData)) {
          console.error('Invalid icon pack data structure:', iconPackData);
          throw new Error('图标包数据格式错误：缺少 icons 或 foregroundLayers 字段');
        }

        // 1. 加载背景图
        const bgUrl = URL.createObjectURL(iconPackData.background);
        const bgImage = await this.loadImage(bgUrl);

        // 2. 为每个图标加载其所有层
        const icons = [];
        for (const iconData of iconsData) {
          const layerImages = await Promise.all(
            iconData.layers.map(async (blob, index) => {
              const url = URL.createObjectURL(blob);
              return {
                index: index + 1,
                url: url,
                image: await this.loadImage(url)
              };
            })
          );

          icons.push({
            name: iconData.name,
            displayName: iconData.name,
            variant: 0,
            layers: layerImages,
            background: {
              url: bgUrl,
              image: bgImage
            }
          });
        }

        this.icons = icons;
        return this.icons;
      } catch (error) {
        console.error('Failed to load icon pack from data:', error);
        throw error;
      }
    }

    /**
     * 清除图片缓存
     */
    clearCache() {
      this.imageCache.clear();
    }
  }

  // 导出到全局
  global.IconLoader = IconLoader;
})(window);
