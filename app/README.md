# CampusSchedule 校园课表 App

一款面向大学生的 Android 课表管理应用,集成**课程表管理、空教室查询与借用、成绩管理(多格式成绩单导入)、个人中心**四大模块,数据全部本地持久化,无需网络。

---

## 一、功能概览

### 1. 登录 / 注册
- 学号 + 密码注册登录,学号格式校验(10 位、唯一性约束)
- 找回密码(学号 + 手机号验证)、手机号绑定
- SessionManager 基于 SharedPreferences 管理登录态,冷启动自动恢复会话
- 注册时可设置「学期起始日期」与「当前周」,驱动课表周次计算

### 2. 课程表
- **网格课表视图**:星期 × 节次矩阵,课程单元格按周次区间生效,支持左右滑动切换周(共 17+ 周),顶部星期行横向滚动
- **「今」按钮**一键回到当前教学周;当前星期列高亮
- 课程增删改(AddEditCourseFragment + ViewModel)
- **课表文件导入**:支持教务系统导出的多种格式(见 §5 解析引擎),自动识别课程名/教师/教室/周次/节次并配色

### 3. 空教室 & 教室借用
- 按教学楼/节次/周次筛选空闲教室(基于课表占用反查)
- 借用申请单:用途、时间、审核人、借教人、附件上传(拍照 / 相册选图,图片落地到应用私有目录)
- 借用记录列表,状态标记

### 4. 成绩管理
- **分学期成绩单**:按学号前两位推算入学年份,自动生成「入学后 8 个学期 + 全部」下拉选项;「全部」为所有学年的自动汇总
- **多格式成绩单导入**:xlsx / docx(Word)/ XML Spreadsheet / HTML 表格,两种识别模式:
  - 表格式:定位「课程 / 成绩」表头,按列映射逐行解析
  - 词流块状式:全文按空格/换行摊成 token 流,以「课程名 → 8~12 位课程编号 → 学分数 → 必修/选修/公选 → 成绩」五元组滑动窗口匹配,对 Word 的软回车、整段粘贴等排版完全免疫
- **4.0 分制绩点**:数字分按 90/80/70/60 分段线性换算,等级制(优/良/中/及格)映射 4.0/3.0/2.0/1.0;平均绩点按学分加权
- **列排序**:点击「学分 / 成绩」表头切换 高→低 / 低→高(成绩排序按实际分值,等级按分数段中值插入:优≈95、良≈85)
- 手动添加成绩、点击课程行修改所属学期、长按学期下拉框清空学期/全部成绩
- 成绩单表格支持横向滑动(课程/课程编号/学分/课程属性/成绩)

### 5. 个人中心 & 主题
- 显示真实姓名、学号、课表/成绩统计
- 亮/暗主题切换(ThemeManager + values-night 双套颜色),切换时保持当前浏览周

---

## 二、技术栈

| 类别 | 技术 | 版本 |
|---|---|---|
| 语言 | Kotlin | 2.2.10(AGP 内置,无需独立插件) |
| 构建 | Android Gradle Plugin | 9.4.1 |
| UI | Material Components (Material3) | 1.12.0 |
| 架构组件 | ViewModel / LiveData / Lifecycle | 2.8.7 |
| 数据库 | Room(kapt 编译) | 2.7.1 |
| 布局 | ConstraintLayout / RecyclerView / Fragment | 2.2.1 / 1.4.0 / 1.8.9 |
| 文本解析 | Jsoup | 1.18.1 |
| 序列化 | Gson | 2.11.0 |
| 图表 | MPAndroidChart | v3.1.0 |
| 视图绑定 | ViewBinding | 启用 |

- compileSdk / targetSdk:37,minSdk:24,Java/JVM Target:11
- 依赖统一通过 Gradle Version Catalog(`gradle/libs.versions.toml`)管理

## 三、架构设计

采用经典 MVVM 分层:

```
UI (Fragment / Adapter)
   │  ViewBinding + LiveData 观察
ViewModel (ScheduleViewModel / GradeViewModel / LoginViewModel / AddEditCourseViewModel)
   │  suspend 函数 + CoroutineScope
Repository (ScheduleRepository,唯一数据入口)
   │  Room DAO × 5
SQLite (Room · AppDatabase v7)
```

- **单向数据流**:Fragment 只发意图、收状态;如成绩模块 `GradeViewModel` 用 `combine(observeGrades, selectedSemesterFlow).asLiveData()` 实现「学期筛选」与「数据变更」自动联动刷新
- **工具层(util)**:SessionManager(会话)、ThemeManager(主题)、ScheduleImporter / GradeImporter(解析引擎)、SectionTimes(节次时间)

## 四、数据库设计(Room v7)

| 实体 | 表 | 关键字段 |
|---|---|---|
| User | users | 学号(唯一)、密码、姓名、手机号、学期起始日期 |
| Course | courses | 名称、教师、教室、dayOfWeek、start/endSection、start/endWeek、color、type |
| Grade | grades | semester、courseName、courseCode、credit、property、gradeText、gpa |
| Classroom | classrooms | 教学楼、教室号、容量 |
| BorrowRecord | borrow_records | 申请人、教室、用途、时间、审核人、借教人、attachment(附件路径)、状态 |

- 复杂类型经 `Converters.kt`(TypeConverter)序列化
- 版本演进 v1→v7 全程 `fallbackToDestructiveMigration()`,升级即重建 + 种子数据自动回填(`seedWeek1CoursesIfEmpty` / `seedClassroomsIfEmpty`)
- schema 快照导出至 `app/schemas/`

## 五、解析引擎(ScheduleImporter / GradeImporter)

教务文件「一份数据、N 种格式」,解析器以**魔数嗅探 + 多后端**设计统一收口:

```
byte[] → 嗅探文件头
  ├─ "PK"     → ZIP 容器 → 含 word/document.xml ? docx : xlsx
  ├─ D0CF11E0 → 拒绝 OLE97-2003,提示另存为 .docx/.xlsx
  ├─ 含 mso-application / <Workbook → XML Spreadsheet 2003
  ├─ 含 <table>                     → Jsoup HTML 解析
  └─ 其他                           → CSV/TSV 分隔解析(UTF-8 → GBK 回退)
        ↓
  统一输出 List<List<String>> 文本网格
        ↓
  ├─ 课表管线:定位「星期一~星期日」表头(正则 星期|周|礼拜)→ 节次解析(阿拉伯区间/中文数字)→ 单元格四段拆分(课程/教师/周次/教室)
  └─ 成绩管线:表头列映射模式 + 词流五元组块状模式(兜底)
```

实现要点:
- **零第三方 Excel/Word 依赖**:xlsx 用 `java.util.zip.ZipInputStream` 解 `sharedStrings.xml` + `sheetN.xml`,单元格 `r="B4"` 折叠 26 进制算列号;docx 提取 `<w:tbl>/<w:tr>/<w:tc>`(命名空间前缀与属性宽松匹配),`<w:t>/<w:br>/<w:tab>` 转文本
- XML Spreadsheet 支持自闭合 `<Cell/>` 与 `ss:Index` 空位对齐,避免列错位
- 周次解析支持 `1-14(周)`、`1-5,9-10(周)` 等教务格式;中文节次「第一二节」→ 1-2 节

## 六、文件选择与图片处理

- SAF(`ActivityResultContracts.OpenDocument`,`*/*`)导入外部文件,内容流方式读取,无存储权限依赖
- 附件:拍照用 `TakePicturePreview`(直接返回 Bitmap,免 FileProvider),相册用 `GetContent("image/*")` 后拷贝至 `filesDir/attachments/` 私有目录

## 七、工程结构

```
app/src/main/java/com/example/campusschedule/
├── CampusApp.kt                  # Application:DB 初始化、种子数据
├── MainActivity.kt               # 单 Activity + Navigation(底部导航 4 Tab)
├── data/
│   ├── local/                    # AppDatabase、Converters、dao/×5
│   ├── model/                    # User / Course / Grade / Classroom / BorrowRecord + gradeTextToGpa
│   └── repository/               # ScheduleRepository
├── ui/
│   ├── login/                    # 登录 / 注册 / 找回密码
│   ├── schedule/                 # 课表页、网格构建、周页适配器、课程增改
│   ├── classroom/                # 空教室、借用申请、借用记录
│   ├── grade/                    # 成绩页 + GradeViewModel
│   └── profile/                  # 个人中心、详情
└── util/                         # ScheduleImporter / GradeImporter / SessionManager / ThemeManager / SectionTimes
```

## 八、构建与运行

**环境要求**:Android Studio(自带 JBR 25)、Android SDK 37

命令行构建需在 `gradle.properties` 中禁用工具链自动下载并指定 JDK:

```properties
org.gradle.java.installations.auto-download=false
org.gradle.java.installations.paths=C\:\\Program Files\\Android\\Android Studio\\jbr
```

```bash
./gradlew assembleDebug          # 构建
./gradlew installDebug           # 安装到已连接设备
```

## 九、已知设计取舍

- 数据库升级采用破坏性迁移(演示期可接受);量产应改为带 `Migration` 的增量迁移
- 成绩单若不含「学期」列,导入时以弹窗指定的学期为准(按学期替换,不污染其他学期)
- 演示种子数据(课表/教室)仅在对应表为空时写入,导入真实数据后不再触发