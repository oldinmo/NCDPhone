# 中央氣象署資料來源

鄉鎮與縣市索引取自交通部中央氣象署公開網站，擷取日期：2026-10-26。

- https://www.cwa.gov.tw/Data/js/info/Info_County.js
- https://www.cwa.gov.tw/Data/js/info/Info_Town.js
- 鄉鎮預報入口：https://www.cwa.gov.tw/V8/C/W/Town/Town.html
- 預報使用該頁公開載入的 `/Data/js/3hr/ChartData_3hr_T_{縣市代碼}.js`。

索引原始檔保留於 app/src/main/assets；僅讀取資料常值，不執行下載的 JavaScript。使用氣象署提供的繁體中文天氣文字，日期時間以 Asia/Taipei 解析。每 30 分鐘快取一次；資料更新超過 6 小時或找不到近 3 小時對應時段時拒絕使用。

這是官方網站的公開資料檔，並非有版本保證的 OpenData API。網站格式改版時可能暫時無法顯示，需更新解析程式。未使用私人 API 授權碼，不傳送經緯度或聯絡人至氣象署；氣象署可看到縣市代碼及連線 IP。行政區的地理編碼依手機系統服務運作。

首頁只顯示行政區、天氣、溫度與圖示；點選可查看來源。此功能不是即時測站觀測或颱風警報。
