package com.exult.android.shapeinf;
import java.io.PushbackInputStream;
import java.io.InputStream;
import com.exult.android.EUtil;
import com.exult.android.ShapeInfo;
import com.exult.android.ShapeInfo;

public class FrameUsecodeInfo extends BaseInfo.FrameInfo {
	private int		usecode;	// Usecode function of the frame/quality at hand,
	// or -1 for default shape usecode.
	private String usecodeName;		// Name of usecode fun explicitly assigned.	
	public int get_usecode()
		{ return usecode; }
	public String getUsecodeName()
		{ return usecodeName; }
	public boolean readNew(InputStream in, int version, boolean patch, int game,
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
		boolean type = EUtil.ReadInt(txtin) != 0;
		if (type) {
			usecodeName = null; //+++++++++FINISH ReadStr(in);
			usecode = -1;
		} else {
			usecode = EUtil.ReadInt(txtin, -1);
		}		
		info.setFrameUsecodeInfo(addVectorInfo(this, info.getFrameUsecodeInfo()));
		return true;
	}
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		return new FrameUsecodeInfo().readNew(in, version, patch, game, info);
	}
}
