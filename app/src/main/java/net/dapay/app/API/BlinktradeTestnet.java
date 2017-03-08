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

public class BlinktradeTestnet extends BlinktradeAPI {
    protected WebSocket CreateWebSocket(WebSocketFactory factory) throws IOException {
        return factory.createSocket("wss://api.testnet.blinktrade.com/trade/");
    }
    protected int GetBrokerID() {
        return 5;
    }
    protected String GetInstrumentSymbol() { return "BTCUSD"; }
    public String GetCurrencySupposedSymbol() { return "US$"; }
    public String GetURL() { return "https://testnet.blinktrade.com/#signin"; }
    public double GetSupposedIncrement() { return 0.01d; }
}
