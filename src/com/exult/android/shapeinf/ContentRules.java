package com.exult.android.shapeinf;
import java.io.PushbackInputStream;
import java.io.InputStream;
import com.exult.android.ShapeInfo;
import com.exult.android.EUtil;
/*
 *	Information about shapes accepted/rejected by containers.
 *	This is meant to be stored in a totally ordered vector.
 */
public class ContentRules extends BaseInfo implements Comparable<ContentRules> {
	private int		shape;
	private boolean	accept;
	
	public int getShape()
		{ return shape; }
	boolean acceptsShape() 
		{ return accept; }
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream)in;
		shape = EUtil.ReadInt(txtin);
		if (shape < 0)
			shape = -1;
		accept = EUtil.ReadInt(txtin)!= 0;
		return true;
	}
	@Override
	public int compareTo(ContentRules i2) {
		return shape - i2.shape;
	}
}
