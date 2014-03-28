package fr.xtof54.jsgo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.testbrowser.R;

public class Message {
    private static GoJsActivity c;
    private static JSONArray headers, jsonmsgs;
    private static int curmsg=0;
    
    public int getMessageId() {return msgid;}
    
    public static void send() {
        // TODO
    }
    
    public static void handleMessages(GoJsActivity main) throws Exception {
        c=main;
        HttpGet httpget = new HttpGet(main.server+"quick_do.php?obj=message&cmd=list&filter_folders=2");
        HttpResponse response = main.httpclient.execute(httpget);
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
                if (s.length()>0 && s.charAt(0)=='{') {
System.out.println("gotanswerjson: "+s);
                    JSONObject o = new JSONObject(s);
                    System.out.println("debugMsgJson "+o.getInt("list_size"));
                    if (o.getInt("list_size")>0) {
                        headers = o.getJSONArray("list_header");
                        jsonmsgs = o.getJSONArray("list_result");
                        curmsg=0;
                        showNextMsg();
                    }
                }
                System.out.println("LOGINstatus "+s);
            }
            fin.close();
        }
    }
    private static void showNextMsg() {
        if (curmsg>=jsonmsgs.length()) return;
        JSONArray jsonmsg = jsonmsgs.getJSONArray(curmsg);
        Message.newMessage(headers, jsonmsg);
        curmsg++;
    }
    
    // ==================================================================
    
    private static ArrayList<Message> msgs = new ArrayList<Message>();
    
    private static void newMessage(JSONArray headers, JSONArray jsonmsg) {
        int msgididx=-1, fromididx=-1, typeidx=-1, subjectidx=-1, txtidx=-1;
        for (int i=0;i<headers.length();i++) {
            String h = headers.getString(i);
            System.out.println("jsonheader "+i+" "+h);
            if (h.equals("id")) msgididx=i;
            else if (h.equals("user_from.id")) fromididx=i;
            else if (h.equals("type")) typeidx=i;
            else if (h.equals("text")) txtidx=i;
            else if (h.equals("subject")) subjectidx=i;
        }
        Message m = new Message();
        msgs.add(m);
        m.msgid = jsonmsg.getInt(msgididx);
        m.type = jsonmsg.getString(typeidx);
        m.fromid = jsonmsg.getInt(fromididx);
        m.subject = jsonmsg.getString(subjectidx);
        m.text = jsonmsg.getString(txtidx);
        m.show();
    }
    
    // ==================================================================
    private int msgid, fromid;
    private String type, subject, text;
    
    private Message() {}
    private void show() {
    	final Message mm = this;
        class MsgDialogFragment extends DialogFragment {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                // Get the layout inflater
                LayoutInflater inflater = getActivity().getLayoutInflater();

                // Inflate and set the layout for the dialog
                // Pass null as the parent view because it's going in the dialog layout
                View msgview = inflater.inflate(R.layout.message, null);
                TextView t = (TextView)msgview.findViewById(R.id.msgLabel);
                t.setMovementMethod(new ScrollingMovementMethod());
                String s = "You have the following message:\n";
                s+= "From: "+fromid+"\n";
                s+= "subject: "+subject+"\n";
                s+= "text: "+text+"\n";
                t.setText(s);
                builder.setView(msgview);

                if (type.equals("INVITATION")) {
                    builder.setPositiveButton("skip", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MsgDialogFragment.this.getDialog().dismiss();
                            showNextMsg();
                        }
                    })
                    .setNegativeButton("decline", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MsgDialogFragment.this.getDialog().dismiss();
                            HttpGet httpget = new HttpGet(c.server+"quick_do.php?obj=message&cmd=decline_inv&mid="+mm.getMessageId());
                            HttpResponse response;
    						try {
    							response = c.httpclient.execute(httpget);
    							System.out.println("invitation declined:");
    	                        Header[] heds = response.getAllHeaders();
    	                        for (Header s : heds)
    	                            System.out.println("[HEADER] "+s);
    						} catch (ClientProtocolException e) {
    							e.printStackTrace();
    						} catch (IOException e) {
    							e.printStackTrace();
    						}
                            showNextMsg();
                        }
                    });
                	builder.setNeutralButton("accept", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MsgDialogFragment.this.getDialog().dismiss();
                            HttpGet httpget = new HttpGet(c.server+"quick_do.php?obj=message&cmd=accept_inv&mid="+mm.getMessageId());
                            HttpResponse response;
    						try {
    							response = c.httpclient.execute(httpget);
    							System.out.println("invitation accepted:");
    	                        Header[] heds = response.getAllHeaders();
    	                        for (Header s : heds)
    	                            System.out.println("[HEADER] "+s);
    						} catch (ClientProtocolException e) {
    							e.printStackTrace();
    						} catch (IOException e) {
    							e.printStackTrace();
    						}
                            showNextMsg();
                        }
                    });
                } else {
                    builder.setPositiveButton("skip", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MsgDialogFragment.this.getDialog().dismiss();
                            showNextMsg();
                        }
                    })
                    .setNegativeButton("mark as read", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MsgDialogFragment.this.getDialog().dismiss();
                            HttpGet httpget = new HttpGet(c.server+"quick_do.php?obj=message&cmd=move_msg&mid="+mm.getMessageId()+"&folder=1");
                            HttpResponse response;
    						try {
    							response = c.httpclient.execute(httpget);
    							System.out.println("message marked as read:");
    	                        Header[] heds = response.getAllHeaders();
    	                        for (Header s : heds)
    	                            System.out.println("[HEADER] "+s);
    						} catch (ClientProtocolException e) {
    							e.printStackTrace();
    						} catch (IOException e) {
    							e.printStackTrace();
    						}
                            showNextMsg();
                        }
                    });
                }
                return builder.create();
            }
        }
        final MsgDialogFragment msgdialog = new MsgDialogFragment();
        msgdialog.show(c.getSupportFragmentManager(),"message");
    }
}
