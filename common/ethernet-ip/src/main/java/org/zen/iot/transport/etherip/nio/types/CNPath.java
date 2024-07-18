
package org.zen.iot.transport.etherip.nio.types;


import org.zen.iot.transport.etherip.nio.protocol.Protocol;

/**
 * Control Net Path
 * <p>
 * Example (with suitable static import):
 * <p>
 * <code>CNPath path = Identity().instance(1).attr(7)</code>
 *
 * @author Jack Wu
 */
abstract public class CNPath implements Protocol {
    // Objects, see Spec 10 p. 1, 13, 25

    /**
     * Create path to Identity object
     *
     * @return {@link CNClassPath}
     */
    public static CNClassPath Identity() {
        return new CNClassPath(0x01, "Identity");
    }

    /**
     * Create path to MessageRouter object
     *
     * @return {@link CNClassPath}
     */
    public static CNClassPath MessageRouter() {
        return new CNClassPath(0x02, "MessageRouter");
    }

    public static CNClassPath ConnectionObject() {
        return new CNClassPath(0x05, "ConnectionObject");
    }

    /**
     * Create path to ConnectionManager object
     *
     * @return {@link CNPath}
     */
    public static CNPath ConnectionManager() {
        return new CNClassPath(0x06, "ConnectionManager");
    }

    public static CNClassPath Port() {
        return new CNClassPath(0xf4, "Port");
    }

    public static CNClassPath TcpIpInterface() {
        return new CNClassPath(0xf5, "TCP/IP Interface");
    }

    public static CNClassPath EthernetLink() {
        return new CNClassPath(0xf6, "Ethernet Link");
    }

    /*
     * Logix5000 Data Access Class to read symbol names and types
     */
    public static CNClassPath SymbolList() {
        return new CNClassPath(0x6B, "Symbol List").instance(0);
    }

    /*
     * Logix5000 Data Access Class to read template attributes
     */
    public static CNClassPath TemplateAttributes() {
        return new CNClassPath(0x6C, "Template attributes");
    }

    /**
     * Create symbol path
     *
     * @param path Name of the tag to put into symbol path
     * @return {@link CNPath}
     */
    public static CNSymbolPath Symbol(final String path) {
        return new CNSymbolPath(path);
    }
}
