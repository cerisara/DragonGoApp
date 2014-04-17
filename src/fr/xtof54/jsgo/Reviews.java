package fr.xtof54.jsgo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;

import fr.xtof54.dragonGoApp.R;
import fr.xtof54.jsgo.EventManager.eventType;
import fr.xtof54.jsgo.GoJsActivity.guistate;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * For now, the reviews that one can follow all come from the DragonGoApp bundle.
 * TODO: download new reviews from a web site
 * 
 * @author xtof
 *
 */
public abstract class Reviews {
	private static String[] sgfs = null;
	static int cursgf=-1, curmove=-1;
	static String comment="";
	public static boolean showCommentsInBig = true;

	private static int tmpchosen = -1;

	public static void setComment(String html) {
		System.out.println("debug comment "+html);
		String s=URLDecoder.decode(html).replace("<br>", "\n").replace("<br />", "\n");
		s=s.replaceAll("</[^>]*>", "\n");
		comment=s.replaceAll("<[^>]*>", " ").trim();
	}
	
	public static void showList() {
		class DetListDialogFragment extends DialogFragment {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				// Get the layout inflater
				LayoutInflater inflater = getActivity().getLayoutInflater();
				// sgfs should never be empty, because this list can only be displayed *after* contReview()
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(GoJsActivity.main, R.layout.detlistitem, sgfs);
				// Inflate and set the layout for the dialog
				// Pass null as the parent view because it's going in the dialog layout
				View listFrameview = inflater.inflate(R.layout.ladder, null);
				{
					TextView ladderlab = (TextView)listFrameview.findViewById(R.id.ladderlab);
					String s = "Choose the file to review:";
					ladderlab.setText(s);
				}
				final ListView ladder = (ListView)listFrameview.findViewById(R.id.ladderList);
				ladder.setAdapter(adapter);
				ladder.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
						tmpchosen = position;
					}
				});
				builder.setView(listFrameview);

				builder.setPositiveButton("Review", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						DetListDialogFragment.this.getDialog().dismiss();
						if (tmpchosen<0) {
							EventManager.getEventManager().sendEvent(eventType.showMessage, "No sgf selected");
						} else {
							cursgf = tmpchosen;
							curmove = 0;
							saveCurReview();
							contReviews();
						}
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						DetListDialogFragment.this.getDialog().dismiss();
					}
				});
				return builder.create();
			}
		}
		tmpchosen=-1;
		final DetListDialogFragment msgdialog = new DetListDialogFragment();
		msgdialog.show(GoJsActivity.main.getSupportFragmentManager(),"reviews");
	}

	public static void saveCurReview() {
		System.out.println("save review pos "+cursgf+" "+curmove);
		PrefUtils.saveToPrefs(GoJsActivity.main, "REVIEWSGF", cursgf);
		PrefUtils.saveToPrefs(GoJsActivity.main, "REVIEWMOVE", curmove);
	}
	
	public static void contReviews() {
		GoJsActivity.main.changeState(guistate.review);
		AssetManager mgr = GoJsActivity.main.getResources().getAssets();
		try {
			if (sgfs==null) {
				sgfs = mgr.list("reviews");
				System.out.println("reviews loaded "+sgfs.length);
				cursgf  = PrefUtils.getFromPrefs(GoJsActivity.main, "REVIEWSGF" , 0);
				curmove = PrefUtils.getFromPrefs(GoJsActivity.main, "REVIEWMOVE" , 0);
			}

			System.out.println("reviews cursgf "+cursgf+" "+curmove);
			Game g = Game.createDebugGame();
			InputStream i = mgr.open("reviews/"+sgfs[cursgf]);
			BufferedReader f = new BufferedReader(new InputStreamReader(i));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				g.addSgfData(s);
			}
			f.close();
            GoJsActivity.main.showGame(g);

			// TODO: pb of synchronisation here: all these commands are run before javascript is ready
			// so nothing happens...
			// sometimes; detComments is even sent *before* the goban appears, which creates an HTML error ?
			
			showCommentsInBig=false;
			for (int j=0;j<curmove;j++)
				GoJsActivity.main.wv.loadUrl("javascript:eidogo.autoPlayers[0].forward()");
			showCommentsInBig=true;

			GoJsActivity.main.wv.loadUrl("javascript:eidogo.autoPlayers[0].detComments()");
		} catch (IOException e) {
			e.printStackTrace();
			EventManager.getEventManager().sendEvent(eventType.showMessage, "Error loading review games");
		}	
	}
}
