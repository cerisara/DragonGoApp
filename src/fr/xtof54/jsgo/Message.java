package fr.xtof54.jsgo;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
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

import fr.xtof54.jsgo.EventManager.eventType;

public class Message {
	private final static String cmdGetListOfMessages = "quick_do.php?obj=message&cmd=list&filter_folders=2";
	
	private static GoJsActivity c;
	private static JSONArray headers, jsonmsgs;
	private static int curmsg=0;
	private static ArrayList<Message> messages = new ArrayList<Message>();

	public int getMessageId() {return msgid;}

	public static void send() {
		String cmd = "quick_do.php?obj=message&cmd=send_msg&ouser=xtof54&msg=salut&subj=test";
		// TODO
	}

	public static void downloadMessages(final ServerConnection server, GoJsActivity main) {
    	c=main;
		messages.clear();
		final EventManager em = EventManager.getEventManager();
		EventManager.EventListener f = new EventManager.EventListener() {
            @Override
            public String getName() {return "downloadMessages";}
			@Override
			public synchronized void reactToEvent() {
				JSONObject o = server.o;
				if (o==null) return;
				try {
					if (o.getInt("list_size")>0) {
						headers = o.getJSONArray("list_header");
						jsonmsgs = o.getJSONArray("list_result");
						curmsg=0;
						showNextMsg();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				em.unregisterListener(eventType.downloadListEnd, this);
			}
		};
		em.registerListener(eventType.downloadListEnd, f);
		server.sendCmdToServer(cmdGetListOfMessages,eventType.downloadListStarted,eventType.downloadListEnd);
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
							// TODO: register a listener to check if the answer has been correctly sent and received
							c.server.sendCmdToServer("quick_do.php?obj=message&cmd=decline_inv&mid="+mm.getMessageId(),eventType.downloadListStarted,eventType.downloadListEnd);
							showNextMsg();
						}
					});
					builder.setNeutralButton("accept", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							MsgDialogFragment.this.getDialog().dismiss();
							// TODO: register a listener to check if the answer has been correctly sent and received
							c.server.sendCmdToServer("quick_do.php?obj=message&cmd=accept_inv&mid="+mm.getMessageId(),eventType.downloadListStarted,eventType.downloadListEnd);
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
							// TODO: register a listener to check if the "mark read" flag has been correctly sent and received
							c.server.sendCmdToServer("quick_do.php?obj=message&cmd=move_msg&mid="+mm.getMessageId()+"&folder=1",eventType.downloadListStarted,eventType.downloadListEnd);
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
