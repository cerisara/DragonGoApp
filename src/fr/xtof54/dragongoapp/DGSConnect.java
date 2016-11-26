package fr.xtof54.dragongoapp;

import org.apache.http.HttpEntity;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import org.apache.http.client.HttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpResponse;
import org.apache.http.Header;

public class DGSConnect {
    public String error="no error";
    private String server = "http://www.dragongoserver.net/";
    private HttpClient httpclient=null;
    
    // must be called from within an Async task
    public boolean connect() {
        boolean loginok=true;
        error="no error";
        System.out.println("start login run");
        HttpParams httpparms = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpparms, 6000);
        HttpConnectionParams.setSoTimeout(httpparms, 6000);
        httpclient = new DefaultHttpClient(httpparms);
        try {
            try {
                // just an anonymous ping for usage stats
                // TODO: use it to check for updates + if enough stats, direct connection with the opponent app for live game
                String cmd = "http://talc1.loria.fr/users/cerisara/DGSping.php?v=2";
                System.out.println("dgsserver: login cmd "+cmd);
                HttpGet httpget = new HttpGet(cmd);
                httpclient.execute(httpget);
            } catch (Exception e) {
            }
            // login
            String u=DragonGoAct.main.login;
            String p=DragonGoAct.main.password;
            String cmd = server+"login.php?quick_mode=1&userid="+u+"&passwd="+p;
            System.out.println("debug login cmd "+cmd);
            HttpGet httpget = new HttpGet(cmd);
            HttpResponse response = httpclient.execute(httpget);
            Header[] heds = response.getAllHeaders();
            for (Header s : heds) System.out.println("[HEADER] "+s);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                BufferedReader fin = new BufferedReader(new InputStreamReader(instream, Charset.forName("UTF-8")));
                for (;;) {
                    String s = fin.readLine();
                    if (s==null) break;
                    System.out.println("LOGINlog "+s);
                    if (s.contains("#Error")) {
                        System.out.println("Error login; check credentials");
                        error=s;
                        loginok=false;
                        break;
                    }
                }
                fin.close();
            } else {
                loginok=false;
                error="no valid server response";
            }
        } catch (Exception e) {
            e.printStackTrace();
            //				    if (s.contains("#Error")) logger.showMsg("Error login; check credentials");
            System.out.println("ERROR serverdgs: "+e);
            error=e.toString();
            loginok=false;
        }
        System.out.println("end login run "+loginok);

        return loginok;
    }

    public boolean sendmove(Game g, String move) {
        if (move.toLowerCase().startsWith("tt")) move="pass";
        System.out.println("sending move "+move+" "+g.oppmoveid+" "+g.id);
        try {
            final String cmd = server+"quick_do.php?obj=game&cmd=move&gid=" + g.id + "&move_id=" + g.oppmoveid + "&move=" + move;
            HttpGet httpget = new HttpGet(cmd);
            HttpResponse response = httpclient.execute(httpget);
            Header[] heds = response.getAllHeaders();
            for (Header s : heds)
                System.out.println("[HEADER] "+s);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                BufferedReader fin = new BufferedReader(new InputStreamReader(instream, Charset.forName("UTF-8")));
                for (;;) {
                    String s = fin.readLine();
                    if (s==null) break;
                    s=s.trim();
                    System.out.println("SendMovelog "+s);
                }
                fin.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            error="sendmove error";
            return false;
        }
        return true;
    }

    public ArrayList<String> downloadSGF(int gameid) {
        error="no error";
        try {
            ArrayList<String> sgf = new ArrayList<String>();
            final String cmd = server+"sgf.php?gid="+gameid+"&owned_comments=1&quick_mode=1";
            HttpGet httpget = new HttpGet(cmd);
            HttpResponse response = httpclient.execute(httpget);
            Header[] heds = response.getAllHeaders();
            for (Header s : heds)
                System.out.println("[HEADER] "+s);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                BufferedReader fin = new BufferedReader(new InputStreamReader(instream, Charset.forName("UTF-8")));
                for (;;) {
                    String s = fin.readLine();
                    if (s==null) break;
                    s=s.trim();
                    if (s.length()>0&&s.charAt(0)!='[') {
                        sgf.add(s);
                    }
                    System.out.println("SGFdownload "+s);
                }
                fin.close();
            }
            return sgf;
        } catch (Exception e) {
            e.printStackTrace();
            error="download SGF error";
            return null;
        }
    }

    public ArrayList<Game> downloadMessagesList() {
        try {
            String cmd = server+"quick_do.php?obj=message&cmd=list&filter_folders=2&with=user_id";
            System.out.println("debug send cmd "+cmd);
            HttpGet httpget = new HttpGet(cmd);
            HttpResponse response = httpclient.execute(httpget);
            Header[] heds = response.getAllHeaders();
            for (Header s : heds)
                System.out.println("[HEADER] "+s);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                BufferedReader fin = new BufferedReader(new InputStreamReader(instream, Charset.forName("UTF-8")));
                for (;;) {
                    String s = fin.readLine();
                    if (s==null) break;
                    s=s.trim();
                    System.out.println("cmdlog "+s);
                }
                fin.close();
                error="error get messages";
            } else {
                error="no server reply";
            }
        } catch (Exception e) {
            e.printStackTrace();
            error=e.toString();
        }
        return null;
    }

    public ArrayList<Game> downloadGamesList() {
        String cmd = server+"quick_do.php?obj=game&cmd=list&view=status";
        boolean hasError = false;
        error="no error";
        try {
            System.out.println("debug send cmd "+cmd);
            HttpGet httpget = new HttpGet(cmd);
            HttpResponse response = httpclient.execute(httpget);
            Header[] heds = response.getAllHeaders();
            for (Header s : heds)
                System.out.println("[HEADER] "+s);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                BufferedReader fin = new BufferedReader(new InputStreamReader(instream, Charset.forName("UTF-8")));
                for (;;) {
                    String s = fin.readLine();
                    if (s==null) break;
                    System.out.println("cmdlog "+s);
                    s=s.trim();
                    if (s.length()>0 && s.charAt(0)=='{') {
                        return Game.loadJSONStatus(s);
                    }
                }
                System.out.println("ZARBI on ne devrait pas arriver ici");
                fin.close();
                hasError=true;
                error="error unk nogame ?";
            } else {
                // entity==null
                hasError=true;
                error="no server reply";
            }
        } catch (Exception e) {
            e.printStackTrace();
            hasError=true;
            error=e.toString();
        }
        System.out.println("server runnable terminated");
        return null;
    }
}

