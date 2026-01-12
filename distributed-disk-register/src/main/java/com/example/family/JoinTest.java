package com.example.family;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class JoinTest {
    public static void main(String[] args) {
        System.out.println("Join (Katılma) Testi Başlıyor...");

    
        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", 50051)
                .usePlaintext()
                .build();

        try {
            FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

            // 2. "Ben 50052 portlu sunucuyum, beni aranıza alın" diyen isteği hazırlıyoruz
            NodeInfo myInfo = NodeInfo.newBuilder()
                    .setHost("127.0.0.1") // Burada da IP adresi kullanıyoruz
                    .setPort(50052)
                    .build();

            JoinRequest request = JoinRequest.newBuilder()
                    .setNode(myInfo)
                    .build();

            // 3. İsteği gönderiyoruz
            System.out.println("50051 sunucusuna katılma isteği gönderiliyor...");
            JoinResponse response = stub.join(request);

            // 4. Sonucu yazdırıyoruz
            if (response.getSuccess()) {
                System.out.println("✅ BAŞARILI! Sunucu yanıtı: " + response.getMessage());
            } else {
                System.out.println("❌ BAŞARISIZ! Sunucu yanıtı: " + response.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Bağlantı hatası: " + e.getMessage());
            e.printStackTrace();
        } finally {
            channel.shutdown();
        }
    }
}
