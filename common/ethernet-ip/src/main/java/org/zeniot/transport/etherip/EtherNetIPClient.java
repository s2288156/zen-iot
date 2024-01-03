package org.zeniot.transport.etherip;

/**
 * @author Jack Wu
 */
public class EtherNetIPClient {

    private final String ip;
    private final int slot;

    private TcpConnection connection;

    public EtherNetIPClient(String ip, int slot) {
        this.ip = ip;
        this.slot = slot;
    }

    public void connectTcp() {
        this.connection = new TcpConnection();
        this.connection.connect(ip);
    }

    public void disconnect() {
        this.connection.close();
    }
}
