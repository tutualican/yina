# Play Store'a çıkış

Bu dosya, Play Console'u aldığın gün sırayla yapılacaklar listesidir. Şu an para harcamadan yapılabilecekler en altta.

## 0. Bilinmesi gereken 4 şey

1. **Kayıt ücreti 25 USD, tek seferlik.** Kart bilgisi + kimlik doğrulama gerekir. Şirket hesabı açacaksan D-U-N-S numarası istenir.
2. **Paket adı (`applicationId`) yayından sonra değiştirilemez.** Şu an `io.github.yina`. Kendi alan adını/GitHub kullanıcı adını kullanmak istiyorsan **ilk yüklemeden önce** değiştir:
   ```bash
   # örnek: dev.alican.yina
   grep -rl "io.github.yina" app/ | xargs sed -i 's/io\.github\.yina/dev.alican.yina/g'
   # klasörü de taşı:
   # app/src/main/java/io/github/yina -> app/src/main/java/dev/alican/yina
   ```
   Sonra `app/build.gradle.kts` içindeki `namespace` ve `applicationId` değerlerini kontrol et.
3. **Yeni kişisel hesaplarda kapalı test şartı var:** üretime geçmeden önce en az **12 tester** ile **14 gün kesintisiz** kapalı test. (Google bu kuralı zaman zaman değiştiriyor; Play Console'daki güncel metne bak.)
4. **Hedef API:** 31 Ağustos 2026'dan itibaren yeni uygulamalar `targetSdk 36` olmalı. Bu proje 36'ya ayarlı.

## 1. Teknik gereksinimler — hazır ✅

| Gereksinim | Durum |
|---|---|
| AAB (`bundleRelease`) | ✅ `release.yml` üretiyor |
| `targetSdk 36`, `minSdk 26` | ✅ |
| 64-bit ABI desteği | ✅ (Kotlin/Compose; Rust eklendiğinde `arm64-v8a`) |
| `debuggable=false`, R8 küçültme | ✅ release derlemesinde |
| Play App Signing | Console'da ilk yüklemede açılır |
| Sürüm kodu artışı | ✅ `VERSION_CODE` = GitHub run number |
| İnternet izni | Yok (Faz 0-2 yerel). Sync eklenince Data safety güncellenmeli |

## 2. İmzalama

### 2.1 Anahtar üret (bir kez, asla kaybetme)

Windows'ta:

```powershell
cd C:\Users\Alican\yina
.\scripts\generate-keystore.ps1
```

Script `release.jks` üretir, `keystore.properties` yazar (ikisi de `.gitignore`'da) ve CI için base64 çıktısını ekrana basar.

Elle yapmak istersen:

```bash
keytool -genkeypair -v -keystore release.jks -alias yina \
  -keyalg RSA -keysize 4096 -validity 10000
```

> `release.jks` ve parolaları **en az iki yerde** yedekle (parola yöneticisi + fiziksel/offline kopya). Kaybedersen aynı paket adıyla güncelleme yayınlayamazsın; Play App Signing'de yalnızca *upload key* yenilenebilir.

### 2.2 GitHub Secrets

`Settings → Secrets and variables → Actions`:

| Secret | İçerik |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 release.jks` (PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks"))`) |
| `KEYSTORE_PASSWORD` | Keystore parolası |
| `KEY_ALIAS` | `yina` |
| `KEY_PASSWORD` | Anahtar parolası |
| `PLAY_SERVICE_ACCOUNT_JSON` | (Play Console açıldıktan sonra) servis hesabı JSON'u |

### 2.3 Sürüm çıkar

```bash
git tag v0.1.0
git push origin v0.1.0
```

`Release` iş akışı: imzalı APK + AAB → GitHub Release. AAB'yi Play Console'a elle yükleyebilirsin.

## 3. Play Console kurulumu

1. **Uygulama oluştur**: ad `Yina`, varsayılan dil Türkçe, uygulama mı oyun mu → Uygulama, ücretsiz.
2. **Play App Signing**: ilk AAB yüklemesinde "Google tarafından yönetilen anahtar" seçeneğini kabul et.
3. **Mağaza kaydı** (aşağıdaki metinler `fastlane/` içinde hazır):
   - Uygulama adı: **en fazla 30 karakter**
   - Kısa açıklama: **en fazla 80**
   - Tam açıklama: **en fazla 4000**
   - Uygulama ikonu: 512×512 PNG (32-bit, alfa yok)
   - Öne çıkan görsel: 1024×500 PNG/JPG
   - Telefon ekran görüntüsü: **en az 2**, en fazla 8 (min 320 px, max 3840 px)
   - 7" ve 10" tablet görselleri opsiyonel ama önerilir
4. **Uygulama içeriği**:
   - Gizlilik politikası URL'si → `docs/PRIVACY.md`yi GitHub Pages ile yayınla
   - Reklam: **Hayır**
   - Uygulama erişimi: tüm işlevler girişsiz kullanılabiliyor → "Tüm işlevler kısıtlama olmadan kullanılabilir"
   - İçerik derecelendirmesi: IARC anketi (şiddet/cinsellik/uyuşturucu yok → genellikle 3+)
   - Hedef kitle: 13+ (çocuklara yönelik değil)
   - Haber uygulaması: Hayır
   - Veri güvenliği: [`DATA_SAFETY.md`](DATA_SAFETY.md)
5. **Test kanalları**: Internal testing → kapalı test (12 tester / 14 gün) → üretim.
6. **Kademeli yayın**: %10 → %50 → %100. İlk sürümde çökme oranını izle.

## 4. Gizlilik politikasını yayınlama (ücretsiz)

1. `docs/PRIVACY.md` içindeki `[KULLANICI ADI]` ve `[E-POSTA]` alanlarını doldur.
2. Depo → **Settings → Pages** → Source: `Deploy from a branch` → Branch: `main`, folder: `/docs`.
3. URL: `https://tutualican.github.io/yina/PRIVACY` (Jekyll `.md` dosyalarını HTML'e çevirir).
4. Bu URL'yi Play Console'a gir.

## 5. Play'e otomatik yükleme (opsiyonel)

1. Google Cloud'da proje aç → **Google Play Android Developer API**'yi etkinleştir.
2. Servis hesabı oluştur → JSON anahtar indir.
3. Play Console → **Users and permissions** → servis hesabını davet et → "Release to testing tracks" + "Manage store presence".
4. JSON'u `PLAY_SERVICE_ACCOUNT_JSON` secret'ına ekle.
5. **Actions → Play Store (internal track) → Run workflow**.

## 6. Play Console yokken ne yapılır? (şimdi)

| Yol | Ücret | Not |
|---|---|---|
| **GitHub Releases** | 0 | `v*` etiketiyle imzalı APK. Kullanıcı "bilinmeyen kaynak" iznini açar. |
| **F-Droid** | 0 | AGPL uygun, Google Play Services yok. `fdroiddata` deposuna merge request gerekir; metadata `fastlane/` benzeri ayrı formatta. |
| **Obtainium / IzzyOnDroid** | 0 | GitHub Releases'i otomatik güncelleme kaynağı olarak kullanır. |
| **Amazon Appstore** | 0 | Daha küçük erişim, ayrı inceleme. |

F-Droid için gerekenler: uygulama tamamen FLOSS, `google-services`/Firebase yok, bağımlılıklar serbest lisanslı, kaynak kod repoda. Bu iskelet bu şartları sağlıyor.

## 7. Yayın öncesi son kontrol

- [ ] `applicationId` son hâlinde
- [ ] `versionName` / `versionCode` arttı
- [ ] Release AAB imzalı (`jarsigner -verify` veya Play Console kabul etti)
- [ ] `./gradlew :app:lintRelease` temiz
- [ ] Gizlilik politikası URL'si canlı
- [ ] Data safety formu gerçek davranışla uyumlu (şu an: veri toplanmıyor)
- [ ] Ekran görüntüleri güncel sürümden
- [ ] Uygulama açıklamasında "Anki" markası "uyumlu" bağlamında, resmî bağlantı iması yok
- [ ] AGPL kaynak bağlantısı uygulama içinde (Ayarlar → Hakkında) — Faz 1'de eklenecek
