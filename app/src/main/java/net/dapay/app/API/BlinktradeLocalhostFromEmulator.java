package net.dapay.app.API;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import net.dapay.app.NaiveSSLContext;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

/**
 * Created by gabriel on 12/29/16.
 */

public class BlinktradeLocalhostFromEmulator extends BlinktradeAPI {
    protected WebSocket CreateWebSocket(WebSocketFactory factory) throws IOException {
        try {
            SSLContext context = NaiveSSLContext.getInstance("TLS");
            factory.setSSLContext(context);
        } catch (NoSuchAlgorithmException ex) {
        }
        return factory.createSocket("wss://10.0.2.2/trade/", 5000);
    }
    protected int GetBrokerID() {
        return 4;
    }
    protected String GetInstrumentSymbol() { return "BTCBRL"; }
    public String GetCurrencySupposedSymbol() { return "R$"; }
    public String GetURL() { return "https://10.0.2.2/#signin"; }
    public double GetSupposedIncrement() { return 0.01d; }
}
