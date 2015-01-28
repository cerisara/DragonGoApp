DragonGoApp
===========

Android app to play the game of go with the Dragon Go Server.

This is a new android app to use for DGS, with the following features:

* Open-source: everyone can know exactly what it's doing and all of the app's internals.
* Ad-free
* Small bandwidth usage: this is especially useful when you have a limited data plan; The app shows you *in real time* your data usage (globally at the mobile level, not only for the app itself);
* Full-featured SGF editor/player based on the open-source eidogo code. Also shows game information and game comments.
* Support dual servers/credentials (DGS and develDGS)
* Playing moves (obviously !), pass, resign, send/receive messages, attach a comment to a move
* invite/accept/refuse challenges, challenge in 19x19 and 9x9 ladders
* Follow reviews of professional games from gobase, which are included in the app
* Read the DGS Forums: by default, only the new/unread messages will be shown, but you can switch to read all messages by pressing menu-settings while in the forums view.

You can install the latest release from [F-droid](https://f-droid.org/repository/browse/?fdfilter=dragongoapp&fdid=fr.xtof54.dragonGoApp)

Limitations:

* Ending game (territory agreement, scoring) is supported by displaying the DGS website, for now.
* When inviting or accepting a game, you cannot change the game settings (handicap...) for now

Quick-start
===========

When run for the first time, a "loading" window appears for a few seconds: it's actually not downloading anything at this stage,
but it's just uncompressing the eidogo javascript library onto the SD-card. This is only done once.

GUI overview
------------

The 5 top-left buttons always present the most useful function at any time:

* *Games* : download status games from DGS server
* *Zm+* : Zoom-in the goban
* *Zm-* : Zoom-out the goban
* *Msg* : download status messages from DGS server
* *Fwd* / *Bck* : navigate in the game; these buttons are redundant with the arrows on the goban, and should be removed later on.
* *Reset* : comes back to the original position (as in the server): very useful when exploring lines of play, just before sending the actual next move you wanna play.
* *Send* : send your last move to DGS. A confirmation dialog is shown.

There is a top-right button *...* that shows a list of additional functionalities:

* *Skip this game* : when you don't want to answer right now
* *Resign game* : pretty clear
* *Add a game message* : press this before sending a move, and a comment will be attached to this move in the SGF. Two standard comments are proposed (to welcome and thank you opponent), but you can write your own. You can also edit or remove your comment as many times as you wish as long as you have not sent the move.
* *View 19x19 ladder* : shows only the ladder positions that you can challenge. Uses a local cache, so it may not be always synchronized with the actual ladder in DGS, but you can re-download the ladder at any time.
* *View 9x9 ladder* : same thing for another ladder
* *View forums* : download the list of forum messages that you have not read yet (or all of them)
* *List saved games* : every game is saved locally, so you can view it, and even play a move from this list. You can also remove all or a single game from your sd card.
* *DGS/login* : these 4 buttons allow to select another user or another server
* *reset GUI* : press it to come back to the initial GUI, as if you just had launched the app
* *Open game reviews* : continues the reading of Pro games reviews
* *recopy Eidogo* : for debugging only
* *redraw last goban* : shortcut to quickly view the last goban when offline
 

Note that although the "look and feel" of DragonGoApp is not very appealing,
this app actually has all the required functionalities (and even more) to really do its job, I mean, playing Go with DGS !
It's just that, as a programmer, I hate spending time on designing colourful and nice GUIs. I'm really only interested by the
core features, sorry about that, but that's just how I am.
In addition to these 4 main buttons, there's also a fifth rightmost small button at the top, which opens a long list of not-so-often-used
commands. The third (and last) interation channel is via the standard android menu.

How to start playing
--------------------

Before playing, you must enter your DGS login and password via the "menu/settings" dialog. The app supports two user logins, and when you
use the "menu/settings" to enter credentials, these credentials are stored for the currently selected user. By default, it's the first
user. Changing to the second user can be done by selecting the "devLogin" in the list of commands trigerred by the fifth rightmost button.
You can also select here the "devDGS" (development DGS) server, but you shouldn't care about it.

* Press *Games* to download the games where it's your turn to play
* Click on the board where you wanna play
* Press *Sent* the next game to move will be shown automatically.

Note that the app is pretty good for playing games, but not very good for counting game score (when you arrive at this stage, the app
just shows the DGS page of the game) or finding an opponent. 
Although you can challenge opponents from within the app in 19x19 and 9x9 ladders, for anything else (waiting room, other tournaments)
I recommend to do it from the web site.

Bandwidth efficiency
--------------------

Note that only very few buttons actually connects to the Internet, and this one is the first one. In fact, the app is designed to
only access the Internet when it's really useful, hence minimizing bandwidth and consumption of your data plan. This has always been
a major design guideline when writing this app, and you can actually always check in real time, on the top-right of the status bar, the
total amount of internet connection that has been consumed by the app since it's been launched. Typically, you can play one move in 6 or 7 games with less than a few dozens of kbits of bandwidth.

By default, the app does not download the game SGF from the server, but it stores locally (on the SD-card) the sgf, i.e.,
all the moves that you play and all your opponent moves that are sent by the server when getting the status with the "Games" button.
When it thus founds a local SGF file for the current game, it shows a 1s-notification "local file found".
This greatly reduces bandwitdh consumption and improves playing speed, BUT this approach has one drawback: you don't see the comments
sent by your opponent and attached to each move, because the "get status" connection does not return these comments ! 
If you want to see them, then you should select the "menu/Always download" dialog,
which always triggers a SGF download for every move of your opponent. Then, you will also always get all comments attached to the moves,
which comments are both shown for a few seconds on a big dialog just after showing each move, and they are also shown in small font below
the goban.


