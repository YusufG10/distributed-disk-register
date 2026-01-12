HaToKuSe: Hata-Tolere Kuyruk Servisi (Dağıtık Abonelik Sistemi)
Bu proje, Java ve gRPC teknolojileri kullanılarak geliştirilmiş; hata toleransı yüksek, dağıtık bir mesaj kayıt ve abonelik sistemidir. Sistem, ödev kapsamında tanımlanan HaToKuSe ilkel protokolü üzerine inşa edilmiştir.

🚀 Mimari Özellikler
Lider-Üye Yapısı: Sistemde bir sunucu "Lider" (varsayılan port 50051) olarak atanır; diğer sunucular "Aile Üyeleri" olarak lidere bağlanır.

Dinamik Üyelik: n sayıda üye sisteme çalışma zamanında dinamik olarak katılabilir veya sistemden ayrılabilir.

Metadata Yönetimi: Lider, hangi mesaj kimliklerinin (message_id) hangi üyelerde saklandığına dair bir Metadata Map tutar.

Hata Toleransı (Replikasyon): Gelen her mesaj, tolerance.conf dosyasında belirtilen değer kadar farklı üyeye kopyalanır (replikasyon).

Veri Kurtarma: Eğer bir mesajın saklandığı sunucu çökerse (crash), Lider otomatik olarak mesajın bir kopyasının bulunduğu diğer canlı üyeden veriyi çekerek istemciye sunar.

🛠️ Kullanılan Teknolojiler
Programlama Dili: Java

Haberleşme: gRPC & Protocol Buffers (HaToKuSe protokolü)

Bağımlılık Yönetimi: Maven

Yapılandırma: tolerance.conf üzerinden dinamik tolerans ayarı

📊 İzleme ve Raporlama
Periyodik Raporlar: Her sunucu (Lider ve Üyeler), her 10 saniyede bir kendi diskindeki mesaj sayısını konsola raporlar.

Heartbeat Mekanizması: Üyeler, dosya sayılarını Lider sunucuya periyodik olarak iletir. Lider, tüm kümenin güncel durumunu (hangi üye ne kadar mesaj tutuyor) raporlar.

⚙️ Kurulum ve Çalıştırma
1. Projeyi Derleme
Maven kullanarak gerekli gRPC sınıflarını üretin:

Bash

mvn clean install
2. tolerance.conf Yapılandırması
Proje ana dizininde tolerance.conf dosyasına replikasyon sayısını yazın:

Plaintext

tolerance=2
3. Çalıştırma Sırası
Lider Sunucu: NodeMain sınıfını parametresiz çalıştırın (Varsayılan Port: 50051).

Üye Sunucular: NodeMain sınıfını farklı port parametreleriyle çalıştırın (Örn: NodeMain 50052, NodeMain 50053).

Ağa Katılım: JoinTest sınıfını çalıştırarak üyelerin Lider'e kayıt olmasını sağlayın.

İstemci: InteractiveClient sınıfını başlatın.

🎮 İstemci Komutları (HaToKuSe Protokolü)
İstemci terminal üzerinden şu metin tabanlı komutları destekler:

Veri Kaydetme: SET <message_id> <message_content>

Örnek: SET msg101 Merhaba_Dunya

Veri Okuma: GET <message_id>

Örnek: GET msg101

Çıkış: cik

🛡️ Hata Toleransı Test Senaryosu
SET komutu ile bir mesaj kaydedilir.

Liderin diskindeki dosya manuel olarak silinir.

GET komutu ile aynı mesaj istenir.

Lider, mesajın kendi diskinde olmadığını fark eder, Metadata tablosundan mesajın kopyasının olduğu 50052 portuna sorar ve veriyi başarıyla getirir.
