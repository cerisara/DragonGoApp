package fr.xtof54.jsgo;

import java.util.List;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import fr.xtof54.dragonGoApp.R;
import fr.xtof54.jsgo.GoJsActivity.guistate;

public class GUI {
	private GUI() {}
	private static GUI gui = new GUI();

	public static GUI getGUI() {
		return gui;
	}

	//	public void showMain() {
	//		GoJsActivity.main.setContentView(R.layout.activity_main);
	//	}

	public void showHome() {
		GoJsActivity.main.initGUI();
	}
}
