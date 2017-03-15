package net.dapay.app.API;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;

/**
 * Created by gabriel on 3/14/17.
 */

public class BlinktradeChileBit extends BlinktradeAPI {
    protected WebSocket CreateWebSocket(WebSocketFactory factory) throws IOException {
        return factory.createSocket("wss://api.blinktrade.com/trade/");
    }
    protected int GetBrokerID() { return 4; }
    protected String GetInstrumentSymbol() { return "BTCCLP"; }
    public String GetCurrencySupposedSymbol() { return "$"; }
    public String GetURL() { return "https://chilebit.net/#signin"; }
    public double GetSupposedIncrement() { return 1d; }
}
