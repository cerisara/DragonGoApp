package fr.xtof54.jsgo;

import fr.xtof54.dragonGoApp.R;

public class GUI {
	private GUI() {}
	private static GUI gui = new GUI();
	
	public static GUI getGUI() {
		return gui;
	}
	
//	public void showMain() {
//		GoJsActivity.main.setContentView(R.layout.activity_main);
//	}
	
	public void showForums() {
		GoJsActivity.main.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				GoJsActivity.main.setContentView(R.layout.forums);
			}
		});
	}
}
