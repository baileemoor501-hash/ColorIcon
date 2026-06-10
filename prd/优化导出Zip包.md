# 优化导出Zip包

- 修改原有导出Zip包的逻辑
    - 之前版本导出zip没有包含预览图，现在增加导出预览图功能，用户输入zip名字的同时，同时导出壁纸图标预览图
    - 使用已经实现的导出预览图功能，导出360*640， 720* 1280 不同分辨率预览图
    - 按照以下示例导出资源
        用户输入文件名 my_wallpapers
        导出资源
        - my_wallpapers
            - my_wallpapers.zip
            - my_wallpapers(1).jpg
            - my_wallpapers(2).jpg
    - 现在导出的是包含zip包、预览图的文件夹