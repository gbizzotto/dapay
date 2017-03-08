package net.dapay.app.API;

/**
 * Created by gabriel on 11/25/16.
 */

public class ExchangeAPI {
    static IExchangeAPI currentAPI = null;
    public static IExchangeAPI GetCurrentAPI() {
        return currentAPI;
    }
    public static void SetCurrentAPI(Class<? extends IExchangeAPI> API) {
        if (currentAPI != null && currentAPI.getClass() != API)
            currentAPI.Logout();
        try {
            currentAPI = (IExchangeAPI) API.newInstance();
        } catch (Exception e) {
            System.out.println(e);
            currentAPI = null;
            return;
        }
    }
}

