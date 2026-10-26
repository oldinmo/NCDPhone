# NCDPhone

為高齡者設計的高對比 Android 通訊介面。開發者：劉崇汎，中臺科技大學高齡健康照護系。

目前版本 **0.1.1 測試版**，Android 10（API 29）以上。請先完成實機驗收再交由長者使用。

## 下載

[GitHub Releases](https://github.com/oldinmo/NCDPhone/releases) 下載 APK，允許該下載來源安裝未知應用程式後安裝。更新請使用相同簽章的 APK，勿先移除 App，以免遺失聯絡人。

## 功能與設定

- 首頁四格照片與名字；超過四位時每 10 秒換頁，最後一頁不足四位顯示新增位置。
- 點聯絡人後顯示照片、姓名、大型綠色打電話按鈕與紅色返回按鈕。
- 預設電話角色提供來電接聽／拒接、通話掛斷、擴音與通話數字鍵盤；通話結束回首頁。
- 聯絡人手動新增或從手機電話簿引用，照片縮圖複製到 App 私有儲存區；電話與 LINE 顯示名稱至少一項。
- LINE 名稱存在時優先用 LINE 視訊。必須是聊天顯示名稱，並有唯一搜尋結果；不是 LINE ID。由照顧者在 LINE 先完成登入與相機／麥克風授權。
- 自訂按鈕在手指放開時執行一次；長按、範圍內移動均可，850ms 內重複點擊不重複執行。頁面切換另有短暫防誤觸。系統授權對話框、鍵盤、原生開關及 LINE 介面仍由各自系統處理。
- 設定欄位有效輸入會自動儲存，開關即時儲存；授權按鈕可再次進入變更或取消。首頁輪播在離開首頁／背景時取消，返回後重新計滿 10 秒。
- 預設 60 秒無操作回首頁，設定可改為 15～3600 秒。在其他 App 的閒置返回、亮屏返回與翻面返回需開啟操作協助及對應選項；通話時暫停。
- 指定預設桌面後 Home 鍵回 NCDPhone，開機由系統載入桌面；不會繞過 PIN 或鎖定畫面。可從設定開啟系統設定，修改預設桌面以退出。
- 照顧者來電定位簡訊預設關閉。啟用時，只對完整比對的照顧者號碼觸發；支援台灣 +886、00886 與國內格式，不以尾碼比對。需簡訊、精確位置及背景位置授權。取得新位置最多等待 30 秒，不發送過期快取；一分鐘內避免重複送出。多 SIM 手機請先設定預設簡訊 SIM。座標連結可用 Google Maps 開啟。
- 版本與開發者資訊直接讀取 `version.properties` 產生的 BuildConfig，沒有第二份手動版本字串。

## 首次使用

1. 在 NCDPhone 設定中指定預設電話及預設桌面。
2. 授權電話、通知；若需定位回傳，再授權簡訊、精確定位與背景位置「一律允許」。
3. 啟用 NCDPhone 操作協助；側載安裝在部分 Android 版本要先在應用程式資訊允許受限制的設定。
4. Android 14 以上允許全螢幕通知。依手機品牌調整自啟動／省電限制。
5. 加入聯絡人，設定照顧者號碼；在取得手機使用者同意後啟用定位簡訊。
6. 完成 [實機驗收](docs/DEVICE_TESTS.md)。LINE 控制及廠商特定背景行為必須逐機驗證。

## Android Studio 開發

開啟本資料夾，選用 **JDK 21**，SDK 36，Gradle 8.13 / Android Gradle Plugin 8.13.2。Android Studio 若使用 JDK 25，請在 Gradle JDK 設定改成 21。

```powershell
$env:JAVA_HOME='C:\Users\User\.jdks\jbr-21.0.11'
.\gradlew.bat assembleDebug lintDebug
.\scripts\test-rules.ps1
.\scripts\build-release.ps1
```

正式簽章的測試 APK 在 `dist/`。首次執行 release 腳本會產生 `.signing/ncdphone-release.jks` 與使用目前 Windows 帳號 DPAPI 加密的密碼。**請妥善備份簽章金鑰與可還原的密碼；遺失後不能直接更新既有安裝。** `.signing/` 不會提交 Git。DPAPI 檔不能直接換帳號／電腦解密。

## 版本發佈

每次更新都增加 `version.properties` 的 `versionCode` 並修改 `versionName`；更新發佈說明。GitHub Actions 對 main/PR 編譯及 lint，推送匹配版本的 `v*` tag 後使用 repository secrets 簽章並發佈 prerelease APK。

自動簽章發佈預設不啟用；本次 APK 在本機簽章後上傳。若使用者另行授權，設定必要 secrets：`NCD_KEYSTORE_BASE64`、`NCD_STORE_PASSWORD`、`NCD_KEY_PASSWORD`，key alias 固定 `ncdphone`，並設定 repository variable `NCD_AUTO_RELEASE=true`。CI secrets 須使用同一本機 release 金鑰，避免使用者更新時簽章不一致。

## 技術限制與隱私

不上架仍受 Android 系統權限、背景啟動、定位、通知及鎖屏限制。LINE 沒有在本專案使用的官方自動視訊 API；本版用 AccessibilityService 以可見文字逐步操作，不使用座標盲點或模糊姓名比對。LINE 版本或語系不同可能無法操作，會逾時停止；通話結束以掛斷控制消失及音訊模式共同判定，仍需實機確認。

聯絡人與照片只存於手機 App 私有目錄，停用系統備份；不記錄 LINE 聊天內容。本 App 不宣告網路權限。定位透過電信簡訊交給指定照顧者，可能產生費用；來電顯示號碼不能提供密碼等級的身分驗證，因此定位回傳應由手機使用者知情啟用。

參考：[Android 預設電話](https://developer.android.com/develop/connectivity/telecom/dialer-app)、[背景 Activity 啟動限制](https://developer.android.com/guide/components/activities/secure-bal)、[AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)。
