// 颜色解析与转换工具
// 提供 HEX/RGB/HSL 互转、格式化、明度判断、下载/剪贴板等实用方法

(function (global) {
  const clamp = (v, min, max) => Math.min(max, Math.max(min, v));

  function parseColor(input) {
    if (typeof input !== 'string') return null;
    const s = input.trim().toLowerCase();
    if (s.startsWith('#')) return fromHex(s);
    if (s.startsWith('rgb')) return fromRgbString(s);
    if (s.startsWith('hsl')) return fromHslString(s);
    // 尝试无#八位/六位/三位
    if (/^[0-9a-f]{8}$/i.test(s)) return fromHex('#' + s);
    if (/^[0-9a-f]{6}$/i.test(s)) return fromHex('#' + s);
    if (/^[0-9a-f]{3}$/i.test(s)) return fromHex('#' + s);
    return null;
  }

  function fromHex(hex) {
    let h = hex.replace('#', '');
    if (h.length === 3) h = h.split('').map(c => c + c).join('');

    // 支持 8 位 HEX（#RRGGBBAA）
    if (h.length === 8) {
      const int = parseInt(h, 16);
      const r = (int >> 24) & 255;
      const g = (int >> 16) & 255;
      const b = (int >> 8) & 255;
      const a = (int & 255) / 255;
      return { r, g, b, a };
    }

    // 6 位 HEX（#RRGGBB）
    const int = parseInt(h, 16);
    const r = (int >> 16) & 255;
    const g = (int >> 8) & 255;
    const b = int & 255;
    return { r, g, b, a: 1 };
  }

  function toHex({ r, g, b }) {
    const h = ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
    return '#' + h.toUpperCase();
  }

  function toHex8({ r, g, b, a = 1 }) {
    const A = Math.round(clamp(a, 0, 1) * 255);
    const int = ((r << 24) + (g << 16) + (b << 8) + A) >>> 0;
    const h = int.toString(16).padStart(8, '0');
    return '#' + h.toUpperCase();
  }

  function fromRgbString(str) {
    const m = str.match(/rgba?\(([^)]+)\)/i);
    if (!m) return null;
    const parts = m[1].split(',').map(s => s.trim());
    let [r, g, b, a] = parts.map(Number);
    if (parts[0].endsWith('%')) {
      // 处理百分比形式 rgb(50%, 0%, 100%)
      const pp = parts.map(p => parseFloat(p));
      r = Math.round(pp[0] * 2.55);
      g = Math.round(pp[1] * 2.55);
      b = Math.round(pp[2] * 2.55);
      a = parts[3] != null ? Number(parts[3]) : 1;
    }
    return { r: clamp(Math.round(r), 0, 255), g: clamp(Math.round(g), 0, 255), b: clamp(Math.round(b), 0, 255), a: a == null ? 1 : a };
  }

  function toRgbString({ r, g, b, a = 1 }) {
    if (a !== 1) return `rgba(${r}, ${g}, ${b}, ${Number(a.toFixed(3))})`;
    return `rgb(${r}, ${g}, ${b})`;
  }

  function fromHslString(str) {
    const m = str.match(/hsla?\(([^)]+)\)/i);
    if (!m) return null;
    const parts = m[1].split(',').map(s => s.trim());
    const h = parseFloat(parts[0]);
    const s = parseFloat(parts[1]);
    const l = parseFloat(parts[2]);
    const a = parts[3] != null ? Number(parts[3]) : 1;
    return hslToRgb({ h, s, l, a });
  }

  function toHslString({ r, g, b, a = 1 }) {
    const { h, s, l } = rgbToHsl({ r, g, b });
    if (a !== 1) return `hsla(${Math.round(h)}, ${Math.round(s)}%, ${Math.round(l)}%, ${Number(a.toFixed(3))})`;
    return `hsl(${Math.round(h)}, ${Math.round(s)}%, ${Math.round(l)}%)`;
  }

  function rgbToHsl({ r, g, b }) {
    r /= 255; g /= 255; b /= 255;
    const max = Math.max(r, g, b), min = Math.min(r, g, b);
    let h, s, l = (max + min) / 2;
    if (max === min) {
      h = s = 0;
    } else {
      const d = max - min;
      s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
      switch (max) {
        case r: h = (g - b) / d + (g < b ? 6 : 0); break;
        case g: h = (b - r) / d + 2; break;
        default: h = (r - g) / d + 4; break;
      }
      h *= 60;
    }
    return { h: (h + 360) % 360, s: s * 100, l: l * 100 };
  }

  function hslToRgb({ h, s, l, a = 1 }) {
    const H = ((h % 360) + 360) % 360;
    const S = clamp(s, 0, 100) / 100;
    const L = clamp(l, 0, 100) / 100;
    if (S === 0) {
      const v = Math.round(L * 255);
      return { r: v, g: v, b: v, a };
    }
    const q = L < 0.5 ? L * (1 + S) : L + S - L * S;
    const p = 2 * L - q;
    const tc = [H / 360 + 1 / 3, H / 360, H / 360 - 1 / 3];
    const rgb = tc.map(t => hue2rgb(p, q, t));
    return { r: Math.round(rgb[0] * 255), g: Math.round(rgb[1] * 255), b: Math.round(rgb[2] * 255), a };
  }

  function hue2rgb(p, q, t) {
    if (t < 0) t += 1;
    if (t > 1) t -= 1;
    if (t < 1 / 6) return p + (q - p) * 6 * t;
    if (t < 1 / 2) return q;
    if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
    return p;
  }

  function luminance({ r, g, b }) {
    // sRGB 线性化
    const srgb = [r, g, b].map(v => {
      const c = v / 255;
      return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    });
    return 0.2126 * srgb[0] + 0.7152 * srgb[1] + 0.0722 * srgb[2];
  }

  function isLight(rgb) { return luminance(rgb) > 0.5; }

  function formatAll(rgb) {
    return {
      hex: toHex(rgb),
      rgb: toRgbString(rgb),
      hsl: toHslString(rgb)
    };
  }

  function copyText(text) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      return navigator.clipboard.writeText(text);
    }
    const ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
    return Promise.resolve();
  }

  function downloadFile(filename, content, type = 'text/plain') {
    const blob = new Blob([content], { type });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    URL.revokeObjectURL(url);
  }

  function parseFlexible(input) {
    const rgb = parseColor(input);
    if (rgb) return rgb;
    return fromHex('#6750A4');
  }

  // ========== 新增: RGB/HSV互转 ==========

  /**
   * RGB转HSV
   * @param {object} rgb - {r, g, b} (0-255)
   * @returns {object} {h, s, v} - h: 0-360, s: 0-100, v: 0-100
   */
  function rgbToHsv({ r, g, b }) {
    r /= 255; g /= 255; b /= 255;
    const max = Math.max(r, g, b), min = Math.min(r, g, b);
    const delta = max - min;

    let h = 0, s = 0, v = max;

    if (delta > 0) {
      s = delta / max;
      if (max === r) {
        h = ((g - b) / delta + (g < b ? 6 : 0)) * 60;
      } else if (max === g) {
        h = ((b - r) / delta + 2) * 60;
      } else {
        h = ((r - g) / delta + 4) * 60;
      }
    }

    return { h: (h + 360) % 360, s: s * 100, v: v * 100 };
  }

  /**
   * HSV转RGB
   * @param {object} hsv - {h, s, v} - h: 0-360, s: 0-100, v: 0-100
   * @returns {object} {r, g, b, a} (0-255)
   */
  function hsvToRgb({ h, s, v, a = 1 }) {
    const H = ((h % 360) + 360) % 360;
    const S = clamp(s, 0, 100) / 100;
    const V = clamp(v, 0, 100) / 100;

    const c = V * S;
    const x = c * (1 - Math.abs(((H / 60) % 2) - 1));
    const m = V - c;

    let r = 0, g = 0, b = 0;

    if (H >= 0 && H < 60) { r = c; g = x; b = 0; }
    else if (H >= 60 && H < 120) { r = x; g = c; b = 0; }
    else if (H >= 120 && H < 180) { r = 0; g = c; b = x; }
    else if (H >= 180 && H < 240) { r = 0; g = x; b = c; }
    else if (H >= 240 && H < 300) { r = x; g = 0; b = c; }
    else { r = c; g = 0; b = x; }

    return {
      r: Math.round((r + m) * 255),
      g: Math.round((g + m) * 255),
      b: Math.round((b + m) * 255),
      a
    };
  }

  // ========== 新增: 颜色生成算法 ==========

  /**
   * 生成邻近色+对比色 (color_generate_near_contrast)
   * 规则: 邻近色偏移±25° (绿色系±40°), 对比色偏移180°
   * @param {string} hexColor - HEX颜色字符串
   * @returns {Array<string>} [原色, 邻近色, 对比色]
   */
  function generateNearContrast(hexColor) {
    const rgb = fromHex(hexColor);
    const hsv = rgbToHsv(rgb);

    // 判断绿色系: 色相在95°-145°范围内
    const isGreen = hsv.h >= 95 && hsv.h <= 145;
    const offset = isGreen ? 40 : 25;

    // 随机选择正向或负向偏移
    const direction = Math.random() > 0.5 ? 1 : -1;

    // 生成邻近色
    const nearHsv = {
      h: (hsv.h + direction * offset + 360) % 360,
      s: hsv.s,
      v: hsv.v
    };
    const nearRgb = hsvToRgb(nearHsv);

    // 生成对比色 (色相+180°)
    const contrastHsv = {
      h: (hsv.h + 180) % 360,
      s: hsv.s,
      v: hsv.v
    };
    const contrastRgb = hsvToRgb(contrastHsv);

    return [
      hexColor,
      toHex(nearRgb),
      toHex(contrastRgb)
    ];
  }

  /**
   * 色相分裂成3色 (color_generate_split_3)
   * 规则: 平分360°成3份, 间隔120°
   * @param {string} hexColor - HEX颜色字符串
   * @returns {Array<string>} [色1, 色2, 色3]
   */
  function generateSplit3(hexColor) {
    const rgb = fromHex(hexColor);
    const hsv = rgbToHsv(rgb);

    const colors = [hexColor]; // 第一个是原色

    for (let i = 1; i < 3; i++) {
      const newHsv = {
        h: (hsv.h + 360 / 3 * i) % 360,
        s: hsv.s,
        v: hsv.v
      };
      colors.push(toHex(hsvToRgb(newHsv)));
    }

    return colors;
  }

  /**
   * 色相分裂成8色 (color_generate_split_8)
   * 规则: 平分360°成8份, 间隔45°
   * @param {string} hexColor - HEX颜色字符串
   * @returns {Array<string>} [色1, 色2, ..., 色8]
   */
  function generateSplit8(hexColor) {
    const rgb = fromHex(hexColor);
    const hsv = rgbToHsv(rgb);

    const colors = [hexColor]; // 第一个是原色

    for (let i = 1; i < 8; i++) {
      const newHsv = {
        h: (hsv.h + 360 / 8 * i) % 360,
        s: hsv.s,
        v: hsv.v
      };
      colors.push(toHex(hsvToRgb(newHsv)));
    }

    return colors;
  }

  /**
   * 色相分裂成16色 (color_generate_split_16)
   * 规则: 平分360°成16份, 间隔22.5°
   * @param {string} hexColor - HEX颜色字符串
   * @returns {Array<string>} [色1, 色2, ..., 色16]
   */
  function generateSplit16(hexColor) {
    const rgb = fromHex(hexColor);
    const hsv = rgbToHsv(rgb);

    const colors = [hexColor]; // 第一个是原色

    for (let i = 1; i < 16; i++) {
      const newHsv = {
        h: (hsv.h + 360 / 16 * i) % 360,
        s: hsv.s,
        v: hsv.v
      };
      colors.push(toHex(hsvToRgb(newHsv)));
    }

    return colors;
  }

  // ========== 新增: 对比度计算 ==========

  /**
   * 计算WCAG对比度
   * @param {number} color1 - ARGB整数
   * @param {number} color2 - ARGB整数
   * @returns {number} 对比度 (1.0-21.0)
   */
  function calculateContrast(color1, color2) {
    // 转换为RGB
    let v1 = Number(color1);
    if (v1 < 0) v1 = 0x100000000 + v1;
    const r1 = (v1 >>> 16) & 0xFF;
    const g1 = (v1 >>> 8) & 0xFF;
    const b1 = v1 & 0xFF;

    let v2 = Number(color2);
    if (v2 < 0) v2 = 0x100000000 + v2;
    const r2 = (v2 >>> 16) & 0xFF;
    const g2 = (v2 >>> 8) & 0xFF;
    const b2 = v2 & 0xFF;

    // 计算相对亮度
    const lum1 = luminance({ r: r1, g: g1, b: b1 });
    const lum2 = luminance({ r: r2, g: g2, b: b2 });

    // 对比度公式: (L1 + 0.05) / (L2 + 0.05), 其中L1 > L2
    const lighter = Math.max(lum1, lum2);
    const darker = Math.min(lum1, lum2);

    return (lighter + 0.05) / (darker + 0.05);
  }

  // ========== 新增: Fisher-Yates洗牌 ==========

  /**
   * Fisher-Yates洗牌算法 (带种子的伪随机)
   * @param {Array} array - 要打乱的数组 (原地修改)
   * @param {number} seed - 随机种子
   */
  function fisherYatesShuffle(array, seed) {
    // 简单的伪随机数生成器 (基于seed)
    let rng = seed;
    const random = () => {
      rng = (rng * 9301 + 49297) % 233280;
      return rng / 233280;
    };

    for (let i = array.length - 1; i > 0; i--) {
      const j = Math.floor(random() * (i + 1));
      // 交换元素
      const temp = array[i];
      array[i] = array[j];
      array[j] = temp;
    }
  }

  global.ColorUtils = {
    parseFlexible,
    parseColor,
    fromHex,
    toHex,
    toHex8,
    fromRgbString,
    toRgbString,
    fromHslString,
    toHslString,
    rgbToHsl,
    hslToRgb,
    luminance,
    isLight,
    formatAll,
    copyText,
    downloadFile,
    clamp,
    // 新增: RGB/HSV互转
    rgbToHsv,
    hsvToRgb,
    // 新增: 颜色生成算法
    generateNearContrast,
    generateSplit3,
    generateSplit8,
    generateSplit16,
    // 新增: 对比度计算
    calculateContrast,
    // 新增: Fisher-Yates洗牌
    fisherYatesShuffle,
    // ARGB / HEX / RGB 互转
    toArgbInt: function toArgbInt({ r, g, b, a = 1 }) {
      const A = Math.round(clamp(a, 0, 1) * 255) >>> 0;
      const v = ((A << 24) | (r << 16) | (g << 8) | b) >>> 0;
      // 转为有符号32位
      return v > 0x7FFFFFFF ? v - 0x100000000 : v;
    },
    fromArgbInt: function fromArgbInt(intVal) {
      let v = Number(intVal);
      if (v < 0) v = 0x100000000 + v; // 转无符号
      const a = (v >>> 24) & 0xFF;
      const r = (v >>> 16) & 0xFF;
      const g = (v >>> 8) & 0xFF;
      const b = v & 0xFF;
      return { r, g, b, a: a / 255 };
    },
    hexFromArgbInt: function hexFromArgbInt(intVal) {
      const { r, g, b } = this.fromArgbInt(intVal);
      return toHex({ r, g, b });
    },
    argbIntFromHex: function argbIntFromHex(hex) {
      return this.toArgbInt(fromHex(hex));
    }
  };
})(window);


