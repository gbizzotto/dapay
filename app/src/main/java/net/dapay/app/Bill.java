package net.dapay.app;

import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.Observable;

/**
 * Created by gabriel on 12/7/16.
 */

public class Bill extends Observable
{
    public static final int STATUS_UNCONFIRMED  = 0;
    public static final int STATUS_CONFIRMED    = 1;
    public static final int STATUS_CANCELLED    = 2;
    public static final int STATUS_DOUBLE_SPENT = 3;

    public static final String ACTION_PRE_SELL = "presell";
    public static final String ACTION_TAKE     = "take";
    public static final String ACTION_MAKE     = "make";
    public static final String ACTION_HOLD     = "hold";

    public static class Deposit {
        String wallet_id;
        String id;
        long timestamp;
        double crypto_amount;
        int status;
        public Deposit(String _w, String _id, long _ts, double _ca, int _status) {
            wallet_id     = _w;
            id            = _id;
            timestamp     = _ts;
            crypto_amount = _ca;
            status        = _status;
        }
        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (!(obj instanceof Deposit))
                return false;
            Deposit other = (Deposit) obj;
            return crypto_amount == other.crypto_amount &&
                    status == other.status;
        }
    }

    private long id;
    private long profile_id;
    private long timestamp;
    private String wallet_id;
    private double fiat_amount;
    private double cyrpto_amount;
    private String label;
    private String action;
    private Hashtable<String, Deposit> deposits;

    public Bill() {}
    public Bill(long _timestamp, long _profile_id, String _wallet_id, double _fiat_amount, double _crypto_amount, String _label, String _action) {
        timestamp = _timestamp;
        profile_id = _profile_id;
        wallet_id = _wallet_id;
        fiat_amount = _fiat_amount;

        // trunk to 8 decimal places
        BigDecimal bd = new BigDecimal(_crypto_amount);
        bd = bd.setScale(8, BigDecimal.ROUND_HALF_DOWN);
        cyrpto_amount = bd.doubleValue();

        label = _label;
        action        = _action;
        deposits = new Hashtable<String, Deposit>();
    }
    public Bill(long _id, long _timestamp, long _profile_id, String _wallet_id, double _fiat_amount, double _crypto_amount, String _label, String _action) {
        this(_timestamp, _profile_id, _wallet_id, _fiat_amount, _crypto_amount, _label, _action);
        id = _id;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long t) {
        this.timestamp = t;
    }
    public String getWalletID() {
        return wallet_id;
    }
    public void setWalletID(String w) {
        wallet_id = w;
    }
    public double getFiatAmount() {
        return fiat_amount;
    }
    public void setFiatAmount(double a) {
        this.fiat_amount = a;
    }
    public double getCryptoAmount() {
        return cyrpto_amount;
    }
    public void setCryptoAmount(double a) {
        this.cyrpto_amount = a;
    }
    public String getAction() { return action; }
    public void setAction(String a) { action = a; }
    public Hashtable<String, Deposit> getDeposits() {
        return deposits;
    }
    public double getAmountDeposited() {
        double total = 0.0d;
        for (Deposit d: deposits.values())
            if (d.status == STATUS_CONFIRMED)
                total += d.crypto_amount;
        return total;
    }
    public double getAmountDepositedAndToBeDeposited() {
        double total = 0.0d;
        for (Deposit d: deposits.values())
            if (d.status == STATUS_UNCONFIRMED || d.status == STATUS_CONFIRMED)
                total += d.crypto_amount;
        return total;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String l) {
        this.label = l;
    }
    public int getStatus() {
        double amount_deposited = getAmountDeposited();
        double amount_to_be_deposited = getAmountDepositedAndToBeDeposited();
        if (amount_deposited >= getCryptoAmount()-0.00000010)
            return STATUS_CONFIRMED;
        else if ((amount_deposited == 0.0d && amount_to_be_deposited == 0.0d)
                || (amount_to_be_deposited >= amount_deposited && amount_to_be_deposited >= getCryptoAmount()-0.00000010))
            return STATUS_UNCONFIRMED;
        else
            return STATUS_CANCELLED;
    }

    public boolean InsertDeposit(Deposit d, boolean monitorForFraud) {
        if (deposits.containsKey(d.id))
            return false;
        deposits.put(d.id, d);
        if (monitorForFraud) {
            if (d.status == STATUS_DOUBLE_SPENT) {
                Notifier notifier = Notifier.getInstance();
                if (notifier != null)
                    notifier.NotifyUser(this, R.string.notification_fraud_detection, R.string.notification_double_spend_attack);
            } else if (this.getAmountDepositedAndToBeDeposited() < this.getCryptoAmount()-0.00000010) {
                Notifier notifier = Notifier.getInstance();
                if (notifier != null)
                    notifier.NotifyUser(this, R.string.notification_fraud_detection, R.string.notification_insufficient_payment);
            }
        }
        setChanged();
        notifyObservers();
        return true;
    }

    /**
     * @return true if the deposit was inserted or altered and need to be committed to the database.
     */
    public boolean UpdateDeposit(Deposit d, boolean monitorForFraud) {
        boolean dirty = (!deposits.get(d.id).equals(d));
        if (dirty) {
            deposits.put(d.id, d);
            if (monitorForFraud) {
                if (d.status == STATUS_DOUBLE_SPENT) {
                    Notifier notifier = Notifier.getInstance();
                    if (notifier != null)
                        notifier.NotifyUser(this, R.string.notification_fraud_detection, R.string.notification_double_spend_attack);
                } else if (this.getAmountDepositedAndToBeDeposited() < this.getCryptoAmount()-0.00000010) {
                    Notifier notifier = Notifier.getInstance();
                    if (notifier != null)
                        notifier.NotifyUser(this, R.string.notification_fraud_detection, R.string.notification_insufficient_payment     );
                }
            }
            setChanged();
            notifyObservers();
        }
        return dirty;
    }
}
