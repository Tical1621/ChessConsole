package org.example.movement;

import org.example.piece.CoordinatesShift;

import java.util.HashSet;
import java.util.Set;

public interface IBishop {
    default Set<CoordinatesShift> getBishopMoves() {

            Set<CoordinatesShift> result = new HashSet<>();
            for (int i = -7; i <= 7; i++) {
                if(i==0) continue;
                result.add(new CoordinatesShift(i,i));
            }
            //top left to bottom right
            for (int i = -7; i <= 7; i++) {
                if(i==0) continue;
                result.add(new CoordinatesShift(i,-i));
            }
            return result;

    }
}
