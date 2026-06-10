// 文件上传处理模块
// 负责图标文件的校验、解析和图标包创建

(function (global) {
  class FileUploadHandler {
    constructor() {
      this.validExtensions = ['.png'];
    }

    /**
     * 校验文件列表
     * @param {FileList} files - 文件列表
     * @returns {object} - { valid: boolean, errors: [] }
     */
    validateIconFiles(files) {
      const errors = [];

      if (!files || files.length === 0) {
        errors.push('未选择任何文件');
        return { valid: false, errors };
      }

      // 检查文件格式
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();

        if (!this.validExtensions.includes(ext)) {
          errors.push(`文件 "${file.name}" 格式不支持，仅支持PNG格式`);
        }

        // 检查文件大小（限制单个文件最大5MB）
        if (file.size > 5 * 1024 * 1024) {
          errors.push(`文件 "${file.name}" 过大（超过5MB）`);
        }
      }

      // 检查命名规则
      const parsed = [];
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const result = this.parseFileName(file.name);

        if (!result) {
          errors.push(`文件 "${file.name}" 命名不符合规则（应为 {prefix}_{number}.png 或 icon_base.png）`);
        } else {
          parsed.push({ file, ...result });
        }
      }

      // 检查前景图序号连续性（按prefix分组检查）
      const foregrounds = parsed.filter(p => p.type === 'foreground');
      if (foregrounds.length > 0) {
        // 按prefix分组
        const groupedByPrefix = {};
        foregrounds.forEach(fg => {
          if (!groupedByPrefix[fg.prefix]) {
            groupedByPrefix[fg.prefix] = [];
          }
          groupedByPrefix[fg.prefix].push(fg);
        });

        // 检查每个prefix组的序号连续性
        for (const prefix in groupedByPrefix) {
          const group = groupedByPrefix[prefix];
          group.sort((a, b) => a.index - b.index);

          // 检查是否从1开始
          if (group[0].index !== 1) {
            errors.push(`前景图 "${prefix}" 序号应从1开始，当前从 ${group[0].index} 开始`);
          }

          // 检查序号连续性
          for (let i = 0; i < group.length - 1; i++) {
            if (group[i + 1].index !== group[i].index + 1) {
              errors.push(`前景图 "${prefix}" 序号不连续：缺少序号 ${group[i].index + 1}`);
              break;
            }
          }
        }
      }

      // 检查是否有背景图
      const backgrounds = parsed.filter(p => p.type === 'background');
      if (backgrounds.length === 0) {
        errors.push('缺少背景图（应命名为 icon_base.png 或 {prefix}_base.png）');
      } else if (backgrounds.length > 1) {
        errors.push('背景图数量超过1个，仅支持单个背景图');
      }

      return {
        valid: errors.length === 0,
        errors
      };
    }

    /**
     * 解析文件名
     * @param {string} filename - 文件名（如 "icon_fg_1.png" 或 "icon_base.png"）
     * @returns {object|null} - { prefix, type, index } 或 null
     */
    parseFileName(filename) {
      // 移除扩展名
      const nameWithoutExt = filename.replace(/\.png$/i, '');

      // 匹配背景图：{prefix}_base 或 icon_base
      const bgMatch = nameWithoutExt.match(/^(.+)_base$/);
      if (bgMatch) {
        return {
          prefix: bgMatch[1],
          type: 'background',
          index: 0
        };
      }

      // 匹配前景图：{prefix}_{number}
      const fgMatch = nameWithoutExt.match(/^(.+?)_(\d+)$/);
      if (fgMatch) {
        return {
          prefix: fgMatch[1],
          type: 'foreground',
          index: parseInt(fgMatch[2], 10)
        };
      }

      return null;
    }

    /**
     * 将文件分组为前景图和背景图（按prefix分组）
     * @param {FileList} files - 文件列表
     * @returns {object} - { iconGroups: Map<prefix, files[]>, background: File }
     */
    groupFiles(files) {
      const iconGroups = new Map(); // prefix -> files[]
      let background = null;

      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const parsed = this.parseFileName(file.name);

        if (!parsed) continue;

        if (parsed.type === 'foreground') {
          if (!iconGroups.has(parsed.prefix)) {
            iconGroups.set(parsed.prefix, []);
          }
          iconGroups.get(parsed.prefix).push({ file, index: parsed.index });
        } else if (parsed.type === 'background') {
          background = file;
        }
      }

      // 对每个图标组内的文件按序号排序
      for (const [prefix, files] of iconGroups) {
        files.sort((a, b) => a.index - b.index);
      }

      return {
        iconGroups,
        background
      };
    }

    /**
     * 从File对象加载图片
     * @param {File} file - 文件对象
     * @returns {Promise<Blob>} - 返回Blob对象
     */
    loadImageFromFile(file) {
      return new Promise((resolve, reject) => {
        // 直接返回Blob，不需要转换为Image对象
        // 这样可以保持原始数据，避免质量损失
        resolve(file);
      });
    }

    /**
     * 创建图标包对象
     * @param {FileList} files - 文件列表
     * @param {string} packName - 图标包名称
     * @returns {Promise<object>} - 图标包对象
     */
    async createIconPack(files, packName) {
      // 先校验
      const validation = this.validateIconFiles(files);
      if (!validation.valid) {
        throw new Error(validation.errors.join('\n'));
      }

      // 分组文件
      const grouped = this.groupFiles(files);

      // 加载背景图
      const background = await this.loadImageFromFile(grouped.background);

      // 为每个图标组创建图标数据
      const icons = [];
      for (const [prefix, fileInfos] of grouped.iconGroups) {
        const layers = [];
        for (const fileInfo of fileInfos) {
          const blob = await this.loadImageFromFile(fileInfo.file);
          layers.push(blob);
        }

        icons.push({
          name: prefix,
          layers: layers
        });
      }

      // 创建图标包对象
      return {
        name: packName,
        icons: icons, // 多个图标，每个图标有自己的layers
        background: background,
        createdAt: new Date().toISOString()
      };
    }
  }

  // 导出到全局
  global.FileUploadHandler = FileUploadHandler;
})(window);
