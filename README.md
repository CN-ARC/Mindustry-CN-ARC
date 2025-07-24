![Logo](core/assets-raw/sprites/ui/logo.png)

[![Build Status](https://github.com/Anuken/Mindustry/workflows/Tests/badge.svg?event=push)](https://github.com/Anuken/Mindustry/actions)
[![Discord](https://img.shields.io/discord/391020510269669376.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://discord.gg/mindustry)  

The automation tower defense RTS, written in Java.  

# 目前无人维护，可使用[MDTX](https://github.com/TinyLake/MindustryX)

_[资料导航站](https://docs.qq.com/sheet/DVEVob2xrcVBzQk5R)_  
_[地图档案馆](https://docs.qq.com/sheet/DVGpmQ3lIR25rdnZo)_  
_[蓝图档案馆](https://docs.qq.com/sheet/DVHNoS3lIcm1NbFFS)_ 

Q群： 697981760 、602885791  
学术开发群：924017776。欢迎有能力改端的玩家加入！

_[下载最新版本|前往最新构建版](https://github.com/Jackson11500/Mindustry-CN-ARC-Builds)_ 
_[download latest version](https://github.com/Jackson11500/Mindustry-CN-ARC-Builds)_

## 学术端介绍
完全功能提升性改端，提供各种辅助显示和功能，提供一端化完美解决方案。    
包含大量游戏原版不显示机制，强烈建议至少已比较了解mindustry基础流程的玩家使用   
初次使用有较长的适应时间，往往需要数小时熟悉功能和调整成自己舒服的设置，如无法接受请回退原版。  

学术端功能可参考这人视频 -> https://space.bilibili.com/135508593/

## 主要负责人
[violet]Lucky_Clover
## 代码
v3学术端制作团队：[violet]Lucky_Clover[white]、squirrel、blac8、[blue]xkldklp[white]、[yellow]miner
v2-支持：[green]Root[blue]Channel[white]、[blue]MyIndustry2[white]、[yellow]wayzer、[brown]Xor
## 翻译
[lightblue]Somall_dumpling
## 工作器件补充说明
[yellow]Carrot

# 发布|fork|自改端声明
如果fork本端，请在Vars.arcVersion后面加上你自己的子版本号，或者你私仓版本的代号（如30123selfclient) \
仅适配简中且为硬编码，并不打算做多语言兼容。(太麻烦了) \
Only support for simplified Chinese, would not support any other languague.

# 下面是anuke-mindustry的说明

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md).

## Building

Bleeding-edge builds are generated automatically for every commit. You can see them [here](https://github.com/Anuken/MindustryBuilds/releases).

If you'd rather compile on your own, follow these instructions.
First, make sure you have [JDK 17](https://adoptium.net/temurin/releases/?os=any&arch=any&version=17) installed. **Other JDK versions will not work.** Open a terminal in the Mindustry directory and run the following commands:

### Windows

_Running:_ `gradlew desktop:run`  
_Building:_ `gradlew desktop:dist`  
_Sprite Packing:_ `gradlew tools:pack`

### Linux/Mac OS

_Running:_ `./gradlew desktop:run`  
_Building:_ `./gradlew desktop:dist`  
_Sprite Packing:_ `./gradlew tools:pack`

### Server

Server builds are bundled with each released build (in Releases). If you'd rather compile on your own, replace 'desktop' with 'server', e.g. `gradlew server:dist`.

### Android

1. Install the Android SDK [here.](https://developer.android.com/studio#command-tools) Make sure you're downloading the "Command line tools only", as Android Studio is not required.
2. In the unzipped Android SDK folder, find the cmdline-tools directory. Then create a folder inside of it called `latest` and put all of its contents into the newly created folder.
3. In the same directory run the command `sdkmanager --licenses` (or `./sdkmanager --licenses` if on linux/mac)
4. Set the `ANDROID_HOME` environment variable to point to your unzipped Android SDK directory.
5. Enable developer mode on your device/emulator. If you are on testing on a phone you can follow [these instructions](https://developer.android.com/studio/command-line/adb#Enabling), otherwise you need to google how to enable your emulator's developer mode specifically.
6. Run `gradlew android:assembleDebug` (or `./gradlew` if on linux/mac). This will create an unsigned APK in `android/build/outputs/apk`.

To debug the application on a connected device/emulator, run `gradlew android:installDebug android:run`.

### Troubleshooting

#### Permission Denied

If the terminal returns `Permission denied` or `Command not found` on Mac/Linux, run `chmod +x ./gradlew` before running `./gradlew`. *This is a one-time procedure.*

#### Where is the `mindustry.gen` package?

As the name implies, `mindustry.gen` is generated *at build time* based on other code. You will not find source code for this package in the repository, and it should not be edited by hand.

The following is a non-exhaustive list of the "source" of generated code in `mindustry.gen`:

- `Call`, `*Packet` classes: Generated from methods marked with `@Remote`.
- All entity classes (`Unit`, `EffectState`, `Posc`, etc): Generated from component classes in the `mindustry.entities.comp` package, and combined using definitions in `mindustry.content.UnitTypes`.
- `Sounds`, `Musics`, `Tex`, `Icon`, etc: Generated based on files in the respective asset folders.

---

Gradle may take up to several minutes to download files. Be patient. <br>
After building, the output .JAR file should be in `/desktop/build/libs/Mindustry.jar` for desktop builds, and in `/server/build/libs/server-release.jar` for server builds.

## Feature Requests

Post feature requests and feedback [here](https://github.com/Anuken/Mindustry-Suggestions/issues/new/choose).

## Downloads

| [![](https://static.itch.io/images/badge.svg)](https://anuke.itch.io/mindustry)    |    [![](https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png)](https://play.google.com/store/apps/details?id=io.anuke.mindustry)   |    [![](https://fdroid.gitlab.io/artwork/badge/get-it-on.png)](https://f-droid.org/packages/io.anuke.mindustry)	| [![](https://flathub.org/assets/badges/flathub-badge-en.svg)](https://flathub.org/apps/details/com.github.Anuken.Mindustry)  
|---	|---	|---	|---	|
