package net.dapay.app.API;

import net.dapay.app.Bill;
import net.dapay.app.Profile;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Future;

/**
 * Created by gabriel on 11/25/16.
 */

public interface IExchangeAPI {

    class ErrorOrPayload<T> {
        public static final int ERROR_NONE               =  0;
        public static final int ERROR_UNKNOWN            =  1;
        public static final int ERROR_NOT_CONNECTED      =  2;
        public static final int ERROR_LOGIN_PASS         =  3;
        public static final int ERROR_NEED_2FA           =  4;
        public static final int ERROR_INVALID_2FA        =  5;
        public static final int ERROR_UNAUTHORIZED       =  6;
        public static final int ERROR_NOT_LOGGED_IN      =  7;
        public static final int ERROR_TIMEOUT            =  8;
        public static final int ERROR_BUSY               =  9;
        public static final int ERROR_INSUFFICIENT_FUNDS = 10;
        public static final int ERROR_NO_BUYER           = 11;
        public static final int ERROR_NOT_ENOUGH_BUYERS  = 12;

        private int errcode;
        private T payload;

        public ErrorOrPayload(int s, T o) {
            errcode = s;
            payload = o;
        }
        public int getErrCode() { return errcode; }
        public T getPayload() { return payload; }
    }

    class Fees {
        double deposit_fixed      = 0;
        double deposit_percent    = 0;
        double maker_fixed        = 0;
        double maker_percent      = 0;
        double taker_fixed        = 0;
        double taker_percent      = 0;
        double withdrawal_fixed   = 0;
        double withdrawal_percent = 0;
        Fees() {}
    }

    double[] GetQuotation(double fiat_amount);
    void GetBookUpdates(Observer o);
    void GetDepositUpdates(Observer o);
    double GetBestBidPrice();
    double GetBestAskPrice();
    String GetCurrencySupposedSymbol();
    String GetCurrencyActualSymbol();
    double GetSupposedIncrement();
    double GetActualIncrement();
    String GetURL();
    double GetMakerPrice();

    Future<Boolean> Connect();
    Future<ErrorOrPayload<Boolean>> SignUp(String login, String password, String _2FA, String fingerPrint);
    Future<ErrorOrPayload<Boolean>> Login(String login, String password, String _2FA, String fingerPrint);
    Future<ErrorOrPayload<Profile>> CreateProfile(String label, String password);
    void ReadInstrumentDescription();
    void SignUpMarketData(boolean full_bids);
    Fees GetFees();
    Future<ErrorOrPayload<Float>> GetCryptoBalance();
    Future<ErrorOrPayload<String>> GetDepositAddress();
    Future<ErrorOrPayload<Hashtable<String, ArrayList<Bill.Deposit>>>> GetDepositStatus();
    ErrorOrPayload<Double> Convert(double fiat_amount);
    ErrorOrPayload<Double> TakeMarket(double crypto_amount);
    ErrorOrPayload<Double> MakeMarket(double crypto_amount);
    void Logout();

    class LastDepositData extends Observable {
        public LastDepositData() { }
        public void SetChanged() {
            setChanged();
        }
    }
}
