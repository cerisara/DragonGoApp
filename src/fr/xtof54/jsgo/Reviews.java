package fr.xtof54.jsgo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import fr.xtof54.jsgo.EventManager.eventType;
import fr.xtof54.jsgo.GoJsActivity.guistate;

import android.content.res.AssetManager;

public abstract class Reviews {
	private static String[] sgfs = null;
	private static int cursgf=-1, curmove=-1;
	private static ArrayList<String> comments = new ArrayList<String>();
	private static ArrayList<Integer> moves = new ArrayList<Integer>();
	static String comment="";
	
	public static void contReviews() {
		GoJsActivity.main.changeState(guistate.review);
		AssetManager mgr = GoJsActivity.main.getResources().getAssets();
		try {
			if (sgfs==null) {
				sgfs = mgr.list("reviews");
				System.out.println("reviews loaded "+sgfs.length);
				cursgf  = PrefUtils.getFromPrefs(GoJsActivity.main, "REVIEWSGF" , 0);
				curmove = PrefUtils.getFromPrefs(GoJsActivity.main, "REVIEWMOVE" , 0);
				System.out.println("reviews cursgf "+cursgf+" "+curmove);
			}
			
			Game g = Game.createDebugGame();
			InputStream i = mgr.open("reviews/"+sgfs[cursgf]);
			BufferedReader f = new BufferedReader(new InputStreamReader(i));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				g.addSgfData(s);
			}
			f.close();
			
			for (int j=0;j<curmove;j++)
				GoJsActivity.main.wv.loadUrl("javascript:eidogo.autoPlayers[0].forward()");

			GoJsActivity.main.showGame(g);
		} catch (IOException e) {
			e.printStackTrace();
			EventManager.getEventManager().sendEvent(eventType.showMessage, "Error loading review games");
		}	
	}
}
