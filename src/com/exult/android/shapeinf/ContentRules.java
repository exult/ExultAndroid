package com.exult.android.shapeinf;
import java.io.PushbackInputStream;
import java.io.InputStream;
import com.exult.android.ShapeInfo;
import com.exult.android.EUtil;
/*
 *	Information about shapes accepted/rejected by containers.
 *	This is meant to be stored in a totally ordered vector.
 */
public class ContentRules extends BaseInfo.OneKeyInfo {
	// Key is the shape;
	private boolean	accept;
	
	public int getShape()
		{ return keyval; }
	boolean acceptsShape() 
		{ return accept; }
	private boolean readNew(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream)in;
		keyval = EUtil.ReadInt(txtin);
		if (keyval < 0)
			keyval = -1;
		accept = EUtil.ReadInt(txtin)!= 0;
		info.setContentRules(addVectorInfo(this, info.getContentRules()));
		return true;
	}
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		return (new ContentRules()).readNew(in, version, patch, game, info);
	}
}
