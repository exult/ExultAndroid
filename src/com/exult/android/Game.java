package com.exult.android;
import java.util.HashMap;

import com.exult.android.shapeinf.ShapeInfoLookup;

public abstract class Game extends GameSingletons {
	protected int gameType;
	private boolean newGame;
	private String avName = "Newbie";	//++FOR NOW.
	private int avSex = -1;
	private int avSkin = -1;
	private HashMap<String,Integer> shapes;
	private HashMap<String,ShapeInfoLookup.StringIntPair> resources; 
	public Game() {
		shapes = new HashMap<String,Integer>();
		resources = new HashMap<String,ShapeInfoLookup.StringIntPair>();
		GameSingletons.game = this;
	}
	public boolean isNewGame() {
		return newGame;
	}
	public void setNewGame() {
		newGame = true;
	}
	public String getAvName() {
		return avName;
	}
	public void setAvName(String nm) {
		avName = nm;
	}
	public void clearAvName() {
		avName = null;
		newGame = false;
	}
	public void clearAvSex() {
		avSex = -1;
	}
	public void clearAvSkin() {
		avSkin = -1;
	}
	public int getAvSex() {
		return avSex;
	}
	public int getAvSkin() {
		return avSkin;
	}
	public void addShape(String nm, int shapenum) {
		shapes.put(nm, shapenum);
	}
	public int getGameType() {
		return gameType;
	}
	public final boolean isSI() {
		return gameType == EConst.SERPENT_ISLE;
	}
	public final boolean isBG() {
		return gameType == EConst.BLACK_GATE;
	}
	public int getShape(String nm) {
		Integer i = shapes.get(nm);
		return i == null ? 0 : i;
	}
	public void addResource(String name, String str, int num) {
		ShapeInfoLookup.StringIntPair ent = new ShapeInfoLookup.StringIntPair(str, num);
		resources.put(name, ent);
	}
	public ShapeInfoLookup.StringIntPair getResource(String res) {
		ShapeInfoLookup.StringIntPair ent = resources.get(res);
		if (ent != null)
			return ent;
		ExultActivity.fatal("Resource " + res + " not found!");
		return null;
	}
	public static class BGGame extends Game {
		public BGGame() {
			gameType = EConst.BLACK_GATE;
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
			
			addShape("gumps/scroll_spells", 0xfff);
			addShape("gumps/spell_scroll", 0xfff);
			addShape("gumps/jawbone", 0xfff);
			addShape("gumps/tooth", 0xfff);

			addShape("sprites/map", 22);
			addShape("sprites/cheatmap", EFile.EXULT_BG_FLX_BGMAP_SHP);
			
			String exultflx = EFile.EXULT_FLX;
			String gameflx = EFile. EXULT_BG_FLX;
			
			addResource("files/shapes/count", null, 9);
			addResource("files/shapes/0", EFile.SHAPES_VGA, 0);
			addResource("files/shapes/1", EFile.FACES_VGA, 0);
			addResource("files/shapes/2", EFile.GUMPS_VGA, 0);
			addResource("files/shapes/3", EFile.SPRITES_VGA, 0);
			addResource("files/shapes/4", EFile.MAINSHP_FLX, 0);
			addResource("files/shapes/5", EFile.ENDSHAPE_FLX, 0);
			addResource("files/shapes/6", EFile.FONTS_VGA, 0);
			addResource("files/shapes/7", exultflx, 0);
			addResource("files/shapes/8", gameflx, 0);

			addResource("files/gameflx", gameflx, 0);

			addResource("files/paperdolvga", gameflx, EFile.EXULT_BG_FLX_BG_PAPERDOL_VGA);
			//++??addResource("files/mrfacesvga", gameflx, EFile.EXULT_BG_FLX_BG_MR_FACES_VGA);
			addResource("config/defaultkeys", gameflx, EFile.EXULT_BG_FLX_DEFAULTKEYS_TXT);
			addResource("config/bodies", gameflx, EFile.EXULT_BG_FLX_BODIES_TXT);
			addResource("config/paperdol_info", gameflx, EFile.EXULT_BG_FLX_PAPERDOL_INFO_TXT);
			addResource("config/shape_info", gameflx, EFile.EXULT_BG_FLX_SHAPE_INFO_TXT);
			addResource("config/shape_files", gameflx, EFile.EXULT_BG_FLX_SHAPE_FILES_TXT);
			addResource("config/avatar_data", gameflx, EFile.EXULT_BG_FLX_AVATAR_DATA_TXT);

			addResource("palettes/count", null, 18);
			addResource("palettes/0", EFile.PALETTES_FLX, 0);
			addResource("palettes/1", EFile.PALETTES_FLX, 1);
			addResource("palettes/2", EFile.PALETTES_FLX, 2);
			addResource("palettes/3", EFile.PALETTES_FLX, 3);
			addResource("palettes/4", EFile.PALETTES_FLX, 4);
			addResource("palettes/5", EFile.PALETTES_FLX, 5);
			addResource("palettes/6", EFile.PALETTES_FLX, 6);
			addResource("palettes/7", EFile.PALETTES_FLX, 7);
			addResource("palettes/8", EFile.PALETTES_FLX, 8);
			addResource("palettes/9", EFile.PALETTES_FLX, 10);
			addResource("palettes/10", EFile.PALETTES_FLX, 11);
			addResource("palettes/11", EFile.PALETTES_FLX, 12);
			addResource("palettes/12", EFile.INTROPAL_DAT, 0);
			addResource("palettes/13", EFile.INTROPAL_DAT, 1);
			addResource("palettes/14", EFile.INTROPAL_DAT, 2);
			addResource("palettes/15", EFile.INTROPAL_DAT, 3);
			addResource("palettes/16", EFile.INTROPAL_DAT, 4);
			addResource("palettes/17", EFile.INTROPAL_DAT, 5);

			addResource("palettes/patch/0", EFile.PATCH_PALETTES, 0);
			addResource("palettes/patch/1", EFile.PATCH_PALETTES, 1);
			addResource("palettes/patch/2", EFile.PATCH_PALETTES, 2);
			addResource("palettes/patch/3", EFile.PATCH_PALETTES, 3);
			addResource("palettes/patch/4", EFile.PATCH_PALETTES, 4);
			addResource("palettes/patch/5", EFile.PATCH_PALETTES, 5);
			addResource("palettes/patch/6", EFile.PATCH_PALETTES, 6);
			addResource("palettes/patch/7", EFile.PATCH_PALETTES, 7);
			addResource("palettes/patch/8", EFile.PATCH_PALETTES, 8);
			addResource("palettes/patch/9", EFile.PATCH_PALETTES, 10);
			addResource("palettes/patch/10", EFile.PATCH_PALETTES, 11);
			addResource("palettes/patch/11", EFile.PATCH_PALETTES, 12);
			addResource("palettes/patch/12", EFile.PATCH_INTROPAL, 0);
			addResource("palettes/patch/13", EFile.PATCH_INTROPAL, 1);
			addResource("palettes/patch/14", EFile.PATCH_INTROPAL, 2);
			addResource("palettes/patch/15", EFile.PATCH_INTROPAL, 3);
			addResource("palettes/patch/16", EFile.PATCH_INTROPAL, 4);
			addResource("palettes/patch/17", EFile.PATCH_INTROPAL, 5);

			addResource("xforms/count", null, 20);
			addResource("xforms/0", EFile.XFORMTBL, 0);
			addResource("xforms/1", EFile.XFORMTBL, 1);
			addResource("xforms/2", EFile.XFORMTBL, 2);
			addResource("xforms/3", EFile.XFORMTBL, 3);
			addResource("xforms/4", EFile.XFORMTBL, 4);
			addResource("xforms/5", EFile.XFORMTBL, 5);
			addResource("xforms/6", EFile.XFORMTBL, 6);
			addResource("xforms/7", EFile.XFORMTBL, 7);
			addResource("xforms/8", EFile.XFORMTBL, 8);
			addResource("xforms/9", EFile.XFORMTBL, 9);
			addResource("xforms/10", EFile.XFORMTBL, 10);
			addResource("xforms/11", EFile.XFORMTBL, 11);
			addResource("xforms/12", EFile.XFORMTBL, 12);
			addResource("xforms/13", EFile.XFORMTBL, 13);
			addResource("xforms/14", EFile.XFORMTBL, 14);
			addResource("xforms/15", EFile.XFORMTBL, 15);
			addResource("xforms/16", EFile.XFORMTBL, 16);
			addResource("xforms/17", EFile.XFORMTBL, 17);
			addResource("xforms/18", EFile.XFORMTBL, 18);
			addResource("xforms/19", EFile.XFORMTBL, 19);
		}
	}
}
