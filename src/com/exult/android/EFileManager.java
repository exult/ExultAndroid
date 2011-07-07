package com.exult.android;
import java.util.TreeMap;

public class EFileManager {
	static EFileManager instance;
	private TreeMap<String, EFile> fileList;
	private EFileManager() {
		fileList = new TreeMap<String,EFile>();
	}
	public static EFileManager instanceOf() {
		if (instance == null)
			instance = new EFileManager();
		return instance;
	}
	public EFile getFileObject(String nm) {
		if (nm == null)
			return null;
		EFile file = fileList.get(nm);
		if (file != null)
			return file;
		String fname = EUtil.U7exists(nm);
		if (fname == null)
			return null;
		if (EUtil.isFlex(fname))
			file = new FlexFile(fname, nm);
		/* +++++FINISH
		else if (EUtil.isIff(s.name))
			uf = new IFFFile(s.name);
		else if (Table::is_table(s.name))
			uf = new TableFile(s.name);
		*/
		else
			file = new EFile(fname, nm);	// Flat file.
		// Failed
		if (file == null) {
			return null;
		}
		fileList.put(nm, file);
		return file;	
	}
	/*
	 * Try files in reverse order, looking for given objnum in file.
	 */
	public EFile getFileObject(String nm1, String nm2) {
		EFile file = getFileObject(nm2);
		if (file == null)
			file = getFileObject(nm1);
		return file;
	}
	public void remove(EFile file) {
		String nm = file.getIdentifier();
		fileList.remove(nm);
	}
	/*
	 * Retrieve given obj with file.
	 */
	public byte[] retrieve(String nm, int objnum) {
		EFile file = getFileObject(nm);
		if (file != null) {
			byte res[] = file.retrieve(objnum);
			if (res != null)
				return res;
		}
		return null;
	}
	/*
	 * Try files in reverse order, looking for given objnum in file.
	 */
	public byte[] retrieve(String nm1, String nm2, int objnum) {
		byte res[] = retrieve(nm2, objnum);
		if (res == null)
			res = retrieve(nm1, objnum);
		return res;
	}
}
