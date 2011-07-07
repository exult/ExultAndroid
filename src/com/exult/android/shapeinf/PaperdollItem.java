package com.exult.android.shapeinf;
import java.io.PushbackInputStream;
import java.io.InputStream;
import com.exult.android.ShapeInfo;
import com.exult.android.EUtil;

/*
 *	Information about an object's paperdoll.
 *	This is meant to be stored in a totally ordered vector.
 */
public class PaperdollItem extends BaseInfo.FrameInfo {
	// FrameInfo.frame  is the frame in the world (-1 for all)
	// FrameInfo.quality is the spot placed in.
	private short	type;				// For weapons, the arm frame type to use.
								// For headgear, head frame to use.
								// Meaningless for all others.
	private boolean	translucent;	// If the paperdoll should be drawn translucently or not
	private boolean	gender;			// Is this object gender specific

	private short	shape;				// The shape (if -1 use world shape and frame)
	private short	frames[] = new short[4];	// The paperdoll frame and alternates.

	private boolean readNew(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream)in;
		frame = (short)EUtil.ReadInt(txtin);
		translucent = EUtil.ReadInt(txtin) != 0;
		quality = EUtil.ReadInt(txtin);
		int ty = EUtil.ReadInt(txtin);
		if (ty == -255) {	// 'Invalid' marker.
			setInvalid(true);
			return true;	// Ignore remainder of the line.
		}
		if (quality != 0 && quality != 3)	// Field only valid for these spots.
			type = 0;	// Ignore it.
		else if (version == 1) {
			switch (ty)	// Convert old data.
				{
				case 2:
				case 7:
					type = 1; break;
				case 3:
					type = 2; break;
				default:
					type = 0; break;
				}
		} else
			type = (short)ty;
		gender = EUtil.ReadInt(txtin) != 0;
		shape = (short)EUtil.ReadInt(txtin);
		frames[0] = (short)EUtil.ReadInt(txtin);
		// Not all items have all entries; those that need, do, though.
		frames[1] = (short)EUtil.ReadInt(txtin, -1);
		frames[2] = (short)EUtil.ReadInt(txtin, -1);
		frames[3] = (short)EUtil.ReadInt(txtin, -1);
		//System.out.println("PaperDollItem: shape = " + shape +
		//		", frame = " + frame + ", spot = " + quality);
		info.setPaperdollInfo(addVectorInfo(this, info.getPaperdollInfo()));
		return true;
	}
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		return (new PaperdollItem()).readNew(in, version, patch, game, info);
	}
	public void invalidate()
		{ type = -255; setInvalid(true); }
	public int getWorldFrame() 
		{ return frame; }
	public int getObjectSpot() 
		{ return quality; }
	public short getSpotFrame() 
		{ return type; }
	public boolean isTranslucent()
		{ return translucent; }
	public boolean isGenderBased()
		{ return gender; }
	public int getPaperdollShape()
		{ return shape; }
	public int getPaperdollBaseframe() { 
		return frames[0]; 
	}
	public int getPaperdollFrame(int num) {
		if (num < 4)
			return frames[num];
		return num;
	}
}
