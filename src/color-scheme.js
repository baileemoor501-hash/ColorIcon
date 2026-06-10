// 颜色方案生成器（近似 HCT）：
// - 模拟 Style 核心：Hue 派生 + Chroma 常量/倍数 + 13 个 tone 阶梯
// - 为简化实现，使用 HSL 近似（保持 Hue，使用 Saturation 代表 chroma，tone -> Lightness）

(function (global) {
  const ToneSteps = [0,10,20,30,40,50,60,70,80,90,95,99,100]; // 13 tones

  const Hue = {
    source: (h)=>h,
    add: (delta)=>(h)=>wrap(h + delta),
    sub: (delta)=>(h)=>wrap(h - delta),
    mapByRotations: (pairs)=>(h)=>{
      const hue = sanitize(h);
      for (let i=0;i<pairs.length-1;i++){
        const [from, rot] = pairs[i];
        const [to] = pairs[i+1];
        if (from <= hue && hue < to) return wrap(hue + rot);
      }
      return hue;
    }
  };

  const HueVibrantSecondary = Hue.mapByRotations([[0,18],[41,15],[61,10],[101,12],[131,15],[181,18],[251,15],[301,12],[360,12]]);
  const HueVibrantTertiary  = Hue.mapByRotations([[0,35],[41,30],[61,20],[101,25],[131,30],[181,35],[251,30],[301,25],[360,25]]);
  const HueExpressiveSecondary = Hue.mapByRotations([[0,45],[21,95],[51,45],[121,20],[151,45],[191,90],[271,45],[321,45],[360,45]]);
  const HueExpressiveTertiary  = Hue.mapByRotations([[0,120],[21,120],[51,20],[121,45],[151,20],[191,15],[271,20],[321,120],[360,120]]);

  const Chroma = {
    constant: (c)=>() => c,
    multiple: (m)=>(c)=> c * m,
    source: (c)=> c,
    maxOut: ()=>() => 100 // 用 100% 饱和度近似
  };

  function wrap(x){ return ((x%360)+360)%360; }
  function sanitize(x){ return (x<0 || x>=360) ? 0 : x; }

  function buildTonalShades(seedHsl, hueFn, chromaFn){
    const baseHue = hueFn(seedHsl.h);
    const baseChroma = chromaFn(seedHsl.s); // 使用种子饱和度为 chroma 源
    const s = clamp(baseChroma, 0, 100);
    return ToneSteps.map(l => ColorUtils.hslToRgb({ h: baseHue, s, l })).map(ColorUtils.toHex);
  }

  const Styles = {
    TONAL_SPOT: {
      a1: { hue: Hue.source,         chroma: Chroma.constant(36) },
      a2: { hue: Hue.source,         chroma: Chroma.constant(16) },
      a3: { hue: Hue.add(60),        chroma: Chroma.constant(24) },
      n1: { hue: Hue.source,         chroma: Chroma.constant(4)  },
      n2: { hue: Hue.source,         chroma: Chroma.constant(8)  },
    },
    VIBRANT: {
      a1: { hue: Hue.source,         chroma: Chroma.maxOut() },
      a2: { hue: HueVibrantSecondary, chroma: Chroma.constant(24) },
      a3: { hue: HueVibrantTertiary,  chroma: Chroma.constant(32) },
      n1: { hue: Hue.source,         chroma: Chroma.constant(10) },
      n2: { hue: Hue.source,         chroma: Chroma.constant(12) },
    },
    EXPRESSIVE: {
      a1: { hue: Hue.add(240),       chroma: Chroma.constant(40) },
      a2: { hue: HueExpressiveSecondary, chroma: Chroma.constant(24) },
      a3: { hue: HueExpressiveTertiary,  chroma: Chroma.constant(32) },
      n1: { hue: Hue.add(15),        chroma: Chroma.constant(8)  },
      n2: { hue: Hue.add(15),        chroma: Chroma.constant(12) },
    },
  };

  function generateScheme(seedHex, styleName = 'TONAL_SPOT', darkTheme = false){
    const rgb = ColorUtils.fromHex(seedHex);
    const seedHsl = ColorUtils.rgbToHsl(rgb);
    // 低彩度保护：若饱和度很低，换用默认种子
    const effectiveSeed = seedHsl.s < 5 ? ColorUtils.rgbToHsl(ColorUtils.fromHex('#1B6EF3')) : seedHsl;
    const style = Styles[styleName] || Styles.TONAL_SPOT;

    const accent1 = buildTonalShades(effectiveSeed, style.a1.hue, style.a1.chroma);
    const accent2 = buildTonalShades(effectiveSeed, style.a2.hue, style.a2.chroma);
    const accent3 = buildTonalShades(effectiveSeed, style.a3.hue, style.a3.chroma);
    const neutral1 = buildTonalShades(effectiveSeed, style.n1.hue, style.n1.chroma);
    const neutral2 = buildTonalShades(effectiveSeed, style.n2.hue, style.n2.chroma);

    const background = darkTheme ? neutral1[8] : neutral1[1];
    const accent = darkTheme ? accent1[2] : accent1[6];

    return { accent1, accent2, accent3, neutral1, neutral2, background, accent };
  }

  function toCssVariables(scheme, prefix = '--md'){ // 输出 CSS 变量
    const groups = ['accent1','accent2','accent3','neutral1','neutral2'];
    let out = `:root{\n`;
    for (const g of groups){
      scheme[g].forEach((hex, i)=>{ out += `  ${prefix}-${g}-${i}: ${hex};\n`; });
    }
    out += `}`;
    return out;
  }

  function toJson(scheme){
    return JSON.stringify(scheme, null, 2);
  }

  function clamp(v, min, max){ return Math.min(max, Math.max(min, v)); }

  global.ColorScheme = { generateScheme, toCssVariables, toJson, ToneSteps };
})(window);


