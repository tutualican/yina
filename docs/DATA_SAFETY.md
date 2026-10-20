# Play Console — Data safety formu

Mevcut sürüm (Faz 0–2, tamamen yerel) için doğru cevaplar:

| Soru | Cevap |
|---|---|
| Uygulama kullanıcı verisi topluyor veya paylaşıyor mu? | **Hayır** |
| Veriler aktarım sırasında şifreleniyor mu? | Uygulanamaz (veri toplanmıyor) |
| Kullanıcı veri silmeyi talep edebilir mi? | Uygulanamaz |
| Bağımsız güvenlik incelemesi | Hayır |
| Reklam veya pazarlama amacıyla veri kullanımı | Hayır |

## Değişiklik tetikleyicileri

Aşağıdakilerden biri eklenirse form **aynı sürümde** güncellenmek zorunda:

| Özellik | Etki |
|---|---|
| AnkiWeb / kendi sync sunucusu | "Uygulama içi mesajlar", "Diğer kullanıcı içeriği" toplanıyor; şifreleme: evet; silme: evet |
| Çökme raporlama (Crashlytics/Sentry) | "Çökme günlükleri", "Cihaz kimlikleri" toplanıyor; amaç: uygulama işlevselliği |
| Analitik | "Uygulama etkileşimleri"; amaç: analitik |
| AI kart üretimi | "Diğer kullanıcı içeriği" üçüncü tarafa gönderiliyor; sağlayıcı adı ve amaç beyan edilmeli |
| Reklam | "Reklam" beyanı + reklam kimliği |

## Notlar

- `INTERNET` izni tek başına veri toplama sayılmaz; önemli olan verinin cihaz dışına çıkıp çıkmadığı.
- Android otomatik yedekleme (`allowBackup=true`) Google hesabına yedekler; Play formunda bu genelde "toplama" sayılmaz, ancak gizlilik politikasında belirtilmelidir (belirtildi).
- Form, uygulamanın gerçek davranışıyla uyuşmazsa politika ihlali sayılır ve uygulama kaldırılabilir.
