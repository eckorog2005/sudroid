/**
 * SudroidDatabaseHelper.java
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

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * loads all the data to the database.
 * 
 * @author rlamb
 *
 */
public class SudroidDatabaseHelper extends SQLiteOpenHelper {
	private Context context;

	/**
	 * Constructor
	 * 
	 * @param context
	 */
	public SudroidDatabaseHelper(Context context) {
		super(context, "SudroidDB", null, 1);
		this.context = context;
	}

	/**
	 * on create of object
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		int bytesRead = 0;
		String sqlCommands = "";
		byte[] buffer = new byte[100];
		try {
			BufferedInputStream in = new BufferedInputStream(
					context.getAssets().open("data.txt"));
			while((bytesRead = in.read(buffer)) != -1){
				sqlCommands = sqlCommands.concat(
						new String(buffer, 0, bytesRead));
			}
			in.close();
			String[] command = sqlCommands.split("\r\n");
			for(int i = 0; i < command.length; i++){
				db.execSQL(command[i]);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * on upgrade
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}

}
