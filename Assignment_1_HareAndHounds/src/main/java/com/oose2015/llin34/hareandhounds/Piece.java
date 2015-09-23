/**
 * @author Li-Yi Lin
 */

package com.oose2015.llin34.hareandhounds;


/**
 * A class that represents a piece on the board game.
 * @author Jason
 *
 */
public class Piece {
	
	private String pieceType;
	private int x;
	private int y;
	
	/**
	 * Construct a piece.
	 * @param pieceType
	 * @param x
	 * @param y
	 */
	public Piece(String pieceType, int x, int y) {
		this.pieceType = pieceType;
		this.x = x;
		this.y = y;
	}

	// Return the x-position of this piece.
	public int getX() { return this.x; }

	// Return the y-position of this piece.
	public int getY() { return this.y; }

	// Set the new x-position of this piece.
	public void setX(int x) { this.x = x; }

	// Set the new x-position of this piece.
	public void setY(int y) { this.y = y; }

}
