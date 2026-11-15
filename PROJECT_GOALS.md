# 🎯 Proje Hedefleri ve Hikayesi (Project Goals)

Bu dosya, projenin neden var olduğunu, hangi problemi çözdüğünü ve ana hedeflerini açıklar. Projeye dahil olan her geliştiricinin ve Yapay Zeka (AI) asistanının, geliştirme yaparken bu hikayeden ve ana hedeflerden sapmaması kritik önem taşır.

## 📖 Hikaye (Story)

Fitness dünyasında çoğu kişi “ne yapacağını bilmediği” için ya yanlış programlarla zaman kaybeder ya da motivasyonunu çabuk kaybeder. Genel antrenman / diyet şablonları kişisel hedeflere, ekipmana, kısıtlamalara ve günlük hayata uymadığı için takip edilemez hale gelir.

**FitAI**, kullanıcının kişisel verilerini (boy, kilo, yaş, hedef, ekipman erişimi, diyet kısıtlamaları) alarak AI destekli kişiselleştirilmiş antrenman programı ve diyet planı üreten; bu planı günlük hayatta takip etmeyi kolaylaştıran bir fitness uygulamasıdır.

Kullanıcı:
- Onboarding ile profilini oluşturur
- AI ile workout + diet planı alır
- Antrenman seanslarını ve öğünlerini kaydeder
- Fotoğraflı meal check-in ile beslenmesini analiz ettirir
- İlerlemeyi (progress) ve akıllı bildirimleri takip eder
- İsterse 7 günlük ücretsiz deneme ile uygulamayı dener (hesap ve cihaz başına bir kez); auth / paywall ile devam eder

Teknik olarak FitAI, Kotlin Multiplatform + Compose Multiplatform ile **Android ve iOS**’ta aynı ürün deneyimini hedefler. Backend tarafında Firebase (Auth, Firestore, Cloud Functions + Gemini) kullanılır; yapılandırma yoksa local mock / offline yol ile geliştirme sürdürülebilir.

## 🧩 Ürün Kapsamı (Product Scope)

| Alan | Ne yapar |
|---|---|
| **Onboarding / Welcome / Splash** | İlk açılış, profil toplama, yönlendirme |
| **Auth / Paywall / Trial** | Giriş-kayıt, abonelik duvarı, hesap ve cihaz başına bir kez 7 günlük deneme |
| **Home** | Bugünkü antrenman özeti, trial durumu, hızlı aksiyonlar |
| **Workout** | Plan listesi, egzersiz detayı, aktif seans takibi |
| **Nutrition** | Diyet planı, öğün kaydı, meal photo analizi |
| **Progress** | Görsel / ölçüm ilerleme takibi |
| **Profile** | Kullanıcı profili ve ayarlar |

Modüler yapı: `core/{domain,data,database,design-system,presentation}` + `feature/{auth,onboarding,home,workout,nutrition,profile}/{domain,data,presentation}`.

## 🎯 Ana Hedefler (Core Objectives)

1. **Kişiselleştirilmiş AI planlar:** Kullanıcı profiline göre anlamlı workout ve diet çıktıları üretmek; genel şablon değil, takip edilebilir plan sunmak.
2. **Hızlı günlük kullanım:** Antrenman başlatma, set/rep kaydı, öğün ekleme gibi sık işlerin en az adımla tamamlanması.
3. **Sezgisel UX:** Ön eğitim gerektirmeden anlaşılır akış; onboarding → plan → takip döngüsü net olsun.
4. **Cross-Platform (KMP):** Tek kod tabanıyla Android ve iOS’ta native hissettiren Compose Multiplatform deneyimi.
5. **Temiz Mimari:** Domain / Data / Presentation sınırları korunacak; UseCase + Repository + MVI ViewModel düzeni bozulmayacak.
6. **Premium UI:** Design system (`FitAI` bileşenleri / tokenlar) ve Material 3 ile tutarlı, modern arayüz; Stitch tasarımlarına sadık kalınacak.
7. **%100 Yerelleştirme:** UI metinleri hardcoded olmayacak; `composeResources` / `LocalStrings` üzerinden yönetilecek.
8. **Offline-first + AI güvenliği:** Yerel SQLDelight verisi öncelikli; AI çağrıları Firebase Functions üzerinden (API key cihazda tutulmaz); Firebase yoksa mock ile geliştirme devam eder.

---
> **AI İçin Not:** Yeni bir özellik kodlarken her zaman kendine şu soruyu sor: *"Bu ekleme kullanıcının kişiselleştirilmiş fitness takip döngüsünü (plan → uygula → kaydet → ilerle) güçlendiriyor mu; mimari ve lokalizasyon kurallarına uyuyor mu?"* Cevap hayır ise yaklaşımı yeniden değerlendir.
