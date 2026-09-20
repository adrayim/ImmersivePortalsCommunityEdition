# Fabric sürüm geçişi durumu

Son ölçüm: 20 Eylül 2026. Bu dal 1.21.1 Fabric temelini ve sürüm geçişi ölçümlerini tutar. 26.3 portu ayrı çalışma ağacında devam etmektedir; henüz çalışır mod veya yayımlanabilir JAR üretmez. Forge kapsam dışıdır.

| Minecraft | Durum |
| --- | --- |
| 1.21.1 | `compileJava` ve JUnit 5 testleri geçti (3 test, 0 hata). Oyun içinde hem komutla oluşturulan portal hem yeni yakılan normal Nether portalı denendi; ayrıntılar aşağıda. `runClient` başarılı çıktı. Günlükte `ImmPtlChunkTickets` hataları sürüyor. |
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

## 1.21.1 oyun testi

Kaynak davranış: [Immersive Portals Wiki — Portals](https://qouteall.fun/immptl/wiki/Portals.html). Test, Creative **Superflat** dünyada yapıldı; ana ses `OFF` olarak ayarlandı. Önceki `/portal` komutuyla oluşturulan portal testi, normal Nether portalı için yeterli kanıt sayılmıyor.

- `Wiki Nether Portal Test` adlı yeni dünyada standart obsidyen çerçeve çakmak taşı ve çelikle oyun arayüzünden yakıldı. Dikey çerçeve içinde Nether arazisi görüldü; [oyun görüntüsü](docs/test-evidence/fabric-1.21.1/nether-portal.png).
- Dikey portala hareket verilerek çağrılan etiketli domuzun Nether boyutunda bulunduğu `/execute in minecraft:the_nether if entity ...` ile doğrulandı. Bu test fiziksel varlık geçişini kapsar.
- Wiki'de desteklendiği belirtilen yatay obsidyen portal da oyun arayüzünden yakıldı. Oyuncu portala düşerek Overworld'den Nether'a geçti; günlükte `Client Changed Dimension`, portal kimliği ve `We Need to Go Deeper` ilerlemesi var. [F3 ekran görüntüsü](docs/test-evidence/fabric-1.21.1/player-in-nether.png).
- Nether tarafındaki yatay portalın üzerine gidildiğinde günlükte Nether → Overworld geçişi görüldü; yerçekimi oyuncuyu hemen Overworld → Nether yönünde tekrar portala soktu. Bu nedenle son F3 görüntüsü Nether'dadır; günlükte iki ayrı geçiş kayıtlıdır.
- Dünya kaydedilip yeniden açıldı. Dikey portalın içinden Nether arazisi yeniden görüntülendi; [yeniden açıldıktan sonraki görüntü](docs/test-evidence/fabric-1.21.1/nether-portal-after-reload.png).
- Ayrı `Fabric 1.21.1 Flat Portal Test` dünyasında komutla oluşturulan portalın hedefindeki altın blok görüldü; kaydetme/yükleme sonrası portal duruyordu. Oyuncu pistonun fiziksel itmesiyle yaklaşık `(40.5, -60, 9.2)` konumundan `(100.0, -60, 30.31)` hedefine geçti.

**Açık sorun:** Wiki dünyasının ilk açılışında Overworld ve Nether için `ImmPtlChunkTickets` `Chunk loading failure` kayıtları oluştu. Günlük satırına eksik olan `ChunkResult` bilgisi eklendi; değişiklik sonrası dünya tekrar açıldığında ve aynı ayarlarla yeni bir düz dünya oluşturulduğunda hata yeniden oluşmadı. Bu iki deneme sorunun çözüldüğünü kanıtlamaz. Sunucu, çok oyunculu oyun, etkileşim/çarpışma ve 1.21.2+ oyun testleri ayrıca yapılmalıdır.
