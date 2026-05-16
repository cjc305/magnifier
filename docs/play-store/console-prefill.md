---
title: Play Console 表單預填答案
created: 2026-05-17
note: 這份是當你進到 Play Console 各表單時,可以直接照貼的答案。所有答案都跟 App 實際行為對齊(完全離線、零資料收集、無廣告)。
---

# Play Console 表單預填答案

## 0. 上架前一次性設定(只做一次)

| 欄位 | 答案 |
|---|---|
| Developer name | cjc305(或你想公開的名字) |
| Developer type | **Personal**(已選) |
| Phone | 你的可接收驗證碼的手機號 |
| Email | 你 Google 帳號的 email(Play 內部聯絡用,不對公眾) |
| Public website | https://github.com/cjc305/magnifier |
| Public phone(對使用者公開) | **建議不填**(個人開發者) |
| Public email(對使用者公開) | **建議不填**(用 GitHub Issues 取代) — Play 允許 personal dev 不公開 email |

## 1. App content → Data safety

> 這是最容易被退件的表單。慢慢填,跟程式碼對齊。

### Data collection and security

| 問題 | 答案 |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |

> Play 對「collect」的定義:資料離開使用者裝置,進入你或第三方的伺服器。本 App 0 網路、0 上傳,所以是 No。

選 No 後,大多數細節分節會跳過,只剩:

| 問題 | 答案 |
|---|---|
| Is all of the user data collected by your app encrypted in transit? | **Not applicable / N/A**(沒收集) |
| Do you provide a way for users to request that their data be deleted? | **Not applicable / N/A**(沒收集,App 解除安裝即清除本機資料) |
| Has your app's data collection and security practices been independently verified against a global security standard? | **No**(個人開發者通常無此認證,不影響上架) |

### Data types(若 Play 仍要你逐項勾,全部勾 No)

- Location:No
- Personal info:No
- Financial info:No
- Health & fitness:No
- Messages:No
- Photos and videos:**No**(雖然 App 存照片到 MediaStore,但那是用戶裝置本機,不是傳到我們伺服器,不算 collect)
- Audio files:No
- Files and docs:No
- Calendar:No
- Contacts:No
- App activity:No
- Web browsing:No
- App info and performance:No
- Device or other IDs:No

## 2. App content → Government apps

| 問題 | 答案 |
|---|---|
| Is your app a government app? | **No** |

## 3. App content → Financial features

| 問題 | 答案 |
|---|---|
| Does your app provide any financial features? | **No** |

## 4. App content → Health apps

| 問題 | 答案 |
|---|---|
| Does your app provide health features? | **No** |

> 雖然能拿來讀藥袋,但 App 不是「健康追蹤 / 醫療建議」工具,Tools category 更精確。Health 類別審核較嚴,別自找麻煩。

## 5. App content → COVID-19 contact tracing and status apps

| 問題 | 答案 |
|---|---|
| Is your app a contact tracing or COVID-19 status app? | **No** |

## 6. App content → News apps

| 問題 | 答案 |
|---|---|
| Is your app a news app? | **No** |

## 7. App content → Ads

| 問題 | 答案 |
|---|---|
| Does your app contain ads? | **No** |

## 8. App content → Target audience and content

### Step 1: Target age groups

勾選的年齡組:

- [ ] Ages 5 and under
- [ ] Ages 6-8
- [ ] Ages 9-12
- [x] **Ages 13-15**
- [x] **Ages 16-17**
- [x] **Ages 18 and over**

> 不勾 12 以下 → 避開 COPPA / Designed for Families 嚴格規範。

### Step 2: Appeals to children

| 問題 | 答案 |
|---|---|
| Does the app design appeal to children under 13? | **No** |
| Does the app include child-safe features? | (跳過,因為前題答 No) |

### Step 3: Sensitive content

- Violence: **None**
- Sexual content: **None**
- Profanity: **None**
- Crude humor: **None**
- Drug, alcohol, tobacco use: **None**
- Gambling: **None**

### Step 4: Negative themes & interactive elements

- Users interact: **No**
- Shares user-generated content: **No**
- Shares location: **No**
- Allows digital purchases: **No**
- Includes ads:**No**

## 9. App content → Content rating(IARC 問卷)

問卷會自動跑出評級,放大鏡 App 的答案組合會得到:

| 區域 | 評級 |
|---|---|
| IARC Generic | **3+** |
| ESRB(美)| **Everyone** |
| PEGI(歐)| **3** |
| USK(德)| **0**(無年齡限制) |
| ACB(澳)| **G** |
| 中華民國(台)| **0+ 或 普遍級** |

問卷常見問題答案:
- Does the app contain violence? — No
- Sexual content? — No
- Profanity? — No
- Drugs/alcohol/tobacco? — No
- Gambling? — No
- User-generated content? — No
- Shares user location? — No
- Allows purchases? — No
- Personal info collected? — No
- Mature themes? — No

> 全 No → Everyone / 3+。

## 10. App content → News

(再次問,跟前面一致)— No。

## 11. Store presence → Main store listing

照貼 [listing.md](listing.md) 對應欄位:

| Play Console 欄位 | 來源 |
|---|---|
| App name | listing.md §1 |
| Short description | listing.md §2 |
| Full description | listing.md §3 |
| App icon(512×512) | listing.md §7 |
| Feature graphic(1024×500) | listing.md §6 |
| Phone screenshots | listing.md §5 |
| 7" tablet screenshots | listing.md §5(選填) |
| 10" tablet screenshots | listing.md §5(選填) |
| Promo video | 不填(選填) |

### App category

| 欄位 | 答案 |
|---|---|
| App or game? | **App** |
| Category | **Tools** |
| Tags(最多 5)| Accessibility, Productivity, Utilities |

### Store listing contact details

| 欄位 | 答案 |
|---|---|
| Email | 你 Play 開發者帳號 email(Play 顯示給使用者) |
| Phone | **建議不填** |
| Website | https://github.com/cjc305/magnifier |
| External marketing | **No**(不向 13 歲以下兒童行銷) |

## 12. App content → Privacy policy

| 欄位 | 答案 |
|---|---|
| Privacy policy URL | **`https://cjc305.github.io/magnifier/privacy.html`** |

(URL 已驗證 200 OK,可直接貼)

## 13. App access

| 問題 | 答案 |
|---|---|
| All functionality is available without restrictions | **Yes,所有功能對所有使用者開放,不需登入** |

## 14. Release → Testing → Internal testing(個人帳號**必走**這條才能進 Closed/Production)

### Track 設定

| 欄位 | 答案 |
|---|---|
| Track name | Internal testing |
| Release name | 1.0 (1) |
| Release notes(zh-TW)| listing.md §8 的 zh 版本 |
| Release notes(en)| listing.md §8 的 en 版本 |
| AAB 上傳 | `app/build/outputs/bundle/release/app-release.aab`(已 4.0MB jarsigner 通過) |

### Internal testers

| 欄位 | 答案 |
|---|---|
| Email list | 你自己 + 1-2 個朋友的 Google 帳號 email |
| 邀請連結會在 release 後產生 | 寄給 testers,他們從連結加入 |

## 15. Release → Testing → Closed testing(**個人帳號上 Production 必經 14 天測試**)

### Track 設定(跟 Internal 類似但更嚴)

| 欄位 | 答案 |
|---|---|
| Track name | Closed testing — alpha |
| 同步上傳同一份 AAB | 是 |
| 需要的 testers 數量 | **至少 20 人**(post-2023/11 個人帳號政策) |

### 找 20 個 testers 的策略

1. 朋友、家人(最容易找)— 5-10 人
2. Reddit r/androidapps / r/AndroidDev — post asking for testers,通常 5-10 個有興趣的人
3. Facebook 社團(無障礙、老花眼相關)— 鎖定 target user
4. Discord(Android dev 社群)
5. 自己的 LinkedIn / Twitter / Threads

### Closed testing 開始的那刻起算 14 天

Day 6 啟動 → Day 20 滿足 → 才能推 Production track。所以實際「Play live」最早是 Day 20-24。

## 16. Release → Production

**等 Closed testing 滿 14 天 + 至少 20 testers 安裝過、用過,才能解鎖。**

到時:
1. Production track → Create new release
2. 上傳同一份 AAB(或更新版,如果 internal/closed 期間有 fix)
3. Release notes(可沿用)
4. 提交審核 → 一般 3-7 天(個人新帳號可能久)

## 17. 風險點再次強調

1. **Data safety 表單**:不要被「Photos and videos」這欄誤導,雖然 App 處理相機+照片,但都在裝置本機,**勾 No**。勾錯被退件最常見。
2. **Privacy policy URL** 提交前再用無痕視窗點一次,確認 200 OK。
3. **Closed testing 14 天**:從你啟動的那秒算,不是平均日均使用。20 testers 必須是真的安裝、用過(Play 看 Vitals)。
4. **AAB 簽章**:你的 `magnifier-release.jks` 一旦遺失,**這個 App 永遠無法更新**。立刻備份兩處。
5. **個人帳號身分驗證**:Google 可能要你上傳身分證 / 信用卡帳單,提早準備。
