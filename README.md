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

Then, a blank window is shown with 4 buttons at the top: note that although the "look and feel" of DragonGoApp is not very appealing,
this app actually has all the required functionalities (and even more) to really do its job, I mean, playing Go with DGS !
It's just that, as a programmer, I hate spending time on designing colourful and nice GUIs. I'm really only interested by the
core features, sorry about that, but that's just how I am.
In addition to these 4 main buttons, there's also a fifth rightmost small button at the top, which opens a long list of not-so-often-used
commands. The third (and last) interation channel is via the standard android menu.

These 4 buttons constitute the main ways to interact with the app: their label (and function) will change, depending on whether you're
playing, writing a message... So the actual function of each of these 4 buttons depends on the context.
If, at any stage, you don't know any more how to come back to the main screen, there's a command called "reset gui" that does exactly
that, that is bring you back to the main entry context that you start with. More on this reset command later on...

But before playing, you must enter your DGS login and password via the "menu/settings" dialog. The app supports two user logins, and when you
use the "menu/settings" to enter credentials, these credentials are stored for the currently selected user. By default, it's the first
user. Changing to the second user can be done by selecting the "devLogin" in the list of commands trigerred by the fifth rightmost button.
You can also select here the "devDGS" (development DGS) server, but you shouldn't care about it.

In the main starting GUI, the first button "Games" downloads your games status from the DGS server. It corresponds to the main page
that you see on the Web server, with the list of games where it's your turn to play. Clicking on this button thus triggers a download
from the DGS server to retrieve in which games it's your turn to play, and what was the last move of your opponent.
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
You can also send your own comment with the command "add game message", which proposes two standard messages, or lets you write your own.

You can also view or remove the local SGF files on the sdcard with the command "list saved games".

When a goban is shown, you can move and play as usual, but to send your last move to the DGS server, you should use the first "SEND"
button, which asks you for a confirmation before doing the actual upload.


