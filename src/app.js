(function () {
  const els = {};
  const RECENT_KEY = 'color-tool.recents.v1';
  const STATE_KEY = 'color-tool.state.v1';

  function $(id) { return document.getElementById(id); }

  // 颜色类型选项定义
  const COLOR_TYPE_OPTIONS = [
    { value: 'color_normal', label: 'Normal - 普通颜色' },
    { value: 'color_source', label: 'Source - 使用主色' },
    { value: 'color_generate_near_contrast', label: 'Near Contrast - 邻近色+对比色' },
    { value: 'color_generate_split_3', label: 'Split 3 - 分裂3色' },
    { value: 'color_generate_split_8', label: 'Split 8 - 分裂8色' },
    { value: 'color_generate_split_16', label: 'Split 16 - 分裂16色' }
  ];

  // 获取所有颜色类型选项
  function getColorTypeOptions() {
    return COLOR_TYPE_OPTIONS.map(opt => opt.value);
  }

  // 获取颜色类型的友好显示名称
  function getColorTypeLabel(type) {
    const option = COLOR_TYPE_OPTIONS.find(opt => opt.value === type);
    return option ? option.label : type;
  }

  function initRefs() {
    els.colorText = $('colorText');
    els.colorInput = $('colorInput');
    els.hueSlider = $('hueSlider');
    els.svCanvas = $('svCanvas');
    els.svThumb = $('svThumb');
    els.styleSelect = $('styleSelect');
    els.themeToggle = $('themeToggle');
    els.currentColorPreview = $('currentColorPreview');
    els.currentColorInfo = $('currentColorInfo');
    els.recentColors = $('recentColors');
    els.paletteContainer = $('paletteContainer');
    els.modal = $('modal');
    els.modalClose = $('modalClose');
    els.modalSwatch = $('modalSwatch');
    els.modalInfo = $('modalInfo');
    els.toast = $('toast');
    els.downloadCSS = $('downloadCSS');
    els.downloadJSON = $('downloadJSON');
    els.downloadPNG = $('downloadPNG');
    els.shareLink = $('shareLink');
    els.resetAll = $('resetAll');

    // 渐变编辑器
    els.gradType = $('gradType');
    els.linearAngle = $('linearAngle');
    els.sweepStart = $('sweepStart');
    els.sweepEnd = $('sweepEnd');
    els.radialX = $('radialX');
    els.radialY = $('radialY');
    els.radialSize = $('radialSize');
    els.gradPreset = $('gradPreset');
    els.gradAddStop = $('gradAddStop');
    els.gradReset = $('gradReset');
    els.gradCopyCss = $('gradCopyCss');
    els.gradExportJson = $('gradExportJson');
    els.gradImportJson = $('gradImportJson');
    els.gradImportFile = $('gradImportFile');
    els.gradPreview = $('gradPreview');
    els.gradTrack = $('gradTrack');
    els.gradStops = $('gradStops');
  }

  let state = {
    seed: '#6750A4',
    style: 'TONAL_SPOT',
    dark: false,
    h: 262, s: 60, v: 100, // 画板内部状态（用 HSV 近似）
    currentTab: 'iconEditorTab', // 当前标签页
    gradient: {
      type: 'color_normal',
      angle: 0,
      colors: ['#6750A4', '#4E8CFF'],
      colorTypes: ['color_normal', 'color_normal'],
      positions: [0, 1],
      radial: 0.5,
      xOffset: 0.5,
      yOffset: 0.5,
      sweepStart: 0,
      sweepEnd: 360,
    },
    iconEditor: {
      currentIcon: null,  // 当前选中的图标名称
      icons: [],          // 所有图标数据
      currentIconPackName: '默认图标包', // 当前图标包名称
      currentIconPackUrl: '', // 当前图标包的URL（如果是从网络导入的）
      currentSchemeId: null, // 当前颜色方案ID
      globalColorConfig: { // 全局颜色配置（应用于所有图标）
        fg: [],  // 前景色层配置
        bg: [],  // 背景色层配置（数组结构，与fg保持一致）
        pre_method: '' // 全局颜色处理方法：''(无) | 'invert' | 'multiply' | 'hue_rotate'
      },
      availableShapes: [],  // 所有可用的Shape数据
      selectedShapes: [],   // 用户选中的Shape ID数组
      wallpapers: [],       // 导入的壁纸数组 [{id, name, blob, thumbnail, width, height}]
      selectedWallpaperId: null,  // 当前选中的壁纸ID
      widgets: [],          // Widget数组 [{id, widget_type, widget_name, download_url, preview_blob, widget_zip_blob, selected}]
      currentWidgetId: null, // 当前编辑的Widget ID
      customWidgetTypes: [] // 用户自定义的Widget类型列表
    }
  };

  function loadStateFromUrl() {
    const p = new URLSearchParams(location.search);
    if (p.get('c')) state.seed = '#' + p.get('c');
    if (p.get('style')) state.style = p.get('style');
    if (p.get('dark')) state.dark = p.get('dark') === '1';
  }
  function loadStateFromStorage() {
    try {
      const raw = localStorage.getItem(STATE_KEY);
      if (raw) {
        const loadedState = JSON.parse(raw);

        // 合并state，但保留iconEditor.icons和availableShapes数组
        const savedIcons = state.iconEditor.icons;
        const savedAvailableShapes = state.iconEditor.availableShapes;
        const savedGlobalColorConfig = state.iconEditor.globalColorConfig;

        state = { ...state, ...loadedState };

        // 恢复icons和availableShapes数组（不从localStorage加载）
        if (state.iconEditor) {
          state.iconEditor.icons = savedIcons;
          state.iconEditor.availableShapes = savedAvailableShapes;

          // 如果localStorage中没有globalColorConfig，使用默认值
          if (!state.iconEditor.globalColorConfig ||
            !state.iconEditor.globalColorConfig.fg ||
            state.iconEditor.globalColorConfig.fg.length === 0) {
            state.iconEditor.globalColorConfig = savedGlobalColorConfig;
          }

          // 如果localStorage中没有selectedShapes，使用默认值
          if (!state.iconEditor.selectedShapes) {
            state.iconEditor.selectedShapes = [];
          }

          // 如果localStorage中没有wallpapers，使用默认值（内存临时存储）
          if (!state.iconEditor.wallpapers) {
            state.iconEditor.wallpapers = [];
          }

          // 如果localStorage中没有selectedWallpaperId，使用默认值
          if (state.iconEditor.selectedWallpaperId === undefined) {
            state.iconEditor.selectedWallpaperId = null;
          }

          // 如果localStorage中没有widgets，使用默认值（向后兼容）
          if (!state.iconEditor.widgets) {
            state.iconEditor.widgets = [];
          }

          // 如果localStorage中没有currentWidgetId，使用默认值
          if (state.iconEditor.currentWidgetId === undefined) {
            state.iconEditor.currentWidgetId = null;
          }

          // 如果localStorage中没有customWidgetTypes，使用默认值
          if (!state.iconEditor.customWidgetTypes) {
            state.iconEditor.customWidgetTypes = [];
          }
        }

        // 兼容层：处理旧数据格式
        if (state.iconEditor && state.iconEditor.globalColorConfig) {
          const config = state.iconEditor.globalColorConfig;

          // 如果bg是对象（旧格式），转换为数组
          if (config.bg && !Array.isArray(config.bg)) {
            config.bg = [config.bg];
          }

          // 如果缺少pre_method字段，添加默认值
          if (config.pre_method === undefined) {
            config.pre_method = '';
          }
        }

        // 如果缺少currentIconPackName，添加默认值
        if (state.iconEditor && !state.iconEditor.currentIconPackName) {
          state.iconEditor.currentIconPackName = '默认图标包';
        }
      }
    } catch { }
  }
  function saveState() {
    localStorage.setItem(STATE_KEY, JSON.stringify({
      seed: state.seed,
      style: state.style,
      dark: state.dark,
      currentTab: state.currentTab,
      gradient: state.gradient,
      iconEditor: {
        currentIcon: state.iconEditor.currentIcon,
        currentIconPackName: state.iconEditor.currentIconPackName,
        currentIconPackUrl: state.iconEditor.currentIconPackUrl,
        currentSchemeId: state.iconEditor.currentSchemeId,
        globalColorConfig: state.iconEditor.globalColorConfig,
        selectedShapes: state.iconEditor.selectedShapes,
        customWidgetTypes: state.iconEditor.customWidgetTypes
        // 不保存icons数组和availableShapes，因为太大
      }
    }));
  }

  function showToast(msg) {
    els.toast.textContent = msg; els.toast.classList.add('show');
    setTimeout(() => els.toast.classList.remove('show'), 1600);
  }

  function updatePreview() {
    const rgb = ColorUtils.parseFlexible(state.seed);
    const info = ColorUtils.formatAll(rgb);
    els.currentColorPreview.style.background = info.hex;
    els.currentColorInfo.textContent = `${info.hex} | ${info.rgb} | ${info.hsl}`;
  }

  function pushRecent(hex) {
    let arr = [];
    try { arr = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]'); } catch { }
    arr = [hex, ...arr.filter(x => x !== hex)].slice(0, 16);
    localStorage.setItem(RECENT_KEY, JSON.stringify(arr));
    renderRecents();
  }
  function renderRecents() {
    let arr = [];
    try { arr = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]'); } catch { }
    els.recentColors.innerHTML = '';
    arr.forEach(hex => {
      const d = document.createElement('div');
      d.className = 'recent-item'; d.style.background = hex; d.title = hex;
      d.addEventListener('click', () => setSeed(hex));
      els.recentColors.appendChild(d);
    });
  }

  function setSeed(hex) {
    state.seed = hex;
    els.colorInput.value = hex;
    els.colorText.value = hex;
    updatePreview();
    recalcAndRender();
    pushRecent(hex);
    saveState();
  }

  function recalcAndRender() {
    const scheme = ColorScheme.generateScheme(state.seed, state.style, state.dark);
    renderPalettes(scheme);
    document.body.className = state.dark ? 'theme-dark' : 'theme-light';
    renderGradientEditor();
  }

  function renderPalettes(scheme) {
    const groups = [
      ['accent1', 'Accent 1'],
      ['accent2', 'Accent 2'],
      ['accent3', 'Accent 3'],
      ['neutral1', 'Neutral 1'],
      ['neutral2', 'Neutral 2']
    ];
    els.paletteContainer.innerHTML = '';
    for (const [key, title] of groups) {
      const wrap = document.createElement('div'); wrap.className = 'palette-group';
      const header = document.createElement('div'); header.className = 'palette-group-header';
      const h = document.createElement('div'); h.className = 'palette-title'; h.textContent = `${title}（${key}）`;
      const note = document.createElement('div'); note.className = 'palette-note'; note.textContent = `${scheme[key].length} 色`;
      header.appendChild(h); header.appendChild(note);
      const swatches = document.createElement('div'); swatches.className = 'swatches';
      scheme[key].forEach((hex, idx) => {
        const sw = document.createElement('div'); sw.className = 'swatch'; sw.style.background = hex;
        const rgb = ColorUtils.fromHex(hex);
        sw.classList.add(ColorUtils.isLight(rgb) ? 'is-light' : 'is-dark');
        const info = document.createElement('div'); info.className = 'swatch-info';
        info.textContent = `${idx}`;
        sw.appendChild(info);
        sw.title = `${hex}`;
        sw.addEventListener('click', () => { ColorUtils.copyText(hex).then(() => showToast(`已复制: ${hex}`)); });
        sw.addEventListener('dblclick', () => openModal(hex));
        swatches.appendChild(sw);
      });
      wrap.appendChild(header); wrap.appendChild(swatches);
      els.paletteContainer.appendChild(wrap);
    }
  }

  // ========== 渐变编辑器 ==========
  function renderGradientEditor() {
    // 显示/隐藏类型相关控件
    const type = state.gradient.type;
    toggleGradMode(type);
    // 数值控件赋值
    els.gradType.value = type;
    els.linearAngle.value = state.gradient.angle;
    els.sweepStart.value = state.gradient.sweepStart;
    els.sweepEnd.value = state.gradient.sweepEnd;
    els.radialX.value = state.gradient.xOffset;
    els.radialY.value = state.gradient.yOffset;
    els.radialSize.value = state.gradient.radial;
    // 预览+轨道+列表
    renderGradPreview();
    renderGradTrack();
    renderGradStopsList();
  }

  function toggleGradMode(type) {
    const show = (cls, on) => {
      document.querySelectorAll('.' + cls).forEach(el => el.classList.toggle('show', on));
    };
    show('grad-linear', type === 'line_gradient');
    show('grad-radial', type === 'radial_gradient');
    show('grad-sweep', type === 'sweep_gradient');
  }

  function getCssGradient(opts) {
    const g = state.gradient;
    const stops = g.colors.map((hex, i) => `${hex} ${Math.round(g.positions[i] * 100)}%`);
    if (g.type === 'color_normal') return g.colors[0] || state.seed;
    if (g.type === 'line_gradient') {
      return `linear-gradient(${g.angle}deg, ${stops.join(', ')})`;
    }
    if (g.type === 'radial_gradient') {
      // 使用像素半径，确保在不同容器尺寸下有可见效果
      const radiusPx = opts && opts.radiusPx != null ? Math.max(1, Math.round(opts.radiusPx)) : null;
      const sz = radiusPx != null ? `${radiusPx}px` : `${Math.round(g.radial * 50)}%`;
      const x = (g.xOffset * 100).toFixed(2), y = (g.yOffset * 100).toFixed(2);
      if (x[0] <= 0.0) {
        x = 1;
      }
      if (x[1] <= 0.0) {
        x[1] = 1;
      }
      // 语法：radial-gradient(circle <size> at <pos>, <stops...>)
      return `radial-gradient(circle ${sz} at ${x}% ${y}%, ${stops.join(', ')})`;
    }
    if (g.type === 'sweep_gradient') {
      const start = Number(g.sweepStart) || 0; const end = Number(g.sweepEnd) || 360;
      const range = end - start;
      const angleStops = g.colors.map((hex, i) => {
        const ang = start + g.positions[i] * range;
        return `${hex} ${Math.round(ang)}deg`;
      });
      return `conic-gradient(from ${start}deg at 50% 50%, ${angleStops.join(', ')})`;
    }
    return state.seed;
  }

  function renderGradPreview() {
    const rect = els.gradPreview.getBoundingClientRect();
    const radiusPx = state.gradient.radial * 0.5 * Math.min(rect.width, rect.height);
    els.gradPreview.style.background = getCssGradient({ radiusPx });
    els.gradPreview.style.transform = '';
  }

  function renderGradTrack() {
    const g = state.gradient;
    els.gradTrack.innerHTML = '';
    // 轨道使用自身尺寸计算像素半径，保证显示一致
    const rect = els.gradTrack.getBoundingClientRect();
    const radiusPx = g.radial * 0.5 * Math.min(rect.width, rect.height);
    els.gradTrack.style.background = getCssGradient({ radiusPx });
    g.positions.forEach((pos, i) => {
      const h = document.createElement('div');
      h.className = 'grad-handle';
      h.style.left = `calc(${pos * 100}% - 6px)`;
      h.style.background = g.colors[i];
      h.title = `${Math.round(pos * 100)}%`;
      h.dataset.index = String(i);
      h.addEventListener('mousedown', (ev) => startDragHandle(ev, h));
      els.gradTrack.appendChild(h);
    });
  }

  function startDragHandle(ev, el) {
    const idx = Number(el.dataset.index);
    const rect = els.gradTrack.getBoundingClientRect();
    const move = (e) => {
      const x = ColorUtils.clamp(e.clientX - rect.left, 0, rect.width);
      const p = x / rect.width;
      state.gradient.positions[idx] = Math.min(1, Math.max(0, p));
      renderGradTrack(); renderGradPreview(); renderGradStopsList();
    };
    const up = () => { window.removeEventListener('mousemove', move); window.removeEventListener('mouseup', up); saveState(); };
    window.addEventListener('mousemove', move);
    window.addEventListener('mouseup', up);
  }

  function renderGradStopsList() {
    const g = state.gradient;
    els.gradStops.innerHTML = '';
    g.colors.forEach((hex, i) => {
      const row = document.createElement('div'); row.className = 'grad-stop-row';
      // color
      const colorBox = document.createElement('div'); colorBox.className = 'grad-stop-color';
      const colorInput = document.createElement('input'); colorInput.type = 'color'; colorInput.value = hex;
      colorInput.addEventListener('input', (e) => { g.colors[i] = e.target.value; renderGradTrack(); renderGradPreview(); saveState(); });
      colorBox.appendChild(colorInput);
      // pos
      const posWrap = document.createElement('div'); posWrap.className = 'grad-stop-pos';
      const posRange = document.createElement('input'); posRange.type = 'range'; posRange.min = '0'; posRange.max = '1'; posRange.step = '0.001'; posRange.value = g.positions[i];
      const posNum = document.createElement('input'); posNum.className = 'input'; posNum.type = 'number'; posNum.min = '0'; posNum.max = '1'; posNum.step = '0.001'; posNum.value = g.positions[i];
      const syncPos = (v) => { const val = Math.min(1, Math.max(0, Number(v) || 0)); g.positions[i] = val; posRange.value = val; posNum.value = val; renderGradTrack(); renderGradPreview(); saveState(); };
      posRange.addEventListener('input', (e) => syncPos(e.target.value));
      posNum.addEventListener('change', (e) => syncPos(e.target.value));
      posWrap.appendChild(posRange); posWrap.appendChild(posNum);
      // type select
      const typeSel = document.createElement('select'); typeSel.className = 'input';
      getColorTypeOptions().forEach(t => {
        const opt = document.createElement('option'); opt.value = t; opt.textContent = getColorTypeLabel(t); typeSel.appendChild(opt);
      });
      typeSel.value = g.colorTypes[i] || 'color_normal';
      typeSel.addEventListener('change', (e) => {
        g.colorTypes[i] = e.target.value;
        if (e.target.value === 'color_source') g.colors[i] = state.seed; // 使用当前主色
        renderGradTrack(); renderGradPreview(); saveState();
      });
      // actions
      const act = document.createElement('div'); act.className = 'grad-stop-actions';
      const upBtn = document.createElement('button'); upBtn.className = 'btn btn-secondary'; upBtn.textContent = '上移';
      upBtn.addEventListener('click', () => { if (i > 0) { swapStops(i, i - 1); } });
      const downBtn = document.createElement('button'); downBtn.className = 'btn btn-secondary'; downBtn.textContent = '下移';
      downBtn.addEventListener('click', () => { if (i < g.colors.length - 1) { swapStops(i, i + 1); } });
      const delBtn = document.createElement('button'); delBtn.className = 'btn btn-secondary'; delBtn.textContent = '删除';
      delBtn.addEventListener('click', () => { if (g.colors.length > 2) { g.colors.splice(i, 1); g.positions.splice(i, 1); g.colorTypes.splice(i, 1); renderGradientEditor(); saveState(); } else showToast('至少需要2个色标'); });
      act.appendChild(upBtn); act.appendChild(downBtn); act.appendChild(delBtn);

      row.appendChild(colorBox);
      row.appendChild(posWrap);
      row.appendChild(typeSel);
      const hexInfo = document.createElement('div'); hexInfo.textContent = hex; row.appendChild(hexInfo);
      row.appendChild(act);
      els.gradStops.appendChild(row);
    });
  }

  function swapStops(a, b) {
    const g = state.gradient;
    [g.colors[a], g.colors[b]] = [g.colors[b], g.colors[a]];
    [g.positions[a], g.positions[b]] = [g.positions[b], g.positions[a]];
    [g.colorTypes[a], g.colorTypes[b]] = [g.colorTypes[b], g.colorTypes[a]];
    renderGradientEditor();
    saveState();
  }

  function addStop() {
    const g = state.gradient;
    const mid = 0.5;
    g.colors.push(state.seed);
    g.positions.push(mid);
    g.colorTypes.push('color_normal');
    normalizePositions();
    renderGradientEditor();
    saveState();
  }

  function normalizePositions() {
    const g = state.gradient;
    // 确保 positions 升序
    const items = g.colors.map((c, i) => ({ c, p: g.positions[i], t: g.colorTypes[i] }));
    items.sort((a, b) => a.p - b.p);
    g.colors = items.map(x => x.c);
    g.positions = items.map(x => x.p);
    g.colorTypes = items.map(x => x.t);
  }

  function resetGradient() {
    state.gradient = {
      type: 'line_gradient',
      angle: 0,
      colors: [state.seed, '#FFFFFF'],
      colorTypes: ['color_normal', 'color_normal'],
      positions: [0, 1],
      radial: 0.5,
      xOffset: 0.5,
      yOffset: 0.5,
      sweepStart: 0,
      sweepEnd: 360,
    };
    renderGradientEditor();
    saveState();
  }

  function applyPreset(name) {
    if (name === 'rainbow') {
      state.gradient = {
        type: 'line_gradient', angle: 90,
        colors: ['#FF0000', '#FFA500', '#FFFF00', '#00FF00', '#00BFFF', '#0000FF', '#8B00FF'],
        colorTypes: Array(7).fill('color_normal'),
        positions: [0, 0.16, 0.32, 0.48, 0.64, 0.8, 1],
        radial: 0.5, xOffset: 0.5, yOffset: 0.5, sweepStart: 0, sweepEnd: 360
      };
    } else if (name === 'warm-cool') {
      state.gradient = {
        type: 'line_gradient', angle: 45,
        colors: ['#FF7A59', '#FFE08A', '#8AD5FF'],
        colorTypes: Array(3).fill('color_normal'),
        positions: [0, 0.5, 1],
        radial: 0.5, xOffset: 0.5, yOffset: 0.5, sweepStart: 0, sweepEnd: 360
      };
    }
    renderGradientEditor(); saveState();
  }

  function exportGradientJson() {
    const g = state.gradient;
    // 校验
    if (!validateGradient(g)) { showToast('JSON导出失败：数据不合法'); return; }
    const json = {
      angle: g.angle,
      colorTypes: g.colorTypes,
      colors: g.colors.map(hex => ColorUtils.argbIntFromHex(hex)),
      positions: g.positions,
      radial: g.radial,
      type: g.type,
      xOffset: g.xOffset,
      yOffset: g.yOffset
    };
    ColorUtils.downloadFile('gradient.json', JSON.stringify(json, null, 2), 'application/json');
  }

  function importGradientJsonText(text) {
    try {
      const obj = JSON.parse(text);
      if (!validateGradient(obj, true)) { showToast('JSON不合法'); return; }
      // 转换为内部结构
      const colors = (obj.colors || []).map(v => ColorUtils.hexFromArgbInt(v));
      state.gradient = {
        type: obj.type || 'line_gradient',
        angle: Number(obj.angle) || 0,
        colors,
        colorTypes: obj.colorTypes || Array(colors.length).fill('color_normal'),
        positions: obj.positions || [0, 1],
        radial: Number(obj.radial) || 0.5,
        xOffset: Number(obj.xOffset) || 0.5,
        yOffset: Number(obj.yOffset) || 0.5,
        sweepStart: Number(obj.sweepStart) || 0,
        sweepEnd: Number(obj.sweepEnd) || 360,
      };
      normalizePositions();
      renderGradientEditor(); saveState(); showToast('导入成功');
    } catch { showToast('JSON解析失败'); }
  }

  function validateGradient(g, isExternal = false) {
    const type = g.type;
    const colors = g.colors || [];
    const positions = g.positions || [];
    if (type !== 'color_normal' && colors.length < 2) return false;
    if (colors.length !== positions.length) return false;
    for (let i = 1; i < positions.length; i++) { if (positions[i] < positions[i - 1]) return false; }
    if (type === 'line_gradient') {
      if (g.angle < 0 || g.angle > 360) return false;
    }
    if (type === 'radial_gradient') {
      if (g.xOffset < 0 || g.xOffset > 1 || g.yOffset < 0 || g.yOffset > 1) return false;
      if (g.radial < 0 || g.radial > 1) return false;
    }
    if (type === 'sweep_gradient') {
      if (g.sweepStart < 0 || g.sweepStart > 360) return false;
      if (g.sweepEnd < 0 || g.sweepEnd > 360) return false;
    }
    if (isExternal) {
      // 外部格式 colors 为 ARGB int
      if (!Array.isArray(g.colors)) return false;
    }
    return true;
  }

  function openModal(hex) {
    const rgb = ColorUtils.fromHex(hex);
    const info = ColorUtils.formatAll(rgb);
    els.modalSwatch.style.background = hex;
    els.modalInfo.textContent = `${info.hex} | ${info.rgb} | ${info.hsl}`;
    els.modal.classList.add('show');
    els.modal.setAttribute('aria-hidden', 'false');
  }
  function closeModal() {
    els.modal.classList.remove('show');
    els.modal.setAttribute('aria-hidden', 'true');
  }

  // 简易 SV 画板渲染（基于当前 hue）
  function drawSV() {
    const ctx = els.svCanvas.getContext('2d');
    const w = els.svCanvas.width, h = els.svCanvas.height;
    const hue = Number(els.hueSlider.value);
    const grdS = ctx.createLinearGradient(0, 0, w, 0);
    grdS.addColorStop(0, 'white');
    grdS.addColorStop(1, ColorUtils.toHex(ColorUtils.hslToRgb({ h: hue, s: 100, l: 50 })));
    ctx.fillStyle = grdS; ctx.fillRect(0, 0, w, h);
    const grdV = ctx.createLinearGradient(0, 0, 0, h);
    grdV.addColorStop(0, 'transparent');
    grdV.addColorStop(1, 'black');
    ctx.fillStyle = grdV; ctx.fillRect(0, 0, w, h);
  }

  function pickFromSV(evt) {
    const rect = els.svCanvas.getBoundingClientRect();
    const x = ColorUtils.clamp(evt.clientX - rect.left, 0, rect.width);
    const y = ColorUtils.clamp(evt.clientY - rect.top, 0, rect.height);
    const s = Math.round((x / rect.width) * 100);
    const v = Math.round((1 - y / rect.height) * 100);
    state.s = s; state.v = v;
    const h = Number(els.hueSlider.value);
    const rgb = hsvToRgb(h, s, v);
    const hex = ColorUtils.toHex(rgb);
    els.svThumb.style.left = x + 'px';
    els.svThumb.style.top = y + 'px';
    setSeed(hex);
  }

  function hsvToRgb(h, s, v) {
    const S = s / 100, V = v / 100;
    const C = V * S;
    const X = C * (1 - Math.abs(((h / 60) % 2) - 1));
    const m = V - C;
    let r = 0, g = 0, b = 0;
    if (0 <= h && h < 60) { r = C; g = X; b = 0; }
    else if (60 <= h && h < 120) { r = X; g = C; b = 0; }
    else if (120 <= h && h < 180) { r = 0; g = C; b = X; }
    else if (180 <= h && h < 240) { r = 0; g = X; b = C; }
    else if (240 <= h && h < 300) { r = X; g = 0; b = C; }
    else { r = C; g = 0; b = X; }
    return { r: Math.round((r + m) * 255), g: Math.round((g + m) * 255), b: Math.round((b + m) * 255) };
  }

  // ========== 标签页切换 ==========
  function switchTab(tabId) {
    // 更新标签按钮状态
    document.querySelectorAll('.tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.tab === tabId);
    });
    // 更新标签内容显示
    document.querySelectorAll('.tab-content').forEach(content => {
      content.classList.toggle('active', content.id === tabId);
    });
    // 保存当前标签到state
    state.currentTab = tabId;
    saveState();

    // 如果切换到图标编辑器，初始化图标
    if (tabId === 'iconEditorTab' && state.iconEditor.icons.length === 0) {
      initIconEditor();
    }
  }

  // ========== 图标编辑器 ==========
  let iconLoader = null;
  let iconRenderer = null;

  async function initIconEditor() {
    try {
      showToast('正在加载图标资源...');

      // 初始化加载器和渲染器
      if (!iconLoader) iconLoader = new IconLoader();
      if (!iconRenderer) iconRenderer = new IconRenderer();

      // 加载图标包
      const icons = await iconLoader.loadIconPack();
      state.iconEditor.icons = icons;

      // 加载Shape
      try {
        const shapeLoader = new ShapeLoader();
        const shapes = await shapeLoader.loadAllShapes();
        state.iconEditor.availableShapes = shapes;
        console.log(`Loaded ${shapes.length} shapes:`, shapes.map(s => s.id));

        // 验证第一个shape的pathData
        if (shapes.length > 0) {
          console.log('First shape sample:', {
            id: shapes[0].id,
            name: shapes[0].name,
            pathDataLength: shapes[0].pathData?.length,
            pathDataPreview: shapes[0].pathData?.substring(0, 50)
          });
        }
      } catch (error) {
        console.error('Failed to load shapes:', error);
        showToast('Shape加载失败，将使用默认背景');
        state.iconEditor.availableShapes = [];
      }

      // 初始化全局颜色配置（如果还没有）
      if (!state.iconEditor.globalColorConfig.fg || state.iconEditor.globalColorConfig.fg.length === 0) {
        // 默认3层前景色（大部分图标都是3层）
        state.iconEditor.globalColorConfig.fg = [
          createDefaultLayerConfig(state.seed),
          createDefaultLayerConfig(state.seed),
          createDefaultLayerConfig(state.seed)
        ];
      }
      if (!state.iconEditor.globalColorConfig.bg || state.iconEditor.globalColorConfig.bg.length === 0) {
        state.iconEditor.globalColorConfig.bg = [createDefaultLayerConfig('#000000')];
      }

      // 渲染图标网格
      renderIconGrid();

      // 渲染颜色编辑面板
      renderIconColorPanel();

      // 渲染Shape面板
      renderShapePanel();

      // 渲染图标包列表
      renderIconPackList();

      // 加载壁纸缓存
      await loadWallpapersFromCache();
      await loadWidgetsFromCache();

      showToast(`已加载 ${icons.length} 个图标`);
    } catch (error) {
      console.error('Failed to initialize icon editor:', error);
      showToast('图标加载失败，请检查资源文件');
    }
  }

  async function loadWallpapersFromCache() {
    try {
      if (!storageManager) {
        storageManager = new StorageManager();
      }
      if (!storageManager.db) {
        await storageManager.init();
      }
      const wallpapers = await storageManager.getAllWallpapers();
      if (wallpapers && wallpapers.length > 0) {
        state.iconEditor.wallpapers = wallpapers;
        renderWallpaperGrid();
      }
    } catch (error) {
      console.error('Failed to load wallpapers from cache:', error);
    }
  }

  async function loadWidgetsFromCache() {
    try {
      if (!storageManager) {
        storageManager = new StorageManager();
      }
      if (!storageManager.db) {
        await storageManager.init();
      }
      const widgets = await storageManager.getAllWidgets();
      if (widgets && widgets.length > 0) {
        state.iconEditor.widgets = widgets;
        renderWidgetGrid();
      }
    } catch (error) {
      console.error('Failed to load widgets from cache:', error);
    }
  }

  function createDefaultLayerConfig(color) {
    // 将HEX颜色转换为ARGB整数
    const rgba = ColorUtils.fromHex(color);
    const argbInt = ColorUtils.toArgbInt(rgba);

    return {
      type: 'color_normal',
      colors: [argbInt],
      colorTypes: ['color_normal'],
      positions: [0],
      angle: 0,
      radial: 0.5,
      xOffset: 0.5,
      yOffset: 0.5
    };
  }

  function renderIconGrid() {
    const gridEl = $('iconGrid');
    if (!gridEl) return;

    gridEl.innerHTML = '';

    // 使用全局颜色配置
    const colorConfig = state.iconEditor.globalColorConfig;

    state.iconEditor.icons.forEach((icon, index) => {
      // 创建图标容器
      const item = document.createElement('div');
      item.className = 'icon-item';
      item.dataset.iconName = icon.name;

      // 获取当前图标应使用的Shape
      const shape = getShapeForIcon(icon.name, state.iconEditor.selectedShapes);

      // 渲染图标Canvas（所有图标使用相同的全局颜色配置）
      const canvas = iconRenderer.compositeIcon(icon, colorConfig, 108, shape);
      canvas.className = 'icon-canvas';

      // 添加图标名称
      const nameEl = document.createElement('div');
      nameEl.className = 'icon-name';
      nameEl.textContent = icon.displayName;

      item.appendChild(canvas);
      item.appendChild(nameEl);

      // 绑定点击事件
      item.addEventListener('click', () => selectIcon(icon.name));

      gridEl.appendChild(item);
    });
  }

  function selectIcon(iconName) {
    // 更新选中状态
    state.iconEditor.currentIcon = iconName;

    // 更新UI高亮
    document.querySelectorAll('.icon-item').forEach(item => {
      item.classList.toggle('selected', item.dataset.iconName === iconName);
    });

    saveState();
  }

  function renderIconColorPanel() {
    // 使用全局颜色配置
    const colorConfig = state.iconEditor.globalColorConfig;

    // 设置全局pre_method控件的值
    const globalPreMethodSelect = $('globalPreMethod');

    if (globalPreMethodSelect) {
      globalPreMethodSelect.value = colorConfig.pre_method || '';
    }

    // 渲染前景色层列表
    renderFgLayers(colorConfig.fg);

    // 渲染背景色
    renderBgLayer(colorConfig.bg);
  }

  function renderFgLayers(fgLayers) {
    const container = $('fgLayers');
    if (!container) return;

    container.innerHTML = '';

    fgLayers.forEach((layer, index) => {
      const item = document.createElement('div');
      item.className = 'layer-item';

      // 层序号
      const indexEl = document.createElement('div');
      indexEl.className = 'layer-index';
      indexEl.textContent = `层 ${index + 1}`;

      // 颜色预览
      const preview = document.createElement('div');
      preview.className = 'layer-color-preview';
      // layer.colors[0] 现在是 ARGB 整数，需要转换为 CSS 颜色格式
      const rgba = ColorUtils.fromArgbInt(layer.colors[0] || -16777216); // 默认黑色 ARGB
      preview.style.background = ColorUtils.toRgbString(rgba);
      preview.title = '点击编辑颜色';
      preview.addEventListener('click', () => editLayerColor('fg', index));

      // 类型显示
      const typeEl = document.createElement('div');
      typeEl.textContent = getColorTypeLabel(layer.type);
      typeEl.style.fontSize = '12px';
      typeEl.style.color = 'var(--muted)';

      // 删除按钮
      const actions = document.createElement('div');
      actions.className = 'layer-actions';
      const delBtn = document.createElement('button');
      delBtn.className = 'btn btn-secondary';
      delBtn.textContent = '删除';
      delBtn.style.fontSize = '12px';
      delBtn.style.padding = '4px 8px';
      delBtn.style.height = 'auto';
      delBtn.addEventListener('click', () => deleteFgLayer(index));
      actions.appendChild(delBtn);

      item.appendChild(indexEl);
      item.appendChild(preview);
      item.appendChild(typeEl);
      item.appendChild(actions);

      container.appendChild(item);
    });
  }

  function renderBgLayer(bgLayers) {
    const container = $('bgLayer');
    if (!container) return;

    container.innerHTML = '';

    // 渲染所有背景层
    bgLayers.forEach((layer, index) => {
      const item = document.createElement('div');
      item.className = 'layer-item';

      // 层序号
      const indexEl = document.createElement('div');
      indexEl.className = 'layer-index';
      indexEl.textContent = `背景层 ${index + 1}`;

      // 颜色预览
      const preview = document.createElement('div');
      preview.className = 'layer-color-preview';
      // layer.colors[0] 现在是 ARGB 整数，需要转换为 CSS 颜色格式
      const rgba = ColorUtils.fromArgbInt(layer.colors[0] || -16777216); // 默认黑色 ARGB
      preview.style.background = ColorUtils.toRgbString(rgba);
      preview.title = '点击编辑颜色';
      preview.addEventListener('click', () => editLayerColor('bg', index));

      // 类型显示
      const typeEl = document.createElement('div');
      typeEl.textContent = getColorTypeLabel(layer.type);
      typeEl.style.fontSize = '12px';
      typeEl.style.color = 'var(--muted)';

      // 删除按钮
      const actions = document.createElement('div');
      actions.className = 'layer-actions';
      const delBtn = document.createElement('button');
      delBtn.className = 'btn btn-secondary';
      delBtn.textContent = '删除';
      delBtn.style.fontSize = '12px';
      delBtn.style.padding = '4px 8px';
      delBtn.style.height = 'auto';
      delBtn.addEventListener('click', () => deleteBgLayer(index));
      actions.appendChild(delBtn);

      item.appendChild(indexEl);
      item.appendChild(preview);
      item.appendChild(typeEl);
      item.appendChild(actions);

      container.appendChild(item);
    });
  }

  function getColorTypeLabel(type) {
    const labels = {
      'color_normal': 'Normal',
      'line_gradient': 'Linear',
      'radial_gradient': 'Radial',
      'sweep_gradient': 'Sweep'
    };
    return labels[type] || type;
  }

  // 当前正在编辑的层信息
  let currentEditingLayer = null;

  function editLayerColor(layerType, layerIndex) {
    currentEditingLayer = { type: layerType, index: layerIndex };

    // 获取当前层的配置
    const colorConfig = state.iconEditor.globalColorConfig;
    const layerConfig = layerType === 'fg' ? colorConfig.fg[layerIndex] : colorConfig.bg[layerIndex];

    // 打开颜色编辑模态窗口
    openColorEditModal(layerConfig, layerType, layerIndex);
  }

  function openColorEditModal(layerConfig, layerType, layerIndex) {
    // 使用现有的模态窗口
    const modal = els.modal;
    const modalContent = modal.querySelector('.modal-content');

    // 清空并重新构建内容 - 集成完整的渐变编辑器
    modalContent.innerHTML = `
      <button id="colorModalClose" class="modal-close" aria-label="关闭">×</button>
      <div style="padding: 20px; max-width: 600px;">
        <h3 style="margin: 0 0 16px; color: var(--text);">编辑 ${layerType === 'fg' ? '前景' : '背景'}色 - 层 ${layerIndex + 1}</h3>

        <div class="grad-toolbar" style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px;">
          <div class="input-group">
            <label for="modalGradType">类型</label>
            <select id="modalGradType" class="input">
              <option value="color_normal">Normal</option>
              <option value="line_gradient">Linear</option>
              <option value="radial_gradient">Radial</option>
              <option value="sweep_gradient">Sweep</option>
            </select>
          </div>

          <div class="input-group grad-only grad-linear">
            <label for="modalLinearAngle">线性角度（°）</label>
            <input id="modalLinearAngle" class="input" type="number" min="0" max="360" step="1" value="0" />
          </div>

          <div class="input-group grad-only grad-radial">
            <label>径向中心X/Y</label>
            <div style="display: flex; gap: 8px;">
              <input id="modalRadialX" class="input" type="number" min="0" max="1" step="0.01" value="0.5" />
              <input id="modalRadialY" class="input" type="number" min="0" max="1" step="0.01" value="0.5" />
            </div>
          </div>

          <div class="input-group grad-only grad-radial">
            <label for="modalRadialSize">径向范围</label>
            <input id="modalRadialSize" class="input" type="number" min="0" max="1" step="0.01" value="0.5" />
          </div>
        </div>

        <div id="modalGradPreview" style="width: 100%; aspect-ratio: 1/1; max-width: 200px; margin: 0 auto; border: 1px solid var(--border); border-radius: 10px; margin-bottom: 12px;"></div>
        <div id="modalGradTrack" style="position: relative; width: 100%; height: 24px; border: 1px dashed var(--border); border-radius: 8px; margin-bottom: 12px;"></div>

        <div id="modalGradStops" style="display: grid; gap: 8px; max-height: 400px; overflow-y: auto; margin-bottom: 16px;"></div>

        <button id="modalAddStop" class="btn btn-secondary" style="width: 100%; margin-bottom: 16px;">添加色标</button>

        <div style="display: flex; gap: 8px; justify-content: flex-end;">
          <button id="colorModalCancel" class="btn btn-secondary">取消</button>
          <button id="colorModalSave" class="btn">保存</button>
        </div>
      </div>
    `;

    // 初始化模态编辑器状态
    // 从 layerConfig.colors 解析颜色和 alpha 值
    const parsedColors = (layerConfig.colors || ['#000000']).map(color => {
      if (typeof color === 'number') {
        // ARGB 整数格式
        const rgba = ColorUtils.fromArgbInt(color);
        return { hex: ColorUtils.toHex(rgba), alpha: rgba.a };
      } else {
        // HEX 字符串或其他格式
        const rgba = ColorUtils.parseColor(color) || { r: 0, g: 0, b: 0, a: 1 };
        return { hex: ColorUtils.toHex(rgba), alpha: rgba.a };
      }
    });

    const modalState = {
      type: layerConfig.type || 'color_normal',
      angle: layerConfig.angle || 0,
      colors: parsedColors.map(c => c.hex),
      alphas: parsedColors.map(c => c.alpha),
      colorTypes: [...(layerConfig.colorTypes || [])],
      positions: [...(layerConfig.positions || [0])],
      radial: layerConfig.radial || 0.5,
      xOffset: layerConfig.xOffset || 0.5,
      yOffset: layerConfig.yOffset || 0.5
    };

    // 确保至少有一个色标
    if (modalState.colors.length === 0) {
      modalState.colors = ['#000000'];
      modalState.alphas = [1];
      modalState.positions = [0];
    }

    // 确保 colors、alphas 和 colorTypes 数组长度一致
    while (modalState.alphas.length < modalState.colors.length) {
      modalState.alphas.push(1);
    }
    while (modalState.colorTypes.length < modalState.colors.length) {
      modalState.colorTypes.push('color_normal');
    }

    // 设置表单值
    const typeSelect = modalContent.querySelector('#modalGradType');
    typeSelect.value = modalState.type;
    modalContent.querySelector('#modalLinearAngle').value = modalState.angle;
    modalContent.querySelector('#modalRadialX').value = modalState.xOffset;
    modalContent.querySelector('#modalRadialY').value = modalState.yOffset;
    modalContent.querySelector('#modalRadialSize').value = modalState.radial;

    // 渲染函数
    function renderModalGradient() {
      updateModalGradPanels(modalState.type);
      renderModalGradPreview(modalState);
      renderModalGradTrack(modalState);
      renderModalGradStops(modalState);
    }

    // 绑定事件
    typeSelect.addEventListener('change', (e) => {
      const newType = e.target.value;
      const oldType = modalState.type;
      modalState.type = newType;

      // 切换类型时自动调整色标数量
      if (newType === 'color_normal') {
        // Normal只保留第一个颜色
        if (modalState.colors.length > 1) {
          modalState.colors = [modalState.colors[0]];
          modalState.alphas = [modalState.alphas[0]];
          modalState.colorTypes = [modalState.colorTypes[0]];
          modalState.positions = [0];
        }
      } else if (oldType === 'color_normal') {
        // 从Normal切换到渐变，至少需要2个色标
        if (modalState.colors.length < 2) {
          modalState.colors.push('#FFFFFF');
          modalState.alphas.push(1);
          modalState.colorTypes.push('color_normal');
          modalState.positions.push(1);
        }
      }

      renderModalGradient();
    });
    modalContent.querySelector('#modalLinearAngle').addEventListener('input', (e) => { modalState.angle = Number(e.target.value); renderModalGradient(); });
    modalContent.querySelector('#modalRadialX').addEventListener('input', (e) => { modalState.xOffset = Number(e.target.value); renderModalGradient(); });
    modalContent.querySelector('#modalRadialY').addEventListener('input', (e) => { modalState.yOffset = Number(e.target.value); renderModalGradient(); });
    modalContent.querySelector('#modalRadialSize').addEventListener('input', (e) => { modalState.radial = Number(e.target.value); renderModalGradient(); });
    modalContent.querySelector('#modalAddStop').addEventListener('click', () => {
      modalState.colors.push('#FFFFFF');
      modalState.alphas.push(1);
      modalState.colorTypes.push('color_normal');
      modalState.positions.push(0.5);
      renderModalGradient();
    });

    modalContent.querySelector('#colorModalClose').addEventListener('click', closeColorEditModal);
    modalContent.querySelector('#colorModalCancel').addEventListener('click', closeColorEditModal);
    modalContent.querySelector('#colorModalSave').addEventListener('click', () => saveColorEdit(modalState));

    // 初始渲染
    renderModalGradient();

    // 显示模态窗口
    modal.classList.add('show');
    modal.setAttribute('aria-hidden', 'false');
  }

  // 模态窗口渲染函数
  function updateModalGradPanels(type) {
    const modal = document.querySelector('.modal');
    if (!modal) return;

    const show = (cls, on) => {
      modal.querySelectorAll('.' + cls).forEach(el => el.style.display = on ? 'block' : 'none');
    };
    show('grad-linear', type === 'line_gradient');
    show('grad-radial', type === 'radial_gradient');
  }

  function getModalCssGradient(modalState) {
    // 将 colors 和 alphas 组合为 rgba 格式
    const stops = modalState.colors.map((hex, i) => {
      const rgba = ColorUtils.fromHex(hex);
      rgba.a = modalState.alphas[i];
      const colorStr = ColorUtils.toRgbString(rgba);
      return `${colorStr} ${Math.round(modalState.positions[i] * 100)}%`;
    });

    if (modalState.type === 'color_normal') {
      const rgba = ColorUtils.fromHex(modalState.colors[0] || '#000000');
      rgba.a = modalState.alphas[0] || 1;
      return ColorUtils.toRgbString(rgba);
    }

    if (modalState.type === 'line_gradient') {
      // 修复：角度0时从上往下（180度）
      const angle = (modalState.angle + 180) % 360;
      return `linear-gradient(${angle}deg, ${stops.join(', ')})`;
    }

    if (modalState.type === 'radial_gradient') {
      const x = (modalState.xOffset * 100).toFixed(2);
      const y = (modalState.yOffset * 100).toFixed(2);
      // 修复：使用像素值而不是百分比，确保在正方形预览中正确显示
      const preview = document.querySelector('#modalGradPreview');
      const size = preview ? Math.min(preview.offsetWidth, preview.offsetHeight) : 300;
      const radiusPx = Math.round(modalState.radial * size / 2);
      return `radial-gradient(circle ${radiusPx}px at ${x}% ${y}%, ${stops.join(', ')})`;
    }

    if (modalState.type === 'sweep_gradient') {
      return `conic-gradient(from 0deg at 50% 50%, ${stops.join(', ')})`;
    }

    return modalState.colors[0] || '#000000';
  }

  function renderModalGradPreview(modalState) {
    const preview = document.querySelector('#modalGradPreview');
    if (!preview) return;
    preview.style.background = getModalCssGradient(modalState);
  }

  function renderModalGradTrack(modalState) {
    const track = document.querySelector('#modalGradTrack');
    if (!track) return;

    track.innerHTML = '';
    track.style.background = getModalCssGradient(modalState);

    modalState.positions.forEach((pos, i) => {
      const handle = document.createElement('div');
      handle.className = 'grad-handle';
      handle.style.position = 'absolute';
      handle.style.left = `calc(${pos * 100}% - 6px)`;
      handle.style.top = '-4px';
      handle.style.width = '12px';
      handle.style.height = '32px';
      handle.style.borderRadius = '6px';
      handle.style.border = '1px solid var(--border)';
      handle.style.background = modalState.colors[i];
      handle.style.cursor = 'ew-resize';
      handle.title = `${Math.round(pos * 100)}%`;

      handle.addEventListener('mousedown', (ev) => {
        const rect = track.getBoundingClientRect();
        const move = (e) => {
          const x = ColorUtils.clamp(e.clientX - rect.left, 0, rect.width);
          const p = x / rect.width;
          modalState.positions[i] = Math.min(1, Math.max(0, p));
          renderModalGradPreview(modalState);
          renderModalGradTrack(modalState);
        };
        const up = () => { window.removeEventListener('mousemove', move); window.removeEventListener('mouseup', up); };
        window.addEventListener('mousemove', move);
        window.addEventListener('mouseup', up);
      });

      track.appendChild(handle);
    });
  }

  function renderModalGradStops(modalState) {
    const container = document.querySelector('#modalGradStops');
    if (!container) return;

    container.innerHTML = '';

    modalState.colors.forEach((hex, i) => {
      const row = document.createElement('div');
      row.style.display = 'grid';
      row.style.gridTemplateColumns = '90px 120px 150px 1fr auto auto';
      row.style.gap = '12px';
      row.style.alignItems = 'center';
      row.style.padding = '10px';
      row.style.background = 'var(--bg)';
      row.style.border = '1px solid var(--border)';
      row.style.borderRadius = '6px';

      // 颜色选择器
      const colorWrap = document.createElement('div');
      colorWrap.style.display = 'flex';
      colorWrap.style.flexDirection = 'column';
      colorWrap.style.gap = '4px';

      const colorInput = document.createElement('input');
      colorInput.type = 'color';
      colorInput.value = hex;
      colorInput.style.width = '40px';
      colorInput.style.height = '40px';
      colorInput.style.border = '1px solid var(--border)';
      colorInput.style.borderRadius = '4px';
      colorInput.style.cursor = 'pointer';
      colorInput.style.padding = '0';
      colorInput.addEventListener('input', (e) => {
        // 仅更新 RGB 值，保持 alpha 不变
        modalState.colors[i] = e.target.value;
        // 根据 alpha 值更新 hexInput 显示格式
        if (modalState.alphas[i] < 1) {
          const rgba = ColorUtils.fromHex(e.target.value);
          rgba.a = modalState.alphas[i];
          hexInput.value = ColorUtils.toRgbString(rgba);
        } else {
          hexInput.value = e.target.value;
        }
        renderModalGradPreview(modalState);
        renderModalGradTrack(modalState);
      });

      // HEX输入框（支持多种格式）
      const hexInput = document.createElement('input');
      hexInput.type = 'text';
      // 根据 alpha 值选择显示格式
      if (modalState.alphas[i] < 1) {
        const rgba = ColorUtils.fromHex(hex);
        rgba.a = modalState.alphas[i];
        hexInput.value = ColorUtils.toRgbString(rgba);
      } else {
        hexInput.value = hex;
      }
      hexInput.className = 'input';
      hexInput.style.width = '80px';
      hexInput.style.height = '28px';
      hexInput.style.fontSize = '11px';
      hexInput.style.fontFamily = 'monospace';
      hexInput.style.textAlign = 'center';
      hexInput.style.padding = '4px';
      hexInput.placeholder = '#RRGGBB/#RRGGBBAA/rgba()';
      hexInput.addEventListener('change', (e) => {
        const val = e.target.value.trim();
        const parsed = ColorUtils.parseColor(val);
        if (parsed) {
          // 解析成功，更新颜色和透明度
          modalState.colors[i] = ColorUtils.toHex(parsed);
          modalState.alphas[i] = parsed.a;
          colorInput.value = modalState.colors[i];
          // 更新显示格式
          if (parsed.a < 1) {
            hexInput.value = ColorUtils.toRgbString(parsed);
          } else {
            hexInput.value = modalState.colors[i];
          }
          renderModalGradPreview(modalState);
          renderModalGradTrack(modalState);
          // 更新 alpha 滑块和显示
          renderModalGradStops(modalState);
        } else {
          hexInput.value = modalState.alphas[i] < 1 ?
            ColorUtils.toRgbString({ ...ColorUtils.fromHex(modalState.colors[i]), a: modalState.alphas[i] }) :
            modalState.colors[i];
          showToast('无效的颜色格式');
        }
      });

      colorWrap.appendChild(colorInput);
      colorWrap.appendChild(hexInput);

      // Alpha 滑块容器
      const alphaWrap = document.createElement('div');
      alphaWrap.style.display = 'flex';
      alphaWrap.style.flexDirection = 'column';
      alphaWrap.style.gap = '4px';

      const alphaLabel = document.createElement('label');
      alphaLabel.textContent = '透明度';
      alphaLabel.style.fontSize = '11px';
      alphaLabel.style.color = 'var(--muted)';

      const alphaInput = document.createElement('input');
      alphaInput.type = 'range';
      alphaInput.min = '0';
      alphaInput.max = '1';
      alphaInput.step = '0.01';
      alphaInput.value = modalState.alphas[i];
      alphaInput.style.width = '100%';
      alphaInput.addEventListener('input', (e) => {
        modalState.alphas[i] = Number(e.target.value);
        alphaValue.textContent = `${Math.round(modalState.alphas[i] * 100)}%`;
        // 同步更新 hexInput 显示格式
        if (modalState.alphas[i] < 1) {
          const rgba = ColorUtils.fromHex(modalState.colors[i]);
          rgba.a = modalState.alphas[i];
          hexInput.value = ColorUtils.toRgbString(rgba);
        } else {
          hexInput.value = modalState.colors[i];
        }
        renderModalGradPreview(modalState);
        renderModalGradTrack(modalState);
      });

      const alphaValue = document.createElement('span');
      alphaValue.textContent = `${Math.round(modalState.alphas[i] * 100)}%`;
      alphaValue.style.fontSize = '11px';
      alphaValue.style.color = 'var(--text)';
      alphaValue.style.textAlign = 'center';

      alphaWrap.appendChild(alphaLabel);
      alphaWrap.appendChild(alphaInput);
      alphaWrap.appendChild(alphaValue);

      // ColorType 选择器
      const colorTypeWrap = document.createElement('div');
      colorTypeWrap.style.display = 'flex';
      colorTypeWrap.style.flexDirection = 'column';
      colorTypeWrap.style.gap = '4px';

      const colorTypeLabel = document.createElement('label');
      colorTypeLabel.textContent = '颜色类型';
      colorTypeLabel.style.fontSize = '11px';
      colorTypeLabel.style.color = 'var(--muted)';

      const colorTypeSelect = document.createElement('select');
      colorTypeSelect.className = 'input';
      colorTypeSelect.style.fontSize = '11px';
      colorTypeSelect.style.padding = '4px';
      colorTypeSelect.style.height = 'auto';
      getColorTypeOptions().forEach(t => {
        const opt = document.createElement('option');
        opt.value = t;
        opt.textContent = getColorTypeLabel(t);
        colorTypeSelect.appendChild(opt);
      });
      colorTypeSelect.value = modalState.colorTypes[i] || 'color_normal';
      colorTypeSelect.addEventListener('change', (e) => {
        modalState.colorTypes[i] = e.target.value;
        renderModalGradPreview(modalState);
        renderModalGradTrack(modalState);
      });

      colorTypeWrap.appendChild(colorTypeLabel);
      colorTypeWrap.appendChild(colorTypeSelect);

      // 位置滑块
      const posInput = document.createElement('input');
      posInput.type = 'range';
      posInput.min = '0';
      posInput.max = '1';
      posInput.step = '0.01';
      posInput.value = modalState.positions[i];
      posInput.style.width = '100%';
      posInput.addEventListener('input', (e) => {
        modalState.positions[i] = Number(e.target.value);
        posLabel.textContent = `${Math.round(modalState.positions[i] * 100)}%`;
        renderModalGradPreview(modalState);
        renderModalGradTrack(modalState);
      });

      // 位置数值
      const posLabel = document.createElement('span');
      posLabel.textContent = `${Math.round(modalState.positions[i] * 100)}%`;
      posLabel.style.fontSize = '12px';
      posLabel.style.color = 'var(--muted)';
      posLabel.style.minWidth = '40px';

      // 删除按钮
      const delBtn = document.createElement('button');
      delBtn.className = 'btn btn-secondary';
      delBtn.textContent = '删除';
      delBtn.style.fontSize = '12px';
      delBtn.style.padding = '4px 8px';
      delBtn.style.height = 'auto';
      delBtn.addEventListener('click', () => {
        if (modalState.colors.length <= 1) {
          showToast('至少需要保留一个色标');
          return;
        }
        modalState.colors.splice(i, 1);
        modalState.alphas.splice(i, 1);
        modalState.colorTypes.splice(i, 1);
        modalState.positions.splice(i, 1);
        renderModalGradPreview(modalState);
        renderModalGradTrack(modalState);
        renderModalGradStops(modalState);
      });

      row.appendChild(colorWrap);
      row.appendChild(alphaWrap);
      row.appendChild(colorTypeWrap);
      row.appendChild(posInput);
      row.appendChild(posLabel);
      row.appendChild(delBtn);

      container.appendChild(row);
    });
  }

  function saveColorEdit(modalState) {
    if (!currentEditingLayer) return;

    const { type: layerType, index: layerIndex } = currentEditingLayer;
    const colorConfig = state.iconEditor.globalColorConfig;
    const layerConfig = layerType === 'fg' ? colorConfig.fg[layerIndex] : colorConfig.bg[layerIndex];

    // 保存模态状态到层配置
    // 将 colors 和 alphas 组合转换为 ARGB 整数
    layerConfig.type = modalState.type;
    layerConfig.colors = modalState.colors.map((hex, i) => {
      const rgba = ColorUtils.fromHex(hex);
      rgba.a = modalState.alphas[i];
      return ColorUtils.toArgbInt(rgba);
    });
    layerConfig.positions = [...modalState.positions];
    layerConfig.colorTypes = [...modalState.colorTypes];
    layerConfig.angle = modalState.angle;
    layerConfig.radial = modalState.radial;
    layerConfig.xOffset = modalState.xOffset;
    layerConfig.yOffset = modalState.yOffset;

    // 更新预览
    renderIconColorPanel();
    updateAllIconPreviews();
    saveState();

    closeColorEditModal();
    showToast('颜色已更新');
  }

  function closeColorEditModal() {
    currentEditingLayer = null;
    els.modal.classList.remove('show');
    els.modal.setAttribute('aria-hidden', 'true');
  }

  function deleteFgLayer(index) {
    const colorConfig = state.iconEditor.globalColorConfig;
    if (colorConfig.fg.length <= 1) {
      showToast('至少需要保留一个前景层');
      return;
    }

    colorConfig.fg.splice(index, 1);
    renderIconColorPanel();
    updateAllIconPreviews(); // 更新所有图标
    saveState();
  }

  function addFgLayer() {
    const colorConfig = state.iconEditor.globalColorConfig;
    colorConfig.fg.push(createDefaultLayerConfig(state.seed));

    renderIconColorPanel();
    updateAllIconPreviews(); // 更新所有图标
    saveState();
  }

  function deleteBgLayer(index) {
    const colorConfig = state.iconEditor.globalColorConfig;
    if (colorConfig.bg.length <= 1) {
      showToast('至少需要保留一个背景层');
      return;
    }

    colorConfig.bg.splice(index, 1);
    renderIconColorPanel();
    updateAllIconPreviews();
    saveState();
  }

  function addBgLayer() {
    const colorConfig = state.iconEditor.globalColorConfig;
    colorConfig.bg.push(createDefaultLayerConfig('#000000'));

    renderIconColorPanel();
    updateAllIconPreviews();
    saveState();
  }

  function updateAllIconPreviews() {
    // 重新渲染整个图标网格（因为颜色配置是全局的）
    renderIconGrid();

    // 重新渲染手机预览区的图标
    renderPhoneIconGrid();

    // 恢复选中状态
    if (state.iconEditor.currentIcon) {
      document.querySelectorAll('.icon-item').forEach(item => {
        item.classList.toggle('selected', item.dataset.iconName === state.iconEditor.currentIcon);
      });
    }
  }

  // ========== JSON导入导出 ==========
  function generateUniqueId() {
    return 'scheme_' + Date.now();
  }

  function exportIconColorsJson() {
    const colorConfig = state.iconEditor.globalColorConfig;

    // 导出前校验
    if (!state.iconEditor.currentIconPackName) {
      showToast('导出失败：缺少图标包名称');
      return;
    }

    // 获取或生成ID
    let schemeId = state.iconEditor.currentSchemeId;
    if (!schemeId) {
      // 如果没有保存过方案，弹窗让用户输入ID
      const defaultId = generateUniqueId();
      schemeId = prompt('请输入配色方案ID（用作文件名）：', defaultId);
      if (!schemeId) {
        showToast('已取消导出');
        return;
      }
      // 保存用户输入的ID到state
      state.iconEditor.currentSchemeId = schemeId;
      saveState();
    }

    // 转换为需求文档格式
    const json = {
      id: schemeId,
      icon_pack_name: state.iconEditor.currentIconPackName,
      icon_pack_url: state.iconEditor.currentIconPackUrl || '',
      pre_method: colorConfig.pre_method || '',
      icon_shape: state.iconEditor.selectedShapes.join(';'), // 添加icon_shape字段
      icon_colors: [{
        fg: colorConfig.fg.map(layer => convertLayerToJson(layer)),
        bg: colorConfig.bg.map(layer => convertLayerToJson(layer))
      }]
    };

    ColorUtils.downloadFile(`${schemeId}.json`, JSON.stringify(json, null, 2), 'application/json');
    showToast('JSON已导出');
  }

  function convertLayerToJson(layer) {
    // layer.colors 现在存储的是 ARGB 整数，直接使用
    const colors = layer.colors;
    const color_hex = layer.colors.map(argbInt => {
      // 从 ARGB 整数转换为 #AARRGGBB 格式
      const rgba = ColorUtils.fromArgbInt(argbInt);
      const a = Math.round(rgba.a * 255);
      const r = rgba.r.toString(16).padStart(2, '0').toUpperCase();
      const g = rgba.g.toString(16).padStart(2, '0').toUpperCase();
      const b = rgba.b.toString(16).padStart(2, '0').toUpperCase();
      return `#${a.toString(16).padStart(2, '0').toUpperCase()}${r}${g}${b}`;
    });

    return {
      colorTypes: layer.colorTypes || layer.colors.map(() => 'color_normal'),
      colors: colors,
      color_hex: color_hex,
      gradient: {
        angle: layer.angle || 0,
        positions: layer.positions || [],
        radial: layer.radial || 1.0,
        xOffset: layer.xOffset || 0.5,
        yOffset: layer.yOffset || 0.5,
        type: layer.type || 'color_normal'
      }
    };
  }

  // 解析颜色值字符串
  // 格式: HEX,HEX,HEX..;HEX,HEX..
  // 分号区分前景色和背景色，逗号区分每一层的颜色值
  // 没有分号时只输入前景色
  function parseColorValuesString(colorString) {
    try {
      // 移除空格
      colorString = colorString.trim().replace(/\s+/g, '');

      if (!colorString) {
        return { valid: false, error: '颜色值字符串不能为空' };
      }

      // 检查是否包含分号
      const parts = colorString.split(';');

      if (parts.length > 2) {
        return { valid: false, error: '格式错误：只能包含一个分号（用于分隔前景色和背景色）' };
      }

      const result = {
        valid: true,
        fg: [],
        bg: []
      };

      // 解析前景色
      const fgPart = parts[0];
      if (!fgPart) {
        return { valid: false, error: '前景色不能为空' };
      }

      const fgColors = fgPart.split(',');
      for (let i = 0; i < fgColors.length; i++) {
        const color = fgColors[i].trim();
        if (!color) {
          return { valid: false, error: `前景色第${i + 1}个颜色值为空` };
        }

        // 验证HEX格式
        if (!isValidHexColor(color)) {
          return { valid: false, error: `前景色第${i + 1}个颜色值"${color}"不是有效的HEX格式（应为#RRGGBB或#RRGGBBAA）` };
        }

        result.fg.push(color);
      }

      // 解析背景色（如果有）
      if (parts.length === 2) {
        const bgPart = parts[1];
        if (!bgPart) {
          return { valid: false, error: '背景色不能为空（如果不需要背景色，请删除分号）' };
        }

        const bgColors = bgPart.split(',');
        for (let i = 0; i < bgColors.length; i++) {
          const color = bgColors[i].trim();
          if (!color) {
            return { valid: false, error: `背景色第${i + 1}个颜色值为空` };
          }

          // 验证HEX格式
          if (!isValidHexColor(color)) {
            return { valid: false, error: `背景色第${i + 1}个颜色值"${color}"不是有效的HEX格式（应为#RRGGBB或#RRGGBBAA）` };
          }

          result.bg.push(color);
        }
      }

      return result;
    } catch (error) {
      return { valid: false, error: '解析失败：' + error.message };
    }
  }

  // 验证HEX颜色格式
  function isValidHexColor(color) {
    // 支持 #RGB, #RRGGBB, #RRGGBBAA 格式
    const hexRegex = /^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$/;
    return hexRegex.test(color);
  }

  // 导入颜色值
  function importColorValues() {
    // 弹窗让用户输入颜色值
    const colorString = prompt(
      '请输入颜色值（格式：HEX,HEX,HEX..;HEX,HEX..）\n' +
      '说明：\n' +
      '- 分号前为前景色，分号后为背景色\n' +
      '- 逗号分隔每一层的颜色值\n' +
      '- 没有分号时只输入前景色\n' +
      '示例：#FF0000,#00FF00,#0000FF;#000000',
      '#FF0000,#00FF00,#0000FF'
    );

    if (!colorString) {
      showToast('已取消导入');
      return;
    }

    // 解析颜色值字符串
    const parseResult = parseColorValuesString(colorString);

    if (!parseResult.valid) {
      showToast('导入失败：' + parseResult.error);
      return;
    }

    // 清空之前的前景色和背景色
    state.iconEditor.globalColorConfig.fg = [];
    state.iconEditor.globalColorConfig.bg = [];

    // 导入前景色
    parseResult.fg.forEach(hexColor => {
      // 将HEX颜色转换为ARGB整数
      const rgba = ColorUtils.fromHex(hexColor);
      const argbInt = ColorUtils.toArgbInt(rgba);

      // 创建层配置
      const layer = {
        type: 'color_normal',
        colors: [argbInt],
        colorTypes: ['color_normal'],
        positions: [0],
        angle: 0,
        radial: 0.5,
        xOffset: 0.5,
        yOffset: 0.5
      };

      state.iconEditor.globalColorConfig.fg.push(layer);
    });

    // 导入背景色（如果有）
    if (parseResult.bg.length > 0) {
      parseResult.bg.forEach(hexColor => {
        // 将HEX颜色转换为ARGB整数
        const rgba = ColorUtils.fromHex(hexColor);
        const argbInt = ColorUtils.toArgbInt(rgba);

        // 创建层配置
        const layer = {
          type: 'color_normal',
          colors: [argbInt],
          colorTypes: ['color_normal'],
          positions: [0],
          angle: 0,
          radial: 0.5,
          xOffset: 0.5,
          yOffset: 0.5
        };

        state.iconEditor.globalColorConfig.bg.push(layer);
      });
    }

    // 更新UI
    renderIconColorPanel();
    updateAllIconPreviews();
    saveState();

    showToast(`导入成功：前景色${parseResult.fg.length}层，背景色${parseResult.bg.length}层`);
  }

  function importIconColorsJson(jsonText) {
    try {
      const data = JSON.parse(jsonText);

      if (!data.icon_colors || !Array.isArray(data.icon_colors) || data.icon_colors.length === 0) {
        showToast('JSON格式错误：缺少icon_colors数组');
        return;
      }

      // 读取icon_pack_name
      if (data.icon_pack_name) {
        state.iconEditor.currentIconPackName = data.icon_pack_name;
      }

      // 读取icon_pack_url
      if (data.icon_pack_url) {
        state.iconEditor.currentIconPackUrl = data.icon_pack_url;
        // 尝试从URL导入图标包
        if (data.icon_pack_url.trim()) {
          try {
            handleIconPackImportFromUrl(data.icon_pack_url).catch(() => {
              console.log('Failed to load icon pack from URL, using default');
            });
          } catch (e) {
            console.log('Failed to load icon pack from URL, using default');
          }
        }
      } else {
        state.iconEditor.currentIconPackUrl = '';
      }

      // 读取全局pre_method
      if (data.pre_method !== undefined) {
        state.iconEditor.globalColorConfig.pre_method = data.pre_method;
      }

      // 读取id字段
      if (data.id) {
        state.iconEditor.currentSchemeId = data.id;
      }

      // 读取icon_shape字段
      if (data.icon_shape) {
        // 按分号分割为数组
        state.iconEditor.selectedShapes = data.icon_shape.split(';').filter(s => s.trim());
      } else {
        // 旧数据没有icon_shape字段，设置为空数组
        state.iconEditor.selectedShapes = [];
      }

      const iconColor = data.icon_colors[0];

      // 转换fg层
      if (iconColor.fg && Array.isArray(iconColor.fg)) {
        state.iconEditor.globalColorConfig.fg = iconColor.fg.map(layer => convertJsonToLayer(layer));
      }

      // 转换bg层（支持数组格式）
      if (iconColor.bg && Array.isArray(iconColor.bg)) {
        state.iconEditor.globalColorConfig.bg = iconColor.bg.map(layer => convertJsonToLayer(layer));
      } else if (iconColor.bg && typeof iconColor.bg === 'object') {
        // 兼容旧格式：bg为单个对象
        state.iconEditor.globalColorConfig.bg = [convertJsonToLayer(iconColor.bg)];
      }

      // 更新UI
      renderIconColorPanel();
      renderShapePanel(); // 更新Shape面板
      updateAllIconPreviews();
      saveState();

      showToast('JSON导入成功');
    } catch (error) {
      console.error('Import JSON failed:', error);
      showToast('JSON解析失败');
    }
  }

  function convertJsonToLayer(jsonLayer) {
    // colors应该保持为ARGB整数格式（不转换）
    return {
      type: jsonLayer.gradient?.type || 'color_normal',
      colors: jsonLayer.colors, // 保持ARGB整数格式
      colorTypes: jsonLayer.colorTypes || jsonLayer.colors.map(() => 'color_normal'),
      positions: jsonLayer.gradient?.positions || [],
      angle: jsonLayer.gradient?.angle || 0,
      radial: jsonLayer.gradient?.radial || 1.0,
      xOffset: jsonLayer.gradient?.xOffset || 0.5,
      yOffset: jsonLayer.gradient?.yOffset || 0.5
    };
  }

  // ========== 图标导出 ==========
  function exportCurrentIcon() {
    const iconName = state.iconEditor.currentIcon;
    if (!iconName) {
      showToast('请先选择一个图标');
      return;
    }

    const icon = state.iconEditor.icons.find(i => i.name === iconName);
    if (!icon) return;

    const colorConfig = state.iconEditor.globalColorConfig;
    const canvas = iconRenderer.compositeIcon(icon, colorConfig, 256); // 导出更高分辨率

    canvas.toBlob((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${iconName}.png`;
      a.click();
      URL.revokeObjectURL(url);
      showToast(`已导出 ${iconName}.png`);
    }, 'image/png');
  }

  function exportAllIcons() {
    if (state.iconEditor.icons.length === 0) {
      showToast('没有可导出的图标');
      return;
    }

    showToast(`正在导出 ${state.iconEditor.icons.length} 个图标...`);

    const colorConfig = state.iconEditor.globalColorConfig;
    let exportedCount = 0;

    // 逐个导出（简化版，不使用ZIP）
    state.iconEditor.icons.forEach((icon, index) => {
      setTimeout(() => {
        const canvas = iconRenderer.compositeIcon(icon, colorConfig, 256);
        canvas.toBlob((blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `${icon.name}.png`;
          a.click();
          URL.revokeObjectURL(url);

          exportedCount++;
          if (exportedCount === state.iconEditor.icons.length) {
            showToast(`已导出全部 ${exportedCount} 个图标`);
          }
        }, 'image/png');
      }, index * 200); // 延迟200ms避免浏览器阻止多个下载
    });
  }

  // ========== 图标包导入 ==========
  let storageManager = null;
  let fileUploadHandler = null;

  async function handleIconPackImport(files) {
    if (!files || files.length === 0) {
      showToast('未选择任何文件');
      return;
    }

    try {
      // 初始化处理器
      if (!fileUploadHandler) fileUploadHandler = new FileUploadHandler();
      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }

      // 显示进度
      showToast('正在验证文件...');

      // 验证文件
      const validation = fileUploadHandler.validateIconFiles(files);
      if (!validation.valid) {
        showToast('文件验证失败：\n' + validation.errors.join('\n'));
        return;
      }

      // 询问图标包名称
      const packName = prompt('请输入图标包名称：', '自定义图标包');
      if (!packName) {
        showToast('已取消导入');
        return;
      }

      showToast('正在创建图标包...');

      // 创建图标包
      const iconPack = await fileUploadHandler.createIconPack(files, packName);

      showToast('正在保存到数据库...');

      // 保存到IndexedDB
      const packId = await storageManager.saveIconPack(iconPack);

      showToast(`图标包 "${packName}" 导入成功！ID: ${packId}`);

      // 刷新图标包列表
      renderIconPackList();
    } catch (error) {
      console.error('Import icon pack failed:', error);
      showToast('导入失败：' + error.message);
    }
  }

  async function handleIconPackImportFromUrl(url) {
    if (!url || !url.trim()) {
      showToast('URL不能为空');
      return;
    }

    url = url.trim();
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      showToast('URL格式错误：必须以http://或https://开头');
      return;
    }

    try {
      if (!fileUploadHandler) fileUploadHandler = new FileUploadHandler();
      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }

      showToast('正在下载ZIP文件...');
      const response = await fetch(url);
      if (!response.ok) throw new Error(`下载失败: ${response.status}`);

      const zipBlob = await response.blob();
      showToast('正在解析ZIP文件...');

      const zip = await JSZip.loadAsync(zipBlob);
      const files = [];

      for (const [filename, zipEntry] of Object.entries(zip.files)) {
        if (zipEntry.dir || !filename.toLowerCase().endsWith('.png')) continue;
        const blob = await zipEntry.async('blob');
        files.push(new File([blob], filename.split('/').pop(), { type: 'image/png' }));
      }

      if (files.length === 0) {
        showToast('ZIP中没有找到PNG文件');
        return;
      }

      const packName = prompt('请输入图标包名称：', '网络图标包');
      if (!packName) {
        showToast('已取消导入');
        return;
      }

      showToast('正在创建图标包...');
      const iconPack = await fileUploadHandler.createIconPack(files, packName);
      iconPack.sourceUrl = url;

      showToast('正在保存到数据库...');
      const packId = await storageManager.saveIconPack(iconPack);

      showToast(`图标包 "${packName}" 导入成功！ID: ${packId}`);
      renderIconPackList();
    } catch (error) {
      console.error('Import from URL failed:', error);
      showToast('导入失败：' + error.message);
    }
  }

  function showImportProgress(current, total) {
    showToast(`正在导入 ${current}/${total}...`);
  }

  // ========== 编辑面板Tab切换 ==========
  function switchEditTab(tabName) {
    // 更新tab按钮状态
    document.querySelectorAll('.edit-tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.tab === tabName);
    });

    // 更新面板内容显示
    document.getElementById('colorEditPanel').classList.toggle('active', tabName === 'color');
    document.getElementById('decorationEditPanel').classList.toggle('active', tabName === 'decoration');
    document.getElementById('shapeEditPanel').classList.toggle('active', tabName === 'shape');
    document.getElementById('wallpaperEditPanel').classList.toggle('active', tabName === 'wallpaper');
    document.getElementById('widgetEditPanel').classList.toggle('active', tabName === 'widget');
  }

  // ========== 预览Tab切换 ==========
  function switchPreviewTab(tabName) {
    // 更新tab按钮状态
    document.querySelectorAll('.preview-tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.tab === tabName);
    });

    // 更新内容区域显示
    const iconPreviewContent = document.getElementById('iconPreviewContent');
    const wallpaperPreviewContent = document.getElementById('wallpaperPreviewContent');

    if (iconPreviewContent) {
      iconPreviewContent.classList.toggle('active', tabName === 'iconPreview');
    }
    if (wallpaperPreviewContent) {
      wallpaperPreviewContent.classList.toggle('active', tabName === 'wallpaperPreview');
    }

    // 如果切换到壁纸预览，更新手机预览区
    if (tabName === 'wallpaperPreview') {
      updatePhonePreview();
      renderPhoneIconGrid();
    }
  }

  // ========== Shape形状管理 ==========
  /**
   * 为图标选择Shape（确定性随机）
   * @param {string} iconName - 图标名称
   * @param {Array} selectedShapes - 选中的Shape ID数组
   * @returns {object|null} - Shape对象或null
   */
  function getShapeForIcon(iconName, selectedShapes) {
    if (!selectedShapes || selectedShapes.length === 0) {
      return null;
    }

    if (selectedShapes.length === 1) {
      // 只有一个shape，直接返回
      const shapeId = selectedShapes[0];
      return state.iconEditor.availableShapes.find(s => s.id === shapeId);
    }

    // 多个shape，使用确定性随机（基于图标名称hash）
    let hash = 0;
    for (let i = 0; i < iconName.length; i++) {
      hash = ((hash << 5) - hash) + iconName.charCodeAt(i);
      hash = hash & hash; // Convert to 32bit integer
    }
    const index = Math.abs(hash) % selectedShapes.length;
    const shapeId = selectedShapes[index];
    return state.iconEditor.availableShapes.find(s => s.id === shapeId);
  }

  function renderShapePanel() {
    const container = document.getElementById('shapeGrid');
    if (!container) {
      console.warn('Shape grid container not found');
      return;
    }

    container.innerHTML = '';

    const shapes = state.iconEditor.availableShapes;

    if (shapes.length === 0) {
      container.innerHTML = '<p style="color: var(--muted); font-size: 12px; padding: 12px;">正在加载形状...</p>';
      return;
    }

    shapes.forEach((shape, index) => {
      const item = document.createElement('div');
      item.className = 'shape-item';
      if (state.iconEditor.selectedShapes.includes(shape.id)) {
        item.classList.add('selected');
      }

      // SVG预览
      const preview = document.createElement('div');
      preview.className = 'shape-preview';

      try {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        const viewBoxWidth = shape.viewBoxWidth || 100;
        const viewBoxHeight = shape.viewBoxHeight || 100;
        svg.setAttribute('viewBox', `0 0 ${viewBoxWidth} ${viewBoxHeight}`);
        svg.setAttribute('preserveAspectRatio', 'xMidYMid meet');
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('d', shape.pathData);
        path.setAttribute('fill', 'var(--accent)');
        svg.appendChild(path);
        preview.appendChild(svg);
      } catch (error) {
        console.error(`Failed to render shape ${shape.id}:`, error);
        // 显示错误占位符
        preview.innerHTML = '<div style="color: var(--muted); font-size: 10px;">渲染失败</div>';
      }

      // Shape名称
      const name = document.createElement('div');
      name.className = 'shape-name';
      name.textContent = shape.name;

      // 选中标记
      const checkbox = document.createElement('div');
      checkbox.className = 'shape-checkbox';
      if (state.iconEditor.selectedShapes.includes(shape.id)) {
        checkbox.innerHTML = '✓';
      }

      item.appendChild(checkbox);
      item.appendChild(preview);
      item.appendChild(name);

      // 点击事件
      item.addEventListener('click', () => toggleShapeSelection(shape.id));

      container.appendChild(item);
    });
  }

  function toggleShapeSelection(shapeId) {
    const selectedShapes = state.iconEditor.selectedShapes;
    const index = selectedShapes.indexOf(shapeId);

    if (index > -1) {
      // 已选中，取消选择
      selectedShapes.splice(index, 1);
      console.log('Deselected shape:', shapeId);
    } else {
      // 未选中，添加选择
      selectedShapes.push(shapeId);
      console.log('Selected shape:', shapeId);
    }

    console.log('Current selected shapes:', selectedShapes);

    // 更新UI
    renderShapePanel();
    updateAllIconPreviews();
    saveState();
  }

  // ========== 图标包管理 ==========
  async function renderIconPackList() {
    const container = $('iconPackList');
    if (!container) return;

    container.innerHTML = '<p style="color: var(--muted); font-size: 12px;">加载中...</p>';

    try {
      // 初始化storageManager
      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }

      // 获取所有图标包
      const packs = await storageManager.getAllIconPacks();

      container.innerHTML = '';

      // 添加默认图标包
      const defaultItem = document.createElement('div');
      defaultItem.className = 'icon-pack-item';
      if (state.iconEditor.currentIconPackName === '默认图标包') {
        defaultItem.classList.add('active');
      }

      const defaultInfo = document.createElement('div');
      defaultInfo.className = 'icon-pack-info';
      defaultInfo.innerHTML = `
        <div class="icon-pack-name">默认图标包</div>
        <div class="icon-pack-meta">内置图标</div>
      `;

      const defaultActions = document.createElement('div');
      defaultActions.className = 'icon-pack-actions';
      const defaultSwitchBtn = document.createElement('button');
      defaultSwitchBtn.className = 'btn btn-secondary';
      defaultSwitchBtn.textContent = '切换';
      defaultSwitchBtn.style.fontSize = '12px';
      defaultSwitchBtn.style.padding = '4px 8px';
      defaultSwitchBtn.addEventListener('click', () => switchIconPack(null, '默认图标包'));
      defaultActions.appendChild(defaultSwitchBtn);

      defaultItem.appendChild(defaultInfo);
      defaultItem.appendChild(defaultActions);
      container.appendChild(defaultItem);

      // 渲染用户导入的图标包
      packs.forEach(pack => {
        const item = document.createElement('div');
        item.className = 'icon-pack-item';
        if (state.iconEditor.currentIconPackName === pack.name) {
          item.classList.add('active');
        }

        const info = document.createElement('div');
        info.className = 'icon-pack-info';

        // 兼容新旧格式
        let iconCount = 0;
        let layerCount = 0;
        if (pack.icons) {
          // 新格式
          iconCount = pack.icons.length;
          layerCount = pack.icons.length > 0 ? pack.icons[0].layers.length : 0;
        } else if (pack.foregroundLayers) {
          // 旧格式
          iconCount = 1;
          layerCount = pack.foregroundLayers.length;
        }

        const displayUrl = pack.sourceUrl && pack.sourceUrl.length > 60
          ? pack.sourceUrl.slice(0, 30) + '..' + pack.sourceUrl.slice(-28)
          : pack.sourceUrl;

        info.innerHTML = `
          <div class="icon-pack-name">${pack.name}</div>
          <div class="icon-pack-meta">${iconCount} 个图标 | ${layerCount} 层 | ${new Date(pack.createdAt).toLocaleDateString()}</div>
          ${pack.sourceUrl ? `<div class="icon-pack-url" style="font-size: 11px; color: var(--muted); margin-top: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" title="${pack.sourceUrl}">来源: ${displayUrl}</div>` : ''}
        `;

        const actions = document.createElement('div');
        actions.className = 'icon-pack-actions';

        const switchBtn = document.createElement('button');
        switchBtn.className = 'btn btn-secondary';
        switchBtn.textContent = '切换';
        switchBtn.style.fontSize = '12px';
        switchBtn.style.padding = '4px 8px';
        switchBtn.addEventListener('click', () => switchIconPack(pack.id, pack.name));

        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn btn-secondary';
        deleteBtn.textContent = '删除';
        deleteBtn.style.fontSize = '12px';
        deleteBtn.style.padding = '4px 8px';
        deleteBtn.addEventListener('click', () => deleteIconPackWithConfirm(pack.id, pack.name));

        actions.appendChild(switchBtn);
        actions.appendChild(deleteBtn);

        item.appendChild(info);
        item.appendChild(actions);
        container.appendChild(item);
      });

      if (packs.length === 0) {
        const emptyMsg = document.createElement('p');
        emptyMsg.style.color = 'var(--muted)';
        emptyMsg.style.fontSize = '12px';
        emptyMsg.style.marginTop = '8px';
        emptyMsg.textContent = '暂无自定义图标包';
        container.appendChild(emptyMsg);
      }
    } catch (error) {
      console.error('Failed to render icon pack list:', error);
      container.innerHTML = '<p style="color: var(--error); font-size: 12px;">加载失败</p>';
    }
  }

  async function switchIconPack(packId, packName) {
    try {
      showToast(`正在切换到 "${packName}"...`);

      if (packId === null) {
        // 切换到默认图标包
        state.iconEditor.currentIconPackName = packName;
        state.iconEditor.currentIconPackUrl = '';
        // 重新加载默认图标
        await initIconEditor();
      } else {
        // 加载自定义图标包
        if (!storageManager) {
          storageManager = new StorageManager();
          await storageManager.init();
        }

        // 从IndexedDB获取图标包
        const pack = await storageManager.getIconPack(packId);
        if (!pack) {
          showToast('图标包不存在');
          return;
        }

        // 使用IconLoader从图标包数据加载（创建新实例以确保有最新方法）
        const loader = new IconLoader();
        const icons = await loader.loadIconPackFromData(pack);

        // 更新state
        state.iconEditor.icons = icons;
        state.iconEditor.currentIconPackName = pack.name;
        state.iconEditor.currentIconPackUrl = pack.sourceUrl || '';
        state.iconEditor.currentIcon = icons[0]?.name || null;

        // 渲染图标网格
        renderIconGrid();

        showToast(`已切换到 "${packName}"`);
      }

      renderIconPackList();
      saveState();
    } catch (error) {
      console.error('Failed to switch icon pack:', error);
      showToast('切换失败：' + error.message);
    }
  }

  async function deleteIconPackWithConfirm(packId, packName) {
    if (!confirm(`确定要删除图标包 "${packName}" 吗？此操作不可恢复。`)) {
      return;
    }

    try {
      showToast('正在删除...');

      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }

      await storageManager.deleteIconPack(packId);

      showToast(`已删除图标包 "${packName}"`);

      // 如果删除的是当前图标包，切换到默认
      if (state.iconEditor.currentIconPackName === packName) {
        await switchIconPack(null, '默认图标包');
      }

      renderIconPackList();
    } catch (error) {
      console.error('Failed to delete icon pack:', error);
      showToast('删除失败：' + error.message);
    }
  }

  // ========== 颜色方案管理 ==========
  async function saveColorScheme() {
    try {
      const schemeName = prompt('请输入颜色方案名称：', '我的配色方案');
      if (!schemeName) {
        showToast('已取消保存');
        return;
      }

      // 初始化storageManager
      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }

      const colorConfig = state.iconEditor.globalColorConfig;

      // 创建颜色方案对象
      const scheme = {
        name: schemeName,
        iconPackName: state.iconEditor.currentIconPackName,
        colorConfig: {
          fg: JSON.parse(JSON.stringify(colorConfig.fg)),
          bg: JSON.parse(JSON.stringify(colorConfig.bg)),
          pre_method: colorConfig.pre_method
        },
        createdAt: new Date().toISOString()
      };

      const schemeId = await storageManager.saveColorScheme(scheme);

      // 保存方案名到state
      state.iconEditor.currentSchemeId = schemeName;
      saveState();

      showToast(`颜色方案 "${schemeName}" 保存成功！ID: ${schemeId}`);
    } catch (error) {
      console.error('Failed to save color scheme:', error);
      showToast('保存失败：' + error.message);
    }
  }

  async function manageColorSchemes() {
    try {
      // 初始化storageManager
      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }

      // 获取所有颜色方案
      const schemes = await storageManager.getAllColorSchemes();

      if (schemes.length === 0) {
        showToast('暂无保存的颜色方案');
        return;
      }

      // 构建方案列表HTML
      let html = '<div style="padding: 20px; max-width: 500px;">';
      html += '<h3 style="margin: 0 0 16px; color: var(--text);">颜色方案管理</h3>';
      html += '<div style="max-height: 400px; overflow-y: auto;">';

      schemes.forEach(scheme => {
        html += `
          <div style="padding: 12px; margin-bottom: 8px; background: var(--bg); border: 1px solid var(--border); border-radius: 6px;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div>
                <div style="font-weight: bold; color: var(--text);">${scheme.name}</div>
                <div style="font-size: 12px; color: var(--muted);">
                  图标包: ${scheme.iconPackName} | ${new Date(scheme.createdAt).toLocaleString()}
                </div>
              </div>
              <div style="display: flex; gap: 8px;">
                <button class="btn btn-secondary" onclick="loadColorScheme(${scheme.id})" style="font-size: 12px; padding: 4px 8px;">加载</button>
                <button class="btn btn-secondary" onclick="deleteColorScheme(${scheme.id}, '${scheme.name}')" style="font-size: 12px; padding: 4px 8px;">删除</button>
              </div>
            </div>
          </div>
        `;
      });

      html += '</div>';
      html += '<button class="btn" onclick="closeColorSchemeModal()" style="width: 100%; margin-top: 16px;">关闭</button>';
      html += '</div>';

      // 使用模态窗口显示
      const modal = els.modal;
      const modalContent = modal.querySelector('.modal-content');
      modalContent.innerHTML = html;
      modal.classList.add('show');
      modal.setAttribute('aria-hidden', 'false');
    } catch (error) {
      console.error('Failed to manage color schemes:', error);
      showToast('加载失败：' + error.message);
    }
  }

  async function loadColorScheme(schemeId) {
    try {
      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }

      const scheme = await storageManager.getColorScheme(schemeId);
      if (!scheme) {
        showToast('方案不存在');
        return;
      }

      // 应用颜色配置
      state.iconEditor.globalColorConfig.fg = JSON.parse(JSON.stringify(scheme.colorConfig.fg));
      state.iconEditor.globalColorConfig.bg = JSON.parse(JSON.stringify(scheme.colorConfig.bg));
      state.iconEditor.globalColorConfig.pre_method = scheme.colorConfig.pre_method || '';

      // 更新currentSchemeId
      state.iconEditor.currentSchemeId = scheme.name;

      // 更新UI
      renderIconColorPanel();
      updateAllIconPreviews();
      saveState();

      closeColorSchemeModal();
      showToast(`已加载方案 "${scheme.name}"`);
    } catch (error) {
      console.error('Failed to load color scheme:', error);
      showToast('加载失败：' + error.message);
    }
  }

  async function deleteColorScheme(schemeId, schemeName) {
    if (!confirm(`确定要删除颜色方案 "${schemeName}" 吗？`)) {
      return;
    }

    try {
      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }

      await storageManager.deleteColorScheme(schemeId);

      showToast(`已删除方案 "${schemeName}"`);

      // 刷新方案列表
      manageColorSchemes();
    } catch (error) {
      console.error('Failed to delete color scheme:', error);
      showToast('删除失败：' + error.message);
    }
  }

  function closeColorSchemeModal() {
    els.modal.classList.remove('show');
    els.modal.setAttribute('aria-hidden', 'true');
  }

  // 将函数暴露到全局，供HTML onclick使用
  window.loadColorScheme = loadColorScheme;
  window.deleteColorScheme = deleteColorScheme;
  window.closeColorSchemeModal = closeColorSchemeModal;

  window.deleteColorScheme = deleteColorScheme;
  window.closeColorSchemeModal = closeColorSchemeModal;

  // ========== 视差壁纸导入功能 ==========
  async function handleParallaxWallpaperImport(files) {
    if (!files || files.length === 0) {
      showToast('未选择任何文件');
      return;
    }

    // 1. 过滤文件
    const validFiles = Array.from(files).filter(file => {
      return file.name.toLowerCase().endsWith('.png') &&
        file.name.toLowerCase().startsWith('wallpaper');
    });

    if (validFiles.length === 0) {
      showToast('未找到有效的视差壁纸文件 (需为png且以wallpaper开头)');
      return;
    }

    // 2. 排序文件 (按文件名升序)
    validFiles.sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true, sensitivity: 'base' }));

    showToast(`正在处理 ${validFiles.length} 个图层...`);

    try {
      // 3. 生成合成图 (用于预览和缩略图)
      const compositeBlob = await generateCompositeImage(validFiles);

      // 生成缩略图
      const thumbnail = await generateThumbnail(compositeBlob);

      // 获取图片尺寸 (使用第一张图或合成图的尺寸)
      const img = await loadImage(compositeBlob);

      // 4. 读取所有图层的Blob并存储
      const layers = [];
      for (const file of validFiles) {
        layers.push({
          name: file.name,
          blob: file // File对象本身就是Blob
        });
      }

      // 5. 添加到state
      const wallpaper = {
        id: Date.now() + '_parallax',
        name: '视差壁纸组 (' + validFiles.length + '层)',
        type: 'parallax', // 标记为视差壁纸
        blob: compositeBlob, // 合成图用于常规预览
        thumbnail: thumbnail,
        width: img.width,
        height: img.height,
        layers: layers // 保存原始图层
      };

      state.iconEditor.wallpapers.push(wallpaper);

      // 保存到IndexedDB
      try {
        if (!storageManager) {
          storageManager = new StorageManager();
          await storageManager.init();
        }
        await storageManager.saveWallpaper(wallpaper);
      } catch (error) {
        console.error('Failed to save wallpaper to cache:', error);
      }

      renderWallpaperGrid();
      showToast('成功导入视差壁纸');

    } catch (error) {
      console.error('Failed to process parallax wallpaper:', error);
      showToast('处理视差壁纸失败: ' + error.message);
    }
  }

  async function generateCompositeImage(files) {
    return new Promise(async (resolve, reject) => {
      try {
        if (files.length === 0) throw new Error('No files to composite');

        // 加载第一张图以确定尺寸
        const firstImg = await loadImageFromBlob(files[0]);
        const width = firstImg.width;
        const height = firstImg.height;

        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');

        // 按顺序绘制每一层
        for (const file of files) {
          const img = await loadImageFromBlob(file);
          ctx.drawImage(img, 0, 0, width, height); // 假设所有图层尺寸一致，简单拉伸适配底层
        }

        canvas.toBlob((blob) => {
          if (blob) resolve(blob);
          else reject(new Error('Canvas toBlob failed'));
        }, 'image/png'); // 合成图保持PNG以保留透明度(虽然壁纸通常不透明，但视差层可能透明)

      } catch (error) {
        reject(error);
      }
    });
  }

  async function loadImageFromBlob(blob) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = () => reject(new Error('Failed to load image'));
      img.src = URL.createObjectURL(blob);
    });
  }

  // ========== 壁纸导入功能 ==========
  async function handleWallpaperImport(files) {
    if (!files || files.length === 0) {
      showToast('未选择任何文件');
      return;
    }

    showToast(`正在处理 ${files.length} 张壁纸...`);

    for (let i = 0; i < files.length; i++) {
      const file = files[i];

      // 验证文件格式
      if (!file.type.match(/^image\/(jpeg|png|webp)$/)) {
        showToast(`跳过不支持的格式: ${file.name}`);
        continue;
      }

      try {
        // 转换为JPG
        const jpgBlob = await convertImageToJPG(file);

        // 生成缩略图
        const thumbnail = await generateThumbnail(jpgBlob);

        // 获取图片尺寸
        const img = await loadImage(jpgBlob);

        // 添加到state
        const wallpaper = {
          id: Date.now() + '_' + i,
          name: file.name,
          blob: jpgBlob,
          thumbnail: thumbnail,
          width: img.width,
          height: img.height
        };

        state.iconEditor.wallpapers.push(wallpaper);

        // 保存到IndexedDB
        try {
          if (!storageManager) {
            storageManager = new StorageManager();
            await storageManager.init();
          }
          await storageManager.saveWallpaper(wallpaper);
        } catch (error) {
          console.error('Failed to save wallpaper to cache:', error);
        }
      } catch (error) {
        console.error('Failed to process wallpaper:', file.name, error);
        showToast(`处理失败: ${file.name}`);
      }
    }

    renderWallpaperGrid();
    showToast(`成功导入 ${state.iconEditor.wallpapers.length} 张壁纸`);
  }

  async function convertImageToJPG(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const img = new Image();
        img.onload = () => {
          try {
            // 检查分辨率限制
            if (img.width > 8192 || img.height > 4192) {
              showToast('图片分辨率过大，建议压缩至8K以内');
            }

            // 创建Canvas
            const canvas = document.createElement('canvas');
            canvas.width = img.width;
            canvas.height = img.height;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0);

            // 转换为JPEG
            if (canvas.toBlob) {
              canvas.toBlob((blob) => {
                if (blob) {
                  resolve(blob);
                } else {
                  reject(new Error('toBlob returned null'));
                }
              }, 'image/jpeg', 0.9);
            } else {
              // 备用方案：使用toDataURL
              const dataUrl = canvas.toDataURL('image/jpeg', 0.9);
              fetch(dataUrl)
                .then(res => res.blob())
                .then(blob => resolve(blob))
                .catch(err => reject(err));
            }
          } catch (error) {
            console.error('Error in convertImageToJPG:', error);
            reject(error);
          }
        };
        img.onerror = (err) => {
          console.error('Image load error:', err);
          reject(new Error('Failed to load image'));
        };
        img.src = e.target.result;
      };
      reader.onerror = (err) => {
        console.error('FileReader error:', err);
        reject(new Error('Failed to read file'));
      };
      reader.readAsDataURL(file);
    });
  }

  async function generateThumbnail(imageBlob) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => {
        try {
          // 目标尺寸：90×195 (9:19.5竖屏比例)
          const targetWidth = 90;
          const targetHeight = 195;
          const targetRatio = 9 / 19.5;
          const sourceRatio = img.width / img.height;

          // 计算裁剪区域（中心裁剪）
          let sx, sy, sw, sh;
          if (sourceRatio > targetRatio) {
            // 图片更宽，裁剪左右
            sh = img.height;
            sw = sh * targetRatio;
            sx = (img.width - sw) / 2;
            sy = 0;
          } else {
            // 图片更高，裁剪上下
            sw = img.width;
            sh = sw / targetRatio;
            sx = 0;
            sy = (img.height - sh) / 2;
          }

          // 创建Canvas
          const canvas = document.createElement('canvas');
          canvas.width = targetWidth;
          canvas.height = targetHeight;
          const ctx = canvas.getContext('2d');
          ctx.drawImage(img, sx, sy, sw, sh, 0, 0, targetWidth, targetHeight);

          // 转换为Blob
          if (canvas.toBlob) {
            canvas.toBlob((blob) => {
              if (blob) {
                resolve(blob);
              } else {
                reject(new Error('toBlob returned null'));
              }
            }, 'image/jpeg', 0.85);
          } else {
            // 备用方案：使用toDataURL
            const dataUrl = canvas.toDataURL('image/jpeg', 0.85);
            fetch(dataUrl)
              .then(res => res.blob())
              .then(blob => resolve(blob))
              .catch(err => reject(err));
          }
        } catch (error) {
          console.error('Error in generateThumbnail:', error);
          reject(error);
        }
      };
      img.onerror = (err) => {
        console.error('Thumbnail image load error:', err);
        reject(new Error('Failed to load image'));
      };
      img.src = URL.createObjectURL(imageBlob);
    });
  }

  async function loadImage(blob) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = () => reject(new Error('Failed to load image'));
      img.src = URL.createObjectURL(blob);
    });
  }

  async function handleWallpaperUrlImport() {
    const url = prompt('请输入图片链接（HTTP/HTTPS）：', 'https://');
    if (!url) {
      showToast('已取消导入');
      return;
    }

    // 验证URL格式
    if (!url.match(/^https?:\/\/.+/)) {
      showToast('请输入有效的HTTP/HTTPS链接');
      return;
    }

    try {
      showToast('正在下载图片...');
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error('下载失败');
      }
      const blob = await response.blob();

      // 验证是否为图片
      if (!blob.type.match(/^image\/(jpeg|png|webp)$/)) {
        showToast('链接不是有效的图片格式');
        return;
      }

      // 创建File对象
      const file = new File([blob], 'wallpaper.jpg', { type: blob.type });
      await handleWallpaperImport([file]);
    } catch (error) {
      console.error('Failed to import from URL:', error);
      showToast('网络错误，请检查网络连接后重试');
    }
  }

  // ========== 壁纸展示与交互 ==========
  function renderWallpaperGrid() {
    const gridEl = $('wallpaperGrid');
    if (!gridEl) return;

    gridEl.innerHTML = '';

    // 安全检查：确保wallpapers数组存在
    if (!state.iconEditor.wallpapers || state.iconEditor.wallpapers.length === 0) {
      gridEl.innerHTML = '<p style="color: var(--muted); font-size: 12px; padding: 12px; text-align: center;">暂无壁纸，请点击上方按钮导入</p>';
      return;
    }

    state.iconEditor.wallpapers.forEach((wallpaper) => {
      const item = document.createElement('div');
      item.className = 'wallpaper-item';
      item.dataset.wallpaperId = wallpaper.id;

      if (state.iconEditor.selectedWallpaperId === wallpaper.id) {
        item.classList.add('selected');
      }

      // 缩略图
      const img = document.createElement('img');
      img.className = 'wallpaper-thumbnail';
      img.src = URL.createObjectURL(wallpaper.thumbnail);
      img.alt = wallpaper.name;

      // 选中标记
      const checkbox = document.createElement('div');
      checkbox.className = 'wallpaper-checkbox';
      if (state.iconEditor.selectedWallpaperId === wallpaper.id) {
        checkbox.innerHTML = '✓';
      }

      // 删除按钮
      const deleteBtn = document.createElement('button');
      deleteBtn.className = 'wallpaper-delete-btn';
      deleteBtn.innerHTML = '×';
      deleteBtn.title = '删除壁纸';
      deleteBtn.style.cssText = 'position: absolute; top: 4px; left: 4px; width: 24px; height: 24px; border-radius: 50%; background: rgba(0,0,0,0.6); color: white; border: none; cursor: pointer; font-size: 18px; line-height: 1; display: flex; align-items: center; justify-content: center;';
      deleteBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        deleteWallpaper(wallpaper.id);
      });

      item.appendChild(img);
      item.appendChild(checkbox);
      item.appendChild(deleteBtn);

      // 绑定点击事件
      item.addEventListener('click', () => selectWallpaper(wallpaper.id));

      gridEl.appendChild(item);
    });
  }

  function selectWallpaper(wallpaperId) {
    // 单选逻辑：如果点击已选中的，则取消选中
    if (state.iconEditor.selectedWallpaperId === wallpaperId) {
      state.iconEditor.selectedWallpaperId = null;
    } else {
      state.iconEditor.selectedWallpaperId = wallpaperId;
    }

    // 更新UI
    renderWallpaperGrid();

    // 更新手机预览
    updatePhonePreview();

    saveState();
  }

  async function deleteWallpaper(wallpaperId) {
    if (!confirm('确定要删除这张壁纸吗？')) {
      return;
    }

    try {
      // 从state中移除
      const index = state.iconEditor.wallpapers.findIndex(w => w.id === wallpaperId);
      if (index !== -1) {
        state.iconEditor.wallpapers.splice(index, 1);
      }

      // 如果删除的是当前选中的壁纸，清除选中状态
      if (state.iconEditor.selectedWallpaperId === wallpaperId) {
        state.iconEditor.selectedWallpaperId = null;
        updatePhonePreview();
      }

      // 从IndexedDB中删除
      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }
      await storageManager.deleteWallpaper(wallpaperId);

      // 重新渲染
      renderWallpaperGrid();
      showToast('壁纸已删除');
    } catch (error) {
      console.error('Failed to delete wallpaper:', error);
      showToast('删除失败：' + error.message);
    }
  }

  // ========== Widget功能实现 ==========

  /**
   * 解析Widget ZIP文件
   * @param {Blob} zipBlob - ZIP Blob对象
   * @param {string} fileName - 文件名
   * @returns {Promise<{preview_blob: Blob, widget_zip_blob: Blob, widget_name: string}>}
   */
  async function parseWidgetZip(zipBlob, fileName) {
    try {
      const zip = await JSZip.loadAsync(zipBlob);
      let preview_blob = null;
      let widget_zip_blob = null;
      let hasSubZip = false;

      // 遍历ZIP文件
      for (const [filename, file] of Object.entries(zip.files)) {
        if (file.dir) continue;

        // 查找Preview图片（不区分大小写）
        const lowerName = filename.toLowerCase();
        if (lowerName.includes('preview') && (lowerName.endsWith('.png') || lowerName.endsWith('.jpg') || lowerName.endsWith('.jpeg') || lowerName.endsWith('.webp'))) {
          preview_blob = await file.async('blob');
        }

        // 查找widget_zip文件
        if (lowerName.endsWith('.zip') && !lowerName.includes('preview')) {
          widget_zip_blob = await file.async('blob');
          hasSubZip = true;
        }
      }

      // 如果没有找到子ZIP文件，说明这是单独的Widget资源
      if (!hasSubZip) {
        // 整个ZIP作为widget_zip_blob
        widget_zip_blob = zipBlob;
        // preview_blob保持为null（用户后续可以手动添加）
        preview_blob = null;
      }

      // 提取widget_name（去除.zip扩展名）
      const widget_name = fileName.replace(/\.zip$/i, '');

      return {
        preview_blob,
        widget_zip_blob,
        widget_name
      };
    } catch (error) {
      console.error('Failed to parse widget ZIP:', error);
      throw error;
    }
  }

  /**
   * 处理本地Widget ZIP导入
   * @param {File} file - 文件对象
   */
  async function handleWidgetImportFromLocal(file) {
    if (!file) return;

    try {
      showToast('正在导入Widget...');

      // 解析ZIP文件
      const { preview_blob, widget_zip_blob, widget_name } = await parseWidgetZip(file, file.name);

      // 生成Widget对象
      const widgetId = 'widget_' + Date.now();
      const widget = {
        id: widgetId,
        widget_type: '',  // 默认为空，等待用户选择
        widget_name: widget_name,
        download_url: file.name,  // 本地文件使用文件名
        preview_blob: preview_blob,
        widget_zip_blob: widget_zip_blob,
        selected: false
      };

      // 添加到state
      state.iconEditor.widgets.push(widget);

      // 保存到IndexedDB
      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }
      await storageManager.saveWidget(widget);

      // 刷新UI
      renderWidgetGrid();
      showToast('Widget导入成功');
      saveState();
    } catch (error) {
      console.error('Failed to import widget:', error);
      showToast('导入失败：' + error.message);
    }
  }

  /**
   * 处理链接导入Widget
   * @param {string} url - Widget ZIP链接
   */
  async function handleWidgetImportFromUrl(url) {
    if (!url || !url.trim()) return;

    try {
      showToast('正在下载Widget...');

      // 下载ZIP文件
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error('下载失败：' + response.statusText);
      }

      const blob = await response.blob();

      // 提取文件名
      const fileName = url.split('/').pop() || 'widget.zip';

      // 解析ZIP文件
      const { preview_blob, widget_zip_blob, widget_name } = await parseWidgetZip(blob, fileName);

      // 生成Widget对象
      const widgetId = 'widget_' + Date.now();
      const widget = {
        id: widgetId,
        widget_type: '',
        widget_name: widget_name,
        download_url: url,  // 链接导入使用完整URL
        preview_blob: preview_blob,
        widget_zip_blob: widget_zip_blob,
        selected: false
      };

      // 添加到state
      state.iconEditor.widgets.push(widget);

      // 保存到IndexedDB
      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }
      await storageManager.saveWidget(widget);

      // 刷新UI
      renderWidgetGrid();
      showToast('Widget导入成功');
      saveState();
    } catch (error) {
      console.error('Failed to import widget from URL:', error);
      showToast('导入失败：' + error.message);
    }
  }

  /**
   * 渲染Widget网格
   */
  function renderWidgetGrid() {
    const gridEl = $('widgetGrid');
    if (!gridEl) return;

    gridEl.innerHTML = '';

    if (!state.iconEditor.widgets || state.iconEditor.widgets.length === 0) {
      gridEl.innerHTML = '<p style="text-align:center;color:var(--muted);padding:40px;">暂无Widget，点击上方按钮导入</p>';
      return;
    }

    state.iconEditor.widgets.forEach(widget => {
      const item = document.createElement('div');
      item.className = 'widget-item' + (widget.selected ? ' selected' : '');
      item.dataset.widgetId = widget.id;

      // 创建缩略图
      const thumbnail = document.createElement('img');
      thumbnail.className = 'widget-thumbnail';
      if (widget.preview_blob) {
        thumbnail.src = URL.createObjectURL(widget.preview_blob);
      } else {
        // 使用占位图
        thumbnail.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect width="100" height="100" fill="%23ddd"/><text x="50%" y="50%" text-anchor="middle" fill="%23999" font-size="12">无预览</text></svg>';
      }

      // 创建复选框
      const checkbox = document.createElement('div');
      checkbox.className = 'widget-checkbox';
      checkbox.textContent = '✓';

      item.appendChild(thumbnail);
      item.appendChild(checkbox);

      // 绑定点击事件
      item.addEventListener('click', () => toggleWidgetSelection(widget.id));

      gridEl.appendChild(item);
    });
  }

  /**
   * 切换Widget选中状态（单选模式，支持取消选中）
   * @param {string} widgetId - Widget ID
   */
  function toggleWidgetSelection(widgetId) {
    const widget = state.iconEditor.widgets.find(w => w.id === widgetId);
    if (!widget) return;

    // 如果点击的是已选中的Widget，则取消选中
    if (widget.selected) {
      widget.selected = false;
      state.iconEditor.currentWidgetId = null;
    } else {
      // 单选模式：先取消所有Widget的选中状态
      state.iconEditor.widgets.forEach(w => {
        w.selected = false;
      });

      // 选中当前Widget
      widget.selected = true;
      state.iconEditor.currentWidgetId = widgetId;
    }

    // 刷新UI
    renderWidgetGrid();
    renderWidgetEditSection();
    updatePhoneWidgetPreview();
    saveState();

    // 保存到IndexedDB
    if (storageManager) {
      storageManager.saveWidget(widget).catch(err => {
        console.error('Failed to save widget:', err);
      });
    }
  }

  /**
   * 渲染Widget编辑区域
   */
  function renderWidgetEditSection() {
    // 获取所有表单控件
    const previewImage = $('widgetPreviewImage');
    const changeWidgetPreview = $('changeWidgetPreview');
    const widgetTypeSelect = $('widgetType');
    const widgetNameInput = $('widgetName');
    const widgetDownloadUrlInput = $('widgetDownloadUrl');
    const deleteWidgetBtn = $('deleteWidget');

    if (!widgetTypeSelect) return;

    // 获取当前编辑的Widget
    const widget = state.iconEditor.widgets.find(w => w.id === state.iconEditor.currentWidgetId);

    // 重新构建widget_type下拉列表（预定义 + 自定义类型）
    const predefinedTypes = [
      { value: '', label: '请选择...' },
      { value: 'cool_clock_widget', label: 'cool_clock_widget - 酷黑时钟' },
      { value: 'Geometry_clock', label: 'Geometry_clock - 几何时钟' },
      { value: 'cmn_new_color_widget', label: 'cmn_new_color_widget - 彩色组件' }
    ];

    // 添加自定义类型
    const customTypes = state.iconEditor.customWidgetTypes.map(type => ({
      value: type,
      label: type + ' (自定义)'
    }));

    // 重建select选项
    widgetTypeSelect.innerHTML = '';
    [...predefinedTypes, ...customTypes].forEach(opt => {
      const option = document.createElement('option');
      option.value = opt.value;
      option.textContent = opt.label;
      widgetTypeSelect.appendChild(option);
    });

    // 添加"自定义..."选项
    const customOption = document.createElement('option');
    customOption.value = 'custom';
    customOption.textContent = '自定义...';
    widgetTypeSelect.appendChild(customOption);

    if (!widget) {
      // 未选中Widget：清空所有字段并禁用
      if (previewImage) {
        previewImage.src = '';
        previewImage.parentElement.style.display = 'none';
      }
      widgetTypeSelect.value = '';
      widgetTypeSelect.disabled = true;
      if (widgetNameInput) {
        widgetNameInput.value = '';
        widgetNameInput.disabled = true;
      }
      if (widgetDownloadUrlInput) {
        widgetDownloadUrlInput.value = '';
        widgetDownloadUrlInput.disabled = true;
      }
      if (changeWidgetPreview) {
        changeWidgetPreview.disabled = true;
        changeWidgetPreview.textContent = '📷 更换预览图';
      }
      if (deleteWidgetBtn) {
        deleteWidgetBtn.disabled = true;
      }
      return;
    }

    // 选中Widget：填充字段并启用
    if (previewImage) {
      if (widget.preview_blob) {
        previewImage.src = URL.createObjectURL(widget.preview_blob);
        previewImage.parentElement.style.display = 'block';
      } else {
        previewImage.src = '';
        previewImage.parentElement.style.display = 'none';
      }
    }

    // 设置widget_type选中值
    if (widget.widget_type) {
      // 检查是否在预定义或自定义类型中
      const allTypes = [...predefinedTypes.map(t => t.value), ...customTypes.map(t => t.value)];
      if (allTypes.includes(widget.widget_type)) {
        widgetTypeSelect.value = widget.widget_type;
      } else {
        // 不在列表中，可能是旧数据，添加到自定义类型
        if (!state.iconEditor.customWidgetTypes.includes(widget.widget_type)) {
          state.iconEditor.customWidgetTypes.push(widget.widget_type);
          // 重新渲染（递归调用一次）
          renderWidgetEditSection();
          return;
        }
      }
    } else {
      widgetTypeSelect.value = '';
    }
    widgetTypeSelect.disabled = false;

    if (widgetNameInput) {
      widgetNameInput.value = widget.widget_name || '';
      widgetNameInput.disabled = false;
    }

    if (widgetDownloadUrlInput) {
      widgetDownloadUrlInput.value = widget.download_url || '';
      widgetDownloadUrlInput.disabled = false;
    }

    if (changeWidgetPreview) {
      changeWidgetPreview.disabled = false;
    }

    if (deleteWidgetBtn) {
      deleteWidgetBtn.disabled = false;
    }
  }

  /**
   * 删除当前Widget
   */
  async function deleteCurrentWidget() {
    const widgetId = state.iconEditor.currentWidgetId;
    if (!widgetId) return;

    if (!confirm('确定要删除这个Widget吗？')) {
      return;
    }

    try {
      // 从state中移除
      const index = state.iconEditor.widgets.findIndex(w => w.id === widgetId);
      if (index !== -1) {
        state.iconEditor.widgets.splice(index, 1);
      }

      // 清除currentWidgetId
      state.iconEditor.currentWidgetId = null;

      // 从IndexedDB中删除
      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }
      await storageManager.deleteWidget(widgetId);

      // 刷新UI
      renderWidgetGrid();
      renderWidgetEditSection();
      updatePhoneWidgetPreview();
      showToast('Widget已删除');
      saveState();
    } catch (error) {
      console.error('Failed to delete widget:', error);
      showToast('删除失败：' + error.message);
    }
  }

  /**
   * 更新手机预览区的Widget预览
   */
  function updatePhoneWidgetPreview() {
    const layerEl = $('widgetPreviewLayer');
    if (!layerEl) return;

    // 清空
    layerEl.innerHTML = '';

    // 获取所有选中的Widget
    const selectedWidgets = state.iconEditor.widgets.filter(w => w.selected);
    if (selectedWidgets.length === 0) return;

    // 渲染每个Widget预览
    selectedWidgets.forEach((widget, index) => {
      if (!widget.preview_blob) return;

      const previewItem = document.createElement('div');
      previewItem.className = 'widget-preview-item';

      const img = document.createElement('img');
      img.src = URL.createObjectURL(widget.preview_blob);

      // 默认定位：水平居中，垂直间距20px
      const containerWidth = layerEl.offsetWidth || 276; // phone-preview默认宽度
      const widgetWidth = 100;
      const left = (containerWidth - widgetWidth) / 2;
      const top = 100 + (index * 120); // 每个Widget间距120px

      previewItem.style.left = left + 'px';
      previewItem.style.top = top + 'px';

      previewItem.appendChild(img);
      layerEl.appendChild(previewItem);
    });
  }

  /**
   * 更新当前Widget的类型
   * @param {string} type - Widget类型
   */
  function updateCurrentWidgetType(type) {
    const widgetId = state.iconEditor.currentWidgetId;
    if (!widgetId) return;

    const widget = state.iconEditor.widgets.find(w => w.id === widgetId);
    if (!widget) return;

    widget.widget_type = type;
    saveState();

    // 保存到IndexedDB
    if (storageManager) {
      storageManager.saveWidget(widget).catch(err => {
        console.error('Failed to save widget:', err);
      });
    }
  }

  /**
   * 更新当前Widget的名称
   * @param {string} name - Widget名称
   */
  function updateCurrentWidgetName(name) {
    const widgetId = state.iconEditor.currentWidgetId;
    if (!widgetId) return;

    const widget = state.iconEditor.widgets.find(w => w.id === widgetId);
    if (!widget) return;

    widget.widget_name = name;
    saveState();

    // 保存到IndexedDB
    if (storageManager) {
      storageManager.saveWidget(widget).catch(err => {
        console.error('Failed to save widget:', err);
      });
    }
  }

  /**
   * 更新当前Widget的下载地址
   * @param {string} url - 下载地址
   */
  function updateCurrentWidgetDownloadUrl(url) {
    const widgetId = state.iconEditor.currentWidgetId;
    if (!widgetId) return;

    const widget = state.iconEditor.widgets.find(w => w.id === widgetId);
    if (!widget) return;

    widget.download_url = url;
    saveState();

    // 保存到IndexedDB
    if (storageManager) {
      storageManager.saveWidget(widget).catch(err => {
        console.error('Failed to save widget:', err);
      });
    }
  }

  /**
   * 处理Widget预览图更换
   * @param {File} file - 图片文件
   */
  async function handleWidgetPreviewChange(file) {
    if (!file) return;

    const widgetId = state.iconEditor.currentWidgetId;
    if (!widgetId) {
      showToast('请先选中一个Widget');
      return;
    }

    const widget = state.iconEditor.widgets.find(w => w.id === widgetId);
    if (!widget) return;

    try {
      // 更新预览图
      widget.preview_blob = file;

      // 保存到IndexedDB
      if (!storageManager) {
        storageManager = new StorageManager();
        await storageManager.init();
      }
      await storageManager.saveWidget(widget);

      // 刷新UI
      renderWidgetGrid();
      renderWidgetEditSection();
      updatePhoneWidgetPreview();
      saveState();

      showToast('预览图已更新');
    } catch (error) {
      console.error('Failed to update widget preview:', error);
      showToast('更新预览图失败：' + error.message);
    }
  }

  // ========== 手机预览区实现 ==========
  function renderPhonePreview() {
    const previewEl = $('phonePreview');
    if (!previewEl) return;

    // 初始化预览区
    updatePhonePreview();
    renderPhoneIconGrid();
  }

  function updatePhonePreview() {
    const previewEl = $('phonePreview');
    if (!previewEl) return;

    // 获取选中的壁纸
    const wallpaperId = state.iconEditor.selectedWallpaperId;
    if (!wallpaperId) {
      // 没有选中壁纸，显示默认背景
      previewEl.style.backgroundImage = '';
      return;
    }

    const wallpaper = state.iconEditor.wallpapers.find(w => w.id === wallpaperId);
    if (!wallpaper) {
      previewEl.style.backgroundImage = '';
      return;
    }

    // 设置壁纸背景
    const url = URL.createObjectURL(wallpaper.blob);
    previewEl.style.backgroundImage = `url(${url})`;
  }

  function renderPhoneIconGrid() {
    const gridEl = $('phoneIconGrid');
    if (!gridEl) return;

    gridEl.innerHTML = '';

    // 获取图标列表
    const icons = state.iconEditor.icons;
    if (icons.length === 0) return;

    // 核心图标名称（按照指定顺序）
    const coreIconNames = ['phone', 'sms', 'camera', 'browser'];

    // 分离核心图标和其他图标（不区分大小写，模糊匹配）
    const coreIconsMap = {};
    const otherIconsData = [];

    icons.forEach(icon => {
      const iconNameLower = icon.name.toLowerCase();
      const matchedCoreName = coreIconNames.find(name => iconNameLower.includes(name));

      if (matchedCoreName) {
        coreIconsMap[matchedCoreName] = icon;
      } else {
        otherIconsData.push(icon);
      }
    });

    // 按照指定顺序排列核心图标
    const coreIconsData = [];
    coreIconNames.forEach(name => {
      if (coreIconsMap[name]) {
        coreIconsData.push(coreIconsMap[name]);
      }
    });

    // 最多显示3行：最底下1行是核心图标，上面最多2行是其他图标
    const maxOtherIcons = 8; // 2行 × 4列
    const displayOtherIcons = otherIconsData.slice(0, maxOtherIcons);

    // 构建图标列表（按行组织）
    const rows = [];

    // 如果有其他图标，按行添加
    if (displayOtherIcons.length > 0) {
      let currentRow = [];
      displayOtherIcons.forEach((icon, index) => {
        currentRow.push(icon);
        if (currentRow.length === 4 || index === displayOtherIcons.length - 1) {
          // 如果当前行不足4个，补齐空位
          while (currentRow.length < 4) {
            currentRow.push(null);
          }
          rows.push(currentRow);
          currentRow = [];
        }
      });
    }

    // 添加核心图标行（最后一行）
    const coreRow = [];
    coreIconsData.forEach(icon => {
      coreRow.push(icon);
    });
    // 补齐核心图标行到4个
    while (coreRow.length < 4) {
      coreRow.push(null);
    }
    rows.push(coreRow);

    // 渲染图标（按行渲染）
    const colorConfig = state.iconEditor.globalColorConfig;

    rows.forEach((row, rowIndex) => {
      const rowEl = document.createElement('div');
      rowEl.className = 'phone-icon-row';

      row.forEach((icon, colIndex) => {
        const item = document.createElement('div');
        item.className = 'phone-icon-item';

        if (icon) {
          // 获取Shape
          const shape = getShapeForIcon(icon.name, state.iconEditor.selectedShapes);

          // 渲染图标（75×75px）
          const canvas = iconRenderer.compositeIcon(icon, colorConfig, 75, shape);
          item.appendChild(canvas);
        }
        // 空白占位符不显示任何内容，只占位

        rowEl.appendChild(item);
      });

      gridEl.appendChild(rowEl);
    });
  }

  // ========== 导出预览图功能 ==========

  async function generatePreviewImageBlob(width, height, cropTop) {
    // 验证是否选中壁纸
    if (!state.iconEditor.selectedWallpaperId) {
      throw new Error('请先导入并选中壁纸');
    }

    // 获取壁纸
    const wallpaper = state.iconEditor.wallpapers.find(w => w.id === state.iconEditor.selectedWallpaperId);
    if (!wallpaper) {
      throw new Error('壁纸数据丢失');
    }

    // 创建Canvas
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');

    // 加载壁纸图片
    const wallpaperImg = await loadImageFromBlob(wallpaper.blob);

    // 计算壁纸的实际绘制区域
    // 壁纸原始尺寸
    const imgWidth = wallpaperImg.width;
    const imgHeight = wallpaperImg.height;

    // 目标Canvas尺寸
    const targetWidth = width;
    const targetHeight = height;

    // 计算目标宽高比
    const targetRatio = targetWidth / targetHeight;
    const sourceRatio = imgWidth / imgHeight;

    let sx, sy, sw, sh;

    if (sourceRatio > targetRatio) {
      // 图片比目标更宽，裁剪左右
      sh = imgHeight;
      sw = sh * targetRatio;
      sx = (imgWidth - sw) / 2;
      sy = 0;
    } else {
      // 图片比目标更高，裁剪上下
      sw = imgWidth;
      sh = sw / targetRatio;
      sx = 0;
      sy = (imgHeight - sh) / 2;
    }

    // 绘制壁纸 (居中裁剪)
    ctx.drawImage(
      wallpaperImg,
      sx, sy, sw, sh, // 源区域
      0, 0, targetWidth, targetHeight // 目标区域
    );

    // 检查是否需要绘制图标
    const iconGrid = document.getElementById('phoneIconGrid');
    if (iconGrid && !iconGrid.classList.contains('hidden')) {
      // 绘制图标
      await drawIconsOnCanvas(ctx, width, height, cropTop);
    }

    // 转换为Blob并返回
    return new Promise((resolve, reject) => {
      canvas.toBlob((blob) => {
        if (blob) {
          resolve(blob);
        } else {
          reject(new Error('生成预览图失败'));
        }
      }, 'image/jpeg', 0.9);
    });
  }

  async function exportPreviewImage() {
    // 验证是否选中壁纸
    if (!state.iconEditor.selectedWallpaperId) {
      showToast('请先导入并选中壁纸');
      return;
    }

    // 弹出选项对话框
    const options = [
      '19.5:9 - 1x (360×780)',
      '19.5:9 - 2x (720×1560)',
      '16:9 - 1x (360×640)',
      '16:9 - 2x (720×1280)'
    ];

    const choice = prompt(
      '请选择导出尺寸（输入序号）：\n' +
      '1. 19.5:9 - 1x (360×780)\n' +
      '2. 19.5:9 - 2x (720×1560)\n' +
      '3. 16:9 - 1x (360×640)\n' +
      '4. 16:9 - 2x (720×1280)',
      '1'
    );

    if (!choice) {
      showToast('已取消导出');
      return;
    }

    const choiceNum = parseInt(choice);
    if (isNaN(choiceNum) || choiceNum < 1 || choiceNum > 4) {
      showToast('无效的选项');
      return;
    }

    // 根据选择确定尺寸和比例
    let width, height, aspectRatio, cropTop;
    switch (choiceNum) {
      case 1: // 19.5:9 - 1x
        width = 360;
        height = 780;
        aspectRatio = '19.5:9';
        cropTop = false;
        break;
      case 2: // 19.5:9 - 2x
        width = 720;
        height = 1560;
        aspectRatio = '19.5:9';
        cropTop = false;
        break;
      case 3: // 16:9 - 1x
        width = 360;
        height = 640;
        aspectRatio = '16:9';
        cropTop = true;
        break;
      case 4: // 16:9 - 2x
        width = 720;
        height = 1280;
        aspectRatio = '16:9';
        cropTop = true;
        break;
    }

    try {
      showToast('正在生成预览图...');

      // 调用新函数生成预览图Blob
      const blob = await generatePreviewImageBlob(width, height, cropTop);

      // 下载预览图
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `preview_${aspectRatio.replace(':', 'x')}_${width}x${height}.jpg`;
      a.click();
      URL.revokeObjectURL(url);
      showToast('预览图导出成功！');

    } catch (error) {
      console.error('Export preview failed:', error);
      showToast('导出失败：' + error.message);
    }
  }

  async function drawIconsOnCanvas(ctx, canvasWidth, canvasHeight, cropTop) {
    const icons = state.iconEditor.icons;
    if (icons.length === 0) return;

    // 核心图标名称
    const coreIconNames = ['phone', 'sms', 'camera', 'browser'];

    // 分离核心图标和其他图标
    const coreIconsMap = {};
    const otherIconsData = [];

    icons.forEach(icon => {
      const iconNameLower = icon.name.toLowerCase();
      const matchedCoreName = coreIconNames.find(name => iconNameLower.includes(name));

      if (matchedCoreName) {
        coreIconsMap[matchedCoreName] = icon;
      } else {
        otherIconsData.push(icon);
      }
    });

    // 按照指定顺序排列核心图标
    const coreIconsData = [];
    coreIconNames.forEach(name => {
      if (coreIconsMap[name]) {
        coreIconsData.push(coreIconsMap[name]);
      }
    });

    // 最多显示3行：最底下1行是核心图标，上面最多2行是其他图标
    const maxOtherIcons = 8;
    const displayOtherIcons = otherIconsData.slice(0, maxOtherIcons);

    // 构建图标列表（按行组织）
    const rows = [];

    // 如果有其他图标，按行添加
    if (displayOtherIcons.length > 0) {
      let currentRow = [];
      displayOtherIcons.forEach((icon, index) => {
        currentRow.push(icon);
        if (currentRow.length === 4 || index === displayOtherIcons.length - 1) {
          while (currentRow.length < 4) {
            currentRow.push(null);
          }
          rows.push(currentRow);
          currentRow = [];
        }
      });
    }

    // 添加核心图标行
    const coreRow = [];
    coreIconsData.forEach(icon => {
      coreRow.push(icon);
    });
    while (coreRow.length < 4) {
      coreRow.push(null);
    }
    rows.push(coreRow);

    // 计算图标尺寸和位置（基于19.5:9的完整画布）
    const fullHeight = cropTop ? (canvasWidth / 9 * 19.5) : canvasHeight;

    // 图标尺寸计算
    const padding = canvasWidth * 0.05; // 左右边距
    const availableWidth = canvasWidth - padding * 2;
    const colGap = availableWidth * 0.03; // 列间距
    const actualIconSize = (availableWidth - colGap * 3) / 4; // 实际图标大小
    const rowGap = actualIconSize * 0.15; // 行间距
    const lastRowGap = actualIconSize * 0.3; // 最后一行额外间距

    // 计算总高度
    const totalRows = rows.length;
    const totalIconsHeight = totalRows * actualIconSize + (totalRows - 1) * rowGap + lastRowGap;

    // 计算起始Y位置（从底部开始，基于完整19.5:9画布）
    const bottomPadding = fullHeight * 0.05;
    let startY = fullHeight - totalIconsHeight - bottomPadding;

    // 如果是16:9裁剪，需要调整Y位置
    if (cropTop) {
      // 计算裁剪偏移量
      const cropOffset = fullHeight - canvasHeight;
      startY = startY - cropOffset;

      // 确保图标不会被裁剪到画布外
      if (startY < 0) {
        startY = padding;
      }
    }

    // 绘制图标
    const colorConfig = state.iconEditor.globalColorConfig;

    for (let rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      const row = rows[rowIndex];
      const isLastRow = rowIndex === rows.length - 1;

      const y = startY + rowIndex * (actualIconSize + rowGap) + (isLastRow ? lastRowGap : 0);

      // 检查图标是否在可见区域内
      if (y + actualIconSize < 0 || y > canvasHeight) {
        continue; // 跳过不可见的图标
      }

      for (let colIndex = 0; colIndex < row.length; colIndex++) {
        const icon = row[colIndex];
        if (!icon) continue;

        const x = padding + colIndex * (actualIconSize + colGap);

        // 获取Shape
        const shape = getShapeForIcon(icon.name, state.iconEditor.selectedShapes);

        // 渲染图标
        const iconCanvas = iconRenderer.compositeIcon(icon, colorConfig, actualIconSize, shape);

        // 绘制到主Canvas
        ctx.drawImage(iconCanvas, x, y, actualIconSize, actualIconSize);
      }
    }
  }

  function loadImageFromBlob(blob) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = reject;
      img.src = URL.createObjectURL(blob);
    });
  }

  // ========== ZIP导出功能 ==========
  async function exportZipPackage() {
    // 验证是否选中壁纸
    if (!state.iconEditor.selectedWallpaperId) {
      showToast('请先导入并选中壁纸');
      return;
    }

    // 弹出命名弹窗
    const today = new Date();
    const dateStr = today.getFullYear() +
      String(today.getMonth() + 1).padStart(2, '0') +
      String(today.getDate()).padStart(2, '0');
    const defaultName = `theme_package_${dateStr}`;

    const packageName = prompt('请输入ZIP包名称（1-30位，禁止特殊字符）：', defaultName);
    if (!packageName) {
      showToast('已取消导出');
      return;
    }

    // 验证文件名合法性
    if (packageName.length < 1 || packageName.length > 30) {
      showToast('文件名长度应为1-30位');
      return;
    }

    if (/[\\/:*?"<>|]/.test(packageName)) {
      showToast('文件名包含非法字符：\\/:*?"<>|');
      return;
    }

    try {
      showToast('正在生成ZIP包...');
      const zipBlob = await generateZipPackage(packageName);

      // 下载ZIP
      const url = URL.createObjectURL(zipBlob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${packageName}.zip`;
      a.click();
      URL.revokeObjectURL(url);

      showToast('导出成功！');
    } catch (error) {
      console.error('Failed to export ZIP:', error);
      showToast('导出失败：' + error.message);
    }
  }

  async function exportThemePackage() {
    // 验证是否选中壁纸
    if (!state.iconEditor.selectedWallpaperId) {
      showToast('请先导入并选中壁纸');
      return;
    }

    // 弹出命名弹窗
    const today = new Date();
    const dateStr = today.getFullYear() +
      String(today.getMonth() + 1).padStart(2, '0') +
      String(today.getDate()).padStart(2, '0');
    const defaultName = `theme_package_${dateStr}`;

    const packageName = prompt('请输入主题包名称（1-30位，禁止特殊字符）：', defaultName);
    if (!packageName) {
      showToast('已取消导出');
      return;
    }

    // 验证文件名合法性
    if (packageName.length < 1 || packageName.length > 30) {
      showToast('文件名长度应为1-30位');
      return;
    }

    if (/[\\/:*?"<>|]/.test(packageName)) {
      showToast('文件名包含非法字符：\\/:*?"<>|');
      return;
    }

    try {
      // 生成预览图1（360×780，19.5:9）
      showToast('正在生成预览图1...');
      const preview1Blob = await generatePreviewImageBlob(360, 780, false);

      // 生成预览图2（720×1560，19.5:9）
      showToast('正在生成预览图2...');
      const preview2Blob = await generatePreviewImageBlob(720, 1560, false);

      // 生成内层ZIP包
      showToast('正在打包ZIP...');
      const innerZipBlob = await generateZipPackage(packageName);

      // 生成外层ZIP包
      const outerZipBlob = await generateOuterZipPackage(packageName, innerZipBlob, preview1Blob, preview2Blob);

      // 下载外层ZIP
      const url = URL.createObjectURL(outerZipBlob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${packageName}.zip`;
      a.click();
      URL.revokeObjectURL(url);

      showToast('导出成功！');
    } catch (error) {
      console.error('Failed to export theme package:', error);
      showToast('导出失败：' + error.message);
    }
  }

  async function generateZipPackage(packageName) {
    return new Promise(async (resolve, reject) => {
      try {
        // 创建JSZip实例
        const zip = new JSZip();

        // 1. 添加壁纸文件
        const wallpaper = state.iconEditor.wallpapers.find(w => w.id === state.iconEditor.selectedWallpaperId);
        if (!wallpaper) {
          reject(new Error('未找到选中的壁纸'));
          return;
        }

        if (wallpaper.type === 'parallax') {
          // 视差壁纸：添加所有原始图层
          if (wallpaper.layers && wallpaper.layers.length > 0) {
            wallpaper.layers.forEach(layer => {
              zip.file(layer.name, layer.blob);
            });
          } else {
            // 异常情况回退
            zip.file('wallpaper.jpg', wallpaper.blob);
          }
        } else {
          // 静态壁纸：添加单张图片
          zip.file('wallpaper.jpg', wallpaper.blob);
        }

        // 2. 生成并添加JSON配置
        const jsonContent = generateIconColorAdapterJson();
        zip.file('icon_color_adapter.json', jsonContent);

        // 3. 添加Widget文件（新增）
        const selectedWidgets = state.iconEditor.widgets.filter(w => w.selected);
        for (const widget of selectedWidgets) {
          // 判断是否为本地导入（download_url为文件名）
          const isLocalFile = !widget.download_url.startsWith('http://') &&
                             !widget.download_url.startsWith('https://');
          if (isLocalFile && widget.widget_zip_blob) {
            // 添加widget_zip文件到ZIP包
            zip.file(widget.download_url, widget.widget_zip_blob);
          }
        }

        // 4. 生成ZIP Blob
        const zipBlob = await zip.generateAsync({
          type: 'blob',
          compression: 'DEFLATE',
          compressionOptions: { level: 6 }
        }, (metadata) => {
          // 进度回调
          const percent = Math.round(metadata.percent);
          showToast(`导出中... ${percent}%`);
        });

        resolve(zipBlob);
      } catch (error) {
        reject(error);
      }
    });
  }

  function generateIconColorAdapterJson() {
    const colorConfig = state.iconEditor.globalColorConfig;

    // 获取或生成ID
    let schemeId = state.iconEditor.currentSchemeId;
    if (!schemeId) {
      schemeId = 'scheme_' + Date.now();
    }

    // 转换为需求文档格式
    const json = {
      id: schemeId,
      icon_pack_name: state.iconEditor.currentIconPackName,
      icon_pack_url: state.iconEditor.currentIconPackUrl || '',
      pre_method: colorConfig.pre_method || '',
      icon_shape: state.iconEditor.selectedShapes.join(';'),
      icon_colors: [{
        fg: colorConfig.fg.map(layer => convertLayerToJson(layer)),
        bg: colorConfig.bg.map(layer => convertLayerToJson(layer))
      }]
    };

    // 添加Widget配置（只导出选中的Widget）
    const selectedWidgets = state.iconEditor.widgets.filter(w => w.selected);
    if (selectedWidgets.length > 0) {
      json.widget_cfgs = selectedWidgets.map(w => ({
        widget_type: w.widget_type,
        widget_name: w.widget_name,
        download_url: w.download_url
      }));
    }

    return JSON.stringify(json, null, 2);
  }

  async function generateOuterZipPackage(packageName, innerZipBlob, preview1Blob, preview2Blob) {
    return new Promise(async (resolve, reject) => {
      try {
        // 创建外层JSZip实例
        const outerZip = new JSZip();

        // 添加内层ZIP文件
        outerZip.file(`${packageName}.zip`, innerZipBlob);

        // 添加第一张预览图（360×640）
        outerZip.file(`${packageName}(1).jpg`, preview1Blob);

        // 添加第二张预览图（720×1280）
        outerZip.file(`${packageName}(2).jpg`, preview2Blob);

        // 生成外层ZIP Blob
        const outerZipBlob = await outerZip.generateAsync({
          type: 'blob',
          compression: 'DEFLATE',
          compressionOptions: { level: 6 }
        });

        resolve(outerZipBlob);
      } catch (error) {
        reject(error);
      }
    });
  }

  function wireEvents() {
    // 标签页切换事件
    document.querySelectorAll('.tab-btn').forEach(btn => {
      btn.addEventListener('click', () => switchTab(btn.dataset.tab));
    });

    // 编辑面板Tab切换事件
    document.querySelectorAll('.edit-tab-btn').forEach(btn => {
      btn.addEventListener('click', () => switchEditTab(btn.dataset.tab));
    });

    // 预览Tab切换事件
    document.querySelectorAll('.preview-tab-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        switchPreviewTab(btn.dataset.tab);
      });
    });

    // 切换图标显示/隐藏
    const toggleIconsBtn = document.getElementById('toggleIconsBtn');
    if (toggleIconsBtn) {
      toggleIconsBtn.addEventListener('click', () => {
        const iconGrid = document.getElementById('phoneIconGrid');
        const toggleText = document.getElementById('toggleIconsText');
        if (iconGrid && toggleText) {
          iconGrid.classList.toggle('hidden');
          toggleText.textContent = iconGrid.classList.contains('hidden') ? '显示图标' : '隐藏图标';
        }
      });
    }

    // 导出预览图
    const exportPreviewBtn = document.getElementById('exportPreviewBtn');
    if (exportPreviewBtn) {
      exportPreviewBtn.addEventListener('click', exportPreviewImage);
    }

    els.colorInput.addEventListener('input', (e) => setSeed(e.target.value));
    els.colorText.addEventListener('change', (e) => {
      const rgb = ColorUtils.parseFlexible(e.target.value);
      setSeed(ColorUtils.toHex(rgb));
    });
    els.hueSlider.addEventListener('input', () => drawSV());
    els.svCanvas.addEventListener('mousedown', (e) => {
      pickFromSV(e);
      const move = (ev) => pickFromSV(ev);
      const up = () => { window.removeEventListener('mousemove', move); window.removeEventListener('mouseup', up); };
      window.addEventListener('mousemove', move);
      window.addEventListener('mouseup', up);
    });
    els.styleSelect.addEventListener('change', (e) => { state.style = e.target.value; saveState(); recalcAndRender(); });
    els.themeToggle.addEventListener('click', () => { state.dark = !state.dark; els.themeToggle.textContent = state.dark ? '深色' : '浅色'; saveState(); recalcAndRender(); });
    els.modal.addEventListener('click', (e) => { if (e.target === els.modal) closeModal(); });
    els.modalClose.addEventListener('click', closeModal);

    els.downloadCSS.addEventListener('click', () => {
      const scheme = ColorScheme.generateScheme(state.seed, state.style, state.dark);
      const css = ColorScheme.toCssVariables(scheme, '--scheme');
      ColorUtils.downloadFile('scheme.css', css, 'text/css');
    });
    els.downloadJSON.addEventListener('click', () => {
      const scheme = ColorScheme.generateScheme(state.seed, state.style, state.dark);
      ColorUtils.downloadFile('scheme.json', ColorScheme.toJson(scheme), 'application/json');
    });
    els.downloadPNG.addEventListener('click', () => {
      const scheme = ColorScheme.generateScheme(state.seed, state.style, state.dark);
      const url = renderSchemePng(scheme);
      const a = document.createElement('a'); a.href = url; a.download = 'scheme.png'; a.click();
      URL.revokeObjectURL(url);
    });
    els.shareLink.addEventListener('click', () => {
      const hex = state.seed.replace('#', '');
      const url = `${location.origin}${location.pathname}?c=${hex}&style=${state.style}&dark=${state.dark ? 1 : 0}`;
      ColorUtils.copyText(url).then(() => showToast('分享链接已复制'));
    });
    els.resetAll.addEventListener('click', () => {
      state = { seed: '#6750A4', style: 'TONAL_SPOT', dark: false, h: 262, s: 60, v: 100 };
      els.styleSelect.value = state.style; els.themeToggle.textContent = '浅色';
      setSeed(state.seed);
      drawSV();
      saveState();
    });

    // 渐变编辑器事件
    els.gradType.addEventListener('change', (e) => {
      const v = e.target.value;
      const prev = state.gradient.type;
      state.gradient.type = v;
      if (prev === 'color_normal' && v !== 'color_normal') {
        // 从Normal切到渐变，自动将当前色作为起点
        state.gradient.colors = [state.seed, '#FFFFFF'];
        state.gradient.positions = [0, 1];
        state.gradient.colorTypes = ['color_normal', 'color_normal'];
      }
      if (v === 'color_normal') {
        // 回退为单色，保留首色
        state.gradient.colors = [state.gradient.colors[0] || state.seed];
        state.gradient.positions = [0];
        state.gradient.colorTypes = [state.gradient.colorTypes?.[0] || 'color_normal'];
      }
      renderGradientEditor(); saveState();
    });
    els.linearAngle.addEventListener('change', (e) => { state.gradient.angle = Number(e.target.value) || 0; renderGradPreview(); renderGradTrack(); saveState(); });
    els.sweepStart.addEventListener('change', (e) => { state.gradient.sweepStart = Number(e.target.value) || 0; renderGradPreview(); renderGradTrack(); saveState(); });
    els.sweepEnd.addEventListener('change', (e) => { state.gradient.sweepEnd = Number(e.target.value) || 360; renderGradPreview(); renderGradTrack(); saveState(); });
    els.radialX.addEventListener('change', (e) => { state.gradient.xOffset = Number(e.target.value) || 0.5; renderGradPreview(); saveState(); });
    els.radialY.addEventListener('change', (e) => { state.gradient.yOffset = Number(e.target.value) || 0.5; renderGradPreview(); saveState(); });
    els.radialSize.addEventListener('change', (e) => { state.gradient.radial = Number(e.target.value) || 0.5; renderGradPreview(); saveState(); });
    els.gradAddStop.addEventListener('click', addStop);
    els.gradReset.addEventListener('click', resetGradient);
    els.gradCopyCss.addEventListener('click', () => { ColorUtils.copyText(getCssGradient()).then(() => showToast('CSS已复制')); });
    els.gradExportJson.addEventListener('click', exportGradientJson);
    els.gradImportJson.addEventListener('click', () => els.gradImportFile.click());
    els.gradImportFile.addEventListener('change', (e) => {
      const f = e.target.files[0]; if (!f) return;
      const reader = new FileReader(); reader.onload = () => importGradientJsonText(String(reader.result || '')); reader.readAsText(f);
      e.target.value = '';
    });
    els.gradPreset.addEventListener('change', (e) => { if (e.target.value !== 'none') applyPreset(e.target.value); });

    // 图标编辑器事件
    const addFgLayerBtn = $('addFgLayer');
    if (addFgLayerBtn) addFgLayerBtn.addEventListener('click', addFgLayer);

    const addBgLayerBtn = $('addBgLayer');
    if (addBgLayerBtn) addBgLayerBtn.addEventListener('click', addBgLayer);

    // 全局pre_method事件
    const globalPreMethodSelect = $('globalPreMethod');

    if (globalPreMethodSelect) {
      globalPreMethodSelect.addEventListener('change', (e) => {
        state.iconEditor.globalColorConfig.pre_method = e.target.value;
        updateAllIconPreviews();
        saveState();
      });
    }

    // 颜色方案管理事件
    const saveColorSchemeBtn = $('saveColorScheme');
    if (saveColorSchemeBtn) saveColorSchemeBtn.addEventListener('click', saveColorScheme);

    const manageColorSchemesBtn = $('manageColorSchemes');
    if (manageColorSchemesBtn) manageColorSchemesBtn.addEventListener('click', manageColorSchemes);

    const iconExportJsonBtn = $('iconExportJson');
    if (iconExportJsonBtn) iconExportJsonBtn.addEventListener('click', exportIconColorsJson);

    const iconImportJsonBtn = $('iconImportJson');
    const iconImportFileInput = $('iconImportFile');
    if (iconImportJsonBtn && iconImportFileInput) {
      iconImportJsonBtn.addEventListener('click', () => iconImportFileInput.click());
      iconImportFileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = () => importIconColorsJson(String(reader.result || ''));
        reader.readAsText(file);
        e.target.value = '';
      });
    }

    const importColorValuesBtn = $('importColorValues');
    if (importColorValuesBtn) importColorValuesBtn.addEventListener('click', importColorValues);

    const exportCurrentIconBtn = $('exportCurrentIcon');
    if (exportCurrentIconBtn) exportCurrentIconBtn.addEventListener('click', exportCurrentIcon);

    const exportAllIconsBtn = $('exportAllIcons');
    if (exportAllIconsBtn) exportAllIconsBtn.addEventListener('click', exportAllIcons);

    // 图标包导入事件
    const importIconPackUrlBtn = $('importIconPackUrl');
    if (importIconPackUrlBtn) {
      importIconPackUrlBtn.addEventListener('click', () => {
        const url = prompt('请输入图标包ZIP文件的URL：', 'https://');
        if (url) handleIconPackImportFromUrl(url);
      });
    }

    const importIconPackBtn = $('importIconPack');
    const importIconPackFileInput = $('importIconPackFile');
    if (importIconPackBtn && importIconPackFileInput) {
      importIconPackBtn.addEventListener('click', () => importIconPackFileInput.click());
      importIconPackFileInput.addEventListener('change', (e) => {
        const files = e.target.files;
        if (files && files.length > 0) {
          handleIconPackImport(files);
        }
        e.target.value = ''; // 清空，允许重复选择同一文件
      });
    }

    // 壁纸相关事件
    const importWallpaperLocalBtn = $('importWallpaperLocal');
    const importWallpaperParallaxBtn = $('importWallpaperParallax');
    const wallpaperFileInput = $('wallpaperFileInput');
    const wallpaperParallaxInput = $('wallpaperParallaxInput');
    const importWallpaperUrlBtn = $('importWallpaperUrl');
    const exportZipBtn = $('exportZipPackage');
    const exportThemePackageBtn = $('exportThemePackage');

    if (importWallpaperLocalBtn && wallpaperFileInput) {
      importWallpaperLocalBtn.addEventListener('click', () => wallpaperFileInput.click());
      wallpaperFileInput.addEventListener('change', (e) => handleWallpaperImport(e.target.files));
    }

    if (importWallpaperParallaxBtn && wallpaperParallaxInput) {
      importWallpaperParallaxBtn.addEventListener('click', () => wallpaperParallaxInput.click());
      wallpaperParallaxInput.addEventListener('change', (e) => handleParallaxWallpaperImport(e.target.files));
    }

    if (importWallpaperUrlBtn) {
      importWallpaperUrlBtn.addEventListener('click', handleWallpaperUrlImport);
    }

    if (exportZipBtn) {
      exportZipBtn.addEventListener('click', exportZipPackage);
    }

    if (exportThemePackageBtn) {
      exportThemePackageBtn.addEventListener('click', exportThemePackage);
    }

    // Widget相关事件
    const importWidgetLocalBtn = $('importWidgetLocal');
    const widgetFileInput = $('widgetFileInput');
    const importWidgetUrlBtn = $('importWidgetUrl');
    const widgetTypeSelect = $('widgetType');
    const widgetTypeCustomInput = $('widgetTypeCustom');
    const widgetNameInput = $('widgetName');
    const deleteWidgetBtn = $('deleteWidget');

    if (importWidgetLocalBtn && widgetFileInput) {
      importWidgetLocalBtn.addEventListener('click', () => widgetFileInput.click());
      widgetFileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) handleWidgetImportFromLocal(file);
        e.target.value = '';
      });
    }

    if (importWidgetUrlBtn) {
      importWidgetUrlBtn.addEventListener('click', () => {
        const url = prompt('请输入Widget ZIP链接：', 'https://');
        if (url) handleWidgetImportFromUrl(url);
      });
    }

    if (widgetTypeSelect) {
      widgetTypeSelect.addEventListener('change', (e) => {
        if (e.target.value === 'custom') {
          // 弹窗输入自定义组件类型
          const customType = prompt('请输入自定义组件类型：', '');
          if (customType && customType.trim()) {
            const trimmedType = customType.trim();
            // 添加到自定义类型列表（如果是新类型）
            if (!state.iconEditor.customWidgetTypes.includes(trimmedType)) {
              state.iconEditor.customWidgetTypes.push(trimmedType);
              saveState();
            }
            // 更新当前Widget类型
            updateCurrentWidgetType(trimmedType);
            // 重新渲染下拉框（包含新类型）
            renderWidgetEditSection();
          } else {
            // 取消输入或输入为空，恢复为当前Widget的原类型
            renderWidgetEditSection();
          }
        } else {
          // 选择了预定义类型或自定义类型列表中的类型
          updateCurrentWidgetType(e.target.value);
        }
      });
    }

    if (widgetNameInput) {
      widgetNameInput.addEventListener('change', (e) => {
        updateCurrentWidgetName(e.target.value);
      });
    }

    if (deleteWidgetBtn) {
      deleteWidgetBtn.addEventListener('click', deleteCurrentWidget);
    }

    // 预览图更换事件
    const changeWidgetPreview = $('changeWidgetPreview');
    const widgetPreviewInput = $('widgetPreviewInput');
    if (changeWidgetPreview && widgetPreviewInput) {
      changeWidgetPreview.addEventListener('click', () => widgetPreviewInput.click());
      widgetPreviewInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) handleWidgetPreviewChange(file);
        e.target.value = '';
      });
    }

    // 下载地址编辑事件
    const widgetDownloadUrlInput = $('widgetDownloadUrl');
    if (widgetDownloadUrlInput) {
      widgetDownloadUrlInput.addEventListener('change', (e) => {
        updateCurrentWidgetDownloadUrl(e.target.value);
      });
    }
  }

  function renderSchemePng(scheme) {
    const groups = ['accent1', 'accent2', 'accent3', 'neutral1', 'neutral2'];
    const cell = 64, pad = 16, titleH = 22;
    const cols = 13, rows = groups.length;
    const w = pad * 2 + cols * cell;
    const h = pad * 2 + rows * (cell + titleH + 8);
    const c = document.createElement('canvas'); c.width = w; c.height = h;
    const ctx = c.getContext('2d');
    ctx.fillStyle = '#ffffff'; ctx.fillRect(0, 0, w, h);
    ctx.font = '12px ui-monospace, monospace'; ctx.fillStyle = '#111';
    groups.forEach((g, row) => {
      ctx.fillText(g, pad, pad + row * (cell + titleH + 8) + 12);
      scheme[g].forEach((hex, col) => {
        const x = pad + col * cell;
        const y = pad + row * (cell + titleH + 8) + titleH;
        ctx.fillStyle = hex; ctx.fillRect(x, y, cell - 2, cell - 2);
        ctx.strokeStyle = '#ddd'; ctx.strokeRect(x, y, cell - 2, cell - 2);
      });
    });
    return c.toDataURL('image/png');
  }

  function boot() {
    initRefs();
    loadStateFromUrl();
    loadStateFromStorage();
    document.body.className = state.dark ? 'theme-dark' : 'theme-light';
    els.styleSelect.value = state.style; els.themeToggle.textContent = state.dark ? '深色' : '浅色';
    setSeed(state.seed);
    renderRecents();
    drawSV();
    wireEvents();
    renderGradientEditor();
    // 恢复标签页状态
    if (state.currentTab) switchTab(state.currentTab);
    // 初始化手机预览区
    renderPhonePreview();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();
})();


