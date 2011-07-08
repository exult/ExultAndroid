package com.exult.android;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.exult.android.NewFileGump.SaveGameDetails;
import com.exult.android.NewFileGump.SaveGameParty;
import com.exult.android.NewFileGump.SaveInfo;

/*
 * A native Android save/restore screen.
 */
public class AndroidSave extends GameSingletons {
	private View myView, mainView;
	private ListView filesView;
	private EditText editView;
	private Button saveBtn, loadBtn, deleteBtn, cancelBtn;
	SaveInfo	games[];		// The list of savegames
	SaveAdapter adapter;
	int		num_games;	// Number of save games
	int		first_free;	// The number of the first free savegame
	int		selected = -1;
	CheckBox selectedBtn = null;
	
	VgaFile.ShapeFile cur_shot;		// Screenshot for current game
	SaveGameDetails cur_details;	// Details of current game
	SaveGameParty cur_party[];	// Party of current game

	// Gamedat is being used as a 'quicksave'
	VgaFile.ShapeFile gd_shot;		// Screenshot in Gamedat
	SaveGameDetails gd_details;	// Details in Gamedat
	SaveGameParty gd_party[];	// Parts in Gamedat

	VgaFile.ShapeFile screenshot;		// The picture to be drawn
	NewFileGump.SaveGameDetails details;	// The game details to show
	NewFileGump.SaveGameParty party[];		// The party to show
	boolean is_readable;		// Is the save game readable
	String filename;		// Filename of the savegame, if exists

	public AndroidSave(Activity exult) {
		myView = exult.findViewById(R.id.save_restore);
		mainView = exult.findViewById(R.id.main_layout);
		mainView.setVisibility(View.INVISIBLE);
		myView.setVisibility(View.VISIBLE);
		filesView = (ListView) exult.findViewById(R.id.sr_files);
		editView = (EditText) exult.findViewById(R.id.sr_editname);
		setButtonHandlers(exult);		
		LoadSaveGameDetails(exult);
		setListHandler();
	}
	private void setListHandler() {
		filesView.setOnItemClickListener(new ListView.OnItemClickListener() {
	        @Override
	        public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
	            //filesView.setSelection(pos);
	            SaveInfo g = games[pos];
	            Boolean enabled = true;
	            CheckBox btn = (CheckBox) v.findViewById(R.id.savename_choice);
	            if (btn == selectedBtn) {
	            	editView.setText("");
	            	selectedBtn.setChecked(false);
	            	selected = -1;
	            	selectedBtn = null;
	            	enabled = false;
	            } else {
	            	editView.setText(g.toString());
	            	btn.setChecked(true);
	            	if (selectedBtn != null)
	            		selectedBtn.setChecked(false);
	            	selected = pos;
	            	selectedBtn = btn;
	            }
	            saveBtn.setEnabled(enabled);
	            loadBtn.setEnabled(enabled);
	            deleteBtn.setEnabled(enabled);
	        }
	    });

	}
	private void setButtonHandlers(Activity exult) {
		saveBtn = (Button) exult.findViewById(R.id.save_button);
		loadBtn = (Button) exult.findViewById(R.id.load_button);
		deleteBtn = (Button) exult.findViewById(R.id.delete_button);
		saveBtn.setEnabled(false);
        loadBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
    	cancelBtn = (Button) exult.findViewById(R.id.save_cancel_button);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { close();}
        });
	}
	private void close() {
		mainView.setVisibility(View.VISIBLE);
		myView.setVisibility(View.INVISIBLE);
	}
	void LoadSaveGameDetails(Activity exult) {	// Loads (and sorts) all the savegame details
		int		i;
		// Gamedat Details
		/* +++++++++FINISH
		gwin.getSaveinfo(gd_shot, gd_details, gd_party);
		*/
		// Current screenshot
		cur_shot = gwin.createMiniScreenshot(false);	// Quick but low quality.

		// Current Details
		cur_details = new SaveGameDetails();

		if (gd_details != null) 
			cur_details.save_count = gd_details.save_count;
		else cur_details.save_count = 0;

		cur_details.party_size = (byte)(partyman.getCount()+1);
		cur_details.game_day = (short) (clock.getTotalHours() / 24);
		cur_details.game_hour = (byte)clock.getHour();
		cur_details.game_minute = (byte)clock.getMinute();
		
		Calendar timeinfo = Calendar.getInstance();

		cur_details.real_day = (byte)timeinfo.get(Calendar.DAY_OF_MONTH);
		cur_details.real_hour = (byte)timeinfo.get(Calendar.HOUR);
		cur_details.real_minute = (byte)timeinfo.get(Calendar.MINUTE);
		cur_details.real_month = (byte)(timeinfo.get(Calendar.MONTH)+1);
		cur_details.real_year = (short)timeinfo.get(Calendar.YEAR);
		cur_details.real_second = (byte)timeinfo.get(Calendar.SECOND);
		// Current Party
		cur_party = new SaveGameParty[cur_details.party_size];
		for (i=0; i<cur_details.party_size ; i++) {
			Actor npc;
			if (i == 0)
				npc = gwin.getMainActor();
			else
				npc = gwin.getNpc(partyman.getMember(i-1));
			cur_party[i] = new SaveGameParty();
			String namestr = npc.getNpcName();
			cur_party[i].namestr = namestr;
			int j, namelen = Math.min(namestr.length(), cur_party[i].name.length);
			for (j = 0; j < namelen; ++j)
				cur_party[i].name[j] = (byte)namestr.charAt(j);
			for ( ; j < namelen; ++j)
				cur_party[i].name[j] = (byte)0;
			cur_party[i].shape = (short) npc.getShapeNum();
			cur_party[i].shape_file = npc.getShapeFile();

			cur_party[i].dext = (byte)npc.getProperty(Actor.dexterity);
			cur_party[i].str = (byte)npc.getProperty(Actor.strength);
			cur_party[i].intel = (byte)npc.getProperty(Actor.intelligence);
			cur_party[i].health = (byte)npc.getProperty(Actor.health);
			cur_party[i].combat = (byte)npc.getProperty(Actor.combat);
			cur_party[i].mana = (byte)npc.getProperty(Actor.mana);
			cur_party[i].magic = (byte)npc.getProperty(Actor.magic);
			cur_party[i].training = (byte)npc.getProperty(Actor.training);
			cur_party[i].exp = npc.getProperty(Actor.exp);
			cur_party[i].food = (byte)npc.getProperty(Actor.food_level);
			cur_party[i].flags = npc.getFlags();
			cur_party[i].flags2 = npc.getFlags2();
		}
		party = cur_party;
		screenshot = cur_shot;
		details = cur_details;
		// Now read save game details
		String mask = String.format(EFile.SAVENAME2, game.isBG() ? "bg" : "si");

		Vector<String> filenames = new Vector<String>();
		EUtil.U7ListFiles(mask, filenames);
		num_games = filenames.size();
		
		games = new SaveInfo[num_games];	

		// Setup basic details
		for (i = 0; i<num_games; i++) {
			games[i] = new SaveInfo();
			games[i].filename = filenames.elementAt(i);
			System.out.println("FILE: " + games[i].filename);
			games[i].SetSeqNumber();
		}
		// First sort the games so they will be sorted by number
		// This is so I can work out the first free game
		SaveInfo.comparator cmp = new SaveInfo.comparator();
		if (num_games > 0)
			Arrays.sort(games, cmp);
		// Read and cache all details
		first_free = -1;
		for (i = 0; i<num_games; i++) {
			games[i].readable = gwin.getSaveInfo(games[i].num, games[i]); 
			if (first_free == -1 && i != games[i].num) 
				first_free = i;
		}
		System.out.println("firstFree = " + first_free);
		if (first_free == -1) 
			first_free = num_games;
		System.out.println("NOW firstFree = " + first_free);
		// Now sort it again, with all the details so it can be done by date
		if (num_games > 0) 
			Arrays.sort(games, cmp);
		adapter = new SaveAdapter(exult, R.layout.savename);
		filesView.setAdapter(adapter);
	}
	void	FreeSaveGameDetails() {	// Frees all the savegame details
		cur_shot = null;
		cur_details = null;
		cur_party = null;
		gd_shot = null;
		gd_details = null;
		gd_party = null;
		filename = null;
		games = null;
	}
	
	private class SaveAdapter extends ArrayAdapter<SaveInfo> {
		private Context ctx;
		
        public SaveAdapter(Context context, int textViewResourceId) {
                super(context, textViewResourceId, games);
                ctx = context;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	View v = convertView;
        	if (v == null) {
        		LayoutInflater vi = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        		v = vi.inflate(R.layout.savename, null);
        	}
        	SaveInfo g = games[position];
        	if (g != null) {
        		CheckBox btn = (CheckBox) v.findViewById(R.id.savename_choice);
        		TextView txt = (TextView) v.findViewById(R.id.savename_text);
        		if (txt != null)
        			txt.setText(g.savename);
        		if(btn != null)
        			btn.setChecked(position == selected);
        	}
        	return v;
        }
	};
}
