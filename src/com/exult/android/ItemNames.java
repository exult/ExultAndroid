package com.exult.android;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.Vector;

public class ItemNames {
	private static final String		// File section names.
		SHAPES_SECT = 	"shapes",
		MSGS_SECT =		"msgs",
		MISC_SECT =		"miscnames";
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
		Vector<String> msglist = null;
		int first_msg;			// First in exultmsg.txt.  Should
								//   follow those in text.flx.
		int num_text_msgs = 0, num_item_names = 0, num_misc_names = 0, total_msgs = 0;
		byte buf[] = new byte[256];
		
		itemfile.seek(0x54);
		int flxcnt = EUtil.Read4(itemfile);
		first_msg = num_item_names = flxcnt;
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
			System.out.println("Read " + nm + " of length " + itemlen);
			if (i < num_item_names)
				names[i] = nm;
			else if (i - num_item_names < num_text_msgs)
				msgs[i - num_item_names] = nm;
			else 
				misc[remapIndex(doremap, i - num_item_names - num_text_msgs)] = nm;
			
		}
		for (i = first_msg; i < total_msgs; i++)
			msgs[i] = msglist.get(i + 0x400);
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

}
