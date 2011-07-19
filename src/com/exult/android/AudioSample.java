package com.exult.android;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
/*
Copyright (C) 2005 The Pentagram team
Copyright (C) 2010 The Exult team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

//CURRENTLY not finished and not used.
public class AudioSample {
	protected int sampleRate;
	protected int	bits;
	protected boolean	stereo;
	protected int	frameSize;
	protected int	decompressorSize;
	protected int	length;

	protected int	bufferSize;
	protected byte buffer[];
	protected int	refcount;
	
	public AudioSample(byte buf[], int sz) {
		this.buffer = buf;
		this.bufferSize = sz;
		refcount = 1;
	}
	public static AudioSample createAudioSample(byte data[], int size) {
		InputStream in = new ByteArrayInputStream(data, 0, size);
		if (VocAudioSample.isThis(in))
			return new VocAudioSample(data,size);
		//++MORE
		else
			return null;
	}
	
	static class VocAudioSample extends AudioSample {
		public VocAudioSample(byte buf[], int sz) {
			super(buf, sz);
		}
		public static boolean isThis(InputStream in) {
			byte buf[] = new byte[19];
			try {
				in.read(buf);
				if (new String(buf).equals("Creative Voice File"))
					return true;
			} catch (IOException e) {
				return false;
			}
			return false;
		}
	}
}
