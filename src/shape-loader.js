// Shape加载器
// 负责加载和解析Android Vector XML格式的Shape文件

(function (global) {
  // Shape文件映射表：shapeId -> filename
  const SHAPE_FILE_MAPPING = {
    'square': 'icon_shape_edit_shape_square.xml',
    'square_small_corner': 'icon_shape_edit_square_small_round.xml',
    'circle': 'icon_shape_edit_shape_circle.xml',
    'squircle': 'icon_shape_edit_shape_square_round.xml',
    'round_square': 'icon_shape_edit_shape_round_square.xml',
    'ios_roundsq': 'icon_shape_edit_shape_round_square.xml',
    'teardrop': 'icon_shape_edit_shape_teardrop.xml',
    'hexagon': 'icon_shape_edit_shape_hexagon.xml',
    'amber': 'icon_shape_edit_shape_amber.xml',
    'stamp': 'icon_shape_edit_shape_stamp.xml',
    'octagon': 'icon_shape_edit_shape_octagon.xml',
    'lemon': 'icon_shape_edit_shape_lemon.xml',
    'hive': 'icon_shape_edit_shape_hive.xml',
    'round_pentagon': 'icon_shape_edit_shape_round_pentagon.xml',
    'round_rectangle': 'icon_shape_edit_shape_round_rectangle.xml',
    'heart': 'icon_shape_edit_shape_heart.xml',
    'star': 'icon_shape_edit_shape_star.xml',
    'shape1': 'icon_shape_edit_shape_1.xml',
    'shape2': 'icon_shape_edit_shape_2.xml',
    'shape3': 'icon_shape_edit_shape_3.xml',
    'shape4': 'icon_shape_edit_shape_4.xml',
    'shape5': 'icon_shape_edit_shape_5.xml',
    'shape6': 'icon_shape_edit_shape_6.xml',
    'shape7': 'icon_shape_edit_shape_7.xml',
    'shape8': 'icon_shape_edit_shape_8.xml',
    'shape9': 'icon_shape_edit_shape_9.xml',
    'shape10': 'icon_shape_edit_shape_10.xml',
    'shape11': 'icon_shape_edit_shape_11.xml',
    'shape12': 'icon_shape_edit_shape_12.xml',
    'shape13': 'icon_shape_edit_shape_13.xml',
    'shape14': 'icon_shape_edit_shape_14.xml',
    'shape15': 'icon_shape_edit_shape_15.xml',
    'butterfly': 'icon_shape_edit_shape_butterfly.xml',
    'cloud': 'icon_shape_edit_shape_cloud.xml',
    'fire': 'icon_shape_edit_shape_fire.xml',
    'fried_egg': 'icon_shape_edit_shape_fried_egg.xml',
    'milk': 'icon_shape_edit_shape_milk.xml',
    'mango': 'icon_shape_edit_shape_mango.xml',
    'sugar': 'icon_shape_edit_shape_sugar.xml',
    'toast': 'icon_shape_edit_shape_toast.xml'
  };

  const SHAPE_FOLDER = 'Shape';

  class ShapeLoader {
    constructor() {
      this.shapeCache = new Map(); // 缓存已加载的Shape数据
    }

    /**
     * 加载单个Shape XML文件
     * @param {string} filename - XML文件名
     * @returns {Promise<string>} - XML文本内容
     */
    async loadShapeXml(filename) {
      const url = `${SHAPE_FOLDER}/${filename}`;
      try {
        const response = await fetch(url);
        if (!response.ok) {
          throw new Error(`Failed to load ${filename}: ${response.status}`);
        }
        return await response.text();
      } catch (error) {
        console.error(`Error loading shape XML: ${filename}`, error);
        throw error;
      }
    }

    /**
     * 解析Shape XML，提取pathData
     * @param {string} xmlText - XML文本内容
     * @returns {object} - { pathData: string }
     */
    /**
     * 规范化pathData格式，使其兼容Web SVG
     * @param {string} pathData - Android Vector pathData
     * @returns {string} - 规范化后的pathData
     */
    normalizePathData(pathData) {
      if (!pathData) return pathData;

      // 移除多余的空格
      let normalized = pathData.trim();

      // 在命令字母前后添加空格（如果没有的话）
      normalized = normalized.replace(/([MmLlHhVvCcSsQqTtAaZz])/g, ' $1 ');

      // 在逗号前后添加空格
      normalized = normalized.replace(/,/g, ' , ');

      // 移除多余的空格
      normalized = normalized.replace(/\s+/g, ' ').trim();

      // 确保数字之间有适当的分隔
      // 处理负号：确保负号前有空格
      normalized = normalized.replace(/([0-9])-/g, '$1 -');

      return normalized;
    }

    parseShapeXml(xmlText) {
      try {
        const parser = new DOMParser();
        const xmlDoc = parser.parseFromString(xmlText, 'text/xml');

        // 检查解析错误
        const parserError = xmlDoc.querySelector('parsererror');
        if (parserError) {
          throw new Error('XML parsing error: ' + parserError.textContent);
        }

        // 获取第一个path元素（形状轮廓，不是选中标记）
        const paths = xmlDoc.querySelectorAll('path');
        if (paths.length === 0) {
          throw new Error('No path element found in XML');
        }

        // 第一个path是形状轮廓
        // 注意：XML命名空间属性需要使用getAttributeNS或直接getAttribute
        let pathData = paths[0].getAttribute('android:pathData');
        if (!pathData) {
          // 尝试不带命名空间前缀
          pathData = paths[0].getAttributeNS('http://schemas.android.com/apk/res/android', 'pathData');
        }
        if (!pathData) {
          throw new Error('No pathData attribute found in first path element');
        }

        // 规范化pathData
        pathData = this.normalizePathData(pathData);

        // 提取viewBox尺寸
        const vectorElement = xmlDoc.querySelector('vector');
        let viewBoxWidth = 100;
        let viewBoxHeight = 100;

        if (vectorElement) {
          const widthAttr = vectorElement.getAttribute('android:viewportWidth') ||
                           vectorElement.getAttributeNS('http://schemas.android.com/apk/res/android', 'viewportWidth');
          const heightAttr = vectorElement.getAttribute('android:viewportHeight') ||
                            vectorElement.getAttributeNS('http://schemas.android.com/apk/res/android', 'viewportHeight');

          if (widthAttr) viewBoxWidth = parseFloat(widthAttr);
          if (heightAttr) viewBoxHeight = parseFloat(heightAttr);
        }

        return { pathData, viewBoxWidth, viewBoxHeight };
      } catch (error) {
        console.error('Error parsing shape XML:', error);
        throw error;
      }
    }

    /**
     * 加载单个Shape
     * @param {string} shapeId - Shape ID
     * @returns {Promise<object>} - { id, pathData, name, filename }
     */
    async loadShape(shapeId) {
      // 检查缓存
      if (this.shapeCache.has(shapeId)) {
        return this.shapeCache.get(shapeId);
      }

      const filename = SHAPE_FILE_MAPPING[shapeId];
      if (!filename) {
        throw new Error(`Unknown shape ID: ${shapeId}`);
      }

      try {
        const xmlText = await this.loadShapeXml(filename);
        const { pathData, viewBoxWidth, viewBoxHeight } = this.parseShapeXml(xmlText);

        const shapeData = {
          id: shapeId,
          pathData,
          viewBoxWidth,
          viewBoxHeight,
          name: this.formatShapeName(shapeId),
          filename
        };

        // 缓存
        this.shapeCache.set(shapeId, shapeData);

        return shapeData;
      } catch (error) {
        console.error(`Failed to load shape: ${shapeId}`, error);
        throw error;
      }
    }

    /**
     * 加载所有Shape
     * @returns {Promise<Array>} - Shape数据数组
     */
    async loadAllShapes() {
      const shapeIds = Object.keys(SHAPE_FILE_MAPPING);

      try {
        // 并行加载所有Shape
        const shapes = await Promise.all(
          shapeIds.map(shapeId => this.loadShape(shapeId))
        );

        return shapes;
      } catch (error) {
        console.error('Failed to load all shapes:', error);
        throw error;
      }
    }

    /**
     * 格式化Shape名称（用于显示）
     * @param {string} shapeId - Shape ID
     * @returns {string} - 格式化后的名称
     */
    formatShapeName(shapeId) {
      // 将下划线替换为空格，首字母大写
      return shapeId
        .split('_')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
    }
  }

  // 导出到全局对象
  global.ShapeLoader = ShapeLoader;
})(window);
