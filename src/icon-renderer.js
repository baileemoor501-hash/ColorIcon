// 图标渲染引擎
// 负责将图标分层图片与颜色配置合成为最终的Canvas

(function (global) {
  class IconRenderer {
    constructor() {
      this.canvasCache = new Map(); // Canvas缓存
      this.maxCacheSize = 50; // 最大缓存数量
    }

    /**
     * 创建离屏Canvas
     * @param {number} width - 宽度
     * @param {number} height - 高度
     * @returns {HTMLCanvasElement}
     */
    createOffscreenCanvas(width, height) {
      const canvas = document.createElement('canvas');
      canvas.width = width;
      canvas.height = height;
      return canvas;
    }

    /**
     * 将颜色配置应用到图层
     * @param {HTMLCanvasElement} canvas - 目标Canvas
     * @param {Image} image - 源图片
     * @param {object} colorConfig - 颜色配置
     */
    applyColorToLayer(canvas, image, colorConfig) {
      const ctx = canvas.getContext('2d');
      const w = canvas.width;
      const h = canvas.height;

      // 清空Canvas
      ctx.clearRect(0, 0, w, h);

      // 绘制原始图片
      ctx.drawImage(image, 0, 0, w, h);

      // 获取图片数据
      const imageData = ctx.getImageData(0, 0, w, h);
      const data = imageData.data;

      // 根据颜色类型应用着色
      const type = colorConfig.type || 'color_normal';

      if (type === 'color_normal') {
        // 纯色填充
        // 从 ARGB 整数或 HEX 字符串中提取颜色和 alpha 值
        let color, colorAlpha;
        const colorValue = colorConfig.colors[0];
        if (typeof colorValue === 'number') {
          // ARGB 整数格式
          color = ColorUtils.fromArgbInt(colorValue);
          colorAlpha = color.a;
        } else {
          // HEX 字符串格式（向后兼容）
          color = ColorUtils.fromHex(colorValue || '#000000');
          colorAlpha = color.a;
        }

        for (let i = 0; i < data.length; i += 4) {
          const alpha = data[i + 3];
          if (alpha > 0) {
            data[i] = color.r;
            data[i + 1] = color.g;
            data[i + 2] = color.b;
            // 应用颜色的 alpha 值
            data[i + 3] = Math.round(alpha * colorAlpha);
          }
        }
      } else if (type === 'line_gradient' || type === 'radial_gradient' || type === 'sweep_gradient') {
        // 渐变填充：使用渐变遮罩
        // 先保存原始alpha通道
        const alphaChannel = new Uint8ClampedArray(w * h);
        for (let i = 0; i < data.length; i += 4) {
          alphaChannel[i / 4] = data[i + 3];
        }

        // 清空并绘制渐变
        ctx.clearRect(0, 0, w, h);
        this.applyGradientToCanvas(ctx, colorConfig, w, h);

        // 恢复alpha通道（使用原始图片的alpha作为遮罩）
        const gradientData = ctx.getImageData(0, 0, w, h);
        const gData = gradientData.data;
        for (let i = 0; i < gData.length; i += 4) {
          gData[i + 3] = alphaChannel[i / 4];
        }
        ctx.putImageData(gradientData, 0, 0);
        return;
      }

      // 应用修改后的图片数据
      ctx.putImageData(imageData, 0, 0);
    }

    /**
     * 将渐变应用到Canvas
     * @param {CanvasRenderingContext2D} ctx - Canvas上下文
     * @param {object} config - 渐变配置
     * @param {number} width - Canvas宽度
     * @param {number} height - Canvas高度
     */
    applyGradientToCanvas(ctx, config, width, height) {
      const type = config.type;
      const colors = config.colors || [];
      const positions = config.positions || [];

      let gradient;

      if (type === 'line_gradient') {
        // 线性渐变 - 修复角度：0度从上往下，加180度偏移
        const angle = ((config.angle || 0) + 180) * Math.PI / 180;
        const centerX = width / 2;
        const centerY = height / 2;
        const length = Math.max(width, height);
        const x0 = centerX - Math.cos(angle) * length / 2;
        const y0 = centerY - Math.sin(angle) * length / 2;
        const x1 = centerX + Math.cos(angle) * length / 2;
        const y1 = centerY + Math.sin(angle) * length / 2;

        gradient = ctx.createLinearGradient(x0, y0, x1, y1);
      } else if (type === 'radial_gradient') {
        // 径向渐变
        const xOffset = config.xOffset || 0.5;
        const yOffset = config.yOffset || 0.5;
        const radial = config.radial || 0.5;
        const cx = width * xOffset;
        const cy = height * yOffset;
        const radius = Math.max(width, height) * radial;

        gradient = ctx.createRadialGradient(cx, cy, 0, cx, cy, radius);
      } else if (type === 'sweep_gradient') {
        // 圆锥渐变（conic gradient）
        const xOffset = config.xOffset || 0.5;
        const yOffset = config.yOffset || 0.5;
        const startAngle = ((config.sweepStart || 0) - 90) * Math.PI / 180; // 调整起始角度
        const cx = width * xOffset;
        const cy = height * yOffset;

        gradient = ctx.createConicGradient(startAngle, cx, cy);
      }

      // 添加颜色停止点
      for (let i = 0; i < colors.length; i++) {
        const colorValue = colors[i];
        const pos = positions[i] || (i / (colors.length - 1));

        // 将 ARGB 整数或 HEX 字符串转换为 CSS 颜色字符串
        let colorStr;
        if (typeof colorValue === 'number') {
          // ARGB 整数格式
          const rgba = ColorUtils.fromArgbInt(colorValue);
          colorStr = ColorUtils.toRgbString(rgba);
        } else {
          // HEX 字符串格式（向后兼容）
          colorStr = colorValue;
        }

        gradient.addColorStop(pos, colorStr);
      }

      // 填充渐变
      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, width, height);
    }

    /**
     * 渲染Shape背景
     * @param {object} shape - Shape对象 { pathData, viewBoxWidth, viewBoxHeight }
     * @param {object} colorConfig - 背景颜色配置
     * @param {number} size - 输出尺寸
     * @returns {HTMLCanvasElement}
     */
    renderShapeBackground(shape, colorConfig, size) {
      const canvas = this.createOffscreenCanvas(size, size);
      const ctx = canvas.getContext('2d');

      try {
        // 创建Path2D对象
        const path2d = new Path2D(shape.pathData);

        // 设置坐标变换：将viewBox映射到canvas尺寸
        const viewBoxWidth = shape.viewBoxWidth || 100;
        const viewBoxHeight = shape.viewBoxHeight || 100;
        const scaleX = size / viewBoxWidth;
        const scaleY = size / viewBoxHeight;
        const scale = Math.min(scaleX, scaleY); // 保持宽高比
        const offsetX = (size - viewBoxWidth * scale) / 2;
        const offsetY = (size - viewBoxHeight * scale) / 2;

        ctx.translate(offsetX, offsetY);
        ctx.scale(scale, scale);

        // 根据颜色类型应用颜色
        const type = colorConfig.type || 'color_normal';

        if (type === 'color_normal') {
          // 纯色填充
          let color, colorAlpha;
          const colorValue = colorConfig.colors[0];
          if (typeof colorValue === 'number') {
            color = ColorUtils.fromArgbInt(colorValue);
            colorAlpha = color.a;
          } else {
            color = ColorUtils.fromHex(colorValue || '#000000');
            colorAlpha = color.a;
          }

          const fillStyle = `rgba(${color.r}, ${color.g}, ${color.b}, ${colorAlpha})`;
          console.log('Filling shape with color:', fillStyle);
          ctx.fillStyle = fillStyle;
          ctx.fill(path2d);
        } else if (type === 'line_gradient' || type === 'radial_gradient' || type === 'sweep_gradient') {
          // 渐变填充
          // 使用shape作为裁剪路径
          ctx.save();
          ctx.clip(path2d);

          // 重置变换后绘制渐变
          ctx.setTransform(1, 0, 0, 1, 0, 0);
          this.applyGradientToCanvas(ctx, colorConfig, size, size);
          ctx.restore();
        }

        return canvas;
      } catch (error) {
        console.error('Error rendering shape background:', error);
        // 返回空canvas作为降级
        return canvas;
      }
    }

    /**
     * 合成图标（背景 + 前景层）
     * @param {object} icon - 图标数据（来自IconLoader）
     * @param {object} colorConfig - 颜色配置 { fg: [], bg: [] }
     * @param {number} size - 输出尺寸（默认108）
     * @param {object} shape - 可选的Shape对象 { pathData, viewBoxWidth, viewBoxHeight }，用于替换默认背景
     * @returns {HTMLCanvasElement}
     */
    compositeIcon(icon, colorConfig, size = 108, shape = null) {
      // 处理颜色配置(颜色生成+全局pre_method)
      const processedConfig = this.processColorConfig(icon.name, colorConfig);

      // 生成缓存键（包含shape ID以确保shape变化时重新渲染）
      const shapeKey = shape ? `_shape_${shape.id}` : '';
      const cacheKey = `${icon.name}_${JSON.stringify(processedConfig)}_${size}${shapeKey}`;

      // 检查缓存
      if (this.canvasCache.has(cacheKey)) {
        return this.canvasCache.get(cacheKey);
      }

      // 创建主Canvas
      const mainCanvas = this.createOffscreenCanvas(size, size);
      const mainCtx = mainCanvas.getContext('2d');

      // 1. 绘制背景层并应用bg颜色（支持数组）
      if (shape && shape.pathData) {
        // 使用Shape背景
        console.log('Using shape background:', shape.id);
        const bgConfigs = processedConfig.bg || [{
          type: 'color_normal',
          colors: ['#000000'],
          positions: [0]
        }];

        // 依次绘制所有背景层（使用Shape）
        for (let i = 0; i < bgConfigs.length; i++) {
          const bgCanvas = this.renderShapeBackground(shape, bgConfigs[i], size);
          mainCtx.drawImage(bgCanvas, 0, 0, size, size);
        }
      } else if (icon.background && icon.background.image) {
        // 使用默认图片背景
        const bgConfigs = processedConfig.bg || [{
          type: 'color_normal',
          colors: ['#000000'],
          positions: [0]
        }];

        // 依次绘制所有背景层
        for (let i = 0; i < bgConfigs.length; i++) {
          const bgCanvas = this.createOffscreenCanvas(size, size);
          this.applyColorToLayer(bgCanvas, icon.background.image, bgConfigs[i]);
          mainCtx.drawImage(bgCanvas, 0, 0, size, size);
        }
      }

      // 2. 依次绘制前景层并应用fg颜色
      const fgConfigs = processedConfig.fg || [];
      for (let i = 0; i < icon.layers.length; i++) {
        const layer = icon.layers[i];
        // 如果fg配置不足，使用最后一个配置
        const fgConfig = fgConfigs[i] || fgConfigs[fgConfigs.length - 1] || {
          type: 'color_normal',
          colors: ['#FFFFFF'],
          positions: [0]
        };

        const layerCanvas = this.createOffscreenCanvas(size, size);
        this.applyColorToLayer(layerCanvas, layer.image, fgConfig);
        mainCtx.drawImage(layerCanvas, 0, 0, size, size);
      }

      // 缓存结果
      this.cacheCanvas(cacheKey, mainCanvas);

      return mainCanvas;
    }

    /**
     * 处理颜色配置(颜色生成+全局pre_method)
     * @param {string} iconName - 图标名称
     * @param {object} colorConfig - 原始颜色配置
     * @returns {object} 处理后的颜色配置
     */
    processColorConfig(iconName, colorConfig) {
      // 深拷贝原始配置(避免修改state)
      const config = JSON.parse(JSON.stringify(colorConfig));

      // 步骤1: 处理颜色生成类型
      config.fg = this.expandColorTypes(config.fg || []);
      config.bg = this.expandColorTypes(config.bg || []);

      // 步骤2: 多对多背景色方案选择
      // 如果背景层>1,随机选择一个背景色,与所有前景色组成一个方案
      if (config.bg.length > 1) {
        const seed = this.hashCode(iconName);
        const bgIndex = seed % config.bg.length;
        config.bg = [config.bg[bgIndex]]; // 只保留一个背景层
      }

      // 步骤3: 应用全局pre_method
      this.applyPreMethod(iconName, config);

      return config;
    }

    /**
     * 展开颜色生成类型
     * @param {Array} layers - 颜色层配置数组
     * @returns {Array} 展开后的颜色层数组
     */
    expandColorTypes(layers) {
      const result = [];

      for (const layer of layers) {
        // 颜色生成类型存储在colorTypes数组中,不是type字段
        const colorType = (layer.colorTypes && layer.colorTypes[0]) || 'color_normal';

        if (colorType === 'color_generate_near_contrast' ||
          colorType === 'color_generate_split_3' ||
          colorType === 'color_generate_split_8' ||
          colorType === 'color_generate_split_16') {

          // 只处理第一个颜色,后续丢弃
          const baseColor = layer.colors[0];
          let newColors = [];

          // 根据类型生成颜色
          const hexColor = ColorUtils.hexFromArgbInt(baseColor);

          if (colorType === 'color_generate_near_contrast') {
            newColors = ColorUtils.generateNearContrast(hexColor);
          } else if (colorType === 'color_generate_split_3') {
            newColors = ColorUtils.generateSplit3(hexColor);
          } else if (colorType === 'color_generate_split_8') {
            newColors = ColorUtils.generateSplit8(hexColor);
          } else if (colorType === 'color_generate_split_16') {
            newColors = ColorUtils.generateSplit16(hexColor);
          }

          // 转换为color_normal层
          newColors.forEach(hex => {
            result.push({
              type: 'color_normal', // 强制使用color_normal,因为只有单个颜色
              colors: [ColorUtils.argbIntFromHex(hex)],
              colorTypes: ['color_normal'], // 生成的层都是普通颜色类型
              positions: [0],
              angle: 0,
              radial: 0.5,
              xOffset: 0.5,
              yOffset: 0.5
            });
          });

          // 颜色生成类型: 只处理第一个,后续层忽略
          return result;
        } else {
          result.push(layer);
        }
      }

      return result;
    }

    /**
     * 应用全局颜色处理
     * @param {string} iconName - 图标名称(用作随机种子)
     * @param {object} config - 颜色配置(会被原地修改)
     */
    applyPreMethod(iconName, config) {
      const method = config.pre_method || '';
      const fg = config.fg || [];
      const bg = config.bg || [];

      if (!method || fg.length <= 1) return; // 无需处理

      // 使用图标名作为种子(保证同一图标预览稳定)
      const seed = this.hashCode(iconName);

      if (method === 'keep_first') {
        // 保持第一个,其他随机
        const first = fg[0];
        const rest = fg.slice(1);
        ColorUtils.fisherYatesShuffle(rest, seed);
        config.fg = [first, ...rest];

      } else if (method === 'keep_last') {
        // 保持最后一个,其他随机
        const last = fg[fg.length - 1];
        const rest = fg.slice(0, -1);
        ColorUtils.fisherYatesShuffle(rest, seed);
        config.fg = [...rest, last];

      } else if (method === 'generate') {
        // 所有随机,过滤低对比度
        const filtered = this.filterByContrast(fg, bg);
        ColorUtils.fisherYatesShuffle(filtered, seed);
        config.fg = filtered;

      } else if (method === 'no_bg') {
        // 背景透明
        config.bg = [{
          type: 'color_normal',
          colors: [0x00FFFFFF], // 透明
          positions: [0]
        }];
        // 前景色随机
        ColorUtils.fisherYatesShuffle(fg, seed);
      }
    }

    /**
     * 过滤低对比度颜色组合
     * @param {Array} fgLayers - 前景色层数组
     * @param {Array} bgLayers - 背景色层数组
     * @returns {Array} 过滤后的前景色层数组
     */
    filterByContrast(fgLayers, bgLayers) {
      if (bgLayers.length === 0 || bgLayers[0].colors.length === 0) {
        return fgLayers;
      }

      const bgColor = bgLayers[0].colors[0];
      const alpha = (bgColor >>> 24) & 0xFF;

      if (alpha < 255) {
        // 背景透明,不过滤
        return fgLayers;
      }

      return fgLayers.filter(layer => {
        const fgColor = layer.colors[0];
        const contrast = ColorUtils.calculateContrast(fgColor, bgColor);
        return contrast > 1.2;
      });
    }

    /**
     * 字符串哈希函数(用作随机种子)
     * @param {string} str - 输入字符串
     * @returns {number} 哈希值
     */
    hashCode(str) {
      let hash = 0;
      for (let i = 0; i < str.length; i++) {
        const char = str.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash; // 转换为32位整数
      }
      return Math.abs(hash);
    }

    /**
     * 缓存Canvas（LRU策略）
     * @param {string} key - 缓存键
     * @param {HTMLCanvasElement} canvas - Canvas对象
     */
    cacheCanvas(key, canvas) {
      // 如果缓存已满，删除最旧的
      if (this.canvasCache.size >= this.maxCacheSize) {
        const firstKey = this.canvasCache.keys().next().value;
        this.canvasCache.delete(firstKey);
      }
      this.canvasCache.set(key, canvas);
    }

    /**
     * 清除缓存
     */
    clearCache() {
      this.canvasCache.clear();
    }

    /**
     * 获取缓存大小
     * @returns {number}
     */
    getCacheSize() {
      return this.canvasCache.size;
    }
  }

  // 导出到全局
  global.IconRenderer = IconRenderer;
})(window);
