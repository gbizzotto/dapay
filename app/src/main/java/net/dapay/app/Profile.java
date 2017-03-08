package net.dapay.app;

/**
 * Created by gabriel on 1/9/17.
 */

public class Profile {
    public int id;
    public String label;
    public String login;
    public int broker_id;
    public String action;

    public Profile(int _id, String _label, String _login, int _broker_id, String _action){
        id = _id;
        label = _label;
        login = _login;
        broker_id = _broker_id;
        action = _action;
    }

    public static Profile currentProfile;
}
