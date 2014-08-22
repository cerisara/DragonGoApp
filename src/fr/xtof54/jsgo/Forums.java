package fr.xtof54.jsgo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.HttpGet;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import fr.xtof54.dragonGoApp.R;
import fr.xtof54.jsgo.GoJsActivity.guistate;

/**
 * Main page, once logged-in:
 * http://www.dragongoserver.net/forum/index.php
 * In this page, look for lines with the string 'class="NewFlag"'. These lines points to cats with unread messages.
 * The first href in this line points to he list of topics in this cat, again with the NewFlag tag (I guess)
 * Again, one can use the first href in the line to go to the target thread with new messages.
 * 
 * In this last page, there is a first table with the titles of the posts, and a second table with the content.
 * Maybe it's good to skip the first table and directly jumps to the second table by
 * looking the table after 'Reading thread posts', and then prints all messages in revers order, but still
 * marking the new ones in colored background thanks to the 'class="NewFlag"' in the line
 * 
 * @author xtof
 *
 */
public class Forums {
	static ArrayList<String> toshow = new ArrayList<String>();
	static ArrayList<String> hrefs = new ArrayList<String>();
	static ArrayList<String> toshow2 = new ArrayList<String>();
	static ArrayList<String> hrefs2 = new ArrayList<String>();
	static String txt;
	static int inList=0;
	private static boolean showAll=false;
    private static String class2find = "class=\"NewFlag\"";
    private static String class2find2 = "class=\"NewFlag\"";
	
	public static void show() {
        if (!GoJsActivity.main.initAndroidServer()) return;
        inList=0;
        Thread forumthread = new Thread(new Runnable() {
			@Override
			public void run() {
				getLastForums();
				showForums(toshow);
			}
		});
        forumthread.start();
	}

	// the back button has been pressed
	public static boolean back() {
		if (inList==1) {
		    // go back to the list of topics
		    toshow2.clear(); hrefs2.clear();
			inList--;
			showForums(toshow);
			return false;
		} else if (inList==2) {
		    // go back to the list of threads for the current topic
			inList--;
			showForums(toshow2);
			return false;
		} else return true;
	}
	
	static void showForums(final List<String> cats) {
		GoJsActivity.main.runOnUiThread(new Runnable() {
			@Override
			public void run() {
			    if (cats.size()==0) {
			        GoJsActivity.main.showMessage("No new forums; repress to see all forums");
			        if (!showAll) switchShowNew();
			        return;
			    }
				System.out.println("set Forum view");
				GoJsActivity.main.setContentView(R.layout.forumcats);
				String[] c = new String[cats.size()];
				for (int i=0;i<c.length;i++) c[i]=cats.get(i);
		        ArrayAdapter<String> adapter = new ArrayAdapter<String>(GoJsActivity.main, R.layout.detlistitem, c);
				final ListView listFrameview = (ListView)GoJsActivity.main.findViewById(R.id.forumCatsList);
				listFrameview.setAdapter(adapter);
				listFrameview.setOnItemClickListener(new OnItemClickListener() {
		            @Override
		            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
		            	final int n=position;
		            	Thread forumcatthread = new Thread(new Runnable() {
							@Override
							public void run() {
				            	Forums.catChosen(n);
							}
						});
		            	forumcatthread.start();
		            }
		        });
				GoJsActivity.main.curstate=guistate.forums;
				System.out.println("forum list done "+c.length);
			}
		});
	}
	
	static void catChosen(int pos) {
		if (inList==1) {
			catChosen2(pos);
			return;
		}
		GUI.showWaitingWin();
		System.out.println("chosen cat "+pos);
		String cmd = GoJsActivity.main.androidServer.getUrl()+"forum/"+hrefs.get(pos);
		System.out.println("direct connect cmd "+cmd);
		HttpGet get = new HttpGet(cmd);
		final String cacheFile = GoJsActivity.main.eidogodir+"/forumsHtmlCats";
		GoJsActivity.main.androidServer.directConnectExecute(get,cacheFile);
		System.out.println("load forums cats tmp file");
		try {
			BufferedReader f = new BufferedReader(new FileReader(cacheFile));
			String firsthalf = null;
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				int i=0,k;
				// loop until all "full lines" in s are captured and handled
				for (;;) {
					// get a full line
					if (firsthalf!=null) {
						// we already have the first half, we look at the end of line
						k=s.indexOf("</tr");
						if (k<0) {
							firsthalf+=s;
							break;
						} else {
							firsthalf+=s.substring(0, k);
							treatLine2(firsthalf);
							firsthalf=null;
						}
						i=k;
						continue;
					}
					int j=s.indexOf("<tr ",i);
					if (j<0) {
						k=0; break;
					}
					k=s.indexOf("</tr",j);
					if (k<0) {
						// there is the beginning but not the end
						firsthalf=s.substring(j);
						break;
					} else {
						treatLine2(s.substring(j, k));
					}
					i=k;
				}
				if (k==0) {
					// no beginning in the current string; we can start over with the next s
				} else if (k<0) {
					// only the first half; we have to complete
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		inList++;
        GUI.hideWaitingWin();
		showForums(toshow2);
	}
	static void catChosen2(int pos) {
	    GUI.showWaitingWin();
		System.out.println("chosen cat2 "+pos);
		String cmd = GoJsActivity.main.androidServer.getUrl()+"forum/"+hrefs2.get(pos);
		System.out.println("direct connect cmd "+cmd);
		HttpGet get = new HttpGet(cmd);
		final String cacheFile = GoJsActivity.main.eidogodir+"/forumsHtmlCats";
		GoJsActivity.main.androidServer.directConnectExecute(get,cacheFile);
		System.out.println("load2 forums cats tmp file");
		try {
			BufferedReader f = new BufferedReader(new FileReader(cacheFile));
			String firsthalf = null;
			txt="";
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				int i=0,k;
				// loop until all "full lines" in s are captured and handled
				for (;;) {
					// get a full line
					if (firsthalf!=null) {
						// we already have the first half, we look at the end of line
						k=s.indexOf("</tr");
						if (k<0) {
							firsthalf+=s;
							break;
						} else {
							firsthalf+=s.substring(0, k);
							treatLine3(firsthalf);
							firsthalf=null;
						}
						i=k;
						continue;
					}
					int j=s.indexOf("<tr ",i);
					if (j<0) {
						k=0; break;
					}
					k=s.indexOf("</tr",j);
					if (k<0) {
						// there is the beginning but not the end
						firsthalf=s.substring(j);
						break;
					} else {
						treatLine3(s.substring(j, k));
					}
					i=k;
				}
				if (k==0) {
					// no beginning in the current string; we can start over with the next s
				} else if (k<0) {
					// only the first half; we have to complete
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		GUI.hideWaitingWin();
		
//		txt = txt.replace("<BR>", "\n");
//		txt = txt.replaceAll("<.*>", "");
		inList++;

		// display
		GoJsActivity.main.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				GoJsActivity.main.setContentView(R.layout.forums);
				final EditText v = (EditText)GoJsActivity.main.findViewById(R.id.forumtext);
				v.setKeyListener(null);
				v.setText(android.text.Html.fromHtml(txt));
			}
		});
	}
	public static void switchShowNew() {
	    showAll=!showAll;
	    if (showAll) {
	        GoJsActivity.main.showMessage("showing old msgs");
            class2find="class=ThreadCnt";
            class2find2="class=Name><";
	    } else {
            class2find="class=\"NewFlag\"";
            class2find2="class=\"NewFlag\"";
	        GoJsActivity.main.showMessage("showing new msgs only");
	    }
	}
	
	private static void treatLine3(String s) {
	    // no need to reprint the subject here, it was already shown in the list on the previous screen
//		int i=s.indexOf("PostHeadNormal Subject");
//		if (i>=0) {
//			int j=s.indexOf("</a>",i);
//			int k=s.lastIndexOf('>', j)+1;
//			txt+="("+s.substring(k, j)+", ";
//		}
		int i=s.indexOf("PostHeadNormal Author");
		if (i>=0) {
			int j=s.indexOf("</A>",i);
			int k=s.lastIndexOf('>', j)+1;
			txt+="<b>"+s.substring(k, j)+"</b><BR> ";
		}
		i=s.indexOf("PostBody");
		if (i>=0) {
			i+=23;
			int j=s.indexOf("</td>",i);
			txt+=s.substring(i, j)+"<BR>";
		}
	}
	private static void treatLine2(String s) {
    	int j=s.indexOf(class2find2);
    	if (j<0) return;
    	int hrefdeb = s.lastIndexOf("href", j);
    	j=s.indexOf('"',hrefdeb)+1;
    	int k=s.indexOf('"',j);
    	hrefs2.add(s.substring(j,k).replace("&amp;", "&"));
    	System.out.println("put in hrefs2 "+s.substring(j,k));
    	j=s.indexOf('>',k)+1;
    	k=s.indexOf('<',j);
    	toshow2.add(s.substring(j, k));
    	System.out.println("put in toshow2 "+s.substring(j, k));
	}
	
	static private void treatLine(String s) {
    	int j=s.indexOf(class2find);
    	if (j<0) return;
    	int hrefdeb = s.lastIndexOf("href", j);
    	j=s.indexOf('"',hrefdeb)+1;
    	int k=s.indexOf('"',j);
    	hrefs.add(s.substring(j,k));
    	j=s.indexOf('>',hrefdeb)+1;
    	k=s.indexOf('<',j);
    	toshow.add(s.substring(j, k));
	}
	
	static void getLastForums() {
	    GUI.showWaitingWin();
		GoJsActivity.main.androidServer.initHttp();
		String cmd = GoJsActivity.main.androidServer.getUrl()+"forum/index.php";
		System.out.println("direct connect cmd "+cmd);
		HttpGet get = new HttpGet(cmd);
		
		final String cacheFile = GoJsActivity.main.eidogodir+"/forumsHtmlString";
		GoJsActivity.main.androidServer.directConnectExecute(get,cacheFile);
		
		System.out.println("load forums tmp file");
        toshow.clear(); hrefs.clear();
        toshow2.clear(); hrefs2.clear();
		try {
			BufferedReader f = new BufferedReader(new FileReader(cacheFile));
			String firsthalf = null;
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				int i=0,k;
				// loop until all "full lines" in s are captured and handled
				for (;;) {
					// get a full line
					if (firsthalf!=null) {
						// we already have the first half, we look at the end of line
						k=s.indexOf("</tr");
						if (k<0) {
							firsthalf+=s;
							break;
						} else {
							firsthalf+=s.substring(0, k);
							treatLine(firsthalf);
							firsthalf=null;
						}
						i=k;
						continue;
					}
					int j=s.indexOf("<tr ",i);
					if (j<0) {
						k=0; break;
					}
					k=s.indexOf("</tr",j);
					if (k<0) {
						// there is the beginning but not the end
						firsthalf=s.substring(j);
						break;
					} else {
						treatLine(s.substring(j, k));
					}
					i=k;
				}
				if (k==0) {
					// no beginning in the current string; we can start over with the next s
				} else if (k<0) {
					// only the first half; we have to complete
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		    GUI.hideWaitingWin();
		}
	}
}
