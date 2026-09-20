# Fabric sürüm geçişi durumu

Son ölçüm: 20 Eylül 2026. Bu dal 1.21.1 Fabric temelini ve sürüm geçişi ölçümlerini tutar. 26.3 portu ayrı çalışma ağacında devam etmektedir; henüz çalışır mod veya yayımlanabilir JAR üretmez. Forge kapsam dışıdır.

| Minecraft | Durum |
| --- | --- |
| 1.21.1 | `compileJava` ve JUnit 5 testleri geçti (3 test, 0 hata). Oyun içinde hem komutla oluşturulan portal hem yeni yakılan normal Nether portalı denendi; ayrıntılar aşağıda. `runClient` başarılı çıktı. Günlükte `ImmPtlChunkTickets` hataları sürüyor. |
| 1.21.2 | `codex/fabric-1.21.2` dalında Java derlemesi geçti ve istemci açıldı. Creative Superflat dünyada dikey ve yatay Nether portalı oluştu, Nether görüntülendi, oyuncu iki yönde boyut değiştirdi; ayrıntılar aşağıda. Görüntü boşlukları ve chunk yükleme hataları sürdüğünden port henüz tamamlanmadı. Sodium ve Iris çalışma zamanı testi yapılmadı. |
| 1.21.3 | Özgün deponun `upstream/1.21.3` WIP dalı ayrı çalışma ağacında derlendi. Derleyici ilk 100 Java hatasında durdu; oyun testi yapılamadı. |
| 1.21.4–1.21.11 | Henüz derleme veya oyun testi yapılmadı. |
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

## 1.21.2 Fabric oyun testi ve açık işler

Test dünyası `Fabric 1.21.2 Portal Superflat T`: Creative Superflat, üstte bir çimen, altında iki toprak ve en altta bedrock; Overworld'de su katmanı yok. `run/options.txt` ana ses seviyesi `0.0`. Minecraft 1.21.2 ve Fabric API 0.106.1 kullanıldı. 1.21.3 kaynak dalından uyarlanan DimLib bu dalda `vendor/dimlib-source` kaynakları ve yerel JAR olarak tutuluyor. Cloth Config 16.0.143 kullanıldı. Sodium ve Iris yalnızca derleme bağımlılığı; çalışma zamanında kapalı.

- `compileJava` ve `test` geçti (3 JUnit testi, 0 hata); istemci ve kayıtlı dünya açıldı. 1.21.2'nin değişen shader, frustum, paket, chunk ve GUI API'leri için uyarlamalar yapıldı. Varsayılan vanilla arazi kurulumunda bloklar görünmediği için mevcut alternatif arazi görünürlük yolu etkinleştirildi.
- Dikey obsidyen çerçeve oyun içinde `fill` komutlarıyla kuruldu, çakmak taşı ve çelikle arayüzden yakıldı. Nether arazisi çerçeve içinden görüldü; [ekran görüntüsü](docs/test-evidence/fabric-1.21.2/nether-through-vertical-portal.png). Dünya kapatılıp yeniden açıldıktan sonra da portal görüntüsü sürdü.
- Oyuncu portal düzleminin iki yanına oyun içi `tp` komutuyla taşındığında Overworld → Nether ve Nether → Overworld geçişleri doğrulandı. Nether'daki [ekran görüntüsü](docs/test-evidence/fabric-1.21.2/player-in-nether.png) kaydedildi; F3 boyutu `minecraft:the_nether` gösterdi ve `We Need to Go Deeper` ilerlemesi alındı. Normal tuşla yürüyerek dikey geçiş bu denemede doğrulanmadı.
- Havada yatay çerçeve oyun komutlarıyla kuruldu ve ateş bloğu yerleştirilerek yakıldı. İki yönlü yatay portal varlıkları oyun içinde listelendi. Oyuncu yerçekimiyle bu portaldan Nether'a düştü; günlükte `Client Changed Dimension` kaydı var. İlk varış noktası lav üstündeydi; sonrasında Nether tarafına obsidyen iniş platformu kondu. Overworld düz dünya olarak kaldı.

**Açık sorunlar:** Dikey portal görünümünde bazı beyaz boşluklar var. `ImmPtlChunkTickets` bazı Overworld chunkları için `Unloaded level chunk` yazıyor. Eski alternatif boyut sis mixini ve bazı çarpışma kodları 1.21.2 API uyarlaması bekliyor. Normal yürüyüş, domuz geçişi, portal etkileşimi, özel sunucu, Sodium/Iris ve uzun süreli yeniden açma testleri tamamlanmadı. Bu dal yayımlanmaya hazır değil. Oyun, kullanıcının kendi testine devam edebilmesi için açık bırakıldı. Forge üzerinde çalışılmadı; uzak Git'e gönderim yapılmadı.

**Portal arkasındaki moblar:** 20 Eylül'de kullanıcı diğer boyuttaki mobların görünmediğini bildirdi. 1.21.2'nin mob vertex shader'ı `minecraft:core/entity` adıyla derleniyor; shader dönüşüm tablosu yalnızca kısa adları eşleştirdiği için portal kesme düzlemi kodu uygulanmıyordu. Kısa ad eşleştirmesi ve `entity` girdisi eklendi; tam kimlik eşleştirmesi Sodium gibi shader'lar için korundu. `gradlew test` tekrar geçti. Çalışan oyun eski sınıfları kullanır: değişikliğin oyun içindeki etkisi yeniden başlatılıp moblara portal içinden bakılarak henüz doğrulanmadı. `gradlew build`, mevcut yerel `vendor/dimlib-1.1.0+mc1.21.2.jar` dosyasını Loom `processIncludeJars` görevi içeri alamadığı için başarısız; derleme ve test görevleri başarılı.

## 1.21.3 ve sonraki sürümler

Özgün deponun 1.21.3 dalı da tamamlanmamış bir porttur. Ayrı 1.21.3 çalışma ağacında derleme ilk 100 Java hatasında durdu; oyun testi yok. 1.21.4 ve sonrası ile 26.x sürümleri için yukarıdaki tablo geçerlidir.
