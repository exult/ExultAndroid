package com.exult.android.shapeinf;
import java.io.PushbackInputStream;
import java.io.InputStream;
import com.exult.android.ShapeInfo;
import com.exult.android.EUtil;


public class PaperdollNpc extends BaseInfo {
	boolean		isFemale;			// Is the NPC Female (or more specifically not male)
	boolean		translucent;		// If the paperdoll should be drawn translucently or not

	// Body info
	short		bodyShape;			// Body Shape
	short		bodyFrame;			// Body Frame

	short		headShape;			// Head Shape
	short		headFrame;			// Normal Head Frame
	short		headFrameHelm;	// Frame when wearing a helm

	short		armsShape;			// Shape for Arms
	short		armsFrame[] = new short[3];		// Frames for arms.
	
	private boolean readNew(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream)in;
		int sexflag = EUtil.ReadInt(txtin);
		if (sexflag == -0xff) {	// means delete entry.
			info.setNpcPaperdollInfo(null);
			setInvalid(true);
			return true;
		}
		isFemale = sexflag != 0;
		translucent = EUtil.ReadInt(txtin) != 0;
		bodyShape = (short)EUtil.ReadInt(txtin);
		bodyFrame = (short)EUtil.ReadInt(txtin);
		headShape = (short)EUtil.ReadInt(txtin);
		headFrame = (short)EUtil.ReadInt(txtin);
		headFrameHelm = (short)EUtil.ReadInt(txtin);
		armsShape = (short)EUtil.ReadInt(txtin);
		armsFrame[0] = (short)EUtil.ReadInt(txtin);
		armsFrame[1] = (short)EUtil.ReadInt(txtin);
		armsFrame[2] = (short)EUtil.ReadInt(txtin);
		if (version < 3)
			// We need this for backward compatibility.
			// We use the setter methods sp that the info
				// will get saved by ES if that is needed.
			info.setGumpData(EUtil.ReadInt(txtin, -1), -1);

		info.setNpcPaperdollInfo(this);
		return true;
	}
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		return (new PaperdollNpc()).readNew(in, version, patch, game, info);
	}

}
