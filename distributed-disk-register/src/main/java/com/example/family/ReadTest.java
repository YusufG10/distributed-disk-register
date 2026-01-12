package com.example.family;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ReadTest {
    public static void main(String[] args) {
        
        int targetPort = 50052;

        System.out.println("Okuma Testi (" + targetPort + ") Başlıyor...");

        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", targetPort)
                .usePlaintext()
                .build();

        try {
            FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

            
            String messageId = "test_mesaji_1";

            GetRequest request = GetRequest.newBuilder()
                    .setMessageId(messageId)
                    .build();

            System.out.println("Sunucuya soruluyor: " + messageId + " sende var mı?");
            GetResponse response = stub.get(request);

            if (response.getFound()) {
                System.out.println("✅ BULUNDU! Dosya İçeriği:");
                System.out.println("------------------------------------------------");
                System.out.println(response.getContent());
                System.out.println("------------------------------------------------");
            } else {
                System.out.println("❌ BULUNAMADI. Replikasyon henüz gerçekleşmemiş olabilir.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            channel.shutdown();
        }
    }
}
