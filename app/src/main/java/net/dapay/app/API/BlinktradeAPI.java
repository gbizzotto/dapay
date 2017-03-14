package net.dapay.app.API;

import net.dapay.app.Bill;
import net.dapay.app.Profile;
import net.dapay.app.util.CustomFuture;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.dapay.app.API.IExchangeAPI.ErrorOrPayload.ERROR_NONE;
import static net.dapay.app.API.IExchangeAPI.ErrorOrPayload.ERROR_NO_BUYER;

/**
 * Created by gabriel on 11/25/16.
 */

public abstract class BlinktradeAPI implements IExchangeAPI {

    protected class Connection extends WebSocketAdapter {
        protected WebSocketFactory mFactory = new WebSocketFactory();
        public WebSocket mWS = null;
        private CountDownLatch mLatch = null;
        private CustomFuture<Boolean> mConnectionStatusFuture = null;

        public void Close() {
            if (mLatch != null)
                mLatch.countDown();
            if (mWS != null && mWS.getState() == WebSocketState.OPEN) {
                mWS.disconnect();
            }
        }
        public Future<Boolean> Connect() {
            Close();
            try {
                mConnectionStatusFuture = new CustomFuture<>();
                mWS = BlinktradeAPI.this.CreateWebSocket(mFactory);
                mWS.addListener(this);
                mWS.connect();
            } catch (Throwable ex) {
                System.out.println(ex.getMessage());
                mConnectionStatusFuture.setDone(false);
            }

            return mConnectionStatusFuture;
        }
        public boolean sendJSONText(String msg) {
            // Check JSON validity
            try {
                JSONObject json = new JSONObject(msg);
            } catch (JSONException e) {
                System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + msg + " =========== Invalid JSON ");
                return false;
            }
            if (mWS.getState() == WebSocketState.OPEN) {
                System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + "=========== Sending " + msg);
                mWS.sendText(msg);
            } else {
                System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + msg + " =========== Can't send, not connected ");
            }
            return true;
        }
        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            if (websocket == mWS) {
                mConnectionStatusFuture.setDone(true);

                mLatch = new CountDownLatch(1);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (!mLatch.await(20, TimeUnit.SECONDS))
                                sendJSONText("{\"TestReqID\": " + GetNextRequestID() + ", \"MsgType\": \"1\", \"FingerPrint\": \"" + mFingerPrint + "\"}");
                        } catch (InterruptedException ex) {
                        }
                    }
                }).start();
            }
        }
        @Override
        public void onDisconnected(WebSocket websocket,
                                   WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame,
                                   boolean closedByServer) throws Exception {
            if (websocket == mWS) {
                mConnectionStatusFuture.setDone(false);
                mLatch.countDown();
            }
        }
        @Override
        public void onTextMessage(WebSocket websocket, String message) throws Exception {
            BlinktradeAPI.this.onTextMessage(message);
        }
    }

    public class BookEntry {
        public double price;
        public long size;
        public BookEntry(double px, long s) {
            this.price = px;
            this.size = s;
        }
    };
    public class BookData extends Observable {
        ArrayList<BookEntry> book_bids = new ArrayList<BookEntry>();
        BookEntry best_ask;
        public BookData() { }
        public void SetChanged() {
            setChanged();
        }
    }

    public class ExecutionReportValues {
        public double avg_px;
        public double fiat_amount;
        public double crypto_amount;
        ExecutionReportValues(double avg, double fiat, double cry) {
            avg_px = avg;
            fiat_amount = fiat;
            crypto_amount = cry;
        }
    }

    protected String mUserID;
    protected String mFingerPrint = "";
    protected int mLastRequestID = 1000000;
    protected String mBidsUpdateReqID;
    protected String mAsksUpdateReqID;
    protected double mIncrement = GetSupposedIncrement();
    protected String mCurrencySign = GetCurrencySupposedSymbol();
    Hashtable<String, CustomFuture<ErrorOrPayload<?>>> mPendingRequests = new Hashtable<>();

    Connection mConnection = new Connection();
    BookData book_data = new BookData();
    IExchangeAPI.LastDepositData mLastDepositData = new IExchangeAPI.LastDepositData();
    IExchangeAPI.Fees mFees = null;

    public String GetCurrencyActualSymbol() {
        return mCurrencySign;
    }
    public double GetActualIncrement() {
        return mIncrement;
    }
    public double GetMakerPrice() {
        // MAKER, HOLD or not enough crypto
        if (GetBestBidPrice() != 0.0) {
            return GetBestBidPrice() + GetActualIncrement();
        } else if (GetBestAskPrice() != 0.0) {
            return GetBestAskPrice() - GetActualIncrement();
        } else {
            return 0.0d;
        }
    }

    public void GetBookUpdates(Observer o) {
        book_data.addObserver(o);
    }

    protected abstract WebSocket CreateWebSocket(WebSocketFactory factory) throws IOException;
    protected abstract int GetBrokerID();
    protected abstract String GetInstrumentSymbol();


    public Future<Boolean> Connect() {
        return mConnection.Connect();
    }
    public void Logout() {
        mConnection.Close();
        mLastDepositData.deleteObservers();
    }

    public Fees GetFees() {
        return mFees;
    }

    public double[] GetQuotation(double fiat_amount) throws ArithmeticException{
        double remaining_fiat_amount = fiat_amount;
        long satoshi_total = 0;
        ArrayList<Double[]> conversion_values = new ArrayList<Double[]>();
        for (int i = 0; i<book_data.book_bids.size() && remaining_fiat_amount > 0 ; ++i) {
            BookEntry entry = book_data.book_bids.get(i);
            double entry_fiat_amount = entry.price * entry.size / 100000000d;
            if (entry_fiat_amount >= remaining_fiat_amount) {
                conversion_values.add(new Double[]{entry.price, remaining_fiat_amount / entry.price});
                satoshi_total += (long) 100000000 * remaining_fiat_amount / entry.price;
                remaining_fiat_amount = 0;
            } else {
                conversion_values.add(new Double[]{entry.price, (double)entry.size});
                satoshi_total += entry.size;
                remaining_fiat_amount -= entry_fiat_amount;
            }
        }
        Double exchange_rate = null;
        if (conversion_values.size() > 0) {
            double weighted_total = 0.0d;
            double total_crypto = 0.0d;
            for (Double[] pair: conversion_values) {
                weighted_total += pair[0] * pair[1];
                total_crypto += pair[1];
            }
            exchange_rate = new Double(weighted_total / total_crypto);
        }
        if (remaining_fiat_amount == 0.0)
            return new double[]{satoshi_total / 100000000.0, exchange_rate};
        else if (remaining_fiat_amount == fiat_amount)
            throw new ArithmeticException();
        else
            return new double[]{satoshi_total * remaining_fiat_amount / fiat_amount / 100000000.0, exchange_rate};
    }

    String GetNextRequestID() {
        return "" + (++mLastRequestID);
    }



    public void onTextMessage(String message) throws Exception {
        JSONObject json = new JSONObject(message);

        System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " ========= Receiving " + json.toString(3));

        String msgType = json.getString("MsgType");
        if (msgType.equals("ERROR")) {
            String req_id = null;
            try {
                if (json.has("ReqID"))
                    req_id = json.getString("ReqID");
                else if (json.has("Request") && json.getJSONObject("Request").has("BalanceReqID"))
                    req_id = json.getJSONObject("Request").getString("BalanceReqID");
            } catch (JSONException e) {
                return;
            }
            if (req_id == null)
                return;
            CustomFuture<ErrorOrPayload<?>> promise = mPendingRequests.get(req_id);
            if (promise == null)
                return;
            if (promise.isCancelled()) {
                mPendingRequests.remove(req_id);
                return;
            }
            try {
                String description = json.getString("Description");
                if (description.equals("Not authorized")) {
                    promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_UNAUTHORIZED, null));
                } else {
                    promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_UNKNOWN, null));
                }
            } catch (JSONException e) {
                promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_UNKNOWN, null));
            }
            mPendingRequests.remove(req_id);
        } else if (msgType.equals("BF")) {
            String req_id;
            try {
                req_id = json.getString("UserReqID");
            } catch (JSONException e) {
                return;
            }
            CustomFuture<ErrorOrPayload<?>> promise = mPendingRequests.get(req_id);
            if (promise == null)
                return;
            if (promise.isCancelled()) {
                mPendingRequests.remove(req_id);
                return;
            }
            try {
                String userStatus = json.getString("UserStatus");
                if (userStatus.equals("1")) {
                    mUserID = json.getString("UserID");
                    mFees = new Fees();
                    ReadFees(json);
                    if (json.has("Broker"))
                        ReadFees(json.getJSONObject("Broker"));
                    if (json.has("Profile"))
                        ReadFees(json.getJSONObject("Profile"));
                    promise.setDone(new ErrorOrPayload<>(ERROR_NONE, new Boolean(true)));
                } else if (userStatus.equals("3")) {
                    String userStatusText = json.getString("UserStatusText");
                    if (userStatusText.equals("MSG_LOGIN_ERROR_INVALID_SECOND_STEP")) {
                        promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_NEED_2FA, false));
                    } else if (userStatusText.equals("MSG_LOGIN_ERROR_INVALID_USERNAME_OR_PASSWORD")) {
                        promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_LOGIN_PASS, false));
                    }
                }
            } catch (JSONException e) {
                promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_UNKNOWN, null));
            }
            mPendingRequests.remove(req_id);
            // Login confirmation
        } else if (msgType.equals("W")) {
            JSONArray values = json.getJSONArray("MDFullGrp");
            for (int i = 0; i < values.length(); i++) {
                JSONObject mdEntry = values.getJSONObject(i);
                long size  = mdEntry.getLong("MDEntrySize");
                double price = mdEntry.getLong("MDEntryPx") / 100000000d;
                boolean is_bid = mdEntry.getString("MDEntryType").equals("0");
                if (is_bid)
                    book_data.book_bids.add(new BookEntry(price, size));
                else
                    book_data.best_ask = new BookEntry(price, size);
            }
        } else if (msgType.equals("X")) {
            JSONArray values = json.getJSONArray("MDIncGrp");
            for (int i = 0; i < values.length(); i++) {
                JSONObject mdEntry = values.getJSONObject(i);
                String action = mdEntry.getString("MDUpdateAction");
                boolean is_bid = mdEntry.getString("MDEntryType").equals("0");
                if (action.equals("2")) {
                    // Delete
                    int pos = Integer.parseInt(mdEntry.getString("MDEntryPositionNo"));
                    if (is_bid) {
                        book_data.book_bids.remove(pos - 1);
                        book_data.SetChanged();
                    } else {
                        book_data.best_ask = null;
                    }
                } else if (action.equals("0")) {
                    // Add
                    int pos = Integer.parseInt(mdEntry.getString("MDEntryPositionNo"));
                    long size  = mdEntry.getLong("MDEntrySize");
                    double price = mdEntry.getLong("MDEntryPx") / 100000000d;
                    if (is_bid) {
                        book_data.book_bids.add(pos - 1, new BookEntry(price, size));
                        book_data.SetChanged();
                    } else {
                        book_data.best_ask = new BookEntry(price, size);
                    }
                }
            }
            book_data.notifyObservers();
        } else if (msgType.equals("U19")) {
            String req_id;
            try {
                req_id = json.getString("DepositReqID");
            } catch (JSONException e) {
                return;
            }
            CustomFuture<ErrorOrPayload<?>> promise = mPendingRequests.get(req_id);
            if (promise == null)
                return;
            try {
                JSONObject depositData = json.getJSONObject("Data");
                promise.setDone(new ErrorOrPayload<>(ERROR_NONE, depositData.getString("InputAddress")));
            } catch (JSONException e) {
                promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_UNKNOWN, null));
            }
            mPendingRequests.remove(req_id);
        } else if (msgType.equals("U23")) {
            // Deposit status update
            try {
                String deposit_id = json.getString("DepositID");
                String state      = json.getString("State");
                double paid_value = json.getInt   ("PaidValue")/100000000.0d;

                String created_at = json.getString("Created");
                SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date d1 = format1.parse(created_at);
                long timestamp = d1.getTime();

                JSONObject depositData = json.getJSONObject("Data");
                if ( ! depositData.has("Confidence"))
                    return;
                String wallet_id = depositData.getString("InputAddress");

                int status = Bill.STATUS_UNCONFIRMED;
                if (state.equals("PROGRESS_CREDIT_GIVEN") || state.equals("CONFIRMED"))
                    status = Bill.STATUS_CONFIRMED;
                else if (state.equals("CANCELLED") || state.equals("PROGRESS_NOT_TRUSTED"))
                    status = Bill.STATUS_CANCELLED;
                else if (state.equals("DOUBLE_SPENT"))
                    status = Bill.STATUS_DOUBLE_SPENT;
                mLastDepositData.SetChanged();
                mLastDepositData.notifyObservers(new Bill.Deposit(wallet_id, deposit_id, timestamp, paid_value, status));
            } catch (JSONException ex) {
            }
        } else if (msgType.equals("U31")) {
            // Deposit list
            String req_id;
            try {
                req_id = json.getString("DepositListReqID");
            } catch (JSONException e) {
                return;
            }
            CustomFuture<ErrorOrPayload<?>> promise = mPendingRequests.get(req_id);
            if (promise == null)
                return;
            if (promise.isCancelled()) {
                mPendingRequests.remove(req_id);
                return;
            }
            try {
                Hashtable<String, ArrayList<Bill.Deposit>> depositsPerBill = new Hashtable<String, ArrayList<Bill.Deposit>>();

                int col_id_paidvalue  = 0;
                int col_id_data       = 0;
                int col_id_state      = 0;
                int col_id_deposit_id = 0;
                int col_id_created_id = 0;
                int col_id_currency   = 0;
                JSONArray columnsArray = json.getJSONArray("Columns");
                for (int col_i=0 ; col_i<columnsArray.length() ; col_i++) {
                    if (columnsArray.get(col_i) instanceof String) {
                        String colName = (String) columnsArray.get(col_i);
                        if (colName.equals("PaidValue"))
                            col_id_paidvalue = col_i;
                        else if (colName.equals("Data"))
                            col_id_data = col_i;
                        else if (colName.equals("State"))
                            col_id_state = col_i;
                        else if (colName.equals("DepositID"))
                            col_id_deposit_id = col_i;
                        else if (colName.equals("Created"))
                            col_id_created_id = col_i;
                        else if (colName.equals("Currency"))
                            col_id_currency = col_i;
                    }
                }
                JSONArray depositsArray = json.getJSONArray("DepositListGrp");
                for (int deposit_i=0 ; deposit_i<depositsArray.length() ; deposit_i++) {
                    JSONArray depositData = (JSONArray) depositsArray.get(deposit_i);
                    if ( ! depositData.getString(col_id_currency).equals("BTC"))
                        continue;
                    String deposit_id = depositData.getString(col_id_deposit_id);
                    double paid_value = depositData.getInt(col_id_paidvalue)/100000000.0;
                    String state = (String) depositData.get(col_id_state);

                    String created_at = (String) depositData.get(col_id_created_id);
                    SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date d1 = format1.parse(created_at);
                    long timestamp = d1.getTime();

                    JSONObject data = (JSONObject) depositData.get(col_id_data);
                    if ( ! data.has("Confidence"))
                        continue;
                    String wallet_id = data.getString("InputAddress");

                    int status = Bill.STATUS_UNCONFIRMED;
                    if (state.equals("PROGRESS_CREDIT_GIVEN") || state.equals("CONFIRMED"))
                        status = Bill.STATUS_CONFIRMED;
                    else if (state.equals("CANCELLED") || state.equals("PROGRESS_NOT_TRUSTED"))
                        status = Bill.STATUS_CANCELLED;
                    else if (state.equals("DOUBLE_SPENT"))
                        status = Bill.STATUS_DOUBLE_SPENT;
                    if (depositsPerBill.containsKey(wallet_id))
                        depositsPerBill.get(wallet_id).add(new Bill.Deposit(wallet_id, deposit_id, timestamp, paid_value, status));
                    else {
                        ArrayList<Bill.Deposit> list = new ArrayList<Bill.Deposit>();
                        list.add(new Bill.Deposit(wallet_id, deposit_id, timestamp, paid_value, status));
                        depositsPerBill.put(wallet_id, list);
                    }
                }
                promise.setDone(new ErrorOrPayload<>(ERROR_NONE, depositsPerBill));
            } catch (JSONException e) {
                promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_UNKNOWN, null));
            }
            mPendingRequests.remove(req_id);
        } else if (msgType.equals("U53")) {
            // API Key creation confirmation
            String req_id;
            try {
                req_id = json.getString("APIKeyCreateReqID");
            } catch (JSONException e) {
                return;
            }
            CustomFuture<ErrorOrPayload<?>> promise = mPendingRequests.get(req_id);
            if (promise == null)
                return;
            if (promise.isCancelled()) {
                mPendingRequests.remove(req_id);
                return;
            }
            try {
                String label  = json.getString("Label").substring("DaPay_".length());
                String APIKey = json.getString("APIKey");
                promise.setDone(new ErrorOrPayload<Profile>(ERROR_NONE, new Profile(0, label, APIKey, 0, Bill.ACTION_TAKE)));
            } catch (JSONException ex) {
            }
        } else if (msgType.equals("8")) {
            // Execution report
            String req_id;
            try {
                req_id = json.getString("ClOrdID");
            } catch (JSONException e) {
                return;
            }
            CustomFuture<ErrorOrPayload<?>> promise = mPendingRequests.get(req_id);
            if (promise == null)
                return;
            if (promise.isCancelled()) {
                // don't delete it, we'll need it latermPendingRequests.remove(req_id);
                return;
            }
            try {
                if (json.has("OrdRejReason")) {
                    String mOrdRejReason = json.getString("OrdRejReason");
                    if (mOrdRejReason.equals("3")) { // insufficient funds
                        mPendingRequests.remove(req_id);
                        promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_INSUFFICIENT_FUNDS, null));
                    } else {
                        mPendingRequests.remove(req_id);
                        promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_UNKNOWN, null));
                    }
                } else {
                    String order_status = json.getString("OrdStatus");
                    if (order_status.equals("2")) {
                        // filled
                        mPendingRequests.remove(req_id);
                        double avgPx = json.getLong("AvgPx") / 100000000.0d;
                        promise.setDone(new ErrorOrPayload<ExecutionReportValues>(
                                ERROR_NONE,
                                new ExecutionReportValues(
                                        avgPx,
                                        Math.round((json.getLong("CumQty") / 1000000.0d) * avgPx) / 100d,
                                        json.getLong("CumQty") / 100000000.0d)));
                    } else if (order_status.equals("1")) {
                        // partially filled
                        // wait for filled or cancel ?
                        // should have sent a FOK in the first place ?
                    }
                }
            } catch (JSONException ex) {
            }
        } else if (msgType.equals("U3")) {
            // Execution report
            String req_id;
            try {
                req_id = json.getString("BalanceReqID");
            } catch (JSONException e) {
                return;
            }
            CustomFuture<ErrorOrPayload<?>> promise = mPendingRequests.get(req_id);
            if (promise == null)
                return;
            if (promise.isCancelled()) {
                mPendingRequests.remove(req_id);
                return;
            }
            try {
                if (json.has(""+GetBrokerID()) && json.getJSONObject(""+GetBrokerID()).has("BTC") ) {
                    double balance = json.getJSONObject(""+GetBrokerID()).getInt("BTC") / 100000000.0d;
                    balance -= json.getJSONObject(""+GetBrokerID()).getInt("BTC_locked") / 100000000.0d;
                    promise.setDone(new ErrorOrPayload<>(ERROR_NONE, new Float(balance)));
                } else {
                    promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_INSUFFICIENT_FUNDS, null));
                }
            } catch (JSONException ex) {
            }
        } else if (msgType.equals("y")) {
            // Security list
//            String req_id;
//            try {
//                req_id = json.getString("SecurityReqID");
//            } catch (JSONException e) {
//                return;
//            }
//            CustomFuture<ErrorOrPayload<?>> promise = mPendingRequests.get(req_id);
//            if (promise == null)
//                return;
//            if (promise.isCancelled()) {
//                mPendingRequests.remove(req_id);
//                return;
//            }
//            try {
                if (json.has("Instruments")) {
                    JSONArray instruments = json.getJSONArray("Instruments");
                    for (int i = 0; i < instruments.length(); i++) {
                        JSONObject instrument = instruments.getJSONObject(i);
                        if (instrument.has("Symbol") && instrument.get("Symbol").equals(GetInstrumentSymbol()) && instrument.has("Currency")) {
                            String currency_str = instrument.getString("Currency");
                            if (json.has("Currencies")) {
                                JSONArray currencies = json.getJSONArray("Currencies");
                                for (int j = 0; i < currencies.length(); j++) {
                                    JSONObject currency = currencies.getJSONObject(j);
                                    if (currency.has("Code") && currency.getString("Code").equals(currency_str) && currency.has("Pip")) {
                                        mIncrement = currency.getLong("Pip") / 100000000.0d;
                                        mCurrencySign = currency.getString("Sign");
//                                        promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_NONE, new Long(currency.getLong("Pip"))));
//                                        promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_NONE, new Long(currency.getLong("Sign"))));
                                    }
                                }
                            }
                        }
                    }
                }
//                } else {
//                    promise.setDone(new ErrorOrPayload<>(ErrorOrPayload.ERROR_UNKNOWN, null));
//                }
//            } catch (JSONException ex) {
//            }
        }
    }

    protected void ReadFees(JSONObject json) {
        try {
            // withdraw
            if (json.has("WithdrawPercentFee")
                    && !json.getString("WithdrawPercentFee").equals("null")) {
                try {
                    mFees.withdrawal_percent = Float.parseFloat(json.getString("WithdrawPercentFee")) / 100;
                } catch (NumberFormatException e) {
                }
            }
            if (json.has("WithdrawFixedFee")
                    && !json.getString("WithdrawFixedFee").equals("null")) {
                try {
                    mFees.withdrawal_fixed = Float.parseFloat(json.getString("WithdrawFixedFee"));
                } catch (NumberFormatException e) {
                }
            }
            // deposit
            if (json.has("DepositPercentFee")
                    && !json.getString("DepositPercentFee").equals("null")) {
                try {
                    mFees.deposit_percent = Float.parseFloat(json.getString("DepositPercentFee")) / 100;
                } catch (NumberFormatException e) {
                }
            }
            if (json.has("DepositFixedFee")
                    && !json.getString("DepositFixedFee").equals("null")) {
                try {
                    mFees.deposit_fixed = Float.parseFloat(json.getString("DepositFixedFee"));
                } catch (NumberFormatException e) {
                }
            }

            // maker
            if (json.has("TransactionFeeSell")
                    && !json.getString("TransactionFeeSell").equals("null")) {
                try {
                    mFees.maker_percent = Float.parseFloat(json.getString("TransactionFeeSell")) / 100;
                } catch (NumberFormatException e) {
                }
            }
            // taker
            if (json.has("TakerTransactionFeeSell")
                    && !json.getString("TakerTransactionFeeSell").equals("null")) {
                try {
                    mFees.taker_percent = Float.parseFloat(json.getString("TakerTransactionFeeSell")) / 100;
                } catch (NumberFormatException e) {
                }
            }
        } catch (JSONException e) {
        }
    }

    public Future<ErrorOrPayload<Boolean>> SignUp(String email, String password, String _2FA, String fingerPrint)
    {
        String request_id = GetNextRequestID();
        CustomFuture result = new CustomFuture<ErrorOrPayload<Boolean>>();
        mPendingRequests.put(request_id, result);

        mConnection.sendJSONText("{\n" +
                "\t\"MsgType\": \"U0\",\n" +
                "\t\"UserReqID\": "+request_id+",\n" +
                "\t\"Username\": \""+email+"\",\n" +
                "\t\"Password\": \""+password+"\",\n" +
                "\t\"Email\": \""+email+"\",\n" +
                "\t\"State\": \"\",\n" +
                "\t\"CountryCode\": \"\",\n" +
                "\t\"BrokerID\": "+this.GetBrokerID()+",\n" +
                "\t\"FingerPrint\": \""+fingerPrint+"\"\n" +
                "}");

        return result;
    }
    public Future<ErrorOrPayload<Boolean>> Login(String login, String password, String _2FA, String fingerPrint) {
        mFingerPrint = fingerPrint;

        String request_id = GetNextRequestID();
        CustomFuture result = new CustomFuture<ErrorOrPayload<Boolean>>();
        mPendingRequests.put(request_id, result);

        sendLogin(login, password, _2FA, request_id);

        return result;
    }

    public void sendLogin(String login, String password, String _2FA, String request_id) {
        mConnection.sendJSONText("{\n" +
                "\t\"MsgType\": \"BE\",\n" +
                "\t\"UserReqID\": "+request_id+",\n" +
                "\t\"BrokerID\": "+GetBrokerID()+",\n" +
                "\t\"Username\": \""+login+"\",\n" +
                "\t\"Password\": \""+password+"\",\n" +
                ((_2FA==null||_2FA.length()==0)?"":"\t\"SecondFactor\": \""+_2FA+"\",\n") +
                "\t\"UserReqTyp\": \"1\",\n" +
                "\t\"FingerPrint\": \""+mFingerPrint+"\",\n" +
                "\t\"UserAgent\": \"DaPay\"\n" +
                "}");
    }

    public void ReadInstrumentDescription() {
        String request_id = GetNextRequestID();
        mConnection.sendJSONText("{\n" +
                "\t\"MsgType\": \"x\",\n" +
                "\t\"SecurityReqID\": "+ request_id +",\n" +
                "\t\"SecurityListRequestType\": 0,\n" +
                "\t\"FingerPrint\": \""+mFingerPrint+"\"\n" +
                "}");
    }

    public void SignUpMarketData(boolean full_bids) {
        // Cancel any other market data requests
        if (mBidsUpdateReqID != null)
            mConnection.sendJSONText("{\n" +
                    "\t\"MsgType\": \"V\",\n" +
                    "\t\"MDReqID\": "+ mBidsUpdateReqID +",\n" +
                    "\t\"MarketDepth\": 0,\n" +
                    "\t\"SubscriptionRequestType\": \"2\",\n" +
                    "\t\"FingerPrint\": \""+mFingerPrint+"\"\n" +
                    "}");
        if (mAsksUpdateReqID != null)
            mConnection.sendJSONText("{\n" +
                    "\t\"MsgType\": \"V\",\n" +
                    "\t\"MDReqID\": "+ mAsksUpdateReqID +",\n" +
                    "\t\"MarketDepth\": 0,\n" +
                    "\t\"SubscriptionRequestType\": \"2\",\n" +
                    "\t\"FingerPrint\": \""+mFingerPrint+"\"\n" +
                    "}");

        book_data.book_bids.clear();

        // Make a new market data request
        mBidsUpdateReqID = GetNextRequestID();
        mConnection.sendJSONText("{\n" +
                "\t\"MsgType\": \"V\",\n" +
                "\t\"MDReqID\": "+ mBidsUpdateReqID +",\n" +
                "\t\"SubscriptionRequestType\": \"1\",\n" +
                "\t\"MarketDepth\": "+ (full_bids?100:1) +",\n" +
                "\t\"MDUpdateType\": \"0\",\n" +
                "\t\"MDEntryTypes\": [\n" +
                "\t\t\"0\"\n" +
                "\t],\n" +
                "\t\"Instruments\": [\n" +
                "\t\t\""+ GetInstrumentSymbol() +"\"\n" +
                "\t],\n" +
                "\t\"FingerPrint\": \""+mFingerPrint+"\"\n" +
                "}");

        // Make a new market data request
        mAsksUpdateReqID = GetNextRequestID();
        mConnection.sendJSONText("{\n" +
                "\t\"MsgType\": \"V\",\n" +
                "\t\"MDReqID\": "+ mAsksUpdateReqID +",\n" +
                "\t\"SubscriptionRequestType\": \"1\",\n" +
                "\t\"MarketDepth\": 1,\n" +
                "\t\"MDUpdateType\": \"0\",\n" +
                "\t\"MDEntryTypes\": [\n" +
                "\t\t\"1\"\n" +
                "\t],\n" +
                "\t\"Instruments\": [\n" +
                "\t\t\""+ GetInstrumentSymbol() +"\"\n" +
                "\t],\n" +
                "\t\"FingerPrint\": \""+mFingerPrint+"\"\n" +
                "}");
    }

    public Future<ErrorOrPayload<String>> GetDepositAddress() {
        String request_id = GetNextRequestID();
        CustomFuture result = new CustomFuture<ErrorOrPayload<String>>();
        mPendingRequests.put(request_id, result);

        mConnection.sendJSONText("{\n" +
                "    \"MsgType\": \"U18\",\n" +
                "    \"DepositReqID\": " + request_id + ",\n" +
                "    \"Currency\": \"BTC\",\n" +
                "    \"FingerPrint\": \"" + mFingerPrint + "\"\n" +
                "}");

        return result;
    }

    protected Future<ErrorOrPayload<ExecutionReportValues>> PlaceTakeOrder(double crypto_amount) {
        String request_id = GetNextRequestID();
        CustomFuture result = new CustomFuture<ErrorOrPayload<ExecutionReportValues>>();
        mPendingRequests.put(request_id, result);

        mConnection.sendJSONText("{\n" +
                "\t\"MsgType\": \"D\",\n" +
                "\t\"ClOrdID\": \"" + request_id + "\",\n" +
                "\t\"Symbol\": \""+ GetInstrumentSymbol() +"\",\n" +
                "\t\"Side\": \"2\",\n" + // 2 = Sell
                "\t\"OrdType\": \"1\",\n" + // 1 = Market
                "\t\"OrderQty\": " + (int) (100000000 * crypto_amount) + ",\n" +
                "\t\"ClientID\": " + mUserID + ",\n" +
                "\t\"FingerPrint\": \"" + mFingerPrint + "\"\n" +
                "}");

        return result;
    }

    protected Future<ErrorOrPayload<ExecutionReportValues>> PlaceMakeOrder(double crypto_amount, double price) {
        String request_id = GetNextRequestID();
        CustomFuture result = new CustomFuture<ErrorOrPayload<ExecutionReportValues>>();
        mPendingRequests.put(request_id, result);

        mConnection.sendJSONText("{\n" +
                "\t\"MsgType\": \"D\",\n" +
                "\t\"ClOrdID\": \"" + request_id + "\",\n" +
                "\t\"Symbol\": \""+ GetInstrumentSymbol() +"\",\n" +
                "\t\"Side\": \"2\",\n" + // 2 = Sell
                "\t\"OrdType\": \"2\",\n" + // 1 = Limit
                "\t\"Price\": "+(long)(price * 100000000)+",\n" +
                "\t\"OrderQty\": " + (long) (100000000 * crypto_amount) + ",\n" +
                "\t\"ClientID\": " + mUserID + ",\n" +
                "\t\"FingerPrint\": \"" + mFingerPrint + "\"\n" +
                "}");

        return result;
    }

    public double GetBestBidPrice() {
        if (book_data.book_bids.size() == 0)
            return 0.0;
        else
            return book_data.book_bids.get(0).price;
    }
    public double GetBestAskPrice() {
        if (book_data.best_ask == null)
            return 0.0;
        else
            return book_data.best_ask.price;
    }

    public ErrorOrPayload<Double> Convert(double fiat_amount) {
        double finalCryptoAmount = 0.0d;
        double boughtFiatAmount = 0.0d;

        while (boughtFiatAmount < fiat_amount) {
            double remaining_crypto_amount = 0.0d;
            try {
                remaining_crypto_amount = GetQuotation(fiat_amount - boughtFiatAmount)[0];
            } catch (ArithmeticException e){
            }
            if (remaining_crypto_amount == 0.0d) {
                if (boughtFiatAmount == 0)
                    return new ErrorOrPayload<Double>(ErrorOrPayload.ERROR_NO_BUYER, null);
                else
                    return new ErrorOrPayload<Double>(ErrorOrPayload.ERROR_NOT_ENOUGH_BUYERS, null);
            }
            Future<ErrorOrPayload<ExecutionReportValues>> selling = PlaceTakeOrder(remaining_crypto_amount);
            try {
                ErrorOrPayload<ExecutionReportValues> selling_result = selling.get(20, TimeUnit.SECONDS);
                if (selling_result.getErrCode() == ERROR_NONE) {
                    finalCryptoAmount += selling_result.getPayload().crypto_amount;
                    boughtFiatAmount  += selling_result.getPayload().fiat_amount;
                } else if (selling_result.getErrCode() == ErrorOrPayload.ERROR_INSUFFICIENT_FUNDS) {
                    return new ErrorOrPayload<Double>(ErrorOrPayload.ERROR_INSUFFICIENT_FUNDS, null);
                }
            } catch (TimeoutException t) {
                return new ErrorOrPayload<Double>(ErrorOrPayload.ERROR_TIMEOUT, null);
            } catch (Throwable t) {
                return new ErrorOrPayload<Double>(ErrorOrPayload.ERROR_UNKNOWN, null);
            }
        }
        if (boughtFiatAmount == fiat_amount)
            return new ErrorOrPayload<Double>(ERROR_NONE, new Double(finalCryptoAmount));
        else
            // we bought more than needed
            return new ErrorOrPayload<Double>(ERROR_NONE, new Double(finalCryptoAmount * (boughtFiatAmount / fiat_amount)));
    }

    public ErrorOrPayload<Double> TakeMarket(double crypto_amount) {
        Future<ErrorOrPayload<ExecutionReportValues>> selling = PlaceTakeOrder(crypto_amount);
        try {
            ErrorOrPayload<ExecutionReportValues> selling_result = selling.get(20, TimeUnit.SECONDS);
            if (selling_result.getErrCode() == ErrorOrPayload.ERROR_INSUFFICIENT_FUNDS)
                return new ErrorOrPayload<Double>(ErrorOrPayload.ERROR_INSUFFICIENT_FUNDS, null);
        } catch (TimeoutException t) {
            return new ErrorOrPayload<Double>(ErrorOrPayload.ERROR_TIMEOUT, null);
        } catch (Throwable t) {
            return new ErrorOrPayload<Double>(ErrorOrPayload.ERROR_UNKNOWN, null);
        }
        return new ErrorOrPayload<Double>(ERROR_NONE, new Double(crypto_amount));
    }

    public ErrorOrPayload<Double> MakeMarket(double crypto_amount) {
        double price = GetMakerPrice();
        if (price == 0.0d)
            return new ErrorOrPayload<Double>(ERROR_NO_BUYER, new Double(0.0d));

        Future<ErrorOrPayload<ExecutionReportValues>> selling = PlaceMakeOrder(crypto_amount, price);

        try {
            ErrorOrPayload<ExecutionReportValues> selling_result = selling.get(20, TimeUnit.SECONDS);
            if (selling_result.getErrCode() == ErrorOrPayload.ERROR_INSUFFICIENT_FUNDS)
                return new ErrorOrPayload<Double>(ErrorOrPayload.ERROR_INSUFFICIENT_FUNDS, null);
        } catch (TimeoutException t) {
            return new ErrorOrPayload<Double>(ErrorOrPayload.ERROR_TIMEOUT, null);
        } catch (Throwable t) {
            return new ErrorOrPayload<Double>(ErrorOrPayload.ERROR_UNKNOWN, null);
        }
        return new ErrorOrPayload<Double>(ERROR_NONE, new Double(crypto_amount));
    }

    public Future<ErrorOrPayload<Hashtable<String, ArrayList<Bill.Deposit>>>> GetDepositStatus() {
        String request_id = GetNextRequestID();
        CustomFuture result = new CustomFuture<ErrorOrPayload<Hashtable<String, ArrayList<Bill.Deposit>>>>();
        mPendingRequests.put(request_id, result);

//        String filters = "";
//        for (int i=0 ; i<bills.size() ; i++) {
//            Bill d = bills.get(i);
//            filters += "\t\t" + String.format("%d", (int)(d.getCryptoAmount()*100000000)) + "";
//            if (i < bills.size() - 1)
//                filters += ",\n";
//        }
        mConnection.sendJSONText("{\n" +
                "\t\"MsgType\": \"U30\",\n" +
                "\t\"DepositListReqID\": "+ request_id +",\n" +
                "\t\"Page\": 0,\n" +
                "\t\"PageSize\": 100,\n" +
                "\t\"StatusList\": [\n" +
                "\t\t\"0\",\n" +
                "\t\t\"1\",\n" +
                "\t\t\"2\",\n" +
                "\t\t\"4\",\n" +
                "\t\t\"8\"\n" +
                "\t], \n" +
//                "\t\"Filter\": [\n" +
//                filters + "\n" +
//                "\t],\n" +
                "\t\"FingerPrint\": \""+mFingerPrint+"\"\n" +
                "}");

        return result;
    }

    public void GetDepositUpdates(Observer o) {
        mLastDepositData.addObserver(o);
    }


    public boolean SendCreateAPIKey(String request_id, String label, String password)
    {
        // Create API Key
        label = "DaPay_" + label;
        return mConnection.sendJSONText(
                "{\n" +
                "   \"MsgType\": \"U52\",\n" +
                "   \"APIKeyCreateReqID\": " + request_id + ",\n" +
                "   \"Label\": \"" + label + "\",\n" +
                (password!=null?("   \"APIPassword\": \"" + password + "\",\n"):"") +
                "   \"PermissionList\": {\n" +
                "      \"U2\": [],\n" +
                "      \"D\": [[\n" +
                "            \"Side\",\n" +
                "            \"eq\",\n" +
                "            \"2\",\n" +
                "            \"OrdType\",\n" +
                "            \"eq\",\n" +
                "            \"2\"],\n" +
                "         [\"Side\",\n" +
                "            \"eq\",\n" +
                "            \"2\",\n" +
                "            \"OrdType\",\n" +
                "            \"eq\",\n" +
                "            \"1\"]\n" +
                "      ],\n" +
                "      \"F\": [],\n" +
                "      \"8\": [],\n" +
                "      \"U23\": [],\n" +
                "      \"U30\": [],\n" +
                "      \"U18\": [[\n" +
                "            \"Currency\",\n" +
                "            \"eq\",\n" +
                "            \"BTC\"]]\n" +
//                "      \"U6\": [[\n" +
//                "            \"Method\",\n" +
//                "            \"eq\",\n" +
//                "            \"bitcoin\"]]\n" +
                "   },\n" +
                "   \"IPWhiteList\": [],\n" +
                "   \"Revocable\": true,\n" +
                "   \"FingerPrint\": \"" + mFingerPrint + "\"\n" +
                "}");
    }

    public Future<ErrorOrPayload<Profile>> CreateProfile(String label, String password) {
        String request_id = GetNextRequestID();
        CustomFuture result = new CustomFuture<ErrorOrPayload<Profile>>();
        mPendingRequests.put(request_id, result);

        if ( ! SendCreateAPIKey(request_id, label, password)) {
            CustomFuture<ErrorOrPayload<?>> promise = mPendingRequests.remove(request_id);
            if (promise != null) {
                promise.setDone(new ErrorOrPayload<Profile>(ErrorOrPayload.ERROR_UNKNOWN, null));
            }
        }

        return result;
    }


    public Future<ErrorOrPayload<Float>> GetCryptoBalance() {
        String request_id = GetNextRequestID();
        CustomFuture result = new CustomFuture<ErrorOrPayload<Float>>();
        mPendingRequests.put(request_id, result);

        mConnection.sendJSONText("{\n" +
                "\t\"MsgType\": \"U2\",\n" +
                "\t\"BalanceReqID\": " + request_id + ",\n" +
                "\t\"FingerPrint\": \"" + mFingerPrint + "\"\n" +
                "}");

        return result;

    }
}
