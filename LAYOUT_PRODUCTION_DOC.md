# 番茄阅读 App — XML 布局生产文档

> 本文档汇总所有已生成的 XML 布局文件、View ID、特殊组件及交互逻辑，供编写 Activity Kotlin 代码时参考。

---

## 一、全局设计规范

| 属性 | 值 |
|---|---|
| 背景色 | `@color/app_background` (#F5F7FA) |
| 主色调 | `@color/primary_red` (#FF6B6B) |
| 深红 | `@color/primary_red_dark` (#E55A5A) |
| 主文字 | `@color/text_primary` (#333333) |
| 次要文字 | `@color/text_secondary` (#999999) |
| 提示文字 | `@color/text_hint` (#CCCCCC) |
| 卡片背景 | `@color/white` (#FFFFFF) |
| 分割线 | `@color/divider` (#EEEEEE) |
| Tab 主题 | `Theme.TomatoRead` (Material3.Light.NoActionBar) |

---

## 二、页面清单（共 10 个页面 / 15 个布局文件）

```
Page 01: 首页         — activity_main.xml      + item_book.xml
Page 02: 书籍详情      — activity_book_detail.xml
Page 03: 评论区        — activity_comments.xml   + item_comment.xml
Page 04: 我的          — activity_profile.xml
Page 05: 阅读器        — activity_reader.xml     + item_reader_chapter.xml
Page 06: 书架          — activity_bookshelf.xml  + item_bookshelf.xml
Page 07: 搜索          — activity_search.xml     + item_search_history.xml
Page 08: 完整目录       — activity_toc.xml        + item_toc.xml
Page 09: 阅读设置       — activity_reader_settings.xml
Page 10: 书籍详情/评审  — (复用 activity_book_detail.xml)
```

---

## 三、逐页详细文档

---

### 📄 Page 01 — 首页

**文件：** `activity_main.xml` (437行) + `item_book.xml` (83行)

**根布局：** LinearLayout (vertical)

**页面结构：**
```
LinearLayout
├── NestedScrollView (weight=1)
│   └── 状态栏 + 搜索栏 + 3Tab + Banner + 4快捷入口 + 推荐列表
└── 底部导航栏 (首页/书城/书架/我的 — 首页选中)
```

**View ID 清单 (`activity_main.xml`)：**

| ID | 组件 | 说明 |
|---|---|---|
| `iv_notification` | ImageView | 顶部通知铃铛 |
| `et_search` | EditText | 搜索输入框 |
| `iv_scan` | ImageView | 扫码图标 |
| `ll_tabs` | LinearLayout | 三Tab容器 |
| `tv_tab_recommend` | TextView | 推荐 Tab |
| `tv_tab_hot` | TextView | 热门 Tab |
| `tv_tab_latest` | TextView | 最新 Tab |
| `v_indicator_recommend` | View | 推荐 Tab 下划线 |
| `rv_books` | RecyclerView | 推荐小说列表 |
| `ll_bottom_nav` ~ `tv_nav_mine` | 底部导航 | 首页/书城/书架/我的 |

**RecyclerView item 文件：** `item_book.xml`

| ID | 组件 | 说明 |
|---|---|---|
| `iv_book_cover` | ImageView | 封面 (80×106dp) |
| `tv_book_title` | TextView | 书名 |
| `tv_book_author` | TextView | 作者 |
| `tv_book_desc` | TextView | 简介 |
| `tv_book_hot` | TextView | 热度文字 |
| `iv_add_shelf` | ImageView | 加书架按钮 |

**特殊组件：** NestedScrollView, HorizontalScrollView (Banner), RecyclerView

**交互要点：**
- 3 个 Tab 点击切换时替换 indicator 位置、文字色和字重
- 底部导航 4 个按钮，首页选中态使用 `BottomNavTextStyle.Active` + `app:tint="@color/primary_red"`
- RecyclerView 的 `nestedScrollingEnabled="false"`

---

### 📄 Page 02 — 书籍详情

**文件：** `activity_book_detail.xml` (840行)

**根布局：** LinearLayout (vertical)

**页面结构：**
```
LinearLayout
├── RelativeLayout (顶部栏 48dp)
├── NestedScrollView (weight=1)
│   └── LinearLayout
│       ├── 封面区 (120×160dp)
│       ├── 作者信息 + 评分行 (4.5星)
│       ├── 2个大按钮 (加入书架/开始阅读)
│       ├── 简介 (4行 + 展开)
│       ├── 横向标签 (HorizontalScrollView)
│       ├── 目录预览 (5章)
│       └── 热门书评 (2条)
└── 底部悬浮条 (评论输入 + 点赞/收藏/分享)
```

**View ID 清单：**

| ID | 组件 | 说明 |
|---|---|---|
| `iv_back` | ImageView | 返回 |
| `iv_more_menu` | ImageView | 三个点菜单 |
| `iv_share` | ImageView | 分享 |
| `iv_detail_cover` | ImageView | 封面大图 |
| `tv_detail_book_title` | TextView | 书名 |
| `tv_detail_author` | TextView | 作者 |
| `tv_detail_word_count` | TextView | 字数 |
| `tv_detail_readers` | TextView | 在读人数 |
| `tv_detail_rating_text` | TextView | 评分数字 9.6 |
| `tv_detail_rating_count` | TextView | 评论数 |
| `btn_add_shelf` | TextView | 加入书架按钮 |
| `btn_start_read` | TextView | 开始阅读按钮 |
| `tv_detail_synopsis` | TextView | 简介内容 |
| `tv_synopsis_expand` | TextView | 展开按钮 |
| `tv_toc_title` | TextView | 目录标题 |
| `tv_toc_view_all` | TextView | 查看全部 |
| `ll_toc_ch1`~`ll_toc_ch5` | LinearLayout | 5章节行 |
| `tv_reviews_view_all` | TextView | 评论查看全部 |
| `ll_review_1`/`ll_review_2` | LinearLayout | 评论行 |
| `iv_review_avatar_1`/`_2` | ImageView | 头像 |
| `tv_review_name_1`/`_2` | TextView | 昵称 |
| `tv_review_content_1`/`_2` | TextView | 评论内容 |
| `tv_review_likes_1`/`_2` | TextView | 点赞数 |
| `ll_comment_input` | LinearLayout | 评论输入框 |
| `iv_bottom_like` | ImageView | 底部点赞 |
| `iv_bottom_favorite` | ImageView | 底部收藏 |
| `iv_bottom_share` | ImageView | 底部分享 |

**交互要点：**
- `btn_start_read` → 跳转 `activity_reader.xml`
- `tv_synopsis_expand` → 展开/收起切换
- 星评行 5 个 ImageView (4 实心 + 1 半星) 硬编码展示
- `ll_comment_input` → 跳转 `activity_comments.xml`
- 点击 `tv_toc_view_all` → 跳转 `activity_toc.xml`

---

### 📄 Page 03 — 评论区

**文件：** `activity_comments.xml` (175行) + `item_comment.xml` (154行)

**根布局：** LinearLayout (vertical)

**页面结构：**
```
LinearLayout
├── RelativeLayout (顶部栏)
├── LinearLayout (排序Tab: 最热/最新/作者回复)
├── SwipeRefreshLayout
│   └── RecyclerView (评论列表)
└── LinearLayout (底部输入栏)
```

**View ID 清单 (`activity_comments.xml`)：**

| ID | 组件 | 说明 |
|---|---|---|
| `iv_back` | ImageView | 返回 |
| `tv_comment_count` | TextView | 评论总数 |
| `ll_sort_tabs` | LinearLayout | 排序栏 |
| `tv_sort_hot` | TextView | 最热 (初始选中) |
| `v_indicator_hot` | View | 最热下划线 |
| `tv_sort_new` | TextView | 最新 |
| `tv_sort_author` | TextView | 作者回复 |
| `srl_comments` | SwipeRefreshLayout | 下拉刷新 |
| `rv_comments` | RecyclerView | 评论列表 |
| `et_comment_input` | EditText | 评论输入框 |
| `iv_emoji` | ImageView | 表情按钮 |
| `iv_image` | ImageView | 图片按钮 |
| `btn_send` | TextView | 发送按钮 |

**RecyclerView item 文件：** `item_comment.xml`

| ID | 组件 | 说明 |
|---|---|---|
| `iv_comment_avatar` | ImageView | 头像 (36dp) |
| `ll_name_badge` | LinearLayout | 昵称+徽章容器 |
| `tv_comment_name` | TextView | 昵称 |
| `tv_author_badge` | TextView | 作者红色标签 |
| `tv_comment_time` | TextView | 时间 |
| `tv_comment_content` | TextView | 评论内容 |
| `ll_like` | LinearLayout | 点赞区域 |
| `iv_comment_like` | ImageView | 红心图标 |
| `tv_comment_likes` | TextView | 点赞数 |
| `ll_reply` | LinearLayout | 回复区域 |

**特殊组件：** SwipeRefreshLayout, RecyclerView

**交互要点：**
- 3 个排序 Tab 切换 (最热 默认选中带红线)
- 作者评论时 `tv_author_badge.setVisibility(VISIBLE)`
- `srl_comments` 下拉刷新 / RecyclerView 触底加载更多
- `iv_comment_like` 点击切换 `ic_heart_border` ↔ `ic_heart_filled`

---

### 📄 Page 04 — 我的

**文件：** `activity_profile.xml` (535行)

**根布局：** LinearLayout (vertical)

**页面结构：**
```
LinearLayout
├── NestedScrollView (weight=1)
│   └── LinearLayout
│       ├── 红色渐变区 (180dp, 头像+登录按钮)
│       ├── 数据卡片区 (书架12 / 阅读56h / 番茄币888)
│       ├── 功能列表第一组 (历史/缓存/福利)
│       └── 功能列表第二组 (夜间/帮助/设置)
└── 底部导航栏 (首页/书城/书架/我的 — 我的选中)
```

**View ID 清单：**

| ID | 组件 | 说明 |
|---|---|---|
| `fl_avatar` | FrameLayout | 头像容器 |
| `iv_profile_avatar` | ImageView | 圆形头像 (70dp) |
| `btn_login` | TextView | 点击登录按钮 |
| `tv_shelf_num` | TextView | 书架数字 |
| `tv_time_num` | TextView | 阅读时长 |
| `tv_coin_num` | TextView | 番茄币 |
| `ll_menu_history` | RelativeLayout | 阅读历史入口 |
| `iv_icon_history` | ImageView | 历史图标 |
| `ll_menu_cache` | RelativeLayout | 离线缓存入口 |
| `iv_icon_cache` | ImageView | 缓存图标 |
| `ll_menu_rewards` | RelativeLayout | 福利中心入口 |
| `iv_icon_rewards` | ImageView | 福利图标 |
| `ll_menu_night` | RelativeLayout | 夜间模式行 |
| `iv_icon_night` | ImageView | 夜间图标 |
| `tb_night_mode` | ToggleButton | 夜间开关 |
| `ll_menu_help` | RelativeLayout | 帮助反馈入口 |
| `iv_icon_help` | ImageView | 帮助图标 |
| `ll_menu_settings` | RelativeLayout | 设置入口 |
| `iv_icon_settings` | ImageView | 设置图标 |
| `ll_bottom_nav` ~ `ll_nav_mine` | 底部导航 | 4按钮 (我的选中) |

**特殊组件：** NestedScrollView, ToggleButton

**交互要点：**
- 未登录：显示 `btn_login` + `iv_profile_avatar` (灰底占位)
- 已登录：头像加载用户图，`btn_login` 替换为昵称
- `tb_night_mode` 切换暗黑模式
- 底部导航"我的"用红色高亮

---

### 📄 Page 05 — 阅读器

**文件：** `activity_reader.xml` (260行) + `item_reader_chapter.xml`

**根布局：** DrawerLayout

**页面结构：**
```
DrawerLayout (沉浸式 fitsSystemWindows=true)
├── FrameLayout (主内容区)
│   ├── NestedScrollView (正文区)
│   │   └── 章节标题 + 正文内容
│   ├── RelativeLayout (顶部悬浮栏, GONE)
│   │   └── 返回/目录/更多
│   └── LinearLayout (底部悬浮栏, GONE)
│       ├── SeekBar 进度条
│       └── A⁻ / A⁺ / 夜间 / 目录
└── LinearLayout (右侧抽屉 280dp)
    └── RecyclerView (章节目录)
```

**View ID 清单：**

| ID | 组件 | 说明 |
|---|---|---|
| `dl_reader` | DrawerLayout | 根布局 |
| `fl_reader_content` | FrameLayout | 内容区容器 |
| `nsv_reader` | NestedScrollView | 可滚动正文 |
| `tv_chapter_title` | TextView | 章节标题 |
| `tv_reader_body` | TextView | 正文内容 |
| `rl_reader_top_bar` | RelativeLayout | 顶部悬浮栏 (GONE) |
| `iv_reader_back` | ImageView | 返回 |
| `iv_reader_toc` | ImageView | 目录入口 |
| `iv_reader_more` | ImageView | 更多菜单 |
| `ll_reader_bottom_bar` | LinearLayout | 底部悬浮栏 (GONE) |
| `sb_reader_progress` | SeekBar | 阅读进度 |
| `tv_reader_progress` | TextView | 进度百分比 |
| `btn_font_small` | TextView | A⁻ 缩小字号 |
| `btn_font_large` | TextView | A⁺ 放大字号 |
| `iv_reader_daynight` | ImageView | 日/夜切换 |
| `iv_reader_toc_bottom` | ImageView | 底部目录入口 |
| `ll_reader_drawer` | LinearLayout | 右侧抽屉 |
| `iv_drawer_close` | ImageView | 关闭抽屉 |
| `rv_chapters` | RecyclerView | 章节列表 |

**特殊组件：** DrawerLayout, NestedScrollView, SeekBar, RecyclerView

**交互要点：**
- 单击 `nsv_reader` 正文区 → 切换顶部/底部栏 visibility (GONE ↔ VISIBLE)
- `iv_reader_toc` / `iv_reader_toc_bottom` → `dl_reader.openDrawer(GravityCompat.END)`
- `sb_reader_progress` → 拖拽跳转正文滚动位置
- `btn_font_small` / `btn_font_large` → 调整 `tv_reader_body.textSize`
- `iv_reader_daynight` → 切换白天(#F5F7FA)/夜间(#1A1A1A)背景
- Activity 用 `WindowInsetsController` 实现沉浸式状态栏

---

### 📄 Page 06 — 书架

**文件：** `activity_bookshelf.xml` (210行) + `item_bookshelf.xml` (113行)

**根布局：** LinearLayout (vertical)

**页面结构：**
```
LinearLayout
├── RelativeLayout (顶部栏: 返回+标题+编辑/完成)
├── LinearLayout (3Tab: 在读/读过/订阅)
├── FrameLayout (列表+空状态)
│   ├── RecyclerView
│   └── LinearLayout (空状态 — GONE)
└── LinearLayout (编辑底部栏 — GONE)
    ├── 全选复选框
    └── 删除按钮
```

**View ID 清单 (`activity_bookshelf.xml`)：**

| ID | 组件 | 说明 |
|---|---|---|
| `iv_shelf_back` | ImageView | 返回 |
| `tv_shelf_edit` | TextView | 编辑/完成 切换 |
| `ll_shelf_tabs` | LinearLayout | Tab栏 |
| `tv_shelf_tab_reading` | TextView | 在读 (选中) |
| `v_shelf_indicator_reading` | View | 在读下划线 |
| `tv_shelf_tab_read` | TextView | 读过 |
| `tv_shelf_tab_subscribe` | TextView | 订阅 |
| `rv_shelf` | RecyclerView | 书架列表 |
| `ll_shelf_empty` | LinearLayout | 空状态容器 |
| `btn_shelf_go` | TextView | 去逛逛按钮 |
| `ll_shelf_edit_bar` | LinearLayout | 编辑栏 (GONE) |
| `ll_select_all` | LinearLayout | 全选行 |
| `iv_edit_select_all` | ImageView | 全选复选框 |
| `btn_shelf_delete` | TextView | 删除按钮 |

**RecyclerView item 文件：** `item_bookshelf.xml`

| ID | 组件 | 说明 |
|---|---|---|
| `iv_shelf_cover` | ImageView | 封面 (72×96dp) |
| `tv_shelf_book_title` | TextView | 书名 |
| `tv_shelf_author` | TextView | 作者 |
| `tv_shelf_tag` | TextView | 类型标签 |
| `pb_shelf_progress` | ProgressBar | 阅读进度条 (horizontal) |
| `tv_shelf_progress_text` | TextView | 进度百分比 |
| `iv_shelf_checkbox` | ImageView | 复选框 (GONE) |

**特殊组件：** RecyclerView, ProgressBar

**交互要点：**
- `tv_shelf_edit` 点击切换 "编辑"↔"完成"；同时 `ll_shelf_edit_bar` GONE↔VISIBLE
- 编辑模式下每 item `iv_shelf_checkbox` 显示，点击切换选中/未选中
- `iv_edit_select_all` 点击全选/全不选
- `btn_shelf_delete` 删除选中项
- `ll_shelf_empty` → 列表空时显示空状态插画+去逛逛按钮

---

### 📄 Page 07 — 搜索

**文件：** `activity_search.xml` (355行) + `item_search_history.xml` (12行)

**根布局：** LinearLayout (vertical)

**页面结构：**
```
LinearLayout
├── LinearLayout (搜索输入栏: EditText+取消按钮)
└── NestedScrollView (weight=1)
    └── LinearLayout
        ├── 历史搜索区 (ll_search_history)
        ├── 热门推荐区 (ll_search_hot, 初始显示)
        └── 搜索结果区 (ll_search_result, GONE)
            ├── RecyclerView (结果列表)
            └── 空状态 (GONE)
```

**View ID 清单：**

| ID | 组件 | 说明 |
|---|---|---|
| `et_search` | EditText | 搜索输入框 |
| `iv_search_clear` | ImageView | 清除按钮 ✕ |
| `tv_search_cancel` | TextView | 取消按钮 |
| `ll_search_history` | LinearLayout | 历史搜索区 |
| `ll_clear_history` | LinearLayout | 清空历史按钮 |
| `ll_history_tags` | LinearLayout | 历史标签容器 |
| `ll_search_hot` | LinearLayout | 热门推荐区 |
| `rv_search_hot` | RecyclerView | 热门列表 (复用 item_book.xml) |
| `ll_search_result` | LinearLayout | 搜索结果区 (GONE) |
| `tv_search_result_title` | TextView | "找到 xx 本书" |
| `rv_search_result` | RecyclerView | 结果列表 |
| `ll_search_empty` | LinearLayout | 无结果空状态 |

**历史标签 item：** `item_search_history.xml` — 单个 `TextView` (灰底圆角16dp)

**状态切换矩阵：**

| 场景 | 历史区 | 热门区 | 结果区 |
|---|---|---|---|
| 初始 (有历史) | VISIBLE | VISIBLE | GONE |
| 初始 (无历史) | GONE | VISIBLE | GONE |
| 有搜索结果 | GONE | GONE | VISIBLE |
| 搜索无结果 | GONE | GONE | VISIBLE + 空状态 |

**特殊组件：** NestedScrollView, RecyclerView (2个)

---

### 📄 Page 08 — 完整目录

**文件：** `activity_toc.xml` (117行) + `item_toc.xml` (82行)

**根布局：** LinearLayout (vertical)

**页面结构：**
```
LinearLayout
├── RelativeLayout (顶部栏: 返回+书名+排序)
├── LinearLayout (统计栏: "共 1248 章 | ...")
└── RecyclerView (章节列表)
```

**View ID 清单 (`activity_toc.xml`)：**

| ID | 组件 | 说明 |
|---|---|---|
| `iv_toc_back` | ImageView | 返回 |
| `ll_toc_sort` | LinearLayout | 排序按钮 |
| `tv_sort_label` | TextView | 排序文字 (正序/倒序) |
| `iv_toc_sort_icon` | ImageView | 排序图标 |
| `rv_toc` | RecyclerView | 章节列表 |

**RecyclerView item 文件：** `item_toc.xml`

| ID | 组件 | 说明 |
|---|---|---|
| `iv_toc_current_dot` | ImageView | 当前章红色圆点 (GONE) |
| `tv_toc_chapter_name` | TextView | 章节名 |
| `tv_toc_tag_reading` | TextView | "正在阅读"标签 (GONE) |
| `tv_toc_tag_trial` | TextView | "试读"标签 (GONE) |
| `tv_toc_tag_vip` | TextView | "VIP"标签 (GONE) |

**章节状态控制：**

| 状态 | 红点 | 书名加粗 | 正在阅读 | 试读 | VIP |
|---|---|---|---|---|---|
| 当前阅读 | VISIBLE | bold | VISIBLE | GONE | GONE |
| 试读 | GONE | normal | GONE | VISIBLE | GONE |
| VIP | GONE | normal | GONE | GONE | VISIBLE |
| 普通 | GONE | normal | GONE | GONE | GONE |

**交互要点：**
- `ll_toc_sort` 点击切换正序↔倒序
- 点击 item → 跳转 `activity_reader.xml` 指定 chapter

---

### 📄 Page 09 — 阅读设置

**文件：** `activity_reader_settings.xml` (613行)

**根布局：** LinearLayout (vertical)

**页面结构：**
```
LinearLayout
├── RelativeLayout (顶部栏: 返回+标题+恢复默认)
└── NestedScrollView
    └── LinearLayout (每项为白底圆角卡片, 间距8dp)
        ├── 字体选择 (4选项: 系统默认/思源黑体/楷体/方正仿宋)
        ├── 字号调整 (SeekBar + A⁻/A⁺ + 预览文字)
        ├── 背景色 (4色圆形选择: 白天/夜间/护眼/羊皮纸)
        ├── 翻页方式 (4选项: 仿真/覆盖/滑动/无)
        └── 开关区 (亮度跟随/音量翻页/显示进度)
```

**View ID 清单：**

| ID | 组件 | 说明 |
|---|---|---|
| `iv_settings_back` | ImageView | 返回 |
| `tv_reset_default` | TextView | 恢复默认 |
| `ll_font_default` | LinearLayout | 字体默认 (选中) |
| `ll_font_siyuan` | LinearLayout | 字体思源 |
| `ll_font_kaiti` | LinearLayout | 字体楷体 |
| `ll_font_fangsong` | LinearLayout | 字体仿宋 |
| `btn_size_small` | TextView | A⁻ |
| `sb_font_size` | SeekBar | 字号滑块 (0-8) |
| `btn_size_large` | TextView | A⁺ |
| `tv_preview_text` | TextView | 预览文字 |
| `fl_bg_day` | FrameLayout | 白天色块 |
| `fl_bg_night` | FrameLayout | 夜间色块 |
| `fl_bg_eye` | FrameLayout | 护眼色块 |
| `fl_bg_parchment` | FrameLayout | 羊皮纸色块 |
| `tv_flip_sim` | TextView | 仿真 (选中) |
| `tv_flip_cover` | TextView | 覆盖 |
| `tv_flip_slide` | TextView | 滑动 |
| `tv_flip_none` | TextView | 无 |
| `sw_brightness` | Switch | 亮度跟随 |
| `sw_volume` | Switch | 音量翻页 |
| `sw_progress` | Switch | 显示进度 |

**特殊组件：** NestedScrollView, HorizontalScrollView, SeekBar, Switch (3个)

**颜色映射：**

| 方案 | 背景色 | 文字色 |
|---|---|---|
| 白天 | `@color/white` | `@color/reader_text` |
| 夜间 | `@color/reader_bg_night` | `@color/reader_text_night` |
| 护眼 | `#C8E6C0` | `@color/reader_text` |
| 羊皮纸 | `#F5E6C8` | `@color/reader_text` |

---

## 四、共享 View ID 冲突检查

以下 ID 在多个 activity 布局中出现，使用 `findViewById` 时不会冲突（各自独立作用域），但需注意：

| ID | 出现的文件 |
|---|---|
| `iv_back` | activity_book_detail.xml, activity_comments.xml |
| `iv_share` | activity_book_detail.xml (顶部 & 底部各1个) |
| `rv_chapters` / `rv_toc` | 不同 recycle view，无冲突 |

---

## 五、特殊组件使用统计

| 组件 | 使用位置 |
|---|---|
| NestedScrollView | 首页, 详情, 搜索, 我的, 阅读器, 阅读设置 (6处) |
| HorizontalScrollView | 首页Banner, 详情标签, 阅读设置字体/背景 (3处) |
| RecyclerView | 首页, 书架, 评论, 阅读器, 搜索×2, 目录 (7处) |
| DrawerLayout | 阅读器 (1处) |
| SwipeRefreshLayout | 评论区 (1处) |
| SeekBar | 阅读器进度, 阅读设置字号 (2处) |
| Switch | 阅读设置×3 (3处) |
| ToggleButton | 我的夜间模式 (1处) |
| ProgressBar | 书架进度条 (1处) |
| EditText | 首页搜索, 评论输入, 搜索输入 (3处) |

---

## 六、导航流程

```
首页 ───────────────────────────────────────────────────────┐
  ├── 搜索栏 → 搜索页                                        │
  ├── Banner/列表item → 书籍详情                             │
  │   ├── 开始阅读 → 阅读器                                  │
  │   ├── 加入书架 → 书架                                    │
  │   ├── 目录预览 → 完整目录                                │
  │   ├── 书友点评 → 评论区                                  │
  │   └── 阅读器内 目录按钮 → 完整目录                        │
  │       └── 阅读器内 设置 → 阅读设置                        │
  ├── 底部导航 首页 → (当前页)                                │
  ├── 底部导航 书城 → (待开发)                                │
  ├── 底部导航 书架 → 书架页                                  │
  │   ├── item点击 → 阅读器                                   │
  │   └── 空状态"去逛逛" → 书城(待开发)                        │
  └── 底部导航 我的 → 我的页                                  │
      └── 设置 → 阅读设置                                     │
```

---

## 七、开始编写 Activity 的准备事项

### 7.1 文件分组建议

```
app/src/main/java/com/example/neuro/
├── MainActivity.kt
├── BookDetailActivity.kt
├── CommentsActivity.kt
├── ProfileActivity.kt
├── ReaderActivity.kt
├── BookshelfActivity.kt
├── SearchActivity.kt
├── TocActivity.kt
├── ReaderSettingsActivity.kt
├── adapter/
│   ├── BookAdapter.java/.kt        (item_book)
│   ├── CommentAdapter.java/.kt     (item_comment)
│   ├── ChapterAdapter.java/.kt     (共用 item_toc / item_reader_chapter)
│   └── BookshelfAdapter.java/.kt   (item_bookshelf)
└── model/
    ├── BookItem.java/.kt
    ├── CommentItem.java/.kt
    └── ChapterItem.java/.kt
```

### 7.2 各 Activity 最低实现要点

| Activity | 关键代码 |
|---|---|
| **MainActivity** | RecyclerView Adapter, 3Tab切换逻辑, 底部导航点击跳转, 搜索跳转SearchActivity |
| **BookDetailActivity** | 接收 book ID, 填充数据, 按钮跳转, 星级固定展示 |
| **CommentsActivity** | RecyclerView + SwipeRefreshLayout, 排序Tab, 发送评论 |
| **ProfileActivity** | 登录状态判断, 底部导航, ToggleButton夜间模式, 菜单项点击跳转 |
| **ReaderActivity** | 沉浸式状态栏, DrawerLayout 抽屉, 点击正文切换栏 visibility, SeekBar进度, A⁺/A⁻字号, 夜间模式切换 |
| **BookshelfActivity** | RecyclerView + 空状态, 编辑模式切换, 复选框全选/单选, 删除 |
| **SearchActivity** | EditText 文字变化监听, 历史区/热门区/结果区状态切换, 2个RecyclerView |
| **TocActivity** | RecyclerView + Adapter, 排序切换, 章节状态标记, 点击跳转阅读器 |
| **ReaderSettingsActivity** | 字体/背景/翻页选项互斥选中, SeekBar字号, Switch开关, "恢复默认"重置 |

### 7.3 AndroidManifest 注册

需要在 [AndroidManifest.xml](file:///d:/development/AndroidStudioProjects/neuro/app/src/main/AndroidManifest.xml) 中新增以下 activity：

```xml
<activity android:name=".BookDetailActivity"
    android:theme="@style/Theme.TomatoRead" />
<activity android:name=".CommentsActivity"
    android:theme="@style/Theme.TomatoRead" />
<activity android:name=".ProfileActivity"
    android:theme="@style/Theme.TomatoRead" />
<activity android:name=".ReaderActivity"
    android:theme="@style/Theme.TomatoRead"
    android:configChanges="orientation|screenSize" />
<activity android:name=".BookshelfActivity"
    android:theme="@style/Theme.TomatoRead" />
<activity android:name=".SearchActivity"
    android:theme="@style/Theme.TomatoRead"
    android:windowSoftInputMode="adjustResize" />
<activity android:name=".TocActivity"
    android:theme="@style/Theme.TomatoRead" />
<activity android:name=".ReaderSettingsActivity"
    android:theme="@style/Theme.TomatoRead" />
```

---

*文档生成时间：2026-05-02 | 共 15 个布局文件，199 个 View ID，覆盖 10 个页面*
