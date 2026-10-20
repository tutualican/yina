# Yina — Tasarım Sistemi

> Sakin bir yüzey, tek bir görev, fiziksel bir nesne hissi.

Yina'nın tasarımı "güzel ekran" hedeflemez; **dikkat dağıtmayan, kendinden emin bir araç** hedefler. Kullanıcı uygulamayı açtığında tek bir soruya cevap arar: *şimdi ne yapmalıyım?* Tasarım bu cevabı bir saniyede vermeli, gerisini saklamalıdır.

## 1. İlkeler

### 1.1 Tek odak
Her ekranda **tek bir birincil eylem** vardır. Ana ekranda bu "çalışmaya başla"dır. İçe aktarma, ayarlar, hakkında ikincildir ve görsel ağırlıkları düşüktür.

### 1.2 Karmaşıklığı gizle, yeteneği saklama
Anki'nin gücü seçeneklerindedir; bu seçenekler ilk açılışta görünmez. Varsayılanlar akıllıdır (FSRS açık, günlük limit 20, hedef tutma %90). İleri düzey her ayar bir "Gelişmiş" katmanının altındadır. Hiçbir özellik silinmez; yalnızca **sırası** değişir.

### 1.3 Fizik
Kullanıcı bir nesneye dokunduğunda nesne gerçek dünyadaki gibi davranır:

- dokunma → nesne **0,975'e küçülür**, 900 stiffness ile (neredeyse anında),
- bırakma → **tek bir yumuşak salınımla** yerine döner (damping 0,82),
- kaydırma → parmağı takip eder, eşiği geçince kendiliğinden tamamlanır, geçmezse geri yaylanır,
- hiçbir şey "ışınlanmaz"; her hareketin bir başlangıcı ve bitişi vardır.

### 1.4 Sessiz görsellik
Süs yok: gradient yalnızca odak kartında, çok düşük kontrastla, derinlik için. İkon çizimi tek ağırlık. Gölge yerine **ton farkı** (surface / surfaceVariant). Renk, dikkat çekmek için değil, **anlam** taşımak için kullanılır.

### 1.5 Güven
Kullanıcı verisini kaybetme korkusu taşımamalı: her yıkıcı işlem geri alınabilir, her sync öncesi yedek alınır, eşitleme durumu her zaman görünür. Arayüz "ne oldu?" sorusunu bırakmaz.

## 2. Renk

Simgeden türetildi: koyu slate zemin, kâğıt beyazı kartlar, tek sıcak vurgu.

| Token | Açık | Koyu | Kullanım |
|---|---|---|---|
| `background` | `#F4F2EC` | `#14171A` | Sayfa zemini |
| `surface` | `#FBFAF6` | `#191D21` | Kart, dialog |
| `surfaceVariant` | `#E7E3D9` | `#262B30` | İkincil kart, çip |
| `primary` | `#2E5B4C` | `#9FCDB8` | Birincil eylem, sayılar |
| `primaryContainer` | `#D7E8DF` | `#23443A` | Odak kartı |
| `onPrimaryContainer` | `#0B2B21` | `#C8EBDA` | Odak kartı metni |
| `tertiary` | `#B4763C` | `#E0A96D` | Yalnızca uyarı/seri gibi nadir vurgular |
| `onSurfaceVariant` | `#5C625E` | `#A8AFA9` | İkincil metin |

Kurallar:

- Ekranda **en fazla bir** doygun renk alanı (odak kartı).
- Kırmızı yalnızca hata ve yıkıcı eylem.
- Renk asla tek başına anlam taşımaz (renk körlüğü): sayı + etiket birlikte.

## 3. Tipografi

Sistem fontu (Roboto). Özel font, cila değil **kimlik** katmanıdır; Faz 3'te OFL lisanslı bir aile eklenebilir.

| Stil | Boyut / satır | Ağırlık | Kullanım |
|---|---|---|---|
| `displayLarge` | 56 / 60, -1,5 | SemiBold | Tekrar sayısı |
| `headlineMedium` | 26 / 32, -0,4 | SemiBold | "Yina" wordmark |
| `headlineSmall` | 22 / 28 | Medium | Boş durum başlığı |
| `titleMedium` | 16 / 22 | Medium | Deste adı |
| `bodyMedium` | 14 / 21 | Normal | Açıklama |
| `bodySmall` | 13 / 18 | Normal | Meta bilgi |
| `labelLarge` | 14 / 18 | Medium | Düğme |
| `labelMedium` | 12 / 16, +0,4 | Medium | Etiket, "BUGÜN" |

- Satır uzunluğu 60 karakteri geçmez.
- Tüm büyük harf yalnızca `labelMedium` + 1,6sp harf aralığı ile, tek kelime.
- Türkçe: `İ/ı` doğru; `uppercase()` çağrılarında `Locale.getDefault()` şart.

## 4. Boşluk ve şekil

4dp ızgara. Sayfa kenarı **20dp**, bölüm arası **14dp**, kart içi **18–26dp**.

| Yarıçap | Değer | Kullanım |
|---|---|---|
| `small` | 14dp | Çipler |
| `medium` | 20dp | Deste satırı |
| `large` | 28dp | Odak kartı |
| `extraLarge` | 34dp | Sheet |

Dokunma hedefi **en az 44dp** (tercihen 48dp). İkon düğmesi 44dp, görsel ikon 20dp.

## 5. Hareket

| Ad | Spec | Nerede |
|---|---|---|
| Basma | `spring(damping=0.62, stiffness=900)` → 0,975 | Her dokunulabilir yüzey |
| Yerleşme | `spring(0.82, 380)` | Bırakma, kaydırma dönüşü |
| Giriş | `spring(0.88, 260)` | Sheet, büyük yüzey |
| Hızlı | `tween(180ms, cubic-bezier(.2,0,0,1))` | Sayı/metin değişimi |
| Solma | `tween(240ms)` | Çapraz geçiş |

Kurallar:

- 400 ms'den uzun animasyon yok.
- Sonsuz/döngüsel animasyon yok (yükleme göstergesi hariç).
- Animasyon **girdiyi engellemez**; kullanıcı her an dokunabilir.
- Liste öğeleri `animateItem()` ile taşınır; ekran girişinde kademeli (40 ms) belirme kullanılır.
- Sistem "hareketi azalt" açıksa yalnızca opaklık geçişi uygulanır.
- Haptik yalnızca birincil eylemde ve kaydırma eşiğinde; her dokunuşta değil.

## 6. Bileşenler

### 6.1 Header
`Yina` wordmark (26sp, Bold, -0,6 tracking), altında bugünün tarihi (`21 Ekim, Çarşamba`). Sağda 44dp daire içinde `+` (içe aktar). Header kaydırmada kaybolur; ekranı kalıcı bir AppBar işgal etmez.

### 6.2 Odak kartı
- 28–30dp yarıçap, `primaryContainer → surface` çok düşük kontrastlı lineer gradient.
- Üstte `BUGÜN` etiketi.
- Ortada dev sayı + "tekrar bekliyor".
- Altta tek birincil pill: "Çalışmaya başla".
- Tekrar yoksa: "Bugünlük her şey tamam." + içe aktarma eylemi.
- Sayı değişimi `AnimatedContent` ile 180 ms'de, yerinden oynamadan.

### 6.3 Deste satırı
- 44dp monogram (deste adının ilk harfi, `primary` %12 zemin).
- Ad + "1.240 kart · 42 tekrar".
- Sağda tekrar sayısı veya `✓`.
- **Basılı tut / dokun:** detay (Faz 2).
- **Sağa kaydır:** "Çalış" ortaya çıkar; 96dp eşik, %70 lastik direnç; eşiği geçince çalışma başlar, geçmezse yayla geri döner.
- Satır yüksekliği 76dp; iki satır metin taşmaz, ellipsis.

### 6.4 Boş durum
Büyük illüstrasyon yok. Başlık + tek cümle + sakin bir "İçe aktar" pill'i. Boşluk, kullanıcıyı suçlamaz.

### 6.5 Dialog
Yalnızca "Hakkında" ve yıkıcı onaylar. Yarıçap 28dp, `surface`, geniş ekranda maksimum 480dp.

## 7. İçerik ve ton

- Cümle düzeni; başlıklarda bile.
- Ünlem yok, emir kipi yok ("Hemen başla!" değil, "Çalışmaya başla").
- Sayılar yalın: `42`, `1.240` (binlik ayırıcı locale'e göre).
- Hata mesajı **ne oldu + ne yapmalı** içerir: "Paket açılamadı. Dosya bozuk olabilir; Anki'den yeniden dışa aktar."
- Teknik terim (FSRS, cloze) yalnızca ileri düzey ekranlarda.

## 8. Erişilebilirlik

- Tüm ikon düğmelerinde `contentDescription`.
- Renk kontrastı: gövde metni ≥ 4,5:1; büyük metin ≥ 3:1.
- Yazı ölçeği %200'de düzen bozulmaz (sabit yükseklik yok).
- Kaydırma eyleminin **alternatifi** her zaman vardır (dokunma → detay → "Çalış").
- TalkBack sırası: header → odak kartı → bölüm → desteler.
- Hareket azaltma desteklenir.

## 9. Yap / Yapma

| Yap | Yapma |
|---|---|
| Tek birincil eylem | İki eşit ağırlıklı düğme |
| Ton farkıyla derinlik | Ağır gölge, blur, neon |
| 180–260 ms geçiş | 600 ms "sinematik" giriş |
| Numarayı büyüt | Rozet, konfeti, seri alevi |
| Sessiz boş durum | Suçlayıcı "Henüz bir şey yok!" |
| Kaydırmanın alternatifi | Yalnızca jestle erişilen işlev |

## 10. Kod haritası

| Dosya | İçerik |
|---|---|
| `ui/theme/Color.kt` | Palet |
| `ui/theme/Type.kt` | Tip ölçeği |
| `ui/theme/Shape.kt` | Yarıçaplar |
| `ui/theme/Motion.kt` | Spring/tween sabitleri |
| `ui/theme/Theme.kt` | `YinaTheme` |
| `ui/components/Pressable.kt` | Fiziksel basma |
| `ui/home/HomeScreen.kt` | Header, odak kartı, kaydırılabilir deste satırı |

## 11. Açık kararlar

- Özel font (OFL) — kimlik için, cila için değil.
- Haptik yoğunluğu — birincil eylem + eşik; cihaz desteği değişken.
- Koyu tema varsayılanı — simge koyu; kullanıcı tercihine bırakılmalı.
- Tablet/foldable iki panelli düzen — Faz 2.
