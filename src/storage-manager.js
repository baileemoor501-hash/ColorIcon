// IndexedDB存储管理模块
// 负责图标包和颜色方案的持久化存储

(function (global) {
  const DB_NAME = 'color-tool-db';
  const DB_VERSION = 3;
  const STORE_ICON_PACKS = 'iconPacks';
  const STORE_COLOR_SCHEMES = 'colorSchemes';
  const STORE_WALLPAPERS = 'wallpapers';
  const STORE_WIDGETS = 'widgets';

  class StorageManager {
    constructor() {
      this.db = null;
      this.isSupported = this.checkIndexedDBSupport();
    }

    /**
     * 检查IndexedDB支持性
     * @returns {boolean}
     */
    checkIndexedDBSupport() {
      return 'indexedDB' in window;
    }

    /**
     * 初始化IndexedDB数据库
     * @returns {Promise<IDBDatabase>}
     */
    init() {
      if (!this.isSupported) {
        return Promise.reject(new Error('IndexedDB is not supported in this browser'));
      }

      return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);

        request.onerror = () => {
          reject(new Error('Failed to open IndexedDB: ' + request.error));
        };

        request.onsuccess = () => {
          this.db = request.result;
          resolve(this.db);
        };

        request.onupgradeneeded = (event) => {
          const db = event.target.result;
          this.createObjectStores(db);
        };
      });
    }

    /**
     * 创建对象存储
     * @param {IDBDatabase} db
     */
    createObjectStores(db) {
      // 创建图标包存储
      if (!db.objectStoreNames.contains(STORE_ICON_PACKS)) {
        const iconPackStore = db.createObjectStore(STORE_ICON_PACKS, {
          keyPath: 'id',
          autoIncrement: true
        });
        iconPackStore.createIndex('name', 'name', { unique: false });
        iconPackStore.createIndex('createdAt', 'createdAt', { unique: false });
      }

      // 创建颜色方案存储
      if (!db.objectStoreNames.contains(STORE_COLOR_SCHEMES)) {
        const colorSchemeStore = db.createObjectStore(STORE_COLOR_SCHEMES, {
          keyPath: 'id',
          autoIncrement: true
        });
        colorSchemeStore.createIndex('name', 'name', { unique: false });
        colorSchemeStore.createIndex('iconPackName', 'iconPackName', { unique: false });
        colorSchemeStore.createIndex('createdAt', 'createdAt', { unique: false });
      }

      // 创建壁纸存储
      if (!db.objectStoreNames.contains(STORE_WALLPAPERS)) {
        const wallpaperStore = db.createObjectStore(STORE_WALLPAPERS, {
          keyPath: 'id'
        });
        wallpaperStore.createIndex('createdAt', 'createdAt', { unique: false });
      }

      // 创建Widget存储
      if (!db.objectStoreNames.contains(STORE_WIDGETS)) {
        const widgetStore = db.createObjectStore(STORE_WIDGETS, {
          keyPath: 'id'
        });
        widgetStore.createIndex('widget_type', 'widget_type', { unique: false });
        widgetStore.createIndex('widget_name', 'widget_name', { unique: false });
      }
    }

    /**
     * 保存图标包
     * @param {object} pack - 图标包对象 { name, icons: [{name, layers: [blob]}], background: blob, createdAt }
     * @returns {Promise<number>} - 返回生成的ID
     */
    saveIconPack(pack) {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_ICON_PACKS], 'readwrite');
        const store = transaction.objectStore(STORE_ICON_PACKS);

        const packData = {
          name: pack.name,
          icons: pack.icons, // Array of {name, layers: [Blob]}
          background: pack.background, // Blob object
          createdAt: pack.createdAt || new Date().toISOString(),
          sourceUrl: pack.sourceUrl || ''
        };

        const request = store.add(packData);

        request.onsuccess = () => {
          resolve(request.result); // 返回自动生成的ID
        };

        request.onerror = () => {
          reject(new Error('Failed to save icon pack: ' + request.error));
        };
      });
    }

    /**
     * 获取指定图标包
     * @param {number} id - 图标包ID
     * @returns {Promise<object>}
     */
    getIconPack(id) {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_ICON_PACKS], 'readonly');
        const store = transaction.objectStore(STORE_ICON_PACKS);
        const request = store.get(id);

        request.onsuccess = () => {
          resolve(request.result);
        };

        request.onerror = () => {
          reject(new Error('Failed to get icon pack: ' + request.error));
        };
      });
    }

    /**
     * 获取所有图标包列表
     * @returns {Promise<Array>}
     */
    getAllIconPacks() {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_ICON_PACKS], 'readonly');
        const store = transaction.objectStore(STORE_ICON_PACKS);
        const request = store.getAll();

        request.onsuccess = () => {
          resolve(request.result || []);
        };

        request.onerror = () => {
          reject(new Error('Failed to get all icon packs: ' + request.error));
        };
      });
    }

    /**
     * 删除图标包
     * @param {number} id - 图标包ID
     * @returns {Promise<void>}
     */
    deleteIconPack(id) {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_ICON_PACKS], 'readwrite');
        const store = transaction.objectStore(STORE_ICON_PACKS);
        const request = store.delete(id);

        request.onsuccess = () => {
          resolve();
        };

        request.onerror = () => {
          reject(new Error('Failed to delete icon pack: ' + request.error));
        };
      });
    }

    /**
     * 保存颜色方案
     * @param {object} scheme - 颜色方案对象 { name, iconPackName, colorConfig, createdAt }
     * @returns {Promise<number>} - 返回生成的ID
     */
    saveColorScheme(scheme) {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_COLOR_SCHEMES], 'readwrite');
        const store = transaction.objectStore(STORE_COLOR_SCHEMES);

        const schemeData = {
          name: scheme.name,
          iconPackName: scheme.iconPackName,
          colorConfig: scheme.colorConfig, // { fg: [], bg: [] }
          createdAt: scheme.createdAt || new Date().toISOString()
        };

        const request = store.add(schemeData);

        request.onsuccess = () => {
          resolve(request.result);
        };

        request.onerror = () => {
          reject(new Error('Failed to save color scheme: ' + request.error));
        };
      });
    }

    /**
     * 获取颜色方案
     * @param {number} id - 方案ID
     * @returns {Promise<object>}
     */
    getColorScheme(id) {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_COLOR_SCHEMES], 'readonly');
        const store = transaction.objectStore(STORE_COLOR_SCHEMES);
        const request = store.get(id);

        request.onsuccess = () => {
          resolve(request.result);
        };

        request.onerror = () => {
          reject(new Error('Failed to get color scheme: ' + request.error));
        };
      });
    }

    /**
     * 获取所有颜色方案
     * @returns {Promise<Array>}
     */
    getAllColorSchemes() {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_COLOR_SCHEMES], 'readonly');
        const store = transaction.objectStore(STORE_COLOR_SCHEMES);
        const request = store.getAll();

        request.onsuccess = () => {
          resolve(request.result || []);
        };

        request.onerror = () => {
          reject(new Error('Failed to get all color schemes: ' + request.error));
        };
      });
    }

    /**
     * 删除颜色方案
     * @param {number} id - 方案ID
     * @returns {Promise<void>}
     */
    deleteColorScheme(id) {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_COLOR_SCHEMES], 'readwrite');
        const store = transaction.objectStore(STORE_COLOR_SCHEMES);
        const request = store.delete(id);

        request.onsuccess = () => {
          resolve();
        };

        request.onerror = () => {
          reject(new Error('Failed to delete color scheme: ' + request.error));
        };
      });
    }

    /**
     * 保存壁纸
     * @param {object} wallpaper - 壁纸对象
     * @returns {Promise<void>}
     */
    saveWallpaper(wallpaper) {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_WALLPAPERS], 'readwrite');
        const store = transaction.objectStore(STORE_WALLPAPERS);
        const request = store.put(wallpaper);

        request.onsuccess = () => {
          resolve();
        };

        request.onerror = () => {
          reject(new Error('Failed to save wallpaper: ' + request.error));
        };
      });
    }

    /**
     * 获取所有壁纸
     * @returns {Promise<Array>}
     */
    getAllWallpapers() {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_WALLPAPERS], 'readonly');
        const store = transaction.objectStore(STORE_WALLPAPERS);
        const request = store.getAll();

        request.onsuccess = () => {
          resolve(request.result || []);
        };

        request.onerror = () => {
          reject(new Error('Failed to get wallpapers: ' + request.error));
        };
      });
    }

    /**
     * 删除壁纸
     * @param {string} id - 壁纸ID
     * @returns {Promise<void>}
     */
    deleteWallpaper(id) {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_WALLPAPERS], 'readwrite');
        const store = transaction.objectStore(STORE_WALLPAPERS);
        const request = store.delete(id);

        request.onsuccess = () => {
          resolve();
        };

        request.onerror = () => {
          reject(new Error('Failed to delete wallpaper: ' + request.error));
        };
      });
    }

    /**
     * 保存Widget
     * @param {object} widget - Widget对象 { id, widget_type, widget_name, download_url, preview_blob, widget_zip_blob, selected }
     * @returns {Promise<void>}
     */
    saveWidget(widget) {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_WIDGETS], 'readwrite');
        const store = transaction.objectStore(STORE_WIDGETS);
        const request = store.put(widget);

        request.onsuccess = () => {
          resolve();
        };

        request.onerror = () => {
          reject(new Error('Failed to save widget: ' + request.error));
        };
      });
    }

    /**
     * 获取所有Widget
     * @returns {Promise<Array>}
     */
    getAllWidgets() {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_WIDGETS], 'readonly');
        const store = transaction.objectStore(STORE_WIDGETS);
        const request = store.getAll();

        request.onsuccess = () => {
          resolve(request.result || []);
        };

        request.onerror = () => {
          reject(new Error('Failed to get widgets: ' + request.error));
        };
      });
    }

    /**
     * 删除Widget
     * @param {string} id - Widget ID
     * @returns {Promise<void>}
     */
    deleteWidget(id) {
      return new Promise((resolve, reject) => {
        if (!this.db) {
          reject(new Error('Database not initialized'));
          return;
        }

        const transaction = this.db.transaction([STORE_WIDGETS], 'readwrite');
        const store = transaction.objectStore(STORE_WIDGETS);
        const request = store.delete(id);

        request.onsuccess = () => {
          resolve();
        };

        request.onerror = () => {
          reject(new Error('Failed to delete widget: ' + request.error));
        };
      });
    }
  }

  // 导出到全局
  global.StorageManager = StorageManager;
})(window);
