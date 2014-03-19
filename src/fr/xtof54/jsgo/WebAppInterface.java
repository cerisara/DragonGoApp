package fr.xtof54.jsgo;

import android.content.Context;
import android.widget.Toast;

public class WebAppInterface {
    Context mContext;

    /** Instantiate the interface and set the context */
    WebAppInterface(Context c) {
        mContext = c;
    }

    /** Show a toast from the web page */
//    @JavascriptInterface
    public void sendSgf1() {
//    	Toast.makeText(mContext, "sent SGF1 ", Toast.LENGTH_SHORT).show();
    }
    public void sendSgf2(Object sgf, Object selectedMove) {
    	Toast.makeText(mContext, "sent SGF2 ", Toast.LENGTH_SHORT).show();
    }
//    public void sendSgf2(String sgf, String selectedMove) {
//        Toast.makeText(mContext, "sent SGF "+selectedMove, Toast.LENGTH_SHORT).show();
//    }
}
