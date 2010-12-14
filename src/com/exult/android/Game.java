package com.exult.android;
import java.util.HashMap;

public abstract class Game extends GameSingletons {
	private int gameType;
	private HashMap<String,Integer> shapes;
	public Game() {
		shapes = new HashMap<String,Integer>();
		GameSingletons.game = this;
	}
	public void addShape(String nm, int shapenum) {
		shapes.put(nm, shapenum);
	}
	public int getGameType() {
		return gameType;
	}
	public int getShape(String nm) {
		Integer i = shapes.get(nm);
		return i == null ? 0 : i;
	}
	public static class BGGame extends Game {
		public BGGame() {
			addShape("gumps/check",2);
			addShape("gumps/fileio",3);
			addShape("gumps/fntext",4);
			addShape("gumps/loadbtn",5);
			addShape("gumps/savebtn",6);
			addShape("gumps/halo",7);
			addShape("gumps/disk",24);
			addShape("gumps/heart",25);
			addShape("gumps/statatts",28);
			addShape("gumps/musicbtn",29);
			addShape("gumps/speechbtn",30);
			addShape("gumps/soundbtn",31);	
			addShape("gumps/spellbook",43);
			addShape("gumps/statsdisplay",47);
			addShape("gumps/combat",46);
			addShape("gumps/quitbtn",56);
			addShape("gumps/yesnobox",69);
			addShape("gumps/yesbtn",70);
			addShape("gumps/nobtn",71);
			addShape("gumps/book",32);
			addShape("gumps/scroll",55);
			addShape("gumps/combatmode",12);
			addShape("gumps/slider",14);
			addShape("gumps/slider_diamond",15);
			addShape("gumps/slider_right",16);
			addShape("gumps/slider_left",17);

			addShape("gumps/box", 0);
			addShape("gumps/crate", 1);
			addShape("gumps/barrel", 8);
			addShape("gumps/bag", 9);
			addShape("gumps/backpack", 10);
			addShape("gumps/basket", 11);
			addShape("gumps/chest", 22);
			addShape("gumps/shipshold", 26);
			addShape("gumps/drawer", 27);
			addShape("gumps/woodsign", 49);
			addShape("gumps/tombstone", 50);
			addShape("gumps/goldsign", 51);
			addShape("gumps/body", 53);

			addShape("sprites/map", 22);
			addShape("sprites/cheatmap", EFile.EXULT_BG_FLX_BGMAP_SHP);
		}
	}
}
