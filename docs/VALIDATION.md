# 0.1.0 驗證紀錄

- 環境：Windows、JDK 21.0.11、Gradle 8.13、Android Gradle Plugin 8.13.2、compileSdk 36、targetSdk 35、minSdk 29。
- `scripts/test-rules.ps1`：14 項檢查通過，涵蓋台灣國際碼／國內碼比對、拒絕空號與尾碼匹配、電話格式與分頁邊界。
- `assembleRelease lintRelease`：通過。lint 0 errors，1 warning（OldTargetApi：target 35，未直接提高至尚未實測的 36）。
- `apksigner verify --verbose`：APK v2 簽章驗證通過。
- Android 10 模擬器 `Pixel_2_Edited_API_29`：嘗試一般啟動及停用快照啟動，ADB 皆停留 offline；**未完成 APK 安裝、畫面檢查或來電模擬**。
- 尚無連接實體手機；**真實電話接聽／掛斷、雙 SIM、LINE 操作、GPS 簡訊、開機／亮屏／翻面返回，以及大字體／橫向布局未實測**。

此檔是檢查紀錄，不表示已通過 `DEVICE_TESTS.md` 的實機驗收。發佈標記為 prerelease。
