package com.example.family;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class NodeMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 50051;
        NodeRegistry registry = new NodeRegistry();
        Server server = ServerBuilder.forPort(port)
                .addService(new FamilyServiceImpl(registry, port))
                .build().start();

        System.out.println("🚀 Sunucu port " + port + " üzerinde aktif.");
        startPeriodicReporting(port, registry);
        server.awaitTermination();
    }

    private static void startPeriodicReporting(int port, NodeRegistry registry) {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            File folder = new File("data_" + port);
            int count = (folder.exists() && folder.listFiles() != null) ? folder.listFiles().length : 0;

            System.out.println("\n📊 [DURUM RAPORU - PORT " + port + "] Saklanan Mesaj: " + count);

            if (port != 50051) {
                // Üye ise Lider'e dosya sayısını bildir (Heartbeat)
                try {
                    ManagedChannel ch = ManagedChannelBuilder.forAddress("127.0.0.1", 50051).usePlaintext().build();
                    FamilyServiceGrpc.newBlockingStub(ch).heartbeat(
                            HeartbeatRequest.newBuilder().setPort(port).setFileCount(count).build());
                    ch.shutdown();
                } catch (Exception ignored) {}
            } else {
                // Lider ise tüm küme istatistiklerini bas
                System.out.println("🌐 Küme Genel Durumu:");
                FamilyServiceImpl.memberFileCounts.forEach((p, c) ->
                        System.out.println("   -> Üye " + p + ": " + c + " mesaj saklıyor"));
            }
        }, 0, 10, TimeUnit.SECONDS);
    }
}