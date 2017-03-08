package net.dapay.app.API;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;

/**
 * Created by gabriel on 1/17/17.
 */

public class BlinktradeTestnetFoxbit extends BlinktradeAPI {
    protected WebSocket CreateWebSocket(WebSocketFactory factory) throws IOException {
        return factory.createSocket("wss://api.testnet.foxbit.exchante/trade/");
    }
    protected int GetBrokerID() {
        return 4;
    }
    protected String GetInstrumentSymbol() { return "BTCRL"; }
    public String GetCurrencySupposedSymbol() { return "R$"; }
    public String GetURL() { return "https://testnet.foxbit.exchange/#signin"; }
    public double GetSupposedIncrement() { return 0.01d; }
}
