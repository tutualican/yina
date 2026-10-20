# Mimari

## İlke

> Anki'nin formatını ve zamanlayıcısını **yeniden yazma**. Motoru göm, üstüne daha iyi bir ürün koy.

AnkiDroid 2.17'den beri Anki'nin Rust çekirdeğini JNI ile kullanıyor. Aynı yolu izliyoruz; fark UI ve ürün katmanında.

```
┌──────────────────────────────────────────────────────────┐
│  UI (Compose)                                            │
│  ui/home, ui/review, ui/editor, ui/browse                │
├──────────────────────────────────────────────────────────┤
│  Application / ViewModel                                 │
│  durum, olaylar, tek yönlü veri akışı                    │
├──────────────────────────────────────────────────────────┤
│  core/engine  ← tek sınır                                │
│  CollectionEngine arayüzü                                │
├──────────────────────────────────────────────────────────┤
│  Rust: rslib (koleksiyon, şema, import/export, sync)     │
│        fsrs-rs (zamanlayıcı)                             │
│        anki_proto (protobuf komutları)                   │
├──────────────────────────────────────────────────────────┤
│  SQLite: collection.anki2 (şema 18) + media klasörü      │
└──────────────────────────────────────────────────────────┘
```

`core/engine` dışında hiçbir kod koleksiyon dosyasına dokunmaz. Bu, ileride motoru değiştirmeyi (veya testte sahte motor kullanmayı) tek dosyalık iş yapar.

## Neden Anki çekirdeği?

| Sorun | Kendi başına yazmanın maliyeti |
|---|---|
| `.apkg` / `.colpkg` (3 düzen: `anki2`, `anki21`, `anki21b`) | zstd + protobuf + zip + medya indeksi |
| Şema 11 → 18 yükseltmeleri | onlarca geçiş, veri kaybı riski |
| Protobuf ayar blobları (`deck_config.config`, `decks.kind`, `notetypes.config`) | tersine mühendislik bakımı |
| FSRS parametreleri, kardeş gömme, leech, filtreli deste | araştırma + yıllar |
| AnkiWeb sync protokolü v11 | `meta/start/applyGraves/applyChanges/chunk/applyChunk/sanityCheck2/finish` |
| Şablon semantiği (`{{FrontSide}}`, cloze, `dir=auto`, JS API) | sürekli kayan hedef |

## Faz 1: Rust çekirdeğini bağlama

Önerilen sıra:

1. **Alt modül / crate**
   - `ankitects/anki` deposunu `rust/anki` altına alt modül olarak ekle (veya `anki` crate'ini Cargo bağımlılığı yap).
   - Yalnızca `rslib` + `fsrs` + `anki_proto` gerekir; Qt/TS katmanı derlenmez.
2. **Köprü**
   - `uniffi` ile `CollectionHandle` arayüzü üret: `open`, `importPackage`, `deckTree`, `nextCard`, `answerCard`, `undo`, `close`.
   - Alternatif: Anki'nin mevcut `Backend` protobuf API'sini JNI üzerinden çağır (AnkiDroid'in yaptığı).
3. **Android derlemesi**
   - `cargo-ndk` ile `arm64-v8a`, `armeabi-v7a`, `x86_64` hedefleri.
   - Çıktı: `app/src/main/jniLibs/<abi>/libyina_core.so`.
   - `uniffi-bindgen` ile Kotlin sarmalayıcı üret.
4. **Kotlin tarafı**
   - `RustCollectionEngine : CollectionEngine`.
   - `AppContainer.engine()` içinde `StubCollectionEngine` yerine onu döndür. Başka değişiklik yok.

### Sözleşme (mevcut)

```kotlin
interface CollectionEngine {
    suspend fun open(): CollectionSnapshot
    suspend fun importPackage(uri: Uri, displayName: String?): ImportResult
    suspend fun close()
}
```

Import, SAF'tan gelen `content://` URI'yi geçici dosyaya kopyalar; Rust tarafı dosya yolu ister. Kopyalama `cacheDir`'e, boyut sınırı kontrolüyle yapılmalı (zip bombası).

## Veri yerleşimi

```
filesDir/collection/
├── collection.anki2      # şema 18
├── collection.anki2-wal
├── collection.anki2-shm
└── media/                # dosyalar + media.db
```

AnkiDroid'in dizinini doğrudan açmak **hedef değil**: iki uygulama aynı dosyaya yazarsa bozulur. Geçiş için tek seferlik içe aktarma sunulacak.

## Render katmanı

- Tek bir `WebView` havuzu (2 örnek: mevcut + sonraki kart), kart geçişinde yeniden kullanılır.
- Şablonlar `loadDataWithBaseURL` ile yüklenir; `baseUrl` medya klasörünü gösterir.
- JS: varsayılan kapalı, kart başına beyaz listede. `addJavascriptInterface` **kullanılmaz**.
- Ağ erişimi `shouldInterceptRequest` ile engellenir (kart içeriği dışarı sızdırmasın).
- Cloze, `{{FrontSide}}`, MathJax, Image Occlusion, RTL test korpusunda zorunlu.

## Eşitleme (Faz 3)

İki katman:

1. **Kendi sunucumuz** — Anki sync protokolü v11 uyumlu. Referans uygulama: `anki-sync-server` (AGPL). Kimlik: e-posta + parola yerine cihaz anahtarı; uçtan uca şifreleme opsiyonel.
2. **Dosya tabanlı köprü** — WebDAV / Syncthing ile `colpkg` yedeği. Sunucu maliyeti sıfır, çakışma yönetimi kullanıcıda.

AnkiWeb'e bağlanmak **ürün stratejisi değil**: resmî API yok, erişim tek taraflı kapatılabilir. İsteğe bağlı "deneysel" olarak sunulabilir, varsayılan olamaz.

Kritik kural: **her sync öncesi yerel yedek**. Tek yönlü sync, kullanıcı verisi kaybının bir numaralı sebebi.

## Güvenlik

AnkiDroid 2.25, kart kaynaklı üç CVE yamaladı (GHSL-2026-083/084/085). Bizim için bağlayıcı gereksinimler:

- Medya yüklemede yol geçişi (`../`, mutlak yol, Windows sürücü harfi) reddedilir.
- Import: zip içindeki dosya sayısı, açılmış boyut ve sıkıştırma oranı sınırlanır.
- Kart içeriği uygulama başlatamaz, dosya açamaz, ağa çıkamaz.
- İmzalama anahtarı repoda tutulmaz (`keystore.properties` gitignore'da).

## Performans bütçesi

| Ölçüm | Hedef |
|---|---|
| 100k kartlık koleksiyon açılışı | < 1,5 sn |
| Kart cevabı → sonraki kart | < 50 ms |
| 10k revlog delta sync | < 5 sn |
| APK boyutu (arm64, tek ABI) | < 25 MB |
| Soğuk açılış | < 800 ms |

## Test stratejisi

1. **Diferansiyel test**: aynı cevap dizisi AnkiDroid ve Yina'da → `due/ivl/factor/queue` birebir.
2. **Altın korpus**: cloze, IO, RTL, furigana, MathJax, JS, 100k kartlık desteler.
3. **Round-trip**: import → export → Anki Desktop'ta aç → fark yok (zaman damgası hariç).
4. **Migrasyon**: AnkiDroid veri dizininden tek seferlik aktarım, kayıp yok.
5. **Fuzz**: bozuk zip, dev medya adları, eksik `meta`.

## Kayıtlar

- Motor: `ankitects/anki` (`rslib`), AGPL-3.0.
- Format: şema 18, `.anki21b` zstd + protobuf.
- Sync: protokol v11, `anki-sync` başlığı, zstd gövde.
