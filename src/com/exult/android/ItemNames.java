package com.exult.android;
import java.io.RandomAccessFile;
import java.io.IOException;
// UNUSED import java.util.Vector;

public class ItemNames {
	/* +++FINISH
	private static final String		// File section names.
		SHAPES_SECT = 	"shapes",
		MSGS_SECT =		"msgs",
		MISC_SECT =		"miscnames";
	*/
	public static String names[];	// The game items' names.
	public static String msgs[];	// Msgs. (0x400 - ).
	public static String misc[];	//Frames, etc (0x500 - 0x5ff/0x685 (BG/SI)).
	
	public final static void init(boolean si, boolean expansion) {
		// Exult new-style messages?
		RandomAccessFile txtfile = EUtil.U7open2(EFile.PATCH_TEXTMSGS, EFile.TEXTMSGS);
		if (txtfile != null) {
			SetupText(txtfile);
		} else {
			txtfile = EUtil.U7open2(EFile.PATCH_TEXT, EFile.TEXT_FLX);
			RandomAccessFile exultmsg = null;
			/* +++++++++FINISH?
			const char *msgs = BUNDLE_CHECK(BUNDLE_EXULTMSG, EXULTMSG);
			if (is_patch && U7exists(PATCH_EXULTMSG))
				U7open(exultmsg, PATCH_EXULTMSG, true);
			else
				U7open(exultmsg, msgs, true);
			*/
			try {
				SetupItemNames(txtfile,  exultmsg, si, expansion);
			} catch (IOException e) {
				System.out.println("ERROR reading in " + EFile.TEXT_FLX);
			}
		}
	}
	/*
	 *	Set up names of items.
	 *
	 *	Msg. names start at 0x400.
	 *	Frame names start at entry 0x500 (reagents,medallions,food,etc.).
	 */

	private static void SetupItemNames
		(
		RandomAccessFile itemfile,
		RandomAccessFile msgfile,
		boolean si,
		boolean expansion
		) throws IOException {
		/* UNUSED  +++LATER
		Vector<String> msglist = null;
		int first_msg;			// First in exultmsg.txt.  Should
		*/
								//   follow those in text.flx.
		int num_text_msgs = 0, num_item_names = 0, num_misc_names = 0, total_msgs = 0;
		byte buf[] = new byte[256];
		
		itemfile.seek(0x54);
		int flxcnt = EUtil.Read4(itemfile);
		/*UNUSED first_msg = */ num_item_names = flxcnt;
		if (flxcnt > 0x400) {
			num_item_names = 0x400;
			num_text_msgs = flxcnt - 0x400;
			if (flxcnt > 0x500) {
				num_text_msgs = 0x100;
				num_misc_names = flxcnt - 0x500;
				int last_name = si ? 0x686 : 0x600;	// Discard all starting from this.
				if (flxcnt > last_name) {
					num_misc_names = last_name - 0x500;
					flxcnt = last_name;
				}
			}
			total_msgs = num_text_msgs;
		}
		if (msgfile != null) {		// Exult msgs. too?
			/*
			first_msg = Read_text_msg_file(msgfile, msglist);
			if (first_msg >= 0) {
				first_msg -= 0x400;
				if (first_msg < num_text_msgs) {
					cerr << "Exult msg. # " << first_msg <<
						" conflicts with 'text.flx'" << endl;
					first_msg = num_text_msgs;
				}
				total_msgs = static_cast<int>(msglist.size() - 0x400);
			} else
				first_msg = num_text_msgs;
			*/
		}
		names = new String[num_item_names];
		msgs = new String[total_msgs];
		misc = new String[num_misc_names];
			// Hack alert: move SI misc_names around to match those of SS.
		boolean doremap = si && !expansion;
		if (doremap)
			flxcnt -= 11;	// Just to be safe.
		int i;
		for(i=0; i < flxcnt; i++)
			{
			itemfile.seek(0x80+i*8);
			int itemoffs = EUtil.Read4(itemfile);
			if(itemoffs == 0)
				continue;
			int itemlen = EUtil.Read4(itemfile);
			itemfile.seek(itemoffs);
			if (itemlen > buf.length)
				buf = new byte[itemlen];
			itemfile.read(buf, 0, itemlen);
			while (itemlen > 0 && buf[itemlen - 1] == 0)	// Skip ending nulls.
				itemlen--;
			String nm = new String(buf, 0, itemlen);
			if (i < num_item_names) {
				names[i] = nm;
			} else if (i - num_item_names < num_text_msgs) {
				msgs[i - num_item_names] = nm;
			} else { 
				misc[remapIndex(doremap, i - num_item_names - num_text_msgs)] = nm;
			}
		}
		/* +++++FINISH
		for (i = first_msg; i < total_msgs; i++)
			msgs[i] = msglist.get(i + 0x400);
		*/
		num_text_msgs = total_msgs;
	} 
	private static int remapIndex(boolean remap, int index) {
		if (!remap)
			return index;
		if (index >= 0x0fa)
			return index +11;
		else if (index >= 0x0b2)
			return index +10;
		else if (index >= 0x0af)
			return index +9;
		else if (index >= 0x094)
			return index +8;
		else if (index >= 0x08b)
			return index +7;
		else if (index >= 0x07f)
			return index +2;
		else
			return index;
	}
	/*
	 *	This sets up item names and messages from Exult's new file,
	 *	"textmsgs.txt".
	 */
	private static void SetupText(RandomAccessFile txtfile){
		/* +++++++LATER
		ReadTextMsgFile(txtfile, names, SHAPES_SECT);
		ReadTextMsgFile(txtfile, msgs, MSGS_SECT);
		ReadTextMsgFile(txtfile, misc, MISC_SECT);
		*/
	}
	/*
	 *	Message #'s.  These are (offset-0x400) in text.flx and exultmsg.txt:
	 */
	public static final int first_move_aside = 0x00,	// For guards when blocked.
	 last_move_aside = 0x02,
	 first_preach = 0x03, last_preach = 0x07,
	 first_preach2 = 0x08, last_preach2 = 0x0b,
	 first_amen = 0x0c, last_amen = 0x0f,
	 first_thief = 0x10, last_thief = 0x13,
	 first_talk = 0x14, last_talk = 0x16,
	 first_waiter_ask = 0x1b, last_waiter_ask = 0x1f,
	 first_more_food = 0x20, last_more_food = 0x24,
	 first_munch = 0x25, last_munch = 0x28,
	 first_ouch = 0x29,
	 last_ouch = 0x2c,
	 first_need_help = 0x30,
	 last_need_help = 0x33,
	 first_will_help = 0x34,
	 last_will_help = 0x36,
	 first_to_battle = 0x39,
	 last_to_battle = 0x3b,
	 first_farmer = 0x3f, last_farmer = 0x41,
	 first_miner = 0x42, last_miner = 0x44,
	 first_miner_gold = 0x45, last_miner_gold = 0x47,
	 first_flee = 0x48,
	 last_flee = 0x4e,
	 first_farmer2 = 0x60, last_farmer2 = 0x62,
	 first_lamp_on = 0x63,
	 last_lamp_on = 0x66,
	 lamp_off = 0x67,
	 first_call_police = 0x69,
	 last_call_police = 0x6d,
	 first_call_guards = 0x6c,
	 last_call_guards = 0x6d,
	 first_theft = 0x6e,		// Warnings.
	 last_theft = 0x70,
	 first_close_shutters = 0x71,
	 last_close_shutters = 0x73,
	 first_open_shutters = 0x74,
	 last_open_shutters = 0x76,
	 first_hunger = 0x77,		// A little hungry.  (3 of each).
	 first_needfood = 0x7a,	// Must have food.
	 first_starving = 0x7b,	// Starving.
	 heard_something = 0x95,
	 first_awakened = 0x95,
	 last_awakened = 0x9a,
	 first_magebane_struck = 0x9b,	// (SI only).
	 last_magebane_struck = 0x9d;	// (SI only).
	//	Messages in exultmsg.txt ( - 0x400):
	public static final int first_chair_thief = 0x100, last_chair_thief = 0x104,
	 first_waiter_banter = 0x105, last_waiter_banter = 0x107,
	 first_waiter_serve = 0x108, last_waiter_serve = 0x109,
	 first_bed_occupied = 0x10a, num_bed_occupied = 3,
	 first_catchup = 0x10d, last_catchup = 0x10f,
	 with_help_from = 0x110, exult_team = 0x111, driven_by_exult = 0x112,
	 end_of_ultima7 = 0x113, end_of_britannia = 0x114,
	 you_cannot_do_that = 0x115, damn_avatar = 0x116,
	 blackgate_destroyed = 0x117, guardian_has_stopped = 0x118,
	 txt_screen0 = 0x119, //to 0x11E
	 txt_screen1 = 0x11F, //to 0x128
	 txt_screen2 = 0x129, //to 0x12E
	 txt_screen3 = 0x12F, //to 0x134
	 txt_screen4 = 0x135, //to 0x138
	 lord_castle = 0x139, dick_castle = 0x13A,
	 bg_fellow = 0x13B, //to 0x13D
	 my_leige = 0x13E, yo_homes = 0x53F,
	 all_we0 = 0x140, //to 0x541
	 and_a0 = 0x142, //to 0x543
	 indeed = 0x144, //to 0x545
	 iree = 0x146,
	 stand_back = 0x147,
	 jump_back = 0x148,
	 batlin = 0x149,
	 you_shall = 0x14B,
	 there_i = 0x14D,
	 batlin2 = 0x14F,
	 you_must = 0x151,
	 soon_i = 0x153,
	 tis_my = 0x155;

}
