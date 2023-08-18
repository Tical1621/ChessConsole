package org.example.piece;


import org.example.board.Board;
import org.example.movement.Coordinates;

import java.util.HashSet;
import java.util.Set;

abstract public class Piece {
    public final Color color;
    public Coordinates coordinates;

    public Piece(Color color, Coordinates coordinates) {
        this.color = color;
        this.coordinates = coordinates;
    }

     public  Set<Coordinates> getAvailableMoveSquares(Board board) {//логика движения фигур
        Set<Coordinates> result = new HashSet<>();
        for(CoordinatesShift shift : getPieceMoves()) {
            if(coordinates.canShift(shift)) {
                Coordinates newCoordinates = coordinates.shift(shift);
                if(isSquareAvailableForMove(newCoordinates,board)) {
                    result.add(newCoordinates);
                }
            }
        }
        return result;
    }
    //чтобы перегрузить метод он должен быть protected
    protected boolean isSquareAvailableForMove(Coordinates coordinates, Board board) {//проверка на пустоту либо на фигуру другого цвета
        return board.isSquareEmpty(coordinates) || board.getPiece(coordinates).color != color;
    }

    protected abstract Set<CoordinatesShift> getPieceMoves();

    protected Set<CoordinatesShift> getPieceAttacks() {
        return getPieceMoves();
    }

    //логика, по которой фигуры могут атаковать в разные клетки
    public Set<Coordinates> getAttackedSquares(Board board) {
        Set<CoordinatesShift> pieceAttacks = getPieceAttacks();
        Set<Coordinates> result = new HashSet<>();
        for(CoordinatesShift pieceAttack : pieceAttacks) {
            if(coordinates.canShift(pieceAttack)) {
                Coordinates shiftCoordinates = coordinates.shift(pieceAttack);
                if(isSquareAvailableForAttack(shiftCoordinates, board)) {
                    result.add(shiftCoordinates);
                }
            }
        }
        return result;
    }
    protected boolean isSquareAvailableForAttack(Coordinates shiftCoordinates,Board board) {
        return true;
    }

}

