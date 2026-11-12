# test-plugin 目录说明

此目录存放演示插件源码，不参与主内核编译。

## 结构
```
test-plugin/
├── src/main/java/com/demo/DemoPlugin.java
├── src/main/resources/plugin.yml
└── README.md
```

## 编译打包

```cmd
cd test-plugin
javac -d out -encoding UTF-8 -cp ..\out src\main\java\com\demo\DemoPlugin.java
copy src\main\resources\plugin.yml out\plugin.yml
jar --create --file ..\run\plugins\DemoPlugin.jar -C out .
```

生成的 `DemoPlugin.jar` 放入 `../run/plugins/` 即可被主内核加载。

## 功能演示

- `/greet [名字]` - 问候指令
- `/demo info|join|count|broadcast` - 演示指令组
- 玩家加入时重写消息为 `[欢迎] xxx 来到了服务器`
- 定时任务每 20 tick 打印心跳
- `onEnable` 时在数据目录写入 `enabled.txt`