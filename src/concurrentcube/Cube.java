package concurrentcube;

import java.util.function.BiConsumer;

public class Cube {
    private final int[][][] state;
    private final int size;
    private final BiConsumer<Integer, Integer> beforeRotation;
    private final BiConsumer<Integer, Integer> afterRotation;
    private final Runnable beforeShowing;
    private final Runnable afterShowing;

    private final int TOP = 0;
    private final int LEFT = 1;
    private final int FRONT = 2;
    private final int RIGHT = 3;
    private final int BACK = 4;
    private final int BOTTOM = 5;

    private final int[] SIDES_TO_CHANGE_AX0 = {TOP, FRONT, BOTTOM, BACK};
    private final int[] SIDES_TO_CHANGE_AX1 = {TOP, RIGHT, BOTTOM, LEFT};
    private final int[] SIDES_TO_CHANGE_AX2 = {LEFT, FRONT, RIGHT, BACK};

    private final boolean DEV_SHOW = true;

    public Cube(int size,
                BiConsumer<Integer, Integer> beforeRotation,
                BiConsumer<Integer, Integer> afterRotation,
                Runnable beforeShowing,
                Runnable afterShowing) {

        this.size = size;
        this.state = new int[6][this.size][this.size];
        this.beforeRotation = beforeRotation;
        this.afterRotation = afterRotation;
        this.beforeShowing = beforeShowing;
        this.afterShowing = afterShowing;

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < this.size; j++) {
                for (int k = 0; k < this.size; k++) {
                    if (DEV_SHOW) {
                        String str = "" + i + j + k;
                        this.state[i][j][k] = Integer.parseInt(str);
                    }
                    else {
                        this.state[i][j][k] = i;
                    }
                }
            }
        }
    }

    private void rotateFace(int side) {
        int[][] temp = new int[size][size];

        for (int i = 0; i < size; i++) {
            System.arraycopy(state[side][i], 0, temp[i], 0, size);
        }

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                state[side][j][size - i - 1] = temp[i][j];
            }
        }
    }

    private void rotateSides(int[][][] indexesToSwap, int[] order, int direction) {
        int[] temp = new int[size];

        int dirHelper = 0;

        if (direction == -1) {
            dirHelper = 3;
        }

        for (int i = 0; i < size; i++) {
            temp[i] = state[order[Math.abs(dirHelper)]][indexesToSwap[0][i][0]][indexesToSwap[0][i][1]];
        }

        for (int i = 1; i < 4; i++) {
            for(int j = 0; j < size; j++) {
                int swap = state[order[Math.abs(dirHelper - i)]][indexesToSwap[i][j][0]][indexesToSwap[i][j][1]];
                state[order[Math.abs(dirHelper - i)]][indexesToSwap[i][j][0]][indexesToSwap[i][j][1]] = temp[j];
                temp[j] = swap;
            }
        }

        for (int i = 0; i < size; i++) {
            state[order[Math.abs(dirHelper)]][indexesToSwap[0][i][0]][indexesToSwap[0][i][1]] = temp[i];
        }
    }

    private void rotateAxis0(int side, int layer) { //left, right
        int[][][] indexesToSwap = new int[4][size][2];

        if (side == LEFT) {
            for (int i = 0; i < 4; i++) {
                int currentSide = SIDES_TO_CHANGE_AX0[i];

                for (int j = 0; j < size; j++) {
                    switch (currentSide) {
                        case TOP, FRONT, BOTTOM -> {
                            indexesToSwap[i][j][0] = j;
                            indexesToSwap[i][j][1] = layer - 1;
                        }
                        case BACK -> {
                            indexesToSwap[i][j][0] = size - j - 1;
                            indexesToSwap[i][j][1] = size - layer;
                        }
                    }
                }
            }

            rotateSides(indexesToSwap, SIDES_TO_CHANGE_AX0, 1);
        }
        else {
            for (int i = 3; i >= 0; i--) {
                int currentSide = SIDES_TO_CHANGE_AX0[i];

                for (int j = 0; j < size; j++) {
                    switch (currentSide) {
                        case TOP, FRONT, BOTTOM -> {
                            indexesToSwap[i][j][0] = size - j - 1;
                            indexesToSwap[i][j][1] = size - layer;
                        }
                        case BACK -> {
                            indexesToSwap[i][j][0] = j;
                            indexesToSwap[i][j][1] = layer - 1;
                        }
                    }
                }
            }
            rotateSides(indexesToSwap, SIDES_TO_CHANGE_AX0, -1);

        }
    }

    private void rotateAxis1(int side, int layer) { //back, front
        int[][][] indexesToSwap = new int[4][size][2];

        if (side == FRONT) {
            for (int i = 0; i < 4; i++) {
                int currentSide = SIDES_TO_CHANGE_AX1[i];

                for (int j = 0; j < size; j++) {
                    switch (currentSide) {
                        case TOP -> {
                            indexesToSwap[i][j][0] = size - layer;
                            indexesToSwap[i][j][1] = j;
                        }
                        case RIGHT -> {
                            indexesToSwap[i][j][0] = j;
                            indexesToSwap[i][j][1] = layer - 1;
                        }
                        case BOTTOM -> {
                            indexesToSwap[i][j][0] = layer - 1;
                            indexesToSwap[i][j][1] = size - j - 1;
                        }
                        case LEFT -> {
                            indexesToSwap[i][j][0] = size - j - 1;
                            indexesToSwap[i][j][1] = size - layer;
                        }
                    }
                }
            }

            rotateSides(indexesToSwap, SIDES_TO_CHANGE_AX1, 1);
        }
        else {
            for (int i = 3; i >= 0; i--) {
                int currentSide = SIDES_TO_CHANGE_AX1[i];

                for (int j = 0; j < size; j++) {
                    switch (currentSide) {
                        case TOP -> {
                            indexesToSwap[i][j][0] = layer - 1;
                            indexesToSwap[i][j][1] = size - j - 1;
                        }
                        case RIGHT -> {
                            indexesToSwap[i][j][0] = size - j - 1;
                            indexesToSwap[i][j][1] = size - layer;
                        }
                        case BOTTOM -> {
                            indexesToSwap[i][j][0] = size - layer;
                            indexesToSwap[i][j][1] = j;
                        }
                        case LEFT -> {
                            indexesToSwap[i][j][0] = j;
                            indexesToSwap[i][j][1] = layer - 1;
                        }
                    }
                }
            }

            rotateSides(indexesToSwap, SIDES_TO_CHANGE_AX1, -1);
        }
    }

    private void rotateAxis2(int side, int layer) { //top, bottom
        int[][][] indexesToSwap = new int[4][size][2];

        if (side == BOTTOM) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < size; j++) {
                    indexesToSwap[i][j][0] = size - layer;
                    indexesToSwap[i][j][1] = j;
                }
            }

            rotateSides(indexesToSwap, SIDES_TO_CHANGE_AX2, 1);
        }
        else {
            for (int i = 3; i >= 0; i--) {
                for (int j = 0; j < size; j++) {
                    indexesToSwap[i][j][0] = layer - 1;
                    indexesToSwap[i][j][1] = size - j - 1;
                }
            }

            rotateSides(indexesToSwap, SIDES_TO_CHANGE_AX2, -1);
        }
    }

    public void rotate(int side, int layer) throws InterruptedException {
        int axis = ((side + 2) % 5) % 3;

        beforeRotation.accept(side, layer);

        switch (axis) {
            case 0 -> rotateAxis0(side, layer); //left, right
            case 1 -> rotateAxis1(side, layer); //front, back
            case 2 -> rotateAxis2(side, layer); //top, bottom
        }

        if (layer == 1 || layer == size) {
            rotateFace(side);
        }

        afterRotation.accept(side, layer);
    }

    public String show() throws InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();

        beforeShowing.run();

        for (int i = 0; i < 6; i++) {
            switch (i) {
                case 0 -> stringBuilder.append("Top(");
                case 1 -> stringBuilder.append("Left(");
                case 2 -> stringBuilder.append("Front(");
                case 3 -> stringBuilder.append("Right(");
                case 4 -> stringBuilder.append("Back(");
                case 5 -> stringBuilder.append("Bottom(");
            }

            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    if (k % size == 0) {
                        stringBuilder.append("\n");
                    }

                    if (DEV_SHOW) {
                        int value = state[i][j][k];
                        int length = (int) (Math.log10(value) + 1);

                        if (value == 0) length = 1;

                        stringBuilder.append("0".repeat(Math.max(0, 3 - length)));
                        stringBuilder.append(value);
                    }
                    else {
                        stringBuilder.append(state[i][j][k]);
                    }

                    if (j != size - 1 || k != size - 1) {
                        stringBuilder.append(", ");
                    }
                }
            }

            stringBuilder.append(")\n");
        }

        afterShowing.run();

        return stringBuilder.toString();
    }
}
