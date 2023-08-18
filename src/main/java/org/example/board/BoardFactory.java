package org.example.board;

import org.example.movement.Coordinates;
import org.example.movement.File;
import org.example.movement.Move;
import org.example.piece.PieceFactory;

public class BoardFactory {//класс порождающий доску

    private PieceFactory pieceFactory = new PieceFactory();

    public Board fromFEN(String fen) {//FEN-аббревиатура строки воссоздания доски с конкретной расстановкой фигур
        //rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
        Board board = new Board(fen);
        String[] parts = fen.split(" ");
        String piecePositions = parts[0];

        String[] fenRows = piecePositions.split("/");

        for (int i = 0; i < fenRows.length; i++) {
            String row = fenRows[i];
            int rank = 8 - i;
            int fileIndex = 0;
            for(int j = 0;j < row.length();j++) {
                char fenChar = row.charAt(j);

                if(Character.isDigit(fenChar)) {
                    fileIndex += Character.getNumericValue(fenChar);
                } else {
                    File file = File.values()[fileIndex];
                    Coordinates coordinates = new Coordinates(file,rank);
                    board.setPiece(coordinates,pieceFactory.fromFEN(fenChar,coordinates));
                    fileIndex++;
                }
            }
        }
        return board;
    }
    public Board copy(Board source) {
        //создал клона, записал в него такой же ход, вернул клона
        Board clone = fromFEN(source.startingFen);
        for (Move move : source.moves) {
            clone.makeMove(move);
        }
        return clone;
    }
}
