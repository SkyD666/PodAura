<div align="center">
    <div>
        <img src="../image/PodAura.svg" style="height: 210px"/>
    </div>
    <h1>🥰 PodAura</h1>
    <p><b>P</b>odcasts <b>O</b>rganized <b>D</b>iversely with <b>A</b>udio-Video <b>U</b>nification for <b>R</b>ich <b>A</b>ccess</p>
    <p>
        <a href="https://github.com/SkyD666/PodAura/releases/latest" style="text-decoration:none">
            <img src="https://img.shields.io/github/v/release/SkyD666/PodAura?display_name=release&style=for-the-badge" alt="GitHub release (latest by date)"/>
        </a>
        <a href="https://github.com/SkyD666/PodAura/releases/latest" style="text-decoration:none" >
            <img src="https://img.shields.io/github/downloads/SkyD666/PodAura/total?style=for-the-badge" alt="GitHub all downloads"/>
        </a>
        <a href="https://www.android.com/versions/nougat-7-0" style="text-decoration:none" >
            <img src="https://img.shields.io/badge/Android 7.0+-brightgreen?style=for-the-badge&logo=android&logoColor=white" alt="Support platform"/>
        </a>
        <a href="https://github.com/SkyD666/PodAura/blob/master/LICENSE" style="text-decoration:none" >
            <img src="https://img.shields.io/github/license/SkyD666/PodAura?style=for-the-badge" alt="GitHub license"/>
        </a>
        <a href="https://t.me/SkyD666Chat" style="text-decoration:none" >
            <img src="https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white&style=for-the-badge" alt="Telegram"/>
        </a>
        <a href="https://discord.gg/pEWEjeJTa3" style="text-decoration:none" >
            <img src="https://img.shields.io/discord/982522006819991622?color=5865F2&label=Discord&logo=discord&logoColor=white&style=for-the-badge" alt="Discord"/>
        </a>
    </p>
    <p>
        <b>PodAura</b>，一个集<b> RSS 订阅与更新、媒体下载与播放</b>为一体的播客工具。
    </p>
    <p>
        使用 <b><a href="https://developer.android.com/topic/architecture#recommended-app-arch">MVI</a></b> 架构，完全采用 <b><a href="https://m3.material.io/">Material You</a></b> 设计风格。使用 <b>Jetpack Compose</b> 开发。
    </p>
    <p>
        <b><a href="../../README.md">English</a></b>&nbsp&nbsp&nbsp|&nbsp&nbsp&nbsp<b>简体中文</b>&nbsp&nbsp&nbsp|&nbsp&nbsp&nbsp<b><a href="README-zh-rTW.md">正體中文</a></b>&nbsp&nbsp&nbsp|&nbsp&nbsp&nbsp<b><a href="https://crowdin.com/project/anivu">帮助我们翻译</a></b>
    </p>
</div>



<a href="https://f-droid.org/packages/com.skyd.anivu"><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on-zh-cn.png" alt="Get it on F-Droid" height="80"></a>

## 💡主要功能

1. **订阅** RSS、**更新** RSS、**阅读** RSS
2. **自动更新 RSS 订阅**
3. **下载** RSS 文章中的附件（enclosure 标签），支持 **BT 种子或磁力链接**
4. 已下载**文件做种**
5. **播放**媒体附件或已下载的**媒体文件**
6. **更改播放速度**、设置**音轨**、**字幕轨道**等
7. **双指旋转缩放视频画面**、**长按**视频**倍速播放**
8. **滑动**调整**音量**、**屏幕亮度和播放位置**
9. 支持**搜索已获取的 RSS 订阅或文章**
10. **播放**手机中的**其他视频**
11. 支持**自定义 MPV** 播放器
12. 支持**自定义播放列表**
13. 支持 Android 原生**画中画**
14. 支持通过 **OPML 导入导出**订阅
15. 支持**深色模式**
16. ......

## 🤩应用截图

<img src="../image/zh-rCN/ic_rss_screen.jpg" alt="ic_rss_screen" style="zoom:80%;" /> <img src="../image/zh-rCN/ic_rss_screen_edit.jpg" alt="ic_rss_screen_edit" style="zoom:80%;" />
<img src="../image/zh-rCN/ic_article_screen.jpg" alt="ic_article_screen" style="zoom:80%;" /> <img src="../image/zh-rCN/ic_read_screen.jpg" alt="ic_read_screen" style="zoom:80%;" />
<img src="../image/zh-rCN/ic_media_screen.jpg" alt="ic_media_screen" style="zoom:80%;" /> <img src="../image/zh-rCN/ic_player_activity_port.jpg" alt="ic_download_screen" style="zoom:80%;" />
<img src="../image/zh-rCN/ic_history_screen.jpg" alt="ic_media_screen" style="zoom:80%;" /> <img src="../image/zh-rCN/ic_download_screen.jpg" alt="ic_download_screen" style="zoom:80%;" />
<img src="../image/zh-rCN/ic_setting_screen.jpg" alt="ic_setting_screen" style="zoom:80%;" /> <img src="../image/zh-rCN/ic_appearance_screen.jpg" alt="ic_appearance_screen" style="zoom:80%;" />
<img src="../image/zh-rCN/ic_rss_config_screen.jpg" alt="ic_rss_config_screen" style="zoom:80%;" /> <img src="../image/zh-rCN/ic_about_screen.jpg" alt="ic_about_screen" style="zoom:80%;" />
<img src="../image/zh-rCN/ic_player_activity.jpg" alt="ic_player_activity" style="zoom:80%;" />

## 🌏翻译

如果您对此项目感兴趣，请**帮助我们进行翻译**，谢谢。

<a title="Crowdin" target="_blank" href="https://crowdin.com/project/anivu"><img src="https://badges.crowdin.net/anivu/localized.svg"></a>

## 🛠主要技术栈

- **MVI** Architecture
- Jetpack **Compose**
- Kotlin ﻿**Coroutines and Flow**
- **Material You**
- **ViewModel**
- **Room**
- **Paging 3**
- **Hilt**
- **MPV**
- **WorkManager**
- **DataStore**
- Splash Screen
- Navigation
- Coil

## ✨Star History

[![Star History Chart](https://api.star-history.com/svg?repos=SkyD666/PodAura)](https://star-history.com/?repos=SkyD666/PodAura#SkyD666/PodAura&Date)

## 🎈其他应用

<table>
<thead>
  <tr>
    <th>工具</th>
    <th>描述</th>
    <th>传送门</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td><img src="../image/Rays.svg" style="height: 100px"/></td>
    <td><b>Rays (Record All Your Stickers)</b>，一个在本地<b>记录、查找、管理表情包</b>的工具。<br/>🥰 您还在为手机中的<b>表情包太多</b>，找不到想要的表情包而苦恼吗？使用这款工具将帮助您<b>管理您存储的表情包</b>，再也不因为找不到表情包而烦恼！😋</td>
    <td><a href="https://github.com/SkyD666/Rays-Android">https://github.com/SkyD666/Rays-Android</a></td>
  </tr>
  <tr>
    <td><img src="../image/Raca.svg" style="height: 100px"/></td>
    <td><b>Raca (Record All Classic Articles)</b>，一个在本地<b>记录、查找抽象段落/评论区小作文</b>的工具。<br/>🤗 您还在为记不住小作文内容，面临<b>前面、中间、后面都忘了</b>的尴尬处境吗？使用这款工具将<b>帮助您记录您所遇到的小作文</b>，再也不因为忘记而烦恼！😋</td>
    <td><a href="https://github.com/SkyD666/Raca-Android">https://github.com/SkyD666/Raca-Android</a></td>
  </tr>
  <tr>
    <td><img src="../image/NightScreen.svg" style="height: 100px"/></td>
    <td><b>NightScreen</b>，当您在<b>夜间🌙</b>使用手机时，NightScreen 可以帮助您<b>减少屏幕亮度</b>，减少对眼睛的伤害。</td>
    <td><a href="https://github.com/SkyD666/NightScreen">https://github.com/SkyD666/NightScreen</a></td>
  </tr>
</tbody>
</table>

## 📃许可证

使用此软件代码需**遵循以下许可证协议**

[**GNU General Public License v3.0**](../../LICENSE)
