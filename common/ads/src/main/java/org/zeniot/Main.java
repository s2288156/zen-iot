package org.zeniot;

import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.messages.PlcReadRequest;

/**
 * @author Wu.Chunyang
 */
public class Main {
    public static void main(String[] args) {
        String connectionString = "ads://127.0.0.1";

        try (PlcConnection plcConnection = PlcDriverManager.getDefault().getConnectionManager().getConnection(connectionString)) {
            if (plcConnection.getMetadata().isReadSupported()) {
                PlcReadRequest.Builder builder = plcConnection.readRequestBuilder();
                // AdsReadRequest readRequest = new AdsReadRequest();
            } else if (plcConnection.getMetadata().isWriteSupported()) {

            }
        } catch (Exception e) {

        }
        System.out.println("Hello world!");
    }
}
