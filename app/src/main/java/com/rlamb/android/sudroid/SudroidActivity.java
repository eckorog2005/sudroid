/**
 * SudroidActivity.java
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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.activity.ComponentActivity;

import com.rlamb.android.sudroid.data.RoomGameRepository;
import com.rlamb.android.sudroid.game.GameActivity;

/**
 * This class displays the main menu to the user.
 * 
 * @author rlamb
 * 
 * @version 1
 * 
 *
 */
public class SudroidActivity extends ComponentActivity {
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        
    	//add main display
    	setContentView(R.layout.main);
        
    	//if easy id was selected, start game in easy mode
        findViewById(R.id.easy).setOnClickListener(
                new OnClickListener() {
            public void onClick(View v) {
                startGame(GameModeEnum.easy);
            }
        });
        
        //if medium id was selected, start game in medium mode
        findViewById(R.id.medium).setOnClickListener(
                new OnClickListener() {
            public void onClick(View v) {
                startGame(GameModeEnum.medium);
            }
        });
        
        //if hard id was selected, start game in hard mode
        findViewById(R.id.hard).setOnClickListener(
                new OnClickListener() {
            public void onClick(View v) {
                startGame(GameModeEnum.hard);
            }
        });
        
        //if weekly puzzle id was selected, start game in weekly puzzle mode.
        //feature for later update, comment out since its not needed now.
        /*findViewById(R.id.weekPuzzle).setOnClickListener(
                new OnClickListener() {
            public void onClick(View v) {           	
                startGame(GameModeEnum.weekly);
            }
        });*/
        
       //if resume id was selected, resume last game
        findViewById(R.id.resume).setOnClickListener(
                new OnClickListener() {
            public void onClick(View v) {
                startGame(GameModeEnum.resume);
            }
        });

        // Leaving a game saves it and finishing one deletes it, so this tracks the database.
        View resume = findViewById(R.id.resume);
        RoomGameRepository.get(this).observeHasSavedGame(this, resume::setEnabled);
    }

    /**
     * starts the mode that was selected
     * @param mode mode to start
     */
	private void startGame(GameModeEnum mode) {
		Intent i;
        i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_MODE,
                mode.ordinal());
        startActivity(i);	
	}

	/**
	 * get the latest weekly puzzle if there.
	 */
	//TODO weekly stuff
    
}