# Katkı

Teşekkürler. Küçük ve odaklı değişiklikler en hızlı birleşenlerdir.

## Geliştirme ortamı

- JDK 17+ (CI: 21)
- Android SDK 36 + build-tools 36.0.0
- Android Studio (Ladybug veya üzeri) önerilir

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

## Kurallar

- **Dil:** kod ve commit mesajları İngilizce, kullanıcıya görünen metinler `strings.xml` üzerinden.
- **Biçim:** Kotlin resmî stil (`kotlin.code.style=official`), 4 boşluk.
- **Mimari sınır:** UI, `core.engine` dışına taşmaz. Koleksiyon dosyasına doğrudan erişim yok.
- **Bağımlılık:** yeni kütüphane eklerken lisans (AGPL uyumu) ve boyut etkisini PR'da belirt.
- **Commit:** `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.

## PR kontrol listesi

- [ ] CI yeşil
- [ ] Davranış değişikliği için test
- [ ] UI değişikliği için ekran görüntüsü
- [ ] `.apkg` / şablon / zamanlayıcı etkisi varsa uyumluluk notu

## Çeviri

`app/src/main/res/values-<dil>/strings.xml` dosyasını kopyala, `app_name` dışındaki tüm anahtarları çevir. Eksik anahtar varsa İngilizceye düşer.

## Lisans

Katkılar AGPL-3.0-or-later altında kabul edilir. Yeni dosyalara şu başlığı ekle:

```
// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Yina contributors
```

## Davranış kuralları

[Contributor Covenant v2.1](https://www.contributor-covenant.org/version/2/1/code_of_conduct/).
