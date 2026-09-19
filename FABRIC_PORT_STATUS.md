# Fabric sürüm geçişi durumu

Son ölçüm: 20 Eylül 2026. Bu dal 26.3 için **devam eden bir porttur**; çalışır mod veya yayımlanabilir JAR üretmez. Forge kapsam dışıdır.

| Minecraft | Durum |
| --- | --- |
| 1.21.1 | `compileJava` ve JUnit 5 testleri geçti (3 test, 0 hata). `runClient` ile Creative Superflat dünya açıldı; ana ses `OFF` yapıldı. Dikey portalın hedefindeki altın blok işareti portaldan görüntülendi. Dünya kaydedilip yeniden açıldığında portal ve işaret duruyordu. Oyuncu, pistonun fiziksel itmesiyle portaldan geçerek yaklaşık `(40.5, -60, 9.2)` konumundan `(100.0, -60, 30.31)` hedefine ulaştı. `runClient` başarılı çıktı. Günlükte üç `ImmPtlChunkTickets` `Chunk loading failure` kaydı var; ayrıca araştırılmalı. |
| 1.21.2 | `-Pminecraft_version=1.21.2 -Pfabric_version=0.106.1+1.21.2` ile, diğer 1.21.1 bağımlılıkları korunarak derleme denendi. Derleyici ilk 100 hatada durdu. |
| 1.21.3–1.21.11 | Henüz derleme veya oyun testi yapılmadı. |
| 26.1, 26.1.1, 26.1.2, 26.2 | Fabric API sürümleri doğrulandı; henüz derleme veya oyun testi yapılmadı. |
| 26.3 | JDK 26 üzerinde Java 25 hedefiyle Gradle 9.5.1, Loom 1.17, Loader 0.19.5 ve Fabric API 0.161.0 kullanıldı. `help` ve `validateAccessWidener` geçti. `compileJava` en az 1.000 hatayla durdu. Oyun testi yapılamadı. |

26.3 için yapılandırma ve sınıf erişim dosyası geçirildi. Derleme önündeki ana işler:

1. DimLib'in yayımlanmış son sürümü 1.21.1 içindir. `DimensionAPI` ve `DimensionTemplate` kullanan kod 26.3'e taşınmalı veya eşdeğer uygulama yazılmalı.
2. Minecraft 26.x içindeki çizim, shader, chunk yönetimi, profil oluşturma ve kayıt API'leri için portal çekirdeği ve mixin hedefleri uyarlanmalı.
3. Sodium 0.9.2 ve Iris 1.11.6 için uyumluluk katmanı yeniden düzenlenmeli. Şu anda bu modların çalışma zamanı entegrasyonu kapalıdır.
4. Java derlemesi geçtikten sonra `fabric.mod.json` sürüm aralığı ve uyumsuzluk kayıtları güncellenmeli; istemci, özel sunucu ve portaldan geçiş senaryoları oyunda test edilmeli.

26.3 dalında `fabric.mod.json` hâlâ 1.21.1 uyumluluğunu ilan eder. Derleme ve oyun testleri geçmeden bu beyan değiştirilmemelidir.
