---
title: 數位放大鏡 隱私權政策 / Magnifier Privacy Policy
layout: default
---

# 數位放大鏡 隱私權政策

**生效日期**:2026-05-17  
**最後更新**:2026-05-17

「數位放大鏡」(`io.github.cjc305.magnifier`,以下簡稱「本 App」)由 cjc305 個人開發。本政策說明本 App 如何處理你在裝置上的資料。

## TL;DR

本 App **完全離線運作**:

- 不上傳任何資料到雲端
- 不收集個人資訊
- 不放廣告
- 不接第三方分析或追蹤 SDK
- 不需要建立帳號

## 我們存取什麼

本 App 在你裝置本機存取以下兩項,**僅供本機使用,從不離開你的裝置**:

### 1. 相機(`CAMERA` 權限)

- **用途**:即時放大鏡預覽、放大倍率 1× 至 10×、手電筒,以及拍照儲存
- **資料流**:相機影像僅在記憶體中處理供畫面顯示;只有你按下拍照按鈕時才會儲存成 JPEG
- **儲存位置**:儲存到你裝置的系統相簿(Android MediaStore `Pictures/Magnifier`)。本 App 不保留任何副本到自己的私有資料夾

### 2. 相片媒體
- Android 13+:`READ_MEDIA_IMAGES`
- Android 12 以下:`READ_EXTERNAL_STORAGE`

- **用途**:在 App 內顯示你過去用本 App 儲存過的放大鏡照片,以及讓你選擇刪除
- **存取範圍**:本 App 僅在 App 介面中顯示你選取或本 App 自己儲存過的圖片;不會主動掃描或上傳你的任何照片
- **刪除**:刪除操作只在你主動按下刪除按鈕時觸發。Android 11 及以上會跳出系統確認對話框,需要你二次同意才會真的刪掉

## 我們不收集什麼

- 你的姓名、Email、電話、地址等個人識別資訊
- 你的位置(本 App 未要求 `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` 任何位置權限)
- 你的通訊錄、麥克風、簡訊、通話紀錄
- 你的拍攝內容(照片本身存在你裝置,本 App 從不上傳)
- 任何分析、追蹤、廣告 ID(本 App 未整合 Google Analytics / Firebase Analytics / 任何第三方分析或廣告 SDK)

## 第三方服務

本 App 目前**未使用任何第三方服務或 SDK** 來蒐集或處理你的資料。

(若未來新增 Firebase Crashlytics 等服務以幫助修 bug,本政策會更新,並在 Play 商店該版本的「更新內容」中明確告知;新版上線前不會偷偷接入。)

## 兒童

本 App 不會主動收集任何使用者資料,因此對 13 歲以下兒童亦無收集行為。本 App 在 Play 商店的目標年齡為 13+。

## 你的權利

由於本 App 不收集你的任何資料、不傳到任何伺服器,所以:

- 沒有資料需要你「下載 / 匯出」
- 沒有資料需要你「刪除請求」(刪除手機 App 即清除所有本機資料)
- 沒有資料需要你「更正」

## 政策變動

若未來本 App 新增任何會上傳資料、接第三方 SDK、或改變資料處理方式的功能,本政策會更新,**更新日期會反映在頁首**,並在 Play 商店版本說明中告知。

## 聯絡

- GitHub Issues:<https://github.com/cjc305/magnifier/issues>
- Email:`<請改成你的公開聯絡 email>`

---

# Magnifier — Privacy Policy

**Effective date**: 2026-05-17  
**Last updated**: 2026-05-17

Magnifier (`io.github.cjc305.magnifier`, "the App") is an independent app developed by cjc305. This policy explains how the App handles data on your device.

## TL;DR

The App is **fully offline**:

- No data is uploaded to the cloud
- No personal information is collected
- No ads
- No third-party analytics or tracking SDKs
- No account required

## What we access

The App accesses two things on your device, **used locally only, never leaves your device**:

### 1. Camera (`CAMERA` permission)

- **Purpose**: Live magnifier preview, 1× to 10× zoom, torch, and photo capture
- **Data flow**: Camera frames are processed in memory for on-screen display only. A JPEG is saved to disk only when you tap the capture button
- **Storage location**: Saved to your device's system gallery (Android MediaStore `Pictures/Magnifier`). The App keeps no copy in its own private folder

### 2. Photo media
- Android 13+: `READ_MEDIA_IMAGES`
- Android 12 and below: `READ_EXTERNAL_STORAGE`

- **Purpose**: Display the magnifier photos you previously saved with the App, and let you delete them
- **Access scope**: The App only displays images you selected or that the App itself saved; it does not scan or upload any of your photos
- **Deletion**: Deletion is triggered only by your tap. On Android 11+, the system shows a confirmation dialog requiring your second consent

## What we do NOT collect

- Your name, email, phone number, address, or any personal identifiers
- Your location (the App requests no location permission)
- Your contacts, microphone, SMS, or call logs
- Your captured photos (they stay on your device; the App never uploads them)
- Any analytics, tracking, or ad ID (the App integrates no Google Analytics / Firebase Analytics / third-party analytics or ad SDKs)

## Third-party services

The App currently uses **no third-party services or SDKs** that collect or process your data.

(If Firebase Crashlytics or similar diagnostics are added later to help fix bugs, this policy will be updated, and the change will be announced in the corresponding Play Store release notes. No SDK will be added silently.)

## Children

The App does not actively collect any user data, so it does not collect data from children under 13 either. The App's target age on the Play Store is 13+.

## Your rights

Because the App collects no data and sends nothing to any server:

- There is no data to "download / export"
- There is no data to "request deletion of" (uninstalling the App removes all local data)
- There is no data to "correct"

## Policy changes

If the App ever adds features that upload data, integrate third-party SDKs, or change how data is handled, this policy will be updated, **the update date will be reflected at the top of this page**, and the change will be disclosed in the Play Store release notes.

## Contact

- GitHub Issues: <https://github.com/cjc305/magnifier/issues>
- Email: `<please replace with your public contact email>`
