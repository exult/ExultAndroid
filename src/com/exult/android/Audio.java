package com.exult.android;
import android.media.MediaPlayer;
import java.io.IOException;

public final class Audio extends GameSingletons {
	public boolean debug = true;
	private MediaPlayer player;
	private int currentTrack = -1;
	public void stop() {
		if (player != null)
			player.stop();
	}
	public void startMusic(int num, boolean repeat, String flex) {
		if (player == null)
			player = new MediaPlayer();
		// -1 and 255 are stop tracks
		if (num == -1 || num == 255) {
			stop();
			return;
		}
		// Already playing it??
		if(currentTrack == num) {
			// OGG is playing?
			if (player.isPlaying())
				return;
		}
		// Work around Usecode bug where track 0 is played at Intro Earthquake
		if (num == 0 && flex == EFile.MAINMUS && game.isBG())
			return;	
		stop();
		currentTrack = num;
		if (!oggPlay(flex, num, repeat)) {
			stop();
		}
	}
	public void	startMusic(int num, boolean continuous) {
		startMusic(num, continuous, EFile.MAINMUS);
	}
	private boolean oggPlay(String filename, int num, boolean repeat) {
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
		if (ogg_name == "") return false;
		String nm = EUtil.U7exists("<PATCH>/music/" + ogg_name);
		if (nm != null)
			ogg_name = nm;
		else {
			ogg_name = EUtil.getSystemPath(basepath + ogg_name);	
		}
		if (debug)
			System.out.println("OGG audio: Music track " + ogg_name);
		try {
			player.setDataSource(ogg_name);
		} catch (IOException e) {
			System.out.println("Failed to play track: " + ogg_name);
			return false;
		}
		player.start();
		return  true;
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

}
