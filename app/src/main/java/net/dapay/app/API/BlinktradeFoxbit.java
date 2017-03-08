package net.dapay.app.API;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;

/**
 * Created by gabriel on 12/29/16.
 */

public class BlinktradeFoxbit extends BlinktradeAPI {
    protected WebSocket CreateWebSocket(WebSocketFactory factory) throws IOException {
        return factory.createSocket("wss://api.blinktrade.com/trade/");
    }
    protected int GetBrokerID() {
        return 4;
    }
    protected String GetInstrumentSymbol() { return "BTCBRL"; }
    public String GetCurrencySupposedSymbol() { return "R$"; }
    public String GetURL() { return "https://foxbit.exchange/#signin"; }
    public double GetSupposedIncrement() { return 0.01d; }
}
