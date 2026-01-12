package com.example.family;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Scanner;

public class InteractiveClient {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("-------------------------------------------------");
        System.out.println("   HATAKUKUSE - DAĞITIK ABONELİK SİSTEMİ   ");
        System.out.println("-------------------------------------------------");
        System.out.print("Lider Portu (Varsayılan 50051): ");
        int port = sc.nextInt(); sc.nextLine();

        ManagedChannel ch = ManagedChannelBuilder.forAddress("127.0.0.1", port).usePlaintext().build();
        FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(ch);

        while (true) {
            System.out.print("\nKomut (SET <id> <msg> / GET <id> / cik) > ");
            String input = sc.nextLine();
            String[] p = input.split(" ", 3);

            try {
                if (p[0].equalsIgnoreCase("SET") && p.length == 3) {
                    StoreResponse res = stub.store(StoreRequest.newBuilder()
                            .setMessageId(p[1]).setContent(p[2]).setIsReplication(false).build());
                    System.out.println("YANIT: " + res.getMessage());
                } else if (p[0].equalsIgnoreCase("GET") && p.length == 2) {
                    GetResponse res = stub.get(GetRequest.newBuilder().setMessageId(p[1]).build());
                    System.out.println(res.getFound() ? "OK: " + res.getContent() : "ERROR: Kayıt Bulunamadı");
                } else if (p[0].equalsIgnoreCase("cik")) break;
                else System.out.println("⚠️ Hatalı komut formatı.");
            } catch (Exception e) {
                System.err.println("🚨 Bağlantı hatası: " + e.getMessage());
            }
        }
        ch.shutdown();
    }
}