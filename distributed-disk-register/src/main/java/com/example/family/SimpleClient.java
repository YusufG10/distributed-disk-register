package com.example.family;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class SimpleClient {
    public static void main(String[] args) {
        System.out.println("İstemci başlatılıyor...");

        // 1. Sunucuya bağlanmak için bir kanal (Channel) açıyoruz
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // Güvenli olmayan bağlantı (Test için gerekli)
                .build();

        try {
            // 2. Servis metodlarını çağırmak için bir 'Stub' (Vekil) oluşturuyoruz
            FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

            // 3. Göndereceğimiz isteği hazırlıyoruz
            String mesajId = "test_mesaji_1";
            String icerik = "Merhaba, bu dağıtık sistemden gelen bir test mesajıdır!";

            StoreRequest request = StoreRequest.newBuilder()
                    .setMessageId(mesajId)
                    .setContent(icerik)
                    .build();

            // 4. İsteği sunucuya gönderiyoruz ve yanıtı alıyoruz
            System.out.println("Sunucuya istek gönderiliyor -> ID: " + mesajId);
            StoreResponse response = stub.store(request);

            // 5. Sunucudan gelen cevabı ekrana yazdırıyoruz
            if (response.getSuccess()) {
                System.out.println("BAŞARILI! Sunucu yanıtı: " + response.getMessage());
            } else {
                System.out.println("BAŞARISIZ! Sunucu yanıtı: " + response.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Bir hata oluştu: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 6. İşimiz bitince kanalı kapatıyoruz
            System.out.println("İstemci kapatılıyor.");
            channel.shutdown();
        }
    }
}