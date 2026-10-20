# Yina

Anki dosyalarıyla çalışan, Android için kart tabanlı tekrar uygulaması.
AnkiDroid'in motorunu ve uyumluluğunu koruyup **içerik üretme, eşitleme ve çalışma deneyimini** yeniden tasarlamayı hedefliyor.

Tasarım tezi tek cümle: **sakin bir yüzey, tek bir görev, fiziksel bir nesne hissi.**
Ayrıntı: [`docs/DESIGN.md`](docs/DESIGN.md).

<p align="center">
  <img src="store/play-icon-512.png" width="128" alt="Yina simgesi" />
</p>

> Bu proje Anki, AnkiDroid veya AnkiWeb ile bağlantılı değildir. "Anki" adı yalnızca dosya formatı uyumluluğunu tanımlamak için kullanılır.

**Durum: Faz 0 — iskelet.** Arayüz, mimari sınırlar, CI/CD ve Play Store hattı hazır; Anki motoru (Rust çekirdek) henüz bağlı değil.

| | |
|---|---|
| Platform | Android 8.0+ (API 26), `targetSdk 36` |
| Dil / UI | Kotlin, Jetpack Compose, Material 3 |
| Lisans | AGPL-3.0-or-later |
| Paket adı | `io.github.yina` (yayından önce değiştirilebilir, sonra **değiştirilemez**) |
| Sürüm | 0.1.0 |

## Neden Yina?

AnkiDroid olgun ve doğru çalışıyor; eksik olduğu yer **ürün**:

- **Mobilde kart yazmak** (HTML, şablon, alan) hâlâ işkence → zengin metin editörü, tek dokunuşla fotoğraf/ses.
- **Eşitleme** kafa karıştırıcı ve tek yönlü sync veri silebiliyor → anlaşılır durum, kart bazında çakışma çözümü, sürüm geçmişi.
- **İstatistik var, karar yok** → günlük çalışma planı, birikmiş yük, sınav tarihine göre tempo.
- **Eklenti yok** → güvenli "tarif" tabanlı otomasyonlar ve isteğe bağlı AI taslak üretimi.

Motoru yeniden yazmıyoruz: Anki'nin Rust çekirdeğini gömüyoruz. Ayrıntı: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Hızlı başlangıç

Gereksinimler: JDK 17+ (CI'da 21), Android SDK 36.

```bash
git clone https://github.com/tutualican/yina.git
cd yina

# Debug APK
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk

# Test + lint
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Android Studio ile: projeyi aç, `app` yapılandırmasını çalıştır.

## GitHub'da yayınlama

1. Depoyu oluştur ve push et.
2. **Actions → CI** her push'ta APK üretir; `yina-debug-<sha>` artifact'ından indirilebilir.
3. Sürüm çıkarmak için etiket at:

```bash
git tag v0.1.0
git push origin v0.1.0
```

`Release` iş akışı imzalı APK + AAB üretir ve GitHub Release'e ekler. İmzalama sırları için: [`docs/PLAY_STORE.md`](docs/PLAY_STORE.md#imzalama).

## Play Store

Mağazaya çıkış için adım adım kontrol listesi: [`docs/PLAY_STORE.md`](docs/PLAY_STORE.md).

- Mağaza metinleri: [`fastlane/metadata/android/`](fastlane/metadata/android)
- Gizlilik politikası taslağı: [`docs/PRIVACY.md`](docs/PRIVACY.md)
- Data safety cevapları: [`docs/DATA_SAFETY.md`](docs/DATA_SAFETY.md)
- Play'e otomatik yükleme: `.github/workflows/play.yml` (servis hesabı gerekir)

## Yol haritası

[`docs/ROADMAP.md`](docs/ROADMAP.md)

- **Faz 0** — iskelet, tasarım sistemi, simge, CI, imzalama, mağaza hattı ✅
- **Faz 1** — Rust çekirdek, `.apkg` içe aktarma, tekrar ekranı, FSRS
- **Faz 2** — kart editörü, tarayıcı, yedek/geri yükleme
- **Faz 3** — kendi sync sunucusu, çakışma çözümü, sürüm geçmişi
- **Faz 4** — çalışma koçu, AI taslak üretimi

## Katkı

[`CONTRIBUTING.md`](CONTRIBUTING.md). Çeviriler `values-*/strings.xml` üzerinden yapılır.

## Lisans

AGPL-3.0-or-later. Ayrıntı: [`LICENSE`](LICENSE).

Bu lisans tesadüf değil: Anki'nin çekirdeği (`rslib`) AGPL-3.0. Onu bağlayan uygulama da AGPL olmak zorunda. Play Store'da AGPL yayınlamak serbesttir; kısıt yalnızca iOS/App Store tarafında.
