package com.exult.android.shapeinf;
import java.io.PushbackInputStream;
import java.io.InputStream;
import com.exult.android.EUtil;
import com.exult.android.ShapeInfo;
/*
 *	Information about frame names.
 *	This is meant to be stored in a totally ordered vector.
 */
public class FrameNameInfo extends BaseInfo.FrameInfo {
	private short	type;		// How the entry is used.
	private int		msgid;		// Item name index in misc_names.
	private int		othermsg;	// Suffix/prefix or default message, depending on type
	
	public int getType() {
		return type;
	}
	public int getMsgid() {
		return msgid;
	}
	public int getOthermsg() {
		return othermsg;
	}
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream) in;
		frame = EUtil.ReadInt(txtin);
		if (frame < 0)
			frame = -1;
		else
			frame &= 0xff;
		quality = EUtil.ReadInt(txtin);
		if (quality < 0)
			quality = -1;
		else
			quality &= 255;
		type = (short)EUtil.ReadInt(txtin);
		if (type >= 0) {
			msgid = EUtil.ReadInt(txtin);
			othermsg = EUtil.ReadInt(txtin, -255);
		}		
		//System.out.println("frameNameInfo for frame " + frame + ", quality = " + quality);
		info.setFrameNameInfo(addVectorInfo(this, info.getFrameNameInfo()));
		return true;
	}
}
