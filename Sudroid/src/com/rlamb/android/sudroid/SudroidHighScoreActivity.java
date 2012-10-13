/**
 * SudroidHighScoreActivity.java
 */

/**
 *  Copyright 2010,2012 Roger Lamb 
 * 
 *  This file is part of Sudroid.
 *
 *  Sudroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Sudroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Sudroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rlamb.android.sudroid;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * Brings up the high score screen
 * 
 * @author rlamb
 * @version 1
 *
 */
public class SudroidHighScoreActivity extends Activity {
	
	 //holds the time that the game was finished in.
	 public static final String finishTime =
	        "com.rlamb.android.sudroid.library.SudroidHighScore.finishTime";
	 
	 private String outputValue;
	 private boolean weekly;
	 private TextView output;
	 
	 //calls on when the activity create it.
	 @Override
	 public void onCreate(Bundle savedInstanceState) {
	    	super.onCreate(savedInstanceState);
	    	
	    	//show the loading dialog
	    	ProgressDialog temp = new ProgressDialog(this);
	    	temp.show();
	    	
	    	//load the correct high score board
	    	outputValue = this.getIntent().getStringExtra(finishTime);
	        weekly = this.getIntent().getBooleanExtra("week", false);
	        if(weekly){
	        	//setContentView(R.layout.high_scores);
	        	//output = (TextView) this.findViewById(R.id.middle);
	        	//outputValue = this.findPlace();
	        }else{
	        	setContentView(R.layout.final_score);
	        	output = (TextView) this.findViewById(R.id.final_middle);
	        }
	        output.setText(outputValue);
	        
	        //delete current board
	        SudroidDatabaseHelper helper = 
	        	new SudroidDatabaseHelper(this);
	    	SQLiteDatabase db = helper.getReadableDatabase();
	    	db.delete("Current", null, null);
	        
	        //when back is selected, go to main menu
	        findViewById(R.id.back).setOnClickListener(
	                new OnClickListener() {
	            public void onClick(View v) {
	                Intent i = new Intent(v.getContext(), SudroidActivity.class);
	                startActivity(i);
	            }
	        });
	        
	        //stop process board
	        temp.dismiss();
	 }

	 /**
	  * Find the place of the use in the weekly standings
	  * 
	  * @return
	  */
	 //TODO private String findPlace() {}
}
