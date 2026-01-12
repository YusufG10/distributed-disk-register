package com.example.family;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FamilyServiceImpl extends FamilyServiceGrpc.FamilyServiceImplBase {
    private static final String DATA_DIR_PREFIX = "data_";
    private final NodeRegistry nodeRegistry;
    private final int serverPort;
    private int toleranceValue = 1;

    // Metadata: Mesaj ID'sine göre hangi portlarda saklandığı
    private static final Map<String, List<Integer>> fileLocationMap = new ConcurrentHashMap<>();
    // İstatistik: Hangi üye kaç mesaj tutuyor
    public static final Map<Integer, Integer> memberFileCounts = new ConcurrentHashMap<>();

    public FamilyServiceImpl(NodeRegistry nodeRegistry, int serverPort) {
        this.nodeRegistry = nodeRegistry;
        this.serverPort = serverPort;
        new File(DATA_DIR_PREFIX + serverPort).mkdir();
        loadToleranceConfig();
    }

    private void loadToleranceConfig() {
        try (FileInputStream input = new FileInputStream("tolerance.conf")) {
            Properties prop = new Properties();
            prop.load(input);
            this.toleranceValue = Integer.parseInt(prop.getProperty("tolerance", "1"));
            System.out.println("⚙️ Tolerans Değeri: " + toleranceValue);
        } catch (IOException ex) {
            this.toleranceValue = 1;
        }
    }

    @Override
    public void store(StoreRequest req, StreamObserver<StoreResponse> resObs) {
        String path = DATA_DIR_PREFIX + serverPort + File.separator + req.getMessageId() + ".txt";
        boolean success = false;
        try (FileWriter fw = new FileWriter(path)) {
            fw.write(req.getContent());
            success = true;
            fileLocationMap.computeIfAbsent(req.getMessageId(), k -> new ArrayList<>()).add(serverPort);
        } catch (IOException e) { e.printStackTrace(); }

        if (success && !req.getIsReplication()) {
            List<NodeInfo> nodes = new ArrayList<>(nodeRegistry.snapshot());
            Collections.shuffle(nodes);
            int targets = Math.min(toleranceValue, nodes.size());
            for (int i = 0; i < targets; i++) {
                NodeInfo n = nodes.get(i);
                try {
                    ManagedChannel ch = ManagedChannelBuilder.forAddress(n.getHost(), n.getPort()).usePlaintext().build();
                    FamilyServiceGrpc.newBlockingStub(ch).store(StoreRequest.newBuilder()
                            .setMessageId(req.getMessageId()).setContent(req.getContent()).setIsReplication(true).build());
                    fileLocationMap.get(req.getMessageId()).add(n.getPort());
                    ch.shutdown();
                } catch (Exception e) { System.out.println("Replikasyon hatası: " + n.getPort()); }
            }
        }
        resObs.onNext(StoreResponse.newBuilder().setSuccess(success).setMessage(success ? "OK" : "ERROR").build());
        resObs.onCompleted();
    }

    @Override
    public void get(GetRequest req, StreamObserver<GetResponse> resObs) {
        File local = new File(DATA_DIR_PREFIX + serverPort + File.separator + req.getMessageId() + ".txt");
        if (local.exists()) {
            try {
                resObs.onNext(GetResponse.newBuilder().setFound(true).setContent(Files.readString(local.toPath())).build());
                resObs.onCompleted();
                return;
            } catch (IOException ignored) {}
        }

        // Hata Toleransı: Kendi diskinde yoksa Metadata haritasından diğer üyeleri bulup iste
        List<Integer> locs = fileLocationMap.getOrDefault(req.getMessageId(), new ArrayList<>());
        for (int p : locs) {
            if (p == serverPort) continue;
            try {
                ManagedChannel ch = ManagedChannelBuilder.forAddress("127.0.0.1", p).usePlaintext().build();
                GetResponse r = FamilyServiceGrpc.newBlockingStub(ch).get(req);
                if (r.getFound()) {
                    resObs.onNext(r); resObs.onCompleted(); ch.shutdown(); return;
                }
                ch.shutdown();
            } catch (Exception ignored) {}
        }
        resObs.onNext(GetResponse.newBuilder().setFound(false).build());
        resObs.onCompleted();
    }

    @Override
    public void join(JoinRequest req, StreamObserver<JoinResponse> resObs) {
        nodeRegistry.add(req.getNode());
        System.out.println("✅ Yeni üye katıldı: " + req.getNode().getPort());
        resObs.onNext(JoinResponse.newBuilder().setSuccess(true).build());
        resObs.onCompleted();
    }

    @Override
    public void heartbeat(HeartbeatRequest req, StreamObserver<HeartbeatResponse> resObs) {
        memberFileCounts.put(req.getPort(), req.getFileCount());
        resObs.onNext(HeartbeatResponse.newBuilder().setSuccess(true).build());
        resObs.onCompleted();
    }
}