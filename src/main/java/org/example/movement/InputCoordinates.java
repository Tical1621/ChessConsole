package org.example.movement;

import org.example.piece.Color;
import org.example.board.Board;
import org.example.board.BoardConsoleRenderer;
import org.example.board.BoardFactory;
import org.example.piece.King;
import org.example.piece.Piece;

import java.util.Scanner;
import java.util.Set;

public class InputCoordinates {

//    public static void main(String[] args) {
//
//        Board board = new Board();
//        board.setupDefaultPiecePositions();
//        Coordinates coordinates = inputPieceCoordinatesForColor(Color.WHITE,board);
//        System.out.println(coordinates);
//    }

    private static final Scanner scanner = new Scanner(System.in);

    public static Coordinates input() {
        while (true) {
            System.out.println("please enter coordinates,example: a1");
            String line = scanner.nextLine();

            if(line.length() !=2) {
                System.out.println("invalid input format");
                continue;//сбросить итерацию цикла
            }

            char fileChar = line.charAt(0);
            char rankChar = line.charAt(1);

            if(!Character.isLetter(fileChar)){//проверка на букву
                System.out.println("invalid input format");
                continue;
            }

            if(!Character.isDigit(rankChar)) {
                System.out.println("invalid input format");
                continue;
            }

            int rank = Character.getNumericValue(rankChar);
            if(rank < 1 || rank > 8) {
                System.out.println("invalid input format");
                continue;
            }
            File file = File.fromChar(fileChar);
            if(file ==null) {
                System.out.println("invalid input format");
                continue;
            }
            return new Coordinates(file,rank);
        }
    }

    public static Coordinates inputPieceCoordinatesForColor(Color color, Board board) {//если ходят белые-вход для белых, если черные-для черных
        while(true) {
            System.out.println("enter coordinates for piece to move");
            Coordinates coordinates = input();
            if(board.isSquareEmpty(coordinates)) {
                System.out.println("empty square");
                continue;
            }
            Piece piece = board.getPiece(coordinates);
            if(piece.color != color) {
                System.out.println("wrong color");
                continue;
            }

            Set<Coordinates> availableMoveSquares = piece.getAvailableMoveSquares(board);
            if(availableMoveSquares.isEmpty()) {
                System.out.println("blocked piece");
                continue;
            }
            return coordinates;
        }
    }

    public static Move inputMove(Board board, Color color, BoardConsoleRenderer renderer) {
        while(true) {
            Coordinates sourceCoordinates = InputCoordinates.inputPieceCoordinatesForColor(
                    color, board
            );
            Piece piece = board.getPiece(sourceCoordinates);
            Set<Coordinates> availableForMoveSquares = piece.getAvailableMoveSquares(board);

            renderer.render(board, piece);

            Coordinates targetCoordinates = InputCoordinates.inputAvailableSquares(
                    availableForMoveSquares);
            Move move = new Move(sourceCoordinates, targetCoordinates);
            //check if kingInCheckAfterMove(from,to)
            //your king is under attack
            if (validateIfKingInCheckAfterMove(board, color, move)) {
                System.out.println("Your king is under attack");
                continue;
            }
            return move;
        }
    }

    //создается копия доски, на которой проверяется под шахом ли король
    private static boolean validateIfKingInCheckAfterMove(Board board, Color color, Move move) {
        Board copy = (new BoardFactory().copy(board));
        copy.makeMove(move);
        //допущение, что король есть на доске
        Piece king = copy.getPiecesByColor(color).stream().filter(piece -> piece instanceof King).findFirst().get();
        return copy.isSquareAttackedByColor(king.coordinates, color.opposite());
    }

    public static Coordinates inputAvailableSquares(Set<Coordinates> coordinates) {
        while(true) {
            System.out.println("please enter you move");
            Coordinates input = input();

            if (!coordinates.contains(input)) {
                System.out.println("not available square for move");
                continue;
            }

            return input;
        }
    }
}
