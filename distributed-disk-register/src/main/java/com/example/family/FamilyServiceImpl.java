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
public void store(StoreRequest req, StreamObserver<StoreResponse> responseObserver) {

    String messageId = req.getMessageId();
    String content = req.getContent();
    boolean isReplication = req.getIsReplication();

    boolean localSaveSuccess = saveToDisk(messageId, content);

    boolean finalSuccess = localSaveSuccess;

    if (!isReplication && localSaveSuccess) {

        List<NodeInfo> allNodes = nodeRegistry.snapshot();
        Collections.shuffle(allNodes);

        int targets = Math.min(toleranceValue, allNodes.size());
        List<NodeInfo> selectedNodes = allNodes.subList(0, targets);

        int successfulReplications = 0;

        for (NodeInfo node : selectedNodes) {
            try {
                ManagedChannel ch = ManagedChannelBuilder
                        .forAddress(node.getHost(), node.getPort())
                        .usePlaintext()
                        .build();

                FamilyServiceGrpc.FamilyServiceBlockingStub stub =
                        FamilyServiceGrpc.newBlockingStub(ch);

                StoreResponse resp = stub.store(
                        StoreRequest.newBuilder()
                                .setMessageId(messageId)
                                .setContent(content)
                                .setIsReplication(true)
                                .build()
                );

                if (resp.getSuccess()) {
                    successfulReplications++;
                }

                ch.shutdown();

            } catch (Exception e) {
                System.err.println("Replikasyon başarısız: " + node.getPort());
            }
        }

        finalSuccess = successfulReplications == targets;

        if (finalSuccess) {
            fileLocationMap.putIfAbsent(messageId, new ArrayList<>());
            fileLocationMap.get(messageId).add(serverPort);
            for (NodeInfo n : selectedNodes) {
                fileLocationMap.get(messageId).add(n.getPort());
            }
        }
    }

    StoreResponse response = StoreResponse.newBuilder()
            .setSuccess(finalSuccess)
            .setMessage(finalSuccess
                    ? "Veri tolerance kadar node'a başarıyla kaydedildi."
                    : "Tolerance şartı sağlanamadı.")
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
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
