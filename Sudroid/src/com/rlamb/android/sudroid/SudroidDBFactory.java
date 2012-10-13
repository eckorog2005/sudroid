/**
 * SudroidDBFactory.java
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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SudroidDBFactory {

	private static SudroidDBFactory factory = null;
	private SudroidDatabaseHelper dbHelper;
	
	/**
	 * Constructor
	 * 
	 * @param context
	 */
	private SudroidDBFactory(Context context){
		super();
		this.dbHelper = new SudroidDatabaseHelper(context);
	}
	
	/**
	 * constructor
	 * 
	 * @param context 
	 * @return
	 */
	public static SudroidDBFactory getInstance(Context context){
		if(factory == null){
			factory = new SudroidDBFactory(context);
		}
		return factory;
	}
	
	/**
	 * get the weekly puzzle
	 * 
	 * @return string of the board
	 */
	public String getWeeklyPuzzle(){
		 SQLiteDatabase db = this.dbHelper.getReadableDatabase();
		 Cursor cursor = db.query("Weekly", new String[]{"_id"},
				 null,null,null, null, null);
		 if(cursor.moveToNext()){
			 int id = cursor.getInt(0);
			 cursor = db.query("Puzzle", new String[]{"pattern"}, 
					 "_id = '"+id+"'", null, null, null, null);
			 if(cursor.moveToNext()){
				 db.close();
				 return cursor.getString(0);
			 }
		 }
		 db.close();
		 return null;
	}
	
	/**
	 * get a random puzzle
	 * 
	 * @param mode game type 
	 * @return puzzle string
	 */
	public String getRandomPuzzle(GameModeEnum mode){
		SQLiteDatabase db = this.dbHelper.getReadableDatabase();
		 Cursor cursor = db.query("Puzzle", new String[]{"pattern"},
				 "difficulty = '"+mode.name()+"'",null,null, null, "random()");
		 if(cursor.moveToNext()){
			 db.close();
			 return cursor.getString(0);
		 }
		 db.close();
		 return null;
	}

	/**
	 * check the weekly puzzle
	 * 
	 */
	public void checkWeeklyPuzzle() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * get the resume puzzle
	 * 
	 * @return the default puzzle
	 */
	public String getResumePuzzle() {
		SQLiteDatabase db = this.dbHelper.getReadableDatabase();
		 Cursor cursor = db.query("Current", new String[]{"_id"},
				 null,null,null, null, null);
		 if(cursor.moveToNext()){
			 int id = cursor.getInt(0);
			 cursor = db.query("Puzzle", new String[]{"pattern"}, "_id = "+id, 
					 null, null, null, null);
			 if(cursor.moveToNext()){
				 db.close();
			 	 return cursor.getString(0);
			 }
		 }
		 db.close();
		 return null;
	}

	/**
	 * get the resume timer
	 * 
	 * @return return the timer last time.
	 */
	public String getResumeTimer() {
		SQLiteDatabase db = this.dbHelper.getReadableDatabase();
		 Cursor cursor = db.query("Current", new String[]{"time"},
				 null,null,null, null, null);
		 if(cursor.moveToNext()){
			 db.close();
			 return cursor.getString(0);
		 }
		 db.close();
		return null;
	}

	/**
	 * get user input from resume game
	 * 
	 * @return user board string
	 */
	public String getResumeInput() {
		SQLiteDatabase db = this.dbHelper.getReadableDatabase();
		 Cursor cursor = db.query("Current", new String[]{"userEntered"},
				 null,null,null, null, null);
		 if(cursor.moveToNext()){
			 db.close();
			 return cursor.getString(0);
		 }
		 db.close();
		return null;
	}

	/**
	 * get the puzzle id from the database
	 * 
	 * @param board board to check
	 * @return
	 */
	public int getPuzzleID(String board) {
		SQLiteDatabase db = this.dbHelper.getReadableDatabase();
		Cursor cursor = db.query("Puzzle", new String[]{"_id"}, 
				"pattern = '"+board+"'", null, null, null, null);
		if(cursor.moveToNext()){
			 db.close();
			 return cursor.getInt(0);
		 }
		db.close();
		return 0;
	}
}
