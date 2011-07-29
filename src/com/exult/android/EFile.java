package com.exult.android;
import java.io.RandomAccessFile;
import java.io.IOException;

/*
 * This base class represents a flat file.
 */
public class EFile {
	private byte buf[];
	protected String identifier;
	protected RandomAccessFile file;
	protected EFile() { }
	public EFile(String fname, String id) {
		identifier = id;
		buf = null;
		try {
			file = new RandomAccessFile(fname, "r");
		} catch (IOException e) {
			file = null;
		}
	}
	public int numberOfObjects() {
		return 1;
	}
	public byte [] retrieve(int objnum) {
		if (objnum != 0)
			return null;
		if (buf == null) 
			try {
				long len = file.length();
				buf = new byte[(int)len];
				file.read(buf);
			} catch (IOException e) {
				return null;
			}
		return buf;
	}
	public final String getIdentifier() {
		return identifier;
	}
	public String getArchiveType() {
		return "NONE";
	}
	public void close() {
		try {
			file.close();
		} catch (IOException e) { }
		buf = null;
		EFileManager.instanceOf().remove(this);
	}
	/* Files we use: */
	public final static String GAMEDAT		= "<GAMEDAT>/";
	public final static String SHAPES_VGA	= "<STATIC>/shapes.vga";
	public final static String PATCH_SHAPES	= "<PATCH>/shapes.vga";
	public final static String FACES_VGA	= "<STATIC>/faces.vga";
	public final static String PATCH_FACES	= "<PATCH>/faces.vga";
	public final static String GUMPS_VGA	= "<STATIC>/gumps.vga";
	public final static String PATCH_GUMPS	= "<PATCH>/gumps.vga";
	public final static String FONTS_VGA	= "<STATIC>/fonts.vga";
	public final static String PATCH_FONTS	= "<PATCH>/fonts.vga";
	public final static String SPRITES_VGA     = "<STATIC>/sprites.vga";
	public final static String PATCH_SPRITES	= "<PATCH>/sprites.vga";
	public final static String MAINSHP_FLX     = "<STATIC>/mainshp.flx";
	public final static String PATCH_MAINSHP     = "<PATCH>/mainshp.flx";
	public final static String ENDSHAPE_FLX    = "<STATIC>/endshape.flx";
	public final static String SHPDIMS		= "<STATIC>/shpdims.dat";
	public final static String PATCH_SHPDIMS	= "<PATCH>/shpdims.dat";
	public final static String TFA		= "<STATIC>/tfa.dat";
	public final static String PATCH_TFA	= "<PATCH>/tfa.dat";
	public final static String WGTVOL		= "<STATIC>/wgtvol.dat";		
	public final static String PATCH_WGTVOL	= "<PATCH>/wgtvol.dat";		
	public final static String U7CHUNKS	= "<STATIC>/u7chunks";
	public final static String PATCH_U7CHUNKS	= "<PATCH>/u7chunks";
	public final static String U7MAP		= "<STATIC>/u7map";
	public final static String PATCH_U7MAP	= "<PATCH>/u7map";
	public final static String TEXT_FLX	= "<STATIC>/text.flx";
	public final static String PATCH_TEXT	= "<PATCH>/text.flx";
	public final static String EXULTMSG	= "<DATA>/exultmsg.txt";
	public final static String BUNDLE_EXULTMSG = "<BUNDLE>/exultmsg.txt";
	public final static String PATCH_EXULTMSG = "<PATCH>/exultmsg.txt";
	public final static String U7IFIX		= "<STATIC>/u7ifix";
	public final static String PATCH_U7IFIX	= "<PATCH>/u7ifix";
	public final static String U7IREG		= "<GAMEDAT>/u7ireg";
	public final static String MULTIMAP_DIR	= "/map";
	public final static String PALETTES_FLX	= "<STATIC>/palettes.flx";
	public final static String PATCH_PALETTES	= "<PATCH>/palettes.flx";
	public final static String INTRO_DAT	= "<STATIC>/intro.dat";
	public final static String PATCH_INTRO	= "<PATCH>/intro.dat";
	public final static String INTROPAL_DAT	= "<STATIC>/intropal.dat";
	public final static String PATCH_INTROPAL	= "<PATCH>/intropal.dat";
	public final static String U7NBUF_DAT	= "<GAMEDAT>/u7nbuf.dat";
	public final static String NPC_DAT		= "<GAMEDAT>/npc.dat";
	public final static String MONSNPCS	= "<GAMEDAT>/monsnpcs.dat";
	public final static String USEDAT		= "<GAMEDAT>/usecode.dat";
	public final static String USEVARS		= "<GAMEDAT>/usecode.var";
	public final static String FLAGINIT	= "<GAMEDAT>/flaginit";
	public final static String GWINDAT		= "<GAMEDAT>/gamewin.dat";
	public final static String GSCHEDULE	= "<GAMEDAT>/schedule.dat";
	public final static String SCHEDULE_DAT	= "<STATIC>/schedule.dat";
	public final static String SHPDIMS_DAT	= "<STATIC>/shpdims.dat";
	public final static String INITGAME	= "<STATIC>/initgame.dat";
	public final static String PATCH_INITGAME	= "<PATCH>/initgame.dat";
	public final static String USECODE		= "<STATIC>/usecode";
	public final static String PATCH_USECODE	= "<PATCH>/usecode";
	public final static String POINTERS	= "<STATIC>/pointers.shp";
	public final static String PATCH_POINTERS	= "<PATCH>/pointers.shp";
	public final static String MAINMUS		= "<STATIC>/mt32mus.dat";
	public final static String MAINMUS_AD		= "<STATIC>/adlibmus.dat";
	public final static String INTROMUS	= "<STATIC>/intrordm.dat";
	public final static String INTROMUS_AD	= "<STATIC>/introadm.dat";
	public final static String	XMIDI_AD	= "<STATIC>/xmidi.ad";
	public final static String	XMIDI_MT	= "<STATIC>/xmidi.mt";
	public final static String U7SPEECH	= "<STATIC>/u7speech.spc";
	public final static String SISPEECH	= "<STATIC>/sispeech.spc";
	public final static String PATCH_U7SPEECH	= "<PATCH>/u7speech.spc";
	public final static String PATCH_SISPEECH	= "<PATCH>/sispeech.spc";
	public final static String XFORMTBL       	= "<STATIC>/xform.tbl";
	public final static String PATCH_XFORMS    = "<PATCH>/xform.tbl";
	public final static String BLENDS       	= "<STATIC>/blends.dat";
	public final static String PATCH_BLENDS    = "<PATCH>/blends.dat";
	public final static String MONSTERS	= "<STATIC>/monsters.dat";
	public final static String PATCH_MONSTERS	= "<PATCH>/monsters.dat";
	public final static String EQUIP		= "<STATIC>/equip.dat";
	public final static String PATCH_EQUIP	= "<PATCH>/equip.dat";
	public final static String READY		= "<STATIC>/ready.dat";
	public final static String PATCH_READY	= "<PATCH>/ready.dat";
	public final static String WIHH		= "<STATIC>/wihh.dat";
	public final static String PATCH_WIHH	= "<PATCH>/wihh.dat";
	public final static String IDENTITY	= "<GAMEDAT>/identity";
	public final static String ENDGAME		= "<STATIC>/endgame.dat";
	public final static String PATCH_ENDGAME		= "<PATCH>/endgame.dat";
	public final static String ENDSCORE_XMI	= "<STATIC>/endscore.xmi";
	public final static String PATCH_ENDSCORE	= "<PATCH>/endscore.xmi";
	public final static String MIDITMPFILE     = "u7midi";
	public final static String MIDISFXFILE     = "u7sfx";
	public final static String SAVENAME	= "<SAVEGAME>/exult%1$02d%2$s.sav";
	public final static String SAVENAME2	= "<SAVEGAME>/exult.*%1$s.sav"; // Regexp.
	public final static String INTROSND	= "<STATIC>/introsnd.dat";
	public final static String PATCH_INTROSND	= "<PATCH>/introsnd.dat";
	public final static String PATCH_ARMOR	= "<PATCH>/armor.dat";
	public final static String ARMOR		= "<STATIC>/armor.dat";
	public final static String WEAPONS		= "<STATIC>/weapons.dat";
	public final static String PATCH_WEAPONS	= "<PATCH>/weapons.dat";
	public final static String AMMO		= "<STATIC>/ammo.dat";
	public final static String PATCH_AMMO	= "<PATCH>/ammo.dat";
	public final static String PAPERDOL	= "<STATIC>/paperdol.vga";
	public final static String PATCH_PAPERDOL	= "<PATCH>/paperdol.vga";
	public final static String OCCLUDE		= "<STATIC>/occlude.dat";
	public final static String PATCH_OCCLUDE	= "<PATCH>/occlude.dat";
	public final static String CONTAINER	= "<STATIC>/container.dat";
	public final static String PATCH_CONTAINER	= "<PATCH>/container.dat";

	public final static String GSCRNSHOT	= "<GAMEDAT>/scrnshot.shp";
	public final static String GSAVEINFO	= "<GAMEDAT>/saveinfo.dat";
	public final static String GEXULTVER	= "<GAMEDAT>/exult.ver";
	public final static String GNEWGAMEVER	= "<GAMEDAT>/newgame.ver";
	public final static String KEYRINGDAT	= "<GAMEDAT>/keyring.dat";
	public final static String NOTEBOOKXML	= "<GAMEDAT>/notebook.xml";

	public final static String TEXTMSGS	= "<STATIC>/textmsg.txt";
	public final static String PATCH_TEXTMSGS	= "<PATCH>/textmsg.txt";
	public final static String PATCH_MINIMAPS	= "<PATCH>/minimaps.vga";

	public final static String R_SINTRO = "<STATIC>/r_sintro.xmi";
	public final static String A_SINTRO = "<STATIC>/a_sintro.xmi";
	public final static String R_SEND = "<STATIC>/r_send.xmi";
	public final static String A_SEND = "<STATIC>/a_send.xmi";

	public final static String U7VOICE_FLX  = "<STATIC>/u7voice.flx";
	public final static String MAINMENU_TIM = "<STATIC>/mainmenu.tim";

	public final static String EXULT_FLX = "<DATA>/exult.flx";
	public final static String EXULT_BG_FLX = "<DATA>/exult_bg.flx";
	public final static String EXULT_SI_FLX = "<DATA>/exult_si.flx";
	public final static String EXULT_GAM_FLX = "<DATA>/exult_%s.flx";

	public final static String BUNDLE_EXULT_FLX = "<BUNDLE>/exult.flx";
	public final static String BUNDLE_EXULT_BG_FLX = "<BUNDLE>/exult_bg.flx";
	public final static String BUNDLE_EXULT_SI_FLX = "<BUNDLE>/exult_si.flx";
	public final static String BUNDLE_EXULT_GAM_FLX = "<BUNDLE>/exult_%s.flx";

	public final static String AUTONOTES	= "autonotes.txt";

	public final static String PATCH_KEYS	= "<PATCH>/patchkeys.txt";

	public final static String EXULT_SERVER	= "<GAMEDAT>/exultserver";

	public final static int NUM_FONTS = 20;

	// U7 game names in "exult.cfg":
	public final static String CFG_BG_NAME		= "blackgate";
	public final static String CFG_FOV_NAME	= "forgeofvirtue";
	public final static String CFG_SI_NAME		= "serpentisle";
	public final static String CFG_SS_NAME		= "silverseed";

	// U7 game titles in "exult.cfg";:
	public final static String CFG_BG_TITLE	= "ULTIMA VII\nTHE BLACK GATE";
	public final static String CFG_FOV_TITLE	= "ULTIMA VII\nTHE FORGE OF VIRTUE";
	public final static String CFG_SI_TITLE	= "ULTIMA VII PART 2\nSERPENT ISLE";
	public final static String CFG_SS_TITLE	= "ULTIMA VII PART 2\nTHE SILVER SEED";

	// Exult SFX Packages:
	public final static String SFX_ROLAND_BG	= "sqsfxbg.flx";
	public final static String SFX_ROLAND_SI	= "sqsfxsi.flx";
	public final static String SFX_BLASTER_BG	= "jmsfx.flx";
	public final static String SFX_BLASTER_SI	= "jmsisfx.flx";
	public final static String SFX_MIDIFILE	= "midisfx.flx";

	// Sections in the exult_xx.flx file, from 'data/exult_xx_flx.h':
	public final static int	EXULT_BG_FLX_BGMAP_SHP = 0;
	public final static int	EXULT_BG_FLX_DEFAULTKEYS_TXT = 1;
	public final static int	EXULT_BG_FLX_MR_FACES_SHP = 2;
	public final static int	EXULT_BG_FLX_U7MENUPAL_PAL = 3;
	public final static int	EXULT_BG_FLX_BG_PAPERDOL_VGA = 4;
	public final static int	EXULT_BG_FLX_BG_MR_FACES_VGA = 5;
	public final static int	EXULT_BG_FLX_BODIES_TXT = 6;
	public final static int	EXULT_BG_FLX_PAPERDOL_INFO_TXT = 7;
	public final static int	EXULT_BG_FLX_SHAPE_INFO_TXT = 8;
	public final static int	EXULT_BG_FLX_SHAPE_FILES_TXT = 9;
	public final static int	EXULT_BG_FLX_AVATAR_DATA_TXT = 10;
	public final static int	EXULT_BG_FLX_BLENDS_DAT = 11;
	public final static int	EXULT_BG_FLX_CONTAINER_DAT = 12;
	
	public final static int	EXULT_SI_FLX_SIMAP_SHP = 0;
	public final static int	EXULT_SI_FLX_DEFAULTKEYS_TXT = 1;
	public final static int	EXULT_SI_FLX_BODIES_TXT = 2;
	public final static int	EXULT_SI_FLX_PAPERDOL_INFO_TXT = 3;
	public final static int	EXULT_SI_FLX_SHAPE_INFO_TXT = 4;
	public final static int	EXULT_SI_FLX_SHAPE_FILES_TXT = 5;
	public final static int	EXULT_SI_FLX_AVATAR_DATA_TXT = 6;
	public final static int	EXULT_SI_FLX_BLENDS_DAT = 7;
	public final static int	EXULT_SI_FLX_CONTAINER_DAT = 8;
	
	// exult.flx sections:
	public final static int	EXULT_FLX_QUOTES_TXT = 		0;
	public final static int	EXULT_FLX_CREDITS_TXT = 		1;
	public final static int	EXULT_FLX_EXULT_LOGO_SHP =		2;
	public final static int	EXULT_FLX_EXULT0_PAL =		3;
	public final static int	EXULT_FLX_MEDITOWN_MID =		4;
	public final static int	EXULT_FLX_FONT_SHP =		5;
	public final static int	EXULT_FLX_FONTON_SHP =		6;
	public final static int	EXULT_FLX_NAVFONT_SHP =		7;
	public final static int	EXULT_FLX_NAVFONTON_SHP =		8;
	public final static int	EXULT_FLX_POINTERS_SHP =		9;
	public final static int	EXULT_FLX_EXTRAS_SHP =		10;
	public final static int	EXULT_FLX_SAVEGUMP_SHP =		11;
	public final static int	EXULT_FLX_SAV_DOWNDOWN_SHP =		12;
	public final static int	EXULT_FLX_SAV_DOWN_SHP =		13;
	public final static int	EXULT_FLX_SAV_UP_SHP =		14;
	public final static int	EXULT_FLX_SAV_UPUP_SHP =		15;
	public final static int	EXULT_FLX_SAV_SLIDER_SHP =		16;
	public final static int	EXULT_FLX_SAV_SELECTED_SHP =		17;
	public final static int	EXULT_FLX_GAMEPLAYOPTIONS_SHP =		18;
	public final static int	EXULT_FLX_GAMEMENU_SHP =		19;
	public final static int	EXULT_FLX_AUDIOOPTIONS_SHP =		20;
	public final static int	EXULT_FLX_VIDEOOPTIONS_SHP =		21;
	public final static int	EXULT_FLX_HP_BAR_SHP =		22;
	public final static int	EXULT_FLX_SFX_ICON_SHP =		23;
	public final static int	EXULT_FLX_NOTEBOOK_SHP =		24;
	public final static int	EXULT_FLX_STATS_EXTRA_SHP =		25;
	public final static int	EXULT_FLX_MTGM_MID =		26;

}
