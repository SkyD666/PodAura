# PodAura 通用全文提取方案

## 1. 背景

当前全文阅读的主流程是：使用 Ktor 下载文章 HTML，交给 Readability 提取正文，再执行 HTML 安全清洗、样式保留和 App 主题色协调。

这套流程适用于正文已经存在于初始 HTML 中的页面，但无法处理纯客户端渲染页面。以 `https://www.ximalaya.com/sound/998951506` 为例，普通请求以及常见搜索引擎 User-Agent 得到的都是页面外壳，正文需要页面 JavaScript 发起后续请求并渲染到 DOM。现有代码通过识别 Ximalaya URL 并调用其私有接口解决了该案例，但这种做法无法扩展，也会持续受到接口变更影响。

本方案将删除所有站点特判，改为通用的分层提取流水线：

```text
文章 URL
  │
  ├─ 1. 静态 HTML 下载
  │      ├─ Readability 候选
  │      └─ 标准结构化数据候选（JSON-LD 等）
  │
  ├─ 2. 通用内容质量评估
  │      └─ 合格 ──────────────────────┐
  │                                    │
  └─ 3. 浏览器引擎渲染兜底             │
         ├─ 等待 DOM 稳定               │
         ├─ 快照 DOM 和安全的计算样式   │
         └─ 再次执行 Readability ───────┤
                                              │
                   4. HTML 清洗与主题协调 ◀──┘
                                              │
                              5. ReadScreen 展示
```

生产代码只依据页面能力和内容结构做决策，不包含域名、路径、站点名称或私有 API。

## 2. 目标

- 删除 Ximalaya 专用 URL 识别、私有接口请求和 JSON 字段解析。
- 对初始 HTML 中已有正文的页面保持快速、低资源消耗的静态提取。
- 对依赖 JavaScript 的页面，以渲染后的 DOM 作为通用兜底输入。
- 支持 Android、iOS、macOS 和 JVM Desktop，不把“通用”限定为某一个平台。
- 不使用“可见字符少于 200 即失败”或其他固定正文长度阈值；短公告、短节目简介和图片型正文必须可用。
- 尽量保留原始排版，同时只允许经过验证的安全 CSS。
- 将来源色映射到 PodAura 主题语义色，避免正文出现与 App 主题不协调的默认蓝色链接或强调色。
- 不向提取页面暴露登录态、App JavaScript Bridge 或 PodAura WebView 的 Cookie/Storage。

## 3. 非目标

- 不绕过登录、付费墙、验证码、地区限制或反自动化策略。
- 不自动点击“展开全文”、登录或 Cookie 同意按钮；首版只观察页面自然渲染结果。
- 不执行无限滚动，也不抓取评论区、相关推荐等非正文内容。
- 不保证像素级复刻来源页面。定位、动画、脚本行为和危险 CSS 必须丢弃。
- 不引入公共第三方全文提取服务，避免把用户阅读 URL 泄露给额外服务。如果未来采用服务端渲染，应由 PodAura 自托管并作为独立决策处理。

## 4. 总体设计

### 4.1 统一结果模型

在 `fullcontent` 包中增加内部模型，记录候选来源和质量信息：

```kotlin
internal enum class ExtractionSource {
    READABILITY,
    STRUCTURED_DATA,
    RENDERED_READABILITY,
}

internal data class ExtractionCandidate(
    val html: String,
    val sourceUrl: String,
    val source: ExtractionSource,
    val diagnostics: ContentDiagnostics,
)

internal data class RenderedPageSnapshot(
    val html: String,
    val finalUrl: String,
)
```

`FullContent` 的公开接口暂时保持不变，避免 ReadScreen 和 ViewModel 被底层提取策略污染。诊断信息只用于日志和测试，不记录正文内容。

### 4.2 静态提取阶段

静态阶段继续使用当前独立的、带超时和响应大小限制的 Ktor `HttpClient`：

1. 校验只允许 `http` 和 `https`。
2. 跟随有限重定向并保存最终 URL。
3. 校验状态码、Content-Type 和最大响应大小。
4. 按响应头、BOM 和 HTML/XML meta 解码字符集。
5. 从同一份文档生成多个候选：
   - Readability 候选；
   - JSON-LD 标准结构化数据候选；
   - 后续可扩展的 Microdata/RDFa 候选。
6. 评估所有候选，选择质量最高且达到结构要求的候选。
7. 如果没有合格候选，再启动浏览器渲染，不因正文较短直接判失败。

### 4.3 标准结构化数据提取

新增 `StructuredContentExtractor`，只识别开放标准，不识别站点：

- 遍历 `script[type="application/ld+json"]`，支持对象、数组和 `@graph`。
- 优先处理具有正文语义的 schema.org 类型，例如 `Article`、`NewsArticle`、`BlogPosting`、`Report`、`Review`、`TechArticle`、`DiscussionForumPosting`、`PodcastEpisode` 和 `AudioObject`。
- 读取标准字段：`articleBody`、`text`、`transcript`、`description`、`headline`、`image`。
- 若字段包含 HTML，按不可信 HTML 处理；若为纯文本，先转义再生成段落。
- URL 和图片仍以最终页面 URL 为基准解析。
- 不解析任意全局变量、Webpack 状态对象或站点自定义 JSON 字段，因为那会把站点特判换一种形式隐藏起来。

结构化数据候选仍必须进入统一清洗流程，不能因为数据来自 JSON-LD 就获得额外信任。

### 4.4 通用内容质量评估

新增 `ContentQualityEvaluator`。它负责判断候选是否更像正文，而不是导航、页脚或空壳，但不得采用固定字符数下限。

硬性有效条件：

- 清洗后至少存在非空可见文本，或存在可渲染的 `img`、`table`、`audio`、`video` 等正文媒体。
- 候选不是只包含脚本、样式、表单、导航控件或空容器。

评分信号：

- 正向：`article`/`main` 语义、连续段落、标题、列表、引用、正文图片、正文媒体、较低的链接文本占比。
- 负向：`nav`/`footer`/`aside`、大量短链接、表单控件、版权/登录/下载 App 等通用页面壳特征、重复菜单项。
- 比率和结构优先于绝对长度。例如 30 字且无导航链接的短公告可以通过；500 字但几乎全是菜单链接的页脚应触发渲染兜底。

评估器输出结构化 diagnostics，例如段落数、媒体数、链接文本占比、语义容器和最终评分。阈值只作用于综合评分，不建立“少于 N 字失败”的规则。

### 4.5 浏览器渲染阶段

在 `commonMain` 定义平台能力接口：

```kotlin
internal interface RenderedPageProvider {
    suspend fun render(url: String): RenderedPageSnapshot
}
```

要求所有实现：

- 在独立、不可见的浏览器实例中加载真实 URL并执行页面 JavaScript。
- 不复用用于展示 RSS HTML 的 `PodAuraWebView`，避免共享 Cookie、Storage、历史记录和生命周期。
- 页面完成初次加载后安装 `MutationObserver`，等待正文 DOM 进入短暂静默期。
- 使用“最短等待 + DOM 静默窗口 + 总超时”的组合，而不是固定 sleep：
  - 初次 load 完成后至少等待约 300 ms；
  - DOM 连续约 800 ms 无显著变化时抓取；
  - 整体硬超时建议 10 s；
  - 数值集中在配置对象中，便于测试和调优。
- 若页面持续变化，在硬超时时抓取最后一个稳定快照，而不是无限等待。
- 记录重定向后的最终 URL。
- 限制快照元素数量和序列化后的字节数；超限返回明确失败。
- 取消协程时立即停止加载并销毁浏览器实例。

#### DOM 与样式快照

只返回 `document.documentElement.outerHTML` 会丢失跨域外链 CSS 的效果，因此注入的快照脚本需要：

1. 克隆主文档 DOM，不包含 iframe 子文档。
2. 删除 `script`、`noscript`、表单控件和事件处理属性。
3. 对可见正文节点读取 `getComputedStyle`。
4. 仅把 `FullContentHtmlProcessor.safeProperties` 对应的属性写入克隆节点的 inline style。
5. 不快照 `position`、`inset`、`z-index`、`display`、行为型 CSS、外部资源 URL 或 CSS 自定义属性。
6. 限制处理节点数、每个节点的属性数和最终 HTML 大小。

浏览器生成的 inline style 仍不是受信输入。`FullContentHtmlProcessor` 必须重新解析、验证每一个声明，删除原 style，并只恢复其自己生成的内部样式快照。这能继续满足现有 `data-podaura-style` 信任边界。

### 4.6 各平台实现

#### Android

- 新建 `AndroidRenderedPageProvider`，使用专用 `android.webkit.WebView`。
- 所有 WebView 操作在主线程执行，通过 `suspendCancellableCoroutine` 返回结果。
- 开启 JavaScript 和 DOM Storage，但不调用 `addJavascriptInterface`。
- 禁止文件访问、content URL 访问、混合内容、弹窗、多窗口、下载和非 HTTP(S) 顶层导航。
- 不接受 SSL 错误，不携带现有 App Cookie，不持久化缓存和表单数据。
- 用 `evaluateJavascript` 执行快照脚本并异步接收结果。

#### iOS 与原生 macOS

- 新建 `AppleRenderedPageProvider`，使用 `WKWebView`。
- 使用非持久化 `WKWebsiteDataStore` 和独立 `WKWebViewConfiguration`。
- 通过 navigation delegate 等待加载，并通过 `evaluateJavaScript` 抓取快照。
- 禁止新窗口、下载和非 HTTP(S) 导航；不复用阅读页面状态。
- 在主线程创建、访问和销毁 WKWebView，协程取消时停止导航并释放 delegate。

#### JVM Desktop

首选对 OpenJFX `javafx-web` 做一轮限时技术验证，再决定是否正式引入：

- `WebEngine` 能加载 URL、运行 JavaScript、访问 DOM，并提供加载状态，能力模型与此方案匹配。
- 使用单例 JavaFX runtime 和专用 JavaFX Application Thread；每次请求创建隔离的 `WebEngine`。
- 为 Windows、Linux、macOS 及项目支持的 CPU 架构声明正确的 JavaFX 平台依赖，并验证 jpackage 产物。
- 用户数据目录必须指向一次性或受控的全文提取目录，任务结束清理；不得复用用户默认浏览器数据。

技术验证必须用至少一个普通文章、一个 JavaScript 渲染页面和一个持续动态更新页面检查兼容性、超时、包体及内存。若 JavaFX WebKit 无法正确运行目标页面，不允许退回站点适配器；需单独评估嵌入 CEF/JCEF 的包体成本，或在 JVM 明确返回 `RenderedPageUnavailable`。该决策是 Phase 4 的合入门槛。

参考平台能力：

- [Android WebView API](https://developer.android.com/reference/android/webkit/WebView)
- [Apple WKWebView API](https://developer.apple.com/documentation/webkit/wkwebview)
- [OpenJFX WebEngine API](https://openjfx.io/javadoc/25/javafx.web/javafx/scene/web/WebEngine.html)

## 5. Repository 编排

`FullContentRepository.fetch` 调整为下面的逻辑：

```kotlin
suspend fun fetch(url: String): FullContent {
    val page = pageFetcher.fetch(url)

    staticExtractor.bestCandidate(page)?.let { candidate ->
        return candidate.toFullContent()
    }

    val rendered = renderedPageProvider.render(page.finalUrl)
    val candidate = renderedExtractor.bestCandidate(rendered)
        ?: throw FullContentException("No readable article content")

    return candidate.toFullContent()
}
```

实现注意点：

- 静态和渲染阶段共用相同的候选生成、质量评估和最终清洗逻辑。
- 渲染阶段不直接把整页 HTML交给 ReadScreen。
- 静态失败不是异常终点；只有静态和渲染均失败时才通知 UI。
- HTTP 下载失败、静态不可读、渲染不可用、渲染超时和清洗失败使用内部分类异常；对用户仍显示简洁、可本地化的信息。
- 同一个 ReadScreen 同时只允许一个全文提取任务；新请求或退出页面时取消旧任务。
- 可选地增加仅内存的短期成功缓存，key 使用最终 URL；首版不持久化未经重新清洗的 HTML。

## 6. 安全设计

### 6.1 网络边界

- 初始 URL 和每次顶层重定向只允许 HTTP(S)。
- 浏览器子资源遵循平台安全策略；阻止 `file:`、`content:`、`intent:`、自定义 scheme 和本地地址访问。
- 增加 SSRF 防护：解析目标地址并拒绝 loopback、link-local、private、multicast 和 unspecified 地址；重定向后重新校验。IPv4、IPv6 和 DNS rebinding 都需要测试。
- 不忽略 TLS/证书错误。
- 不携带 Feed 请求中的 Authorization、Cookie 或自定义认证头。

### 6.2 浏览器隔离

- 使用临时 Cookie/Storage/Cache；任务完成后销毁。
- 不注册原生 JavaScript Bridge，不向页面暴露 Kotlin/Java/Objective-C 对象。
- 禁止权限请求：定位、摄像头、麦克风、通知、剪贴板和文件选择器。
- 禁止弹窗、新窗口、下载和外部 App 启动。
- 快照仅来自主 frame。

### 6.3 HTML/CSS 清洗

- 保持现有 safelist 机制，默认删除脚本、iframe、表单、事件属性和未知 scheme。
- 输入中的 `style` 和 `data-podaura-style` 一律视为不可信；内部 marker 只能由 processor 在同一次处理过程中生成。
- CSS 属性采用允许列表，值还需拒绝 URL、expression、behavior、控制字符、非有限数字和危险布局。
- 链接与图片 URL 解析为绝对地址，并再次校验 scheme。
- 最终 WebView 使用不透明 origin 展示清洗后 HTML，仅通过 `<base>` 解析相对资源；不得把文章 URL 作为 `loadDataWithBaseURL` origin。

## 7. 样式与 App 主题协调

现有 `HarmonizedSource` 模式继续作为全文阅读的默认模式：

- 保留安全的字体、字号、字重、行高、间距、对齐、边框、表格、列表、图片尺寸等样式。
- 来源正文色、链接蓝色和强调色映射到 `--podaura-*` 语义 token，再由各平台渲染器解析为当前 Material 主题色。
- 背景色只在确有语义时保留，并保证与当前主题具有可读对比度。
- 不覆盖代码块、引用、表格等组件所需的结构差异。
- 外链 CSS 不直接注入最终阅读 WebView；只保留渲染快照中经过属性和值双重校验的计算结果。
- JVM/Apple 的 Compose HTML renderer 与 Android WebView 应使用同一套 token 和同一份清洗后 HTML，避免平台视觉分叉。

## 8. 代码组织建议

```text
shared/src/commonMain/kotlin/com/skyd/podaura/model/repository/fullcontent/
  FullContentRepository.kt             # 仅负责编排
  FullContentPageFetcher.kt             # HTTP、限制、字符集、重定向
  FullContentExtractor.kt               # 候选生成和选择
  StructuredContentExtractor.kt         # JSON-LD 等开放标准
  ContentQualityEvaluator.kt            # 无固定长度阈值的结构评分
  FullContentHtmlProcessor.kt            # Readability、CSS 物化和清洗
  RenderedPageProvider.kt               # 平台能力接口和数据模型
  RenderedPageSnapshotScript.kt         # DOM 稳定检测与安全样式快照脚本

shared/src/androidMain/.../fullcontent/
  AndroidRenderedPageProvider.kt

shared/src/appleMain/.../fullcontent/
  AppleRenderedPageProvider.kt

shared/src/jvmMain/.../fullcontent/
  JvmRenderedPageProvider.kt
```

依赖注入增加 `expect/actual fullContentPlatformModule`，各平台绑定自己的 `RenderedPageProvider`。`RepositoryModule` 只依赖接口，不使用平台条件判断。

## 9. 分阶段实施计划

### Phase 1：移除站点特判并建立可测试边界

- 从 `FullContentRepository` 删除 Ximalaya endpoint、URL 识别、trackId、Referrer 和私有 JSON 解析。
- Repository 构造函数不再因该适配器依赖 `Json`。
- 删除对应的站点专用 MockEngine 测试。
- 抽出 `FullContentPageFetcher`，保持现有响应限制和字符集能力。
- 引入 `ExtractionCandidate`、`ExtractionSource` 和 diagnostics。

完成条件：生产源码中不存在全文提取相关的域名或站点 API；普通静态文章测试全部通过。

### Phase 2：通用静态候选与质量评估

- 将 Readability 封装为候选生成器，而不是直接决定成功或异常。
- 实现 JSON-LD 递归解析和标准字段映射。
- 实现无最小字符数的结构质量评估器。
- 保证短文本、纯图片、表格正文和音频简介可以通过。
- 对导航/页脚空壳返回“需要渲染”，而不是错误正文。

完成条件：静态页面不回归；构造的 footer-only 页面触发渲染；短正文不会因长度被拒绝。

### Phase 3：浏览器快照协议与 Android 实现

- 定义 `RenderedPageProvider`、超时、取消和错误类型。
- 实现 MutationObserver 稳定检测与计算样式快照脚本。
- 完成 Android 隔离 WebView 实现和 Koin 绑定。
- 通过渲染 DOM 后再次走统一 Readability、质量评估和清洗。

完成条件：Android 上 Ximalaya 示例链接及至少两个其他 JavaScript 页面无需站点规则即可提取；退出页面不会泄漏 WebView。

### Phase 4：Apple 与 JVM 实现

- 使用 WKWebView 完成 iOS/macOS provider。
- 完成 JavaFX WebEngine 技术验证。
- 验证通过后加入 JVM provider、平台依赖和 jpackage 配置。
- 若 JavaFX 验证失败，形成 CEF/JCEF 体积和维护成本报告，再决定 JVM 的正式策略；不得用站点私有接口填补差异。

完成条件：各平台使用同一组行为测试；所有支持平台对 JavaScript 页面给出一致正文，或返回明确的能力不可用错误。

### Phase 5：安全、性能和发布收尾

- 加入 SSRF、重定向、scheme、恶意 HTML/CSS 和超大 DOM 测试。
- 加入浏览器超时、取消、连续请求和资源释放测试。
- 记录匿名阶段耗时、候选来源、失败类别和文档规模；不记录 URL、标题或正文。
- 完成真实站点回归矩阵和主题视觉检查。
- 更新用户提示和必要的本地化字符串。

完成条件：测试矩阵通过，无 Cookie/Storage/JavaScript Bridge 泄漏，性能指标达到下一节预算。

## 10. 测试计划

### 10.1 Common 单元测试

- HTTP：重定向、Content-Type、响应大小、UTF-8/UTF-16/GBK/ISO-8859-1。
- JSON-LD：对象、数组、`@graph`、HTML 字段、纯文本字段、错误 JSON、未知类型。
- 质量评估：
  - 极短但有效的文本；
  - 纯图片和表格正文；
  - footer-only、高链接密度导航；
  - 正文中合理数量的链接；
  - 多语言和无空格语言。
- 清洗：脚本、事件属性、危险 URL、伪造 marker、危险 CSS、负数/非有限 CSS 数值。
- 样式：外部计算样式快照、主题色 token、深色/浅色主题、嵌套继承。

### 10.2 Provider 合约测试

为各平台 provider 使用同一套本地测试页面：

- 加载完成时已有正文。
- 延迟异步插入正文。
- DOM 连续更新后停止。
- 永不停止的计时器页面。
- 页面重定向。
- 弹窗、下载、权限和非 HTTP(S) 导航。
- 超大 DOM 和超大序列化结果。
- 用户取消和连续启动两次提取。

本地 fixture 应由测试服务器提供，避免 CI 依赖外部站点。

### 10.3 真实页面手工回归

- 普通博客、新闻、文档、Podcast/音频详情、短公告、图片型文章。
- 至少三个不同技术栈的客户端渲染页面。
- 将 Ximalaya 示例作为回归样本之一，但它只出现在测试清单/fixture 中，不驱动任何生产逻辑。
- 验证来源页面变更或失败时，系统不会回退到任何站点私有接口。

### 10.4 UI 与视觉测试

- Feed/全文切换、加载、失败、重试和返回后滚动位置。
- 浅色、深色和动态主题下的正文色、链接色、代码块、引用、表格和图片。
- Android WebView、Apple/JVM Compose renderer 的关键截图对比。
- 字号偏好、选择文本、图片点击和时间戳链接行为不回归。

## 11. 性能预算

- 静态可提取页面不启动浏览器引擎。
- 静态阶段沿用 5 MiB 响应上限和 20 s 网络上限，可在实现时拆分连接/读取预算。
- 渲染阶段默认硬超时 10 s，配置化但不允许无限等待。
- DOM 元素上限沿用或低于 50,000；最终快照上限建议 5 MiB。
- 浏览器渲染默认串行，避免用户连续点击造成多个高内存实例。
- 提取完成、失败或取消后立即停止加载并释放实例。
- JVM 技术验证需要记录首次启动耗时、稳态耗时、峰值内存和安装包增量，作为是否采用 JavaFX 的合入依据。

## 12. 可观测性

仅记录不含用户内容的结构化事件：

- `static_readability_success`
- `static_structured_data_success`
- `render_fallback_started`
- `rendered_readability_success`
- `failure_network` / `failure_timeout` / `failure_unsafe_url`
- `failure_no_candidate` / `failure_renderer_unavailable`

指标包括阶段耗时、元素数量、快照字节数和平台。不得记录完整 URL、query、页面标题、正文、Cookie 或浏览器脚本返回值。

## 13. 风险与应对

| 风险 | 影响 | 应对 |
| --- | --- | --- |
| 页面反自动化或要求登录 | 仍无法得到正文 | 明确失败，不绕过限制，不添加站点后门 |
| 页面长期更新导致无法“稳定” | 等待过久 | DOM 静默窗口加硬超时，超时抓取最后快照 |
| 浏览器引擎内存较高 | 移动端卡顿或桌面包体增加 | 静态优先、渲染串行、及时销毁、JVM 设置合入门槛 |
| 渲染 DOM 带来攻击面 | Cookie、Bridge 或本地资源风险 | 临时数据仓、无 Bridge、scheme/地址校验、禁止权限和弹窗 |
| 计算样式导致快照过大 | 性能和 OOM | 属性/节点/字节三重上限，只处理可见候选节点 |
| 外链 CSS 无法在静态阶段保留 | 样式差异 | 渲染阶段提取安全计算样式；静态阶段继续处理内联和内嵌 CSS |
| JavaFX WebKit 兼容性不足 | JVM 行为不一致 | Phase 4 技术验证；必要时单独评估 CEF/JCEF，不恢复站点特判 |

## 14. 验收标准

- `FullContentRepository` 及相关生产代码不包含 Ximalaya 或其他站点名称、域名、路径匹配、私有 endpoint 或字段映射。
- 普通静态文章仍只发起一次页面请求，不启动浏览器引擎。
- 初始 HTML 只有页面外壳、正文由 JavaScript 注入时，支持的平台能从渲染 DOM 提取正文。
- Ximalaya 示例链接不再只显示页脚，并且不是通过私有 API 获得正文。
- 短正文不会因少于 200 个可见字符而失败。
- 原始安全排版尽量保留，危险 CSS/HTML 被清除，来源蓝色等强调色与 PodAura 当前主题协调。
- 清洗后的 HTML 在 Android WebView 中运行于不透明 origin。
- 配置变更和页面重入不重置已恢复的阅读位置。
- Common、Android、iOS simulator、macOS 和 JVM 的编译与相关测试通过。
- 渲染任务在成功、失败、超时和取消路径上均无浏览器实例、协程或 delegate 泄漏。

## 15. 建议提交拆分

1. `refactor: remove site-specific full-content adapters`
2. `feat: add structured full-content candidates and quality scoring`
3. `feat(android): extract full content from rendered pages`
4. `feat(apple): add rendered-page full-content extraction`
5. `feat(desktop): add isolated JavaFX page rendering`
6. `test: cover dynamic extraction safety and lifecycle`

每个提交都应独立可编译；在对应平台 provider 未完成前，使用显式的 `RenderedPageUnavailable` 实现，不允许临时恢复站点特判。
