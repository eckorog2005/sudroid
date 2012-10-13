/**
 * Board.java
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

import java.util.ArrayList;



/**
 * <P>This class makes a Sudoku board.  contains to see if each move is valid and other basic checks.
 * 
 * @author Roger Lamb
 * 
 * @version 1
 *
 */
public class Board {
	
	private Cell[][] board;			//the board
	private final int size = 9;		//max max of board and max number
	private final int invalid = 0;	//last invalid number (THIS PROBABLY SHOULD STAY AT ZERO)
	
	/**
	 * Constructor
	 */
	public Board(){
		this.board = new Cell[this.size][this.size];
		for(int i = 0; i < this.size; i++){
			for(int j = 0; j < this.size; j++){
				this.board[i][j] = new Cell();
			}
		}
	}
	
	/**
	 * Constructor
	 * 
	 */
	public Board(String list){
		int position = 0;
		int max = list.length();
		this.board = new Cell[this.size][this.size];
		for(int i = 0; i < this.size; i++){
			for(int j = 0; j < this.size; j++){
				if(position != max){
					this.board[i][j] = new Cell(
							Integer.valueOf(list.charAt(position)));
					position++;
				}else{
					this.board[i][j] = new Cell();
				}
			}
		}
	}
	
	/**
	 * clone Constructor
	 * 
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		Board out = new Board(); //= (Board) super.clone();
		out.board = new Cell[out.size][out.size];
		for(int i = 0; i < out.size; i++){
			for(int j = 0; j < out.size; j++){
				out.board[i][j] = new Cell(this.board[i][j].getValue());
			}
		}
		return out;
	}
	
	/**
	 * This method adds the value i to the column and row position specified by the parameters. will overwrite value if one
	 * is there already.
	 * 
	 * @param i the number to add
	 * @param col the column to add the number(1-max)
	 * @param row the row to add the number(1-max)
	 * @throws InvalidNumberValueException 
	 * @throws InvalidSodokuPositionException 
	 * @throws InvalidPositionLocation 
	 */
	public boolean addNumber(int i, Position pos) throws InvalidNumberValueException, InvalidPositionLocationException, InvalidSodokuPositionException{
		
		//declarations
		boolean added = false;
		int oldValue = this.board[pos.getRow()-1][pos.getCol()-1].getValue();
		
		//check limits of position, value of number, and if cell can be changed before continuing
		if(isLegalNumber(i) && isLegalPosition(pos) && !this.getCell(pos).isInvalidSelection()){
			
			//add the value to the position
			this.board[pos.getRow()-1][pos.getCol()-1].setValue(i);
			//see if no same values around, if not clear any lingering flags
			if(this.validatesRule(i, pos)){
				added = true;
				this.clearError(pos);
			//else, throw flag(s)	
			}else{
				this.board[pos.getRow()-1][pos.getCol()-1].setErrorFlag(true);
				this.addErrors(i, pos);
				throw new InvalidSodokuPositionException("This position for the value is not allowed by Soduko constraint");
			}
			
			//clear existing errors if affected, and add new ones
			if(oldValue != this.invalid){
				this.clearErrors(oldValue, pos);
			}
			
		//throw invalid errors if needed	
		}else{
			if(!isLegalNumber(i)){
				throw new InvalidNumberValueException("The number was not in valid range");
			}else{
				if(!isLegalNumber(pos.getCol())){
					throw new InvalidPositionLocationException("The column position is an invalid one");
				}else{
					throw new InvalidPositionLocationException("The row position is an invalid one");
				}
			}
		}
		return added;
	}
	
	/**
	 * check for new errors with added number and marks them
	 * 
	 * @param value value of the square
	 * @param pos position of the square
	 */
	private void addErrors(int value, Position pos) {
		//get same values that are associated with this square
		ArrayList<Position> locations = this.getAssociatedValuePositions(value, pos);
		
		for(Position temp: locations){
			if(!this.validatesRule(value,temp)){
				this.getCell(temp).setErrorFlag(true);
			}
		}	
	}

	/**
	 * clears the errors associated with the old value
	 * 
	 * @param oldValue old value of the square
	 * @param pos position that has seen changed
	 */
	private void clearErrors(int oldValue, Position pos) {
		
		//get same values that are associated with this square
		ArrayList<Position> locations = 
			this.getAssociatedValuePositions(oldValue, pos);
		
		for(Position temp: locations){
			if(this.validatesRule(oldValue,temp)){
				this.getCell(temp).setErrorFlag(false);
			}
		}
	}

	/**
	 * checks to see if the position is allowed here by sudoku rules
	 * 
	 * @param value value to check
	 * @param pos position of location
	 * @return
	 */
	private boolean validatesRule(int value, Position pos) {
		if(isAllowedByRow(value,pos) && isAllowedByCol(value, pos) && isAllowedByBlock(value, pos)){
			return true;
		}
		return false;
	}

	/**
	 * this class returns the cell of the same values by assocation(same block, row, and/or col) 
	 * of that in the position and value passed in 
	 * 
	 * @param value - value to check
	 * @param pos - position
	 * @return
	 */
	private ArrayList<Position> getAssociatedValuePositions(int value,
			Position pos) {
		
		//Declarations
		ArrayList<Position> list = new ArrayList<Position>();
		Cell[] row = this.getRow(pos.getRow());
		Cell[] col = this.getCol(pos.getCol());
		//block declarations
		int magicValue = (int)(Math.sqrt(this.size));
		int rowCorner = ((pos.getRow()-1)/magicValue) * magicValue;
		int colCorner = ((pos.getCol()-1)/magicValue) * magicValue;
		
		//search row
		for(int i = 0; i<row.length;i++){
			if(row[i].getValue() == value && !pos.equals(new Position(pos.getRow(),i))){
				list.add(new Position(pos.getRow(),i+1));
			}
		}
		
		//search column
		for(int i = 0; i<col.length;i++){
			if(col[i].getValue() == value && !pos.equals(new Position(i, pos.getCol()))){
				list.add(new Position(i+1,pos.getCol()));
			}
		}
		
		//search block
		for(int i = rowCorner; i < rowCorner + magicValue; i++){
			for(int j = colCorner; j < colCorner + magicValue; j++){
				if(this.board[i][j].getValue() == value && !pos.equals(new Position(i+1,j+1))){
					list.add(new Position(i+1,j+1));
				}
			}
		}
		
		//return list
		return list;
	}

	/**
	 * gets all the errors on the board
	 * 
	 * @return arraylist of error positions
	 */
	public ArrayList<Position> getErrors(){
		ArrayList<Position> temp = new ArrayList<Position>();
		for(int i = 0; i < this.size; i++){
			for(int j = 0; j < this.size; j++){
				if(this.board[i][j].isError() == true){
					temp.add(new Position(i+1,j+1));
				}
			}
		}
		return temp;
	}

	/**
	 * clear the error at this position
	 * 
	 * @param pos position of error
	 */
	private void clearError(Position pos) {
		this.board[pos.getRow()-1][pos.getCol()-1].setErrorFlag(false);
	}

	/**
	 * checks to see if the number is valid in the block
	 * 
	 * @param value value to be added
	 * @param pos the position
	 * @return
	 */
	private boolean isAllowedByBlock(int value, Position pos) {
		int magicValue = (int)(Math.sqrt(this.size));
		int rowCorner = ((pos.getRow()-1)/magicValue) * magicValue;
		int colCorner = ((pos.getCol()-1)/magicValue) * magicValue;
		for(int i = rowCorner; i < rowCorner + magicValue; i++){
			for(int j = colCorner; j < colCorner + magicValue; j++){
				if(!pos.equals(new Position(i+1,j+1)) && this.board[i][j].getValue() == value){
					return false;
				}
			}
		}
		return true;
	}
	
	
	/**
	 * clears the cell
	 * 
	 * @param pos cell position to clear
	 */
	public void clearNumber(Position pos){
		if(!this.board[pos.getRow()-1][pos.getCol()-1].isInvalidSelection()){
			int oldValue = this.board[pos.getRow()-1][pos.getCol()-1].getValue();
			this.board[pos.getRow()-1][pos.getCol()-1].setErrorFlag(false);
			this.board[pos.getRow()-1][pos.getCol()-1].setValue(invalid);
			this.board[pos.getRow()-1][pos.getCol()-1].setInvalidSelection(false);
			this.clearErrors(oldValue, pos);
		}
	}

	/**
	 * check if the number is not an error in the column
	 * 
	 * @param value value of cell
	 * @param pos position to be added
	 * @return
	 */
	private boolean isAllowedByCol(int value, Position pos) {
		for(int i = 0; i < this.size; i++){
			if(!pos.equals(new Position(i+1,pos.getCol())) && this.board[i][pos.getCol()-1].getValue() == value){
				return false;
			}
		}
		return true;
	}

	/**
	 * check if the number is not an error in the row
	 * 
	 * @param value value of cell
	 * @param pos position to be added
	 * @return
	 */
	private boolean isAllowedByRow(int value, Position pos) {
		for(int i = 0; i < this.size; i++){
			if(!pos.equals(new Position(pos.getRow(),i+1)) && this.board[pos.getRow()-1][i].getValue() == value){
				return false;
			}
		}
		return true;
	}

	/**
	 * checks to see if the position is legal
	 * 
	 * @param pos position to check
	 * @return
	 */
	private boolean isLegalPosition(Position pos) {
		if(pos.getCol() > this.invalid && pos.getCol() <= this.size &&
				pos.getRow() > this.invalid && pos.getRow() <= this.size){
			return true;
		}
		return false;
	}

	/**
	 * check if legal number
	 * 
	 * @param i value to check
	 * @return
	 */
	private boolean isLegalNumber(int i) {
		if(i > this.invalid && i <= this.size){
			return true;
		}
		return false;
	}
	
	/**
	 * clears the board
	 * 
	 */
	public void clearBoard(){
		for(int i = 0; i < this.size; i++){
			for(int j = 0; j < this.size; j++){
				this.clearNumber(new Position(i+1,j+1));
			}
		}
	}
	
	/**
	 * see if the board is finished
	 * 
	 * @return true if finished, false otherwise
	 */
	public boolean isFinished(){
		
		//check for errors
		boolean finished = true;
		for(int i = 0; i < this.size; i++){
			for(int j = 0; j < this.size; j++){
				if(this.board[i][j].isError() || 
						this.board[i][j].getValue() == this.invalid){
					finished = false;
					break;
				}
			}
			if(!finished){
				break;
			}
		}
		
		return finished;
	}
	
	/**
	 * get a cell row in the board
	 * 
	 * @param row row to get
	 * @return the row
	 */
	public Cell[] getRow(int row){
		Cell[] out = new Cell[this.size];
		for(int i = 0; i < this.size; i++){
			out[i] = this.board[row-1][i];
		}
		return out;
	}
	
	/**
	 * get a cell col in the board
	 * 
	 * @param col col to get
	 * @return the col
	 */
	public Cell[] getCol(int col){
		Cell[] out = new Cell[this.size];
		for(int i = 0; i < this.size; i++){
			out[i] = this.board[i][col-1];
		}
		return out;
	}
	
	/**
	 * get the cell at the passed position
	 * 
	 * @param pos position to get cell
	 * @return the cell
	 */
	public Cell getCell(Position pos){
		return this.board[pos.getRow()-1][pos.getCol()-1];
	}
	
	/**
	 * prints the board to standard output
	 */
	public void printStandardBoardToConsole(){
		System.out.println("   1 2 3 4 5 6 7 8 9");
		System.out.println("   -----------------");
		for(int i = 0; i < 9; i++){
			System.out.print((i+1)+"| ");
			for(int j = 0; j < 9; j++){
				if(this.board[i][j].getValue() != 0){
					System.out.print(this.board[i][j].getValue() + " ");
				}else{
					System.out.print("  ");
				}
			}
			System.out.println();
		}
	}
	
	/**
	 * returns a list of values that are currently allowed at the position
	 * 
	 * @param pos position to check for values
	 * @return
	 */
	public ArrayList<Integer> getValidValues(Position pos){
		ArrayList<Integer> list = new ArrayList<Integer>();
		for(int i = 1; i <= this.size; i++){
			if(this.isAllowedByBlock(i, pos) && this.isAllowedByCol(i, pos)
					&& this.isAllowedByRow(i, pos)){
				list.add(Integer.valueOf(i));
			}
		}
		return list;
	}
	
	public boolean isValidAdd(int value, Position pos){
		if(this.isAllowedByBlock(value, pos) && this.isAllowedByCol(value, pos)
				&& this.isAllowedByRow(value, pos)){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Checks to see if the current state of the board is valid(no errors).
	 * DO NOT use to check if board is done, just checks if board is ok as of 
	 * now.
	 * 
	 * @return
	 */
	public boolean isValid(){
		for(int i = 0; i < 9; i++){
			for(int j = 0; j < 9; j++){
				if(this.board[i][j].getValue() != 0){
					if(this.board[i][j].isError()){
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * returns the number of black spaces in the board
	 * 
	 * @return
	 */
	public int numberOfEmptySpaces() {
		int counter = 0;
		for(int i = 0; i < 9; i++){
			for(int j = 0; j < 9; j++){
				if(this.board[i][j].getValue() == 0){
					counter++;
				}
			}
		}
		return counter;
	}

	/**
	 * checks to see if the current board state only has one solution
	 * 
	 * @return true if only one solution
	 */
	public boolean isOnlyOneSolution() {
		if(!this.isValid()){
			return false;
		}
		Board copy;
		try {
			copy = (Board)this.clone();
		} catch (CloneNotSupportedException e) {
			return false;
		}
		int numOfSolutions = copy.Solver(false);
		if(numOfSolutions == 1){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * checks to see if the board is full of numbers or not
	 * 
	 * @return
	 */
	public boolean isFull(){
		for(int i = 0; i < 9; i++){
			for(int j = 0; j < 9; j++){
				if(this.board[i][j].getValue() == 0){
					return false;
				}
			}
		}
		return true;
	}
	
	
	/**
	 * attempts to find all solutions
	 * 
	 * @return the number of correct solutions
	 */
	private int Solver(boolean printFlag) {
		int counter = 0; 
		if(this.isFull()){
			if(this.isFinished()){
				if(printFlag){
				}
				return 1;
			}else{
				return 0;
			}
		}
		Position pos = this.getNextEmptySpace();
		if(pos != null){
			for(int i = 1; i <= this.size; i++){
				if(this.isValidAdd(i, pos)){
					try {
						this.addNumber(i, pos);
					} catch (InvalidNumberValueException e) {
					} catch (InvalidPositionLocationException e) {
					} catch (InvalidSodokuPositionException e) {
					}
					counter = counter + this.Solver(printFlag);
					this.clearNumber(pos);
				}
			}
		}
		return counter;
	}

	/**
	 * returns the next position that is empty
	 * 
	 * @return
	 */
	private Position getNextEmptySpace() {
		for(int i = 0; i < this.size; i++){
			for(int j = 0; j < this.size; j++){
				if(this.board[i][j].getValue() == 0){
					return new Position(i+1,j+1);
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns a list of the empty positions
	 * 
	 * @return
	 */
	public ArrayList<Position> getEmptyList(){
		ArrayList<Position> out = new ArrayList<Position>();
		for(int i = 0; i < this.size; i++){
			for(int j = 0; j < this.size; j++){
				if(this.board[i][j].getValue() == this.invalid){
					out.add(new Position(i+1, j+1));
				}
			}
		}
		return out;
	}

	
	/**
	 * creates the string list of numbers
	 * 
	 * @return string number list
	 */
	public String makeNumberString(){
		StringBuilder out = new StringBuilder(81);
		for(int i = 0; i < this.size; i++){
			for(int j = 0; j < this.size; j++){
				out.append(Integer.toString(this.board[i][j].getValue()));
			}
		}
		return out.toString();
	}
	
	/**
	 * get the size of the board
	 * 
	 * @return size of board
	 */
	public int getSize(){
		return this.size;
	}

	public void printNumberListSolutions() {
		if(!this.isValid()){
			return;
		}
		Board copy;
		try {
			copy = (Board)this.clone();
		} catch (CloneNotSupportedException e) {
			return;
		}
		copy.Solver(true);
	}

	/**
	 * returns the positions that has values
	 * 
	 * @return
	 */
	public ArrayList<Position> getNonEmptyList() {
		ArrayList<Position> out = new ArrayList<Position>();
		for(int i = 0; i < this.size; i++){
			for(int j = 0; j < this.size; j++){
				if(this.board[i][j].getValue() != this.invalid){
					out.add(new Position(i+1, j+1));
				}
			}
		}
		return out;
	}
	
	/**
	 * get the user entered data from the board
	 * 
	 * @return
	 */
	public String getUserEnteredString(){
		StringBuilder out = new StringBuilder(81);
		for(int i = 0; i < this.size; i++){
			for(int j = 0; j < this.size; j++){
				if(this.board[i][j].getValue() != invalid && 
						!this.board[i][j].isInvalidSelection()){
					out.append(Integer.toString(this.board[i][j].getValue()));
				}else{
					out.append("0");
				}
			}
		}
		return out.toString();
	}
}
