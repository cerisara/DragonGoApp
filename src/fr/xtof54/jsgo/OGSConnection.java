package fr.xtof54.jsgo;

import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xtof2 on 04/06/15.
 */
public class OGSConnection {

    public static void login() {
        final String OGSCLIENTID = "todo: how to save it outside git ?";
        final String OGSCLIENTSECRET = "todo";

        final String pwd = PrefUtils.getFromPrefs(GoJsActivity.main.getApplicationContext(),PrefUtils.PREFS_LOGIN_OGS_PASSWD,null);
        if (pwd==null) GoJsActivity.main.showMessage("You must first create an application-specific password on online-go.com," +
                "and enter this password in this app Settings menu");
        else {
            final String user = PrefUtils.getFromPrefs(GoJsActivity.main.getApplicationContext(),PrefUtils.PREFS_LOGIN_OGS_USERNAME,null);
            if (user==null) GoJsActivity.main.showMessage("You must first enter your OGS credentials via the Settings menu");
            else {
                System.out.println("all info is here:");
                System.out.println("OGSuser "+user);
                System.out.println("OGSpwd "+pwd);
                Thread push = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("login to OGS server");
                        HttpParams httpparms = new BasicHttpParams();
                        HttpConnectionParams.setConnectionTimeout(httpparms, 6000);
                        HttpConnectionParams.setSoTimeout(httpparms, 6000);
                        HttpClient httpclient = new DefaultHttpClient(httpparms);
                        try {
                            String cmd = "https://online-go.com/oauth2/access_token";

                            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
                            formparams.add(new BasicNameValuePair("client_id", OGSCLIENTID));
                            formparams.add(new BasicNameValuePair("client_secret", OGSCLIENTSECRET));
                            formparams.add(new BasicNameValuePair("grant_type", "password"));
                            formparams.add(new BasicNameValuePair("username", user));
                            formparams.add(new BasicNameValuePair("password", pwd));
                            UrlEncodedFormEntity entity;
                            entity = new UrlEncodedFormEntity(formparams, "UTF-8");
                            HttpPost httppost = new HttpPost(cmd);
                            httppost.setEntity(entity);
                            HttpResponse response = httpclient.execute(httppost);

                            // retrieve the access token from the response
                            BufferedReader fin = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
                            for (; ; ) {
                                String s = fin.readLine();
                                if (s==null) break;
                                System.out.println("ogslog "+s);
                                s=s.trim();
                                if (s.indexOf("error")>=0) {
                                    GoJsActivity.main.showMessage("ERROR login OGS "+s);
                                } else {
                                    // TODO retrieve the token
                                }
                            }
                            fin.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                push.start();

            }
        }
    }

    private static void askUserForOGScredentials() {

    }
}
