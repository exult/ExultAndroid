package com.exult.android;
import android.media.MediaPlayer;
import java.io.IOException;
import java.io.OutputStream;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.media.AudioFormat;

public final class Audio extends GameSingletons {
	// FOR NOW, limit to 2 channels for speed.
	public static final int MIXER_CHANNELS = 2;	// Max. # channels.
	public static final int MAX_VOLUME = 256, MAX_SOUND_FALLOFF = 24;
	public final static int // enum Combat_song
		CSBattle_Over = 0,
		CSAttacked1 = 1,
		CSAttacked2 = 2,
		CSVictory = 3,
		CSRun_Away = 4,
		CSDanger = 5,
		CSHidden_Danger = 6;
	public boolean debug = false;
	private MediaPlayer players[] = new MediaPlayer[MIXER_CHANNELS];
	private int currentTrack = -1, currentTrackInd = -1;
	private errorListener err = new errorListener();
	private completionListener completion = new completionListener();
	private FlexFile sfxFile;
	private static int bg2siSfxs[];
	//	Find player index, or return -1;
	private int findPlayer(MediaPlayer player) {
		int cnt = players.length;
		for (int i = 0; i < cnt; ++i)
			if (players[i] == player)
				return i;
		return -1;
	}
	private MediaPlayer getPlayer(int ind) {
		return (ind >= 0 && ind < players.length) ? players[ind] : null; 
	}
	private int addPlayer(MediaPlayer p) {
		int cnt = players.length;
		for (int i = 0; i < cnt; ++i) {
			if (players[i] == null) {
				players[i] = p;
				return i;
			} else if (!players[i].isPlaying()) {
				players[i].release();
				players[i] = p;
				return i;
			}
		}
		return -1;
	}
	private void release(MediaPlayer player) {
		player.release();
		int i = findPlayer(player);
		if (i >= 0) {
			players[i] = null;
		}
	}
	public static Audio instanceOf() {
		return audio;
	}
	public Audio() {
		initSfx();
	}
	private static String canSfx(String nm) {
		String fname = EUtil.U7exists("<DATA>/" + nm);
		return fname;
	}
	//	Return SFX file name.
	private static String haveSfx() {
		String fname = null;
		if (game.isBG()) {
			fname = canSfx(EFile.SFX_ROLAND_BG);
			if (fname == null)
				fname = canSfx(EFile.SFX_BLASTER_BG);
		} else if (game.isSI()) {
			fname = canSfx(EFile.SFX_ROLAND_SI);
			if (fname == null)
				fname = canSfx(EFile.SFX_BLASTER_SI);
		}
		return fname;
	}
	private void initSfx() {
		String fname = haveSfx();
		if (fname != null)
			sfxFile = new FlexFile(fname, fname);
		else {
			System.out.println("Audio: sound effects file not found.");
		}
		bg2siSfxs = game.isSI() ? bgconv : null;
	}
	//	Play positioned at a given tile.
	public int playSfx(int num, Tile t, int volume, int repeat) {
		//++++++FINISH
		return playSfx(num, repeat);
	}
	public int playSfx(int num, Tile t) {
		return playSfx(num, t, MAX_VOLUME, 0);
	}
	//	Play positioned at a given object.
	public int playSfx(int num, GameObject obj, int volume, int repeat) {
		if (debug)System.out.println("playSfx: " + num + ", vol = " + volume 
				+ ", repeat = " + repeat);
		//+++++++++FINISH
		return playSfx(num, repeat);
	}
	public int playSfx(int num, GameObject obj) {
		return playSfx(num, obj, MAX_VOLUME, 0);
	}
	// Return 'ind', or -1 if too far away.
	public int updateSfx(int ind, GameObject obj) {
		return ind; //++++++FINISH
	}
	public boolean isPlaying(int ind) {
		return ind >= 0 && ind < players.length && players[ind] != null &&
					players[ind].isPlaying();
	}
	public void stopSfx(int ind) {
		MediaPlayer p = getPlayer(ind);
		if (p != null) {
			p.release();
			players[ind] = null;
		}
	}
	//	Returns channel/index or -1 if failed.
	//	repeat == -1 means loop continuously.
	public int playSfx(int num) {
		return playSfx(num, 0); 
	}
	public int playSfx(int num, int repeat) {
		if (sfxFile == null)
			return -1;
		/* ++++++++EXPERIMENTING
		int size = AudioTrack.getMinBufferSize(22050, AudioFormat.CHANNEL_OUT_STEREO,
				AudioFormat.ENCODING_PCM_16BIT);
		if (data.length > size)
			size = data.length;
		System.out.println("size = " + size);
		AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC, 22050,
				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
				32000, AudioTrack.MODE_STATIC);
		player.write(data, 0, data.length);
		player.play();
		return 0;
		*/
		if (addPlayer(null) == -1)
			return -1;						// All channels are busy.
		String nm = null;
		nm = EUtil.getSystemPath(
				String.format("<DATA>/tempsfx%1$d.wav", num));
		if (debug) System.out.println("Audio: playing SFX #" + num);
		if (EUtil.U7exists(nm) != null) {
			return playFile(nm, repeat == -1);
		}
		byte data[] = sfxFile.retrieve(num);
		int ind = -1;
		if (data != null) 
			try {
				OutputStream out = EUtil.U7create(nm);
				out.write(data);
				out.close();
				ind = playFile(nm, repeat == -1);
			} catch (IOException e) {
				System.out.println("Audio: Failed to play track: " + nm);
			}
		return ind;
	}
	public static int gameSfx(int sfx) {
		return bg2siSfxs != null ? bg2siSfxs[sfx] : sfx;
	}
	// Stop all tracks.
	public void stop() {
		int cnt = players.length;
		for (int i = 0; i < cnt; ++i) {
			if (players[i] != null) {
				players[i].release();
				players[i] = null;
			}
		}
		currentTrack = -1;
	}
	public void cancelStreams() {
		stop();
	}
	public int getCurrentTrack() {
		return currentTrack;
	}
	//	Play from the 'Combat_song' list above.
	public void startMusicCombat(int song, boolean continuous) {
		int num = -1;
		if (!game.isSI()) {
			switch (song){
			case CSBattle_Over:
				num = 9; break;
			case CSAttacked1:
				num = 11; break;
			case CSAttacked2:
				num = 12; break;
			case CSVictory:
				num = 15; break;
			case CSRun_Away:
				num = 16; break;
			case CSDanger:
				num = 10;
				break;
			case CSHidden_Danger:
				num = 18;
				break;
			default:
				System.out.println(
					"Error: Unable to Find combat track for song " + song + ".");
				break;
			}
		} else  {
			switch (song) {
			case CSBattle_Over:
				num = 0; break;
			case CSAttacked1:
				num = 2; break;
			case CSAttacked2:
				num = 3; break;
			case CSVictory:
				num = 6; break;
			case CSRun_Away:
				num = 7; break;
			case CSDanger:
				num = 1; break;
			case CSHidden_Danger:
				num = 9; break;
			default:
				System.out.println(
						"Error: Unable to Find combat track for song " + song + ".");
				break;
			}
		}
		startMusic(num, continuous);
	}	
	//	Returns channel/player index, or -1 if unsuccessful.
	public int startMusic(int num, boolean repeat, String flex) {
		if (debug)
			System.out.println("Audio:  startMusic " + num + ", repeat = " + repeat);
		// -1 and 255 are stop tracks
		if (num == -1 || num == 255) {
			stop();
			return -1;
		}
		// Already playing it??
		if (currentTrack == num && currentTrackInd >= 0) {
			// OGG is playing?
			MediaPlayer player = players[currentTrackInd];
			if (player != null && player.isPlaying())
				return -1;
		}
		// Work around Usecode bug where track 0 is played at Intro Earthquake
		if (num == 0 && flex == EFile.MAINMUS && game.isBG())
			return -1;	
		stop();
		int ind = oggPlay(flex, num, repeat);
		if (ind >= 0) {
			currentTrackInd = ind;
			currentTrack = num;
		}
		return ind;
	}
	public int	startMusic(int num, boolean repeat) {
		return startMusic(num, repeat, EFile.MAINMUS);
	}
	//	Returns channel/player index, or -1 if unsuccessful.
	private int oggPlay(String filename, int num, boolean repeat) {
		String ogg_name = "";
		String basepath = "<MUSIC>/";

		if (filename == EFile.EXULT_FLX && num == EFile.EXULT_FLX_MEDITOWN_MID)
			ogg_name = "exult.ogg";
		else if (game.isBG()) {
			if (filename == EFile.INTROMUS || filename == EFile.INTROMUS_AD) {
				if (num == 0)
					ogg_name = "00bg.ogg";
				else if (num == 1)
					ogg_name = "01bg.ogg";
				else if (num == 2)
					ogg_name = "02bg.ogg";
				else if (num == 3)
					ogg_name = "03bg.ogg";
				else if (num == 4)
					ogg_name = "endcr01.ogg";
				else if (num == 5)
					ogg_name = "endcr02.ogg";
			} else if (filename == EFile.ENDSCORE_XMI) {
				if (num == 1 || num == 3)
					ogg_name = "end01bg.ogg";
				else if (num == 2 || num == 4)
					ogg_name = "end02bg.ogg";
			} else if (filename == EFile.MAINMUS || filename == EFile.MAINMUS_AD) {
				ogg_name = String.format("%1$02dbg.ogg", num);
			}
		} else if (game.isSI()) {
			if(filename == EFile.MAINSHP_FLX)
				{
				if (num == 28 || num == 27) 
					ogg_name = "03bg.ogg";
				else if(num == 30 || num == 29)
					ogg_name = "endcr01.ogg";
				else if(num == 32 || num == 31)
					ogg_name = "endcr02.ogg";
				}
			else if(filename == EFile.R_SINTRO || filename == EFile.A_SINTRO)
				ogg_name = "si01.ogg";
			else if(filename == EFile.R_SEND || filename == EFile.A_SEND)
				ogg_name = "si13.ogg";
			else if (filename == EFile.MAINMUS || filename == EFile.MAINMUS_AD) {
				if (num >= 0 && num < bgconvmusic.length)
					ogg_name = bgconvmusic[num];
				else {
					ogg_name = String.format("%1$02dsi.ogg", num);
				}
			}
		} else {
			ogg_name = String.format("%1$02dmus.ogg", num);
			basepath = "<STATIC>/music/";
		}
		if (ogg_name == "") 
			return -1;
		String nm = EUtil.U7exists("<PATCH>/music/" + ogg_name);
		if (nm != null)
			ogg_name = nm;
		else {
			ogg_name = EUtil.getSystemPath(basepath + ogg_name);	
		}
		return playFile(ogg_name, repeat);
	}
	//	Returns player/channel index, or -1 if failed.
	private int playFile(String fname, boolean repeat) {
		MediaPlayer player = null;
		int ind = -1;
		if (debug)
			System.out.println("Audio: Music track " + fname);
		try {
			player = new MediaPlayer();
			ind = addPlayer(player);
			player.setOnErrorListener(err);
			player.setOnCompletionListener(completion);
			player.setDataSource(fname);
			player.prepare();
			player.setLooping(repeat);
			player.start();
		} catch (IOException e) {
			System.out.println("Audio: Failed to play track: " + fname);
			ExultActivity.showToast("Failed to play track: " + fname);
			if (player != null) {
				release(player);
			}
			return -1;
		}
		return  ind;
	}
	private static class errorListener implements android.media.MediaPlayer.OnErrorListener {
		public boolean onError(MediaPlayer mp, int what, int extra) {
			String msg = "Audio: Error callback, what = " + what + 
					  ", extra = " + extra;
			System.out.println(msg);
			ExultActivity.showToast(msg);
			return true;
		}
	}
	private static class completionListener implements android.media.MediaPlayer.OnCompletionListener {
		public void onCompletion(MediaPlayer mp) {
			//System.out.println("Audio: Track has completed.");
			Audio.instanceOf().release(mp);
		}
	}
	private static final String bgconvmusic[] = {
		"09bg.ogg", 	// 0
		"10bg.ogg", 	// 1
		"11bg.ogg", 	// 2
		"12bg.ogg", 	// 3
		"13bg.ogg", 	// 4
		"14bg.ogg", 	// 5
		"15bg.ogg", 	// 6
		"16bg.ogg", 	// 7
		"17bg.ogg", 	// 8
		"18bg.ogg", 	// 9
		"19bg.ogg", 	// 10
		"20bg.ogg", 	// 11
		"21bg.ogg", 	// 12
		"22bg.ogg", 	// 13
		"23bg.ogg", 	// 14
		"24bg.ogg", 	// 15
		"25bg.ogg", 	// 16
		"26bg.ogg", 	// 17
		"27bg.ogg", 	// 18
		"28bg.ogg", 	// 19
		"29bg.ogg", 	// 20
		"30bg.ogg", 	// 21
		"31bg.ogg", 	// 22
		"32bg.ogg", 	// 23
		"33bg.ogg", 	// 24
		"34bg.ogg", 	// 25
		"35bg.ogg", 	// 26
		"36bg.ogg", 	// 27
		"37bg.ogg", 	// 28
		"38bg.ogg", 	// 29
		"40bg.ogg", 	// 30
		"41bg.ogg", 	// 31
		"42bg.ogg", 	// 32
		"43bg.ogg", 	// 33
		"44bg.ogg", 	// 34
		"45bg.ogg", 	// 35
		"46bg.ogg", 	// 36
		"47bg.ogg", 	// 37
		"48bg.ogg", 	// 38
		"", 			// 39
		"", 			// 40
		"", 			// 41
		"52bg.ogg", 	// 42
		"53bg.ogg", 	// 43
		"55bg.ogg", 	// 44
		"56bg.ogg", 	// 45
		"57bg.ogg", 	// 46
		"58bg.ogg", 	// 47
		"59bg.ogg", 	// 48
		"",	 			// 49
		"si02.ogg", 	// 50
		"si08.ogg", 	// 51
		"si07.ogg", 	// 52
		"si12.ogg", 	// 53
		"si10.ogg", 	// 54
		"si03.ogg", 	// 55
		"si11.ogg", 	// 56
		"si05.ogg", 	// 57
		"si06.ogg", 	// 58
		"", 			// 59
		"",				// 60
		"",			 	// 61
		"", 			// 62
		"si14.ogg", 	// 63
		"si04.ogg", 	// 64
		"si09.ogg", 	// 65
		"07bg.ogg", 	// 66
		"06bg.ogg", 	// 67
		"04bg.ogg", 	// 68
		"05bg.ogg", 	// 69
		"03bg.ogg" 		// 70
		};
	// sfx with ??? are converted to sfx #135 so you can tell
	// it's wrong. Some I suspect to be something so it's not set 135
	private static final int bgconv[] = {
		12,			//Bow Twang			0
		80,			//Missile ??		1
		9,			//Blade				2
		11,			//Blunt				3
		125,		//Hit				4
		61,			//Graze				5
		92,			//Rotating			6
		40,			//Explos #1			7
		41,			//Explos #2			8
		42,			//Explos #3			9
		127,		//Whip pta			10
		71,			//Thunder			11
		44,			//Fireball			12
		65,			//Torches			13
		94,			//Gumps!!!!!		14
		56,			//Gavel				15
		121,		//Treadle			16
		117,		//Clock tick		17
		118,		//Clock tock		18
		16,			//Chime				19
		45,			//Fire 1			20
		46,			//Fire 2			21
		47,			//Fire 3			22
		28,			//Bell Ding			23
		30,			//Bell Dong			24
		72,			//Log Saw			25
		78,			//Mill Stone		26
		68,			//Key				27
		70,			//Lever				28
		135,		//Roulette			29
		32,			//Creeeeaack		30
		31,			//Creeeeaack		31
		89,			//Portcullis		32
		88,			//Portcullis close	33
		35,			//Drawbridge		34
		34,			//Drawbridge		35
		135,		//Fuse  ???			36
		95,			//Shadoobie			37
		99,			//Splash			38
		126,		//W. Anchor			39
		37,			//D. Anchor			40
		18,			//Creeeeaack		41
		17,			//Creeeeaack		42
		2,			//gumpster			43
		1,			//gumpster			44
		49,			//Forge				45
		33,			//Douse				46
		7,			//Bellows			47
		50,			//Fountain			48
		109,		//Surf's up			49
		107,		//Stream			50
		133,		//Waterfall			51
		129,		//Wind    ???		52
		135,		//Rainman  ???		53
		114,		//Swamp 1			54
		110,		//Swamp 2			55
		111,		//Swamp 3			56
		112,		//Swamp 4			57
		113,		//Swamp 5			58
		132,		//Waterwheel		59
		39,			//Eruption ???		60
		22,			//Crickets			61
		116,		//Thunder			62
		128,		//Whirlpool			63
		64,			//Heal				64
		20,			//Spell				65
		67,			//Spell				66
		130,		//Wizard   ???		67
		57,			//General			68
		48,			//Fizzle			69
		84,			//New Spell			70
		82,			//MPdrain			71
		83,			//MPgain			72
		134,		//Footstep L		73
		134,		//Footstep R		74
		108,		//Success			75
		43,			//Failure			76
		55,			//Moongate			77
		54,			//Moongate B		78
		26,			//Entity Hum		79
		101,		//Entity Hum		80
		115,		//Entity Hum		81
		96,			//Shreik			82
		135,		//Slap      ???		83
		135,		//Oooffff   ???		84
		135,		//Whaahh    ???		85
		10,			//Blocked !!		86
		52,			//Furl				87
		124,		//Unfurl			88
		135,		//MISSING			89
		36,			//Drink ???			90
		38,			//Eat   ???			91
		135,		//Whip ptb			92
		135,		//Doorslam			93
		135,		//Portcullis		94
		135,		//Drawbridge		95
		135,		//Closed			96
		100,		//SpinnWheel		97
		79,			//Minning   ???		98
		59,			//Minning   ???		99
		93,			//Shutters			100
		135,		//1armbandit ???	101
		73,			//Loom				102
		103,		//Stalags			103
		75,			//MagicWeap			104
		86,			//Poison			105
		65,			//Ignite			106
		62,			//Yo yo LA ???		107
		131,		//Wind Spell		108
		90,			//Protect			109
		91,			//PoisonSpel ???	110
		66,			//IgniteSpel		111
		21,			//CradleRock		112
		5,			//Beeezzzzz			113
		74,			//Machines			114
		255,		//Static - not used in SI	115
		136			//Tick Tock			116
	};

}
