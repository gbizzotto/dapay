package net.dapay.app.API;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;

/**
 * Created by gabriel on 3/14/17.
 */

public class BlinktradeSurBitcoin extends BlinktradeAPI {
    protected WebSocket CreateWebSocket(WebSocketFactory factory) throws IOException {
        return factory.createSocket("wss://api.blinktrade.com/trade/");
    }
    protected int GetBrokerID() {
        return 1;
    }
    protected String GetInstrumentSymbol() { return "BTCVEF"; }
    public String GetCurrencySupposedSymbol() { return "BsF"; }
    public String GetURL() { return "https://surbitcoin.com/#signin"; }
    public double GetSupposedIncrement() { return 1d; }
}
