# Yina

Anki dosyalarıyla çalışan bir Android bilgi kartı uygulaması. Hedef, Anki'nin
dosya biçimini ve zamanlayıcısını korurken telefonda daha sade bir çalışma
deneyimi sunmak.

> Yina; Anki, AnkiDroid veya AnkiWeb ile bağlantılı değildir. "Anki" adı yalnızca
> dosya biçimi uyumluluğunu anlatmak için kullanılıyor.

## Durum

Proje **Faz 0**'da: uygulama iskeleti, tasarım sistemi, simge, CI ve mağaza
hattı hazır. Anki motoru (Rust çekirdeği) henüz bağlı değil.

Bu nedenle şu an:

- uygulama açılır ve ana ekranı gösterir,
- `.apkg` / `.colpkg` seçilebilir, ancak dosya henüz okunmaz,
- gerçek deste, kart veya tekrar verisi görünmez.

Faz 1'in konusu tam olarak bu: Anki'nin `rslib` çekirdeğini Android'e derleyip
UniFFI üzerinden Kotlin'e bağlamak.

## Teknik özet

| | |
|---|---|
| Platform | Android 8.0+ (API 26), `targetSdk 36` |
| Dil / arayüz | Kotlin, Jetpack Compose, Material 3 |
| Paket adı | `io.github.yina` |
| Sürüm | 0.1.0 |
| Lisans | AGPL-3.0-or-later |

Uygulama yerel öncelikli çalışır: hesap, sunucu, analitik veya reklam yok.
Mevcut sürümde `INTERNET` izni bile istemez.

## Nasıl çalışıyor?

Arayüz ile motor birbirinden ayrı tutuluyor:

```text
Compose ekranları
      │
      ▼
HomeViewModel
      │
      ▼
CollectionEngine        ← arayüzün bildiği tek motor sınırı
      │
      ├── StubCollectionEngine   (Faz 0, bugün)
      └── AnkiCollectionEngine   (Faz 1, rslib + UniFFI)
```

`CollectionEngine`, arayüzün ihtiyaç duyduğu işlemleri (deste listesi, tekrar
oturumu, kart cevaplama, içe aktarma) tanımlar. Faz 0'da bunun boş bir
uygulaması kullanılıyor; Faz 1'de aynı sınırın arkasına Anki çekirdeği
yerleştirilecek. Böylece ekranların motor değiştiğinde yeniden yazılması
gerekmiyor.

Ayrıntılar: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Tasarım

Amaç süslü değil, sakin bir uygulama: her ekranda tek bir birincil eylem,
karmaşıklığı arka planda tutan varsayılanlar ve dokunuşa fiziksel tepki veren
yüzeyler. Renkler uygulama simgesinden türetildi; hareket yay (spring)
eğrileriyle tanımlandı.

Tasarım ilkeleri, renk/tipografi ölçeği ve hareket kuralları:
[`docs/DESIGN.md`](docs/DESIGN.md).

## Derleme

Gereksinimler: JDK 17 veya üzeri (CI'da 21) ve Android SDK 36.

```bash
git clone https://github.com/tutualican/yina.git
cd yina

# Hata ayıklama APK'sı
./gradlew :app:assembleDebug
# çıktı: app/build/outputs/apk/debug/app-debug.apk

# Birim testleri ve lint
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Windows'ta `./gradlew` yerine `gradlew.bat` kullanın. Android Studio ile
açmak için projeyi içe aktarıp `app` yapılandırmasını çalıştırmak yeterli.

## Proje yapısı

```text
app/src/main/java/io/github/yina/
  MainActivity.kt
  core/
    engine/        CollectionEngine ve Faz 0 uygulaması
    model/         Deck gibi alan modelleri
  di/              Elle kurulan bağımlılıklar
  ui/
    components/    Yeniden kullanılan küçük bileşenler
    home/          Ana ekran ve ViewModel
    theme/         Renk, tipografi, şekil, hareket
app/src/main/res/  Simgeler, dizeler (tr/en), tema
docs/              Mimari, tasarım, Play Store, gizlilik
fastlane/          Mağaza metinleri
```

## Sürüm ve yayın

Her push'ta CI, hata ayıklama APK'sı üretir. Etiket atıldığında `Release`
iş akışı imzalı APK ve AAB oluşturup GitHub Release'e ekler:

```bash
git tag v0.1.0
git push origin v0.1.0
```

İmzalama anahtarları ve Play Console adımları:
[`docs/PLAY_STORE.md`](docs/PLAY_STORE.md).

## Yol haritası

- **Faz 0** — iskelet, tasarım sistemi, simge, CI, imzalama, mağaza hattı ✅
- **Faz 1** — Anki Rust çekirdeği, `.apkg` içe aktarma, tekrar ekranı, FSRS
- **Faz 2** — kart editörü, kart tarayıcı, yedekleme/geri yükleme
- **Faz 3** — kendi eşitleme sunucusu, çakışma çözümü, sürüm geçmişi
- **Faz 4** — çalışma planı, isteğe bağlı yapay zekâ taslakları

Ayrıntılı liste: [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Katkı

Katkı kuralları: [`CONTRIBUTING.md`](CONTRIBUTING.md). Çeviriler
`app/src/main/res/values-*/strings.xml` dosyalarından yapılır.

## Lisans

AGPL-3.0-or-later. Tam metin: [`LICENSE`](LICENSE).

Bu lisans bilinçli bir seçim: Anki'nin çekirdeği (`rslib`) AGPL-3.0 altında ve
onu bağlayan uygulamanın da AGPL olması gerekiyor. Play Store'da AGPL yayınlamak
mümkün; kısıt esas olarak iOS/App Store tarafında.
