# UniTrack+ (大學生日常) 🎓

[![Android](https://img.shields.io/badge/Platform-Android%20(API%2024%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Storage-Room%20(SQLite)-00599C?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Firebase](https://img.shields.io/badge/Backend-Firebase%20(Auth%20%2B%20Firestore)-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com/)

> **專為大學生量身打造的全方位學業與生活管理助理。**  
> 集結「智慧課表」、「畢業學分審查」、「GPA / 學業儀表板」、「個人記帳與預算管理」於一體，支援純本機離線隱私保護與雲端雙向同步。

---

## 📑 目錄

- [✨ 核心功能](#-核心功能)
- [🏗️ 技術架構](#️-技術架構)
- [📂 專案目錄結構](#-專案目錄結構)
- [🚀 快速開始](#-快速開始)
  - [環境需求](#環境需求)
  - [專案建置與執行](#專案建置與執行)
  - [Firebase 與環境變數設定](#firebase-與環境變數設定)
- [📊 功能模組詳解](#-功能模組詳解)
  - [1. 儀表板 (Dashboard)](#1-儀表板-dashboard)
  - [2. 智慧課表與成績管理 (Timetable & Grades)](#2-智慧課表與成績管理-timetable--grades)
  - [3. 畢業審查與學分稽核 (Graduation Audit)](#3-畢業審查與學分稽核-graduation-audit)
  - [4. 個人記帳與預算規劃 (Expense Tracker)](#4-個人記帳與預算規劃-expense-tracker)
  - [5. 帳號與資料同步 (Auth & Sync)](#5-帳號與資料同步-auth--sync)
- [🧪 測試與品質保證](#-測試與品質保證)
- [📄 授權條款](#-授權條款)

---

## ✨ 核心功能

* 📅 **智慧週課表**：視覺化課表排程、支援自訂節次（1~14節）、多學期無縫切換、課堂色塊客製與今日課表快速檢視。
* 🎓 **學分與畢業審查**：全類別學分稽核（校共同、院核心、系專業/基礎/核心模組、通識、自由選修、體育），即時計算已修得與修習中進度，搭配畢業門檻檢核清單（如英檢、服務學習、專題）。
* 📈 **GPA 與學業儀表板**：支援 4.3 制、4.0 制與百分制等多種 GPA 計算演算法，即時試算各學期與歷年累計 GPA / 平均分數。
* 💰 **生活記帳與預算控制**：多類別收支紀錄、支付方式分類、月度預算進度條與超支警示。
* 🔒 **隱私至上 & 訪客模式 (Guest Mode)**：無須登入即可 100% 離線使用，所有資料安全儲存於手機本機 Room 資料庫。
* ☁️ **雲端雙向同步 (Cloud Sync)**：整合 Firebase Auth（Google Sign-In 與 Email 登入）及 Cloud Firestore，支援多裝置一鍵備份與還原。
* 📦 **資料備份與還原**：支援 JSON 格式檔案本機一鍵匯出與匯入，方便資料備份與設備遷移。

---

## 🏗️ 技術架構

UniTrack+ 遵循 **Modern Android Architecture (MVVM + Clean Architecture)** 開發範式：

```mermaid
graph TD
    subgraph UI Layer [UI Layer (Jetpack Compose & Material 3)]
        View[Screens & Composables]
        VM[StudentViewModel]
    end

    subgraph Domain & Data Layer [Data & Repository Layer]
        AuthRepo[AuthRepository]
        StudentRepo[StudentRepository]
        SyncRepo[FirestoreSyncRepository]
    end

    subgraph Data Sources [Data Sources]
        RoomDB[(Room Local DB / SQLite)]
        FirebaseAuth[(Firebase Auth & Credential Manager)]
        Firestore[(Cloud Firestore)]
    end

    View -->|Observe StateFlow| VM
    View -->|User Events| VM
    VM --> AuthRepo
    VM --> StudentRepo
    VM --> SyncRepo

    StudentRepo --> RoomDB
    AuthRepo --> FirebaseAuth
    SyncRepo --> RoomDB
    SyncRepo --> Firestore
```

### 使用技術與套件

| 領域 | 使用技術 / 函式庫 | 說明 |
| :--- | :--- | :--- |
| **語言** | Kotlin 2.0+ | 現代化、簡潔且具備空安全性的程式語言 |
| **UI 介面** | Jetpack Compose + Material 3 | 宣告式 UI 框架、動態配色、Edge-to-Edge 全螢幕佈局 |
| **非同步 & 狀態** | Kotlin Coroutines + Flow / StateFlow | 響應式資料流與生命週期感知狀態管理 |
| **本機資料庫** | Android Jetpack Room + KSP | 具備型別安全的 SQLite 物件映射與抽象層 |
| **雲端認證** | Firebase Authentication + Credential Manager | 支援 Google 帳號授權登入與 Email 帳密體系 |
| **雲端資料庫** | Cloud Firestore | 彈性高擴展的 NoSQL 雲端即時同步資料庫 |
| **測試框架** | JUnit 4 + Robolectric + Roborazzi | 單元測試與截圖對比測試 (Screenshot Testing) |

---

## 📂 專案目錄結構

```text
D:\UniTrack+
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt               # 主入口點與 Jetpack Compose Navigation 導航圖
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/                    # Room DB, DAO (Course, Graduation, Expense)
│   │   │   │   │   ├── model/                    # 實體資料模型 (Entities, Enums, AuthModels)
│   │   │   │   │   └── repository/               # StudentRepository, AuthRepository, FirestoreSyncRepository
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/               # 通用 UI 元件 (卡片、進度條、對話框)
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── auth/                 # 登入與註冊頁面 (Google / Email / 訪客)
│   │   │   │   │   │   ├── dashboard/            # 學業與日常綜合儀表板
│   │   │   │   │   │   ├── timetable/            # 課表視圖、課程增修、成績登記
│   │   │   │   │   │   ├── graduation/           # 畢業審查、學分分類檢核、門檻清單
│   │   │   │   │   │   ├── expense/              # 記帳收支記錄、月預算設定
│   │   │   │   │   │   └── settings/             # 帳號管理、雲端同步、JSON 匯入匯出
│   │   │   │   │   ├── theme/                    # Material 3 主題、字體與色彩配置
│   │   │   │   │   └── viewmodel/                # StudentViewModel (全域狀態與業務邏輯核心)
│   │   │   └── res/                              # 應用程式資源 (Icons, Strings, Themes)
│   │   └── test/                                 # 單元測試與 Roborazzi 截圖測試
│   └── build.gradle.kts                          # 模組建置腳本與相依套件設定
├── gradle/                                       # Gradle Wrapper 與 Version Catalog (libs.versions.toml)
├── .env.example                                  # 環境變數設定範本
├── build.gradle.kts                              # 專案級建置設定
└── settings.gradle.kts                           # 專案模組與儲存庫宣告
```

---

## 🚀 快速開始

### 環境需求

* **Android Studio**: Ladybug / Koala 或更高版本
* **JDK**: OpenJDK 11 或 17+
* **Android SDK**: 
  * `compileSdk`: 37
  * `minSdk`: 24 (Android 7.0+)
  * `targetSdk`: 37

### 專案建置與執行

1. **複製專案**
   ```bash
   git clone https://github.com/ZhaoHong04126/UniTrack.git
   cd UniTrack+
   ```

2. **在 Android Studio 中開啟**
   * 打開 Android Studio，選擇 `Open an Existing Project` 並選擇本專案目錄。
   * 等待 Gradle 同步完成。

3. **執行專案**
   * 連接實體 Android 裝置或啟動 Android 模擬器 (AVD)。
   * 點選 Android Studio 工具列上的 **Run (Shift + F10)** 即可部署並啟動 App。

### Firebase 與環境變數設定

本專案內建支援 Firebase 認證與 Firestore 雲端同步：
1. 專案預設啟用 `googleServices.missing.passthrough=true`，若未提供 `google-services.json`，應用程式仍可於**訪客模式 (Guest Mode)** 下完美離線運作。
2. 若需啟用完整的 Google 登入與 Cloud Firestore 同步功能：
   * 在 [Firebase Console](https://console.firebase.google.com/) 建立專案。
   * 新增 Android 應用程式（套件名稱為 `com.unitrack.app`）。
   * 下載 `google-services.json` 並放置於 `app/` 目錄下。
   * 於 Firebase Console 啟用 **Authentication (Email/Password & Google)** 與 **Cloud Firestore**。

---

## 📊 功能模組詳解

### 1. 儀表板 (Dashboard)
* **今日課堂即時卡片**：自動依當前星期推薦今日要上的課程與教室位置。
* **學業成就快覽**：即時顯示累計 GPA、平均分數及修習學分總覽。
* **生活與財務摘要**：本月剩餘預算百分比與今日消費動態。

### 2. 智慧課表與成績管理 (Timetable & Grades)
* **網格週課表**：直覺展示週一至週日各節次課程，自動避開衝突與靈活顯示。
* **課程資訊管理**：支援填寫課程代碼、授課教師、上課教室、學分數與課程所屬領域。
* **成績登記系統**：支援即時登錄成績（支援等第或百分制），即時更新修畢狀態與學分計算。

### 3. 畢業審查與學分稽核 (Graduation Audit)
* **自訂畢業標準**：可依各校各系規定調整校共同、院核心、系專業模組、通識、自由選修等標準門檻。
* **學分完成度檢驗**：圖表化呈現各分類「已修畢 (Earned)」、「修習中 (In-progress)」與「目標學分 (Target)」。
* **畢業門檻勾選清單**：自訂外語檢定 (如 TOEIC/TOEFL)、服務學習、畢業專題、專業證照等檢核項目。

### 4. 個人記帳與預算規劃 (Expense Tracker)
* **快速記帳**：預設餐飲、交通、娛樂、學習、住宿等豐富標籤，記錄消費時間與支付方式。
* **預算監控**：自訂每月花費上限，以進度條警示預算消耗程度，防止月底透支。

### 5. 帳號與資料同步 (Auth & Sync)
* **免登入離線體驗**：無聯網限制，開箱即用。
* **雲端備份與還原**：隨時將本機完整數據一鍵上傳至 Firestore，更換手機無縫銜接。
* **JSON 檔案匯出/匯入**：標準 JSON 格式匯出，便於個人數據備份與手動分析。

---

## 🧪 測試與品質保證

本專案配置有單元測試與截圖比對測試：

* **執行單元測試與 Robolectric 測試**：
  ```bash
  ./gradlew testDebugUnitTest
  ```
* **執行 Roborazzi 截圖測試驗證 UI**：
  ```bash
  ./gradlew verifyRoborazziDebug
  ```
* **更新 Roborazzi 截圖基準檔**：
  ```bash
  ./gradlew recordRoborazziDebug
  ```

---
