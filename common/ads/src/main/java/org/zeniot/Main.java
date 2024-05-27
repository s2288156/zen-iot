// package org.zeniot;
//
//
// import lombok.extern.slf4j.Slf4j;
// import org.apache.plc4x.java.DefaultPlcDriverManager;
// import org.apache.plc4x.java.ads.AdsPlcDriver;
// import org.apache.plc4x.java.ads.configuration.AdsConfiguration;
// import org.apache.plc4x.java.ads.readwrite.AdsDataType;
// import org.apache.plc4x.java.ads.readwrite.AdsReadRequest;
// import org.apache.plc4x.java.ads.readwrite.AdsReadWriteRequest;
// import org.apache.plc4x.java.ads.readwrite.AmsTCPPacket;
// import org.apache.plc4x.java.ads.tag.AdsTag;
// import org.apache.plc4x.java.api.PlcConnection;
// import org.apache.plc4x.java.api.PlcConnectionManager;
// import org.apache.plc4x.java.api.PlcDriverManager;
// import org.apache.plc4x.java.api.messages.PlcReadRequest;
// import org.apache.plc4x.java.api.messages.PlcReadResponse;
// import org.apache.plc4x.java.api.model.PlcTag;
// import org.apache.plc4x.java.spi.messages.DefaultPlcReadRequest;
// import org.apache.plc4x.java.transport.tcp.TcpChannelFactory;
//
// @Slf4j
// public class Main {
//
//     private static final String b = "MAIN.b";
//
//     public static void main(String[] args) {
//
//         String spsIp = "172.18.161.236";
//         String clientIp = "192.168.50.44";
//         String sourceAmsNetId = "192.168.50.44.1.1";
//         int sourceAmsPort = 851;
//         String targetAmsNetId = "172.28.131.49.1.1";
//         int targetAmsPort = 851;
//         String connectionString = String.format("ads:tcp://%s?source-ams-net-id=%s&source-ams-port=%d&target-ams-net-id=%s&target-ams-port=%d", spsIp, sourceAmsNetId, sourceAmsPort, targetAmsNetId, targetAmsPort);
//
//         // AdsReadRequest
//         try (PlcConnection plcConnection = new DefaultPlcDriverManager().getConnection(connectionString)) {
//             plcConnection.connect();
//             log.info("is connected: {}", plcConnection.isConnected());
//
//             PlcReadRequest.Builder readRequestBuilder = plcConnection.readRequestBuilder();
//             PlcReadRequest request = readRequestBuilder.addTagAddress(b, "0xf00f/0x0:SINT").build();
//             PlcReadResponse readResponse = request.execute().get();
//             log.info("{}, responseCode: {}, value: {}", b, readResponse.getResponseCode(b), readResponse.getPlcValue(b));
//
//         } catch (Exception e) {
//             log.error("", e);
//         }
//         log.info("{}", connectionString);
//     }
// }
