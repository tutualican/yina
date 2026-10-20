# Yol haritası

## Faz 0 — İskelet ✅
- [x] Kotlin + Compose projesi, Material 3, açık/koyu tema
- [x] `CollectionEngine` sınırı + sahte motor
- [x] `Application`, ViewModel, tek yönlü durum akışı
- [x] Dosya seçici ile `.apkg` seçimi (motor bağlı değil)
- [x] CI: derleme + birim test + lint + APK artifact
- [x] Release: imzalı APK/AAB + GitHub Release
- [x] Play Store dokümantasyonu ve fastlane metinleri
- [x] AGPL-3.0 lisansı

## Faz 1 — Gerçek motor (3–4 ay)
- [ ] `rslib` + `fsrs` crate'lerinin Android'e derlenmesi (`cargo-ndk`)
- [ ] UniFFI köprüsü: `open/import/next/answer/undo/close`
- [ ] `.apkg` / `.colpkg` içe aktarma (üç düzen)
- [ ] Şema 11 → 18 yükseltme testleri
- [ ] Yina ekranı: ön/arka, 4 cevap düğmesi, FSRS aralıkları
- [ ] Zamanlayıcı eşdeğerlik testi (AnkiDroid ile diferansiyel)
- [ ] Çökme raporlama yok; yerel hata günlüğü
- [ ] İlk kapalı test sürümü

## Faz 2 — Ürün farkı (3 ay)
- [ ] Mobil kart editörü (zengin metin, fotoğraf, ses)
- [ ] Not türü / şablon seçimi, cloze
- [ ] Kart tarayıcı + arama sözdizimi
- [ ] Toplu işlemler (taşı, etiketle, askıya al)
- [ ] Yedek: otomatik + sürüm geçmişi + geri yükleme
- [ ] İstatistik: tutma oranı, günlük yük, ısı haritası
- [ ] Erişilebilirlik: TalkBack, yazı ölçeği, kontrast

## Faz 3 — Eşitleme (3 ay)
- [ ] Kendi sync sunucusu (protokol v11)
- [ ] Cihaz anahtarı ile kimlik, uçtan uca şifreleme (opsiyonel)
- [ ] Kart bazında çakışma çözümü
- [ ] Sync öncesi otomatik yedek
- [ ] WebDAV / Syncthing ile dosya tabanlı yedek
- [ ] (Deneysel) AnkiWeb köprüsü — varsayılan kapalı

## Faz 4 — Koç ve AI (3 ay)
- [ ] Günlük plan: "bugün 42 kart, ~18 dk"
- [ ] Birikmiş yük ve tempo uyarıları
- [ ] Sınav tarihine göre planlama
- [ ] Leech triage: zor kartları sadeleştirme önerisi
- [ ] PDF/foto → taslak kart (kullanıcı onayı zorunlu)
- [ ] Yazılı cevabın anlamsal değerlendirmesi
- [ ] AI özellikleri varsayılan kapalı, açık rıza ile

## Yapılmayacaklar
- Eklenti mağazası (Android platformu izin vermiyor)
- iOS (AGPL + App Store uyumsuzluğu) — ayrı karar
- Sosyal ağ / liderlik tablosu
- Hazır deste mağazası (telif + moderasyon)
