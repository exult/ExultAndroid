package com.exult.android;

public class GameSingletons {
	public static GameWindow gwin;
	public static ImageBuf win;
	public static EFileManager fman;
	public static TimeQueue tqueue;
	public static GameMap gmap;
	public static EffectsManager eman;
	public static FontsVgaFile fonts;
	public static void init(GameWindow gw) {
		gwin = gw;
		win = gwin.getWin();
		fman = EFileManager.instanceOf();
		gmap = gwin.getMap();
		tqueue = gwin.getTqueue();
		eman = gwin.getEffects();
		fonts = new FontsVgaFile();
	}
}
