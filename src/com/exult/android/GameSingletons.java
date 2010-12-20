package com.exult.android;

public class GameSingletons {
	public static GameWindow gwin;
	public static ImageBuf win;
	public static EFileManager fman;
	public static TimeQueue tqueue;
	public static GameMap gmap;
	public static EffectsManager eman;
	public static FontsVgaFile fonts;
	public static UsecodeMachine ucmachine;
	public static Conversation conv;	// This stays null until needed.
	public static PartyManager partyman;
	public static GumpManager gumpman;
	public static Game game;
	public static DraggingInfo drag;
	public static Mouse mouse;
	public static Cheat cheat;
	public static void init(GameWindow gw) {
		gwin = gw;
		win = gwin.getWin();
		fman = EFileManager.instanceOf();
		gmap = gwin.getMap();
		tqueue = gwin.getTqueue();
		eman = gwin.getEffects();
		fonts = new FontsVgaFile();
		ucmachine = gwin.getUsecode();
		partyman = new PartyManager();
		gumpman = new GumpManager();
		mouse = new Mouse();
		cheat = new Cheat();
	}
}
