package concurrentcube;

import java.util.function.BiConsumer;

public class Cube {
    private int[][][] state;
    private final int size;
    private final BiConsumer<Integer, Integer> beforeRotation;
    private final BiConsumer<Integer, Integer> afterRotation;
    private final Runnable beforeShowing;
    private final Runnable afterShowing;

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
                    String str = "" + i + j + k;
                    this.state[i][j][k] = Integer.parseInt(str);
                }
            }
        }
    }

    private void rotateFace(int side) {
        int[][] temp = new int[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                temp[i][j] = state[side][i][j];
            }
        }

        //if (direction == -1) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    state[side][j][size - i - 1] = temp[i][j];
                }
            }
        /*}
        else {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    state[side][size - j - 1][i] = temp[i][j];
                }
            }
        }*/
    }

    public void rotate(int side, int layer) throws InterruptedException {
        int axis = ((side + 2) % 5) % 3; //?
        int[] temp = new int[size];

        beforeRotation.accept(side, layer);

        switch (side) {
            case 0: {
                for (int j = 0; j < size; j++) {
                    temp[j] = state[4][layer - 1][j];
                }

                for (int i = 3; i >= 1; i--) {
                    for(int j = 0; j < size; j++) {
                        int swap = state[i][layer - 1][j];
                        state[i][layer - 1][j] = temp[j];
                        temp[j] = swap;
                    }
                }

                for (int j = 0; j < size; j++) {
                    state[4][layer - 1][j] = temp[j];
                }

                if (layer == 1 || layer == size) {
                    rotateFace(0);
                }

                break;
            }
            case 1: {

                break;
            }
            case 2: {

                break;
            }
            case 3: {

                break;
            }
            case 4: {

                break;
            }
            case 5: {
                for (int j = 0; j < size; j++) {
                    temp[j] = state[1][size - layer][j];
                }

                for (int i = 2; i < 5; i++) {
                    for(int j = 0; j < size; j++) {
                        int swap = state[i][size - layer][j];
                        state[i][size - layer][j] = temp[j];
                        temp[j] = swap;
                    }
                }

                for (int j = 0; j < size; j++) {
                    state[1][size - layer][j] = temp[j];
                }

                if (layer == 1 || layer == size) {
                    rotateFace(5);
                }

                break;
            }
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
                    //temp
                    if (state[i][j][k] == 0) {
                        stringBuilder.append("000");
                    }
                    else if (state[i][j][k] == 1) {
                        stringBuilder.append("001");
                    }
                    else if (state[i][j][k] == 2) {
                        stringBuilder.append("002");
                    }
                    else if (state[i][j][k] > 9 && state[i][j][k] < 99){
                        stringBuilder.append("0").append(state[i][j][k]);
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
