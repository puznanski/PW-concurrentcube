package concurrentcube;

import java.util.concurrent.Semaphore;
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

    private final int NUMBER_OF_GROUPS = 4;
    private final int NUMBER_OF_SHOW_GROUP = 3;

    private final Semaphore mutex = new Semaphore(1, true);
    private final Semaphore firstsOnes = new Semaphore(0, true);
    private final Semaphore[] others = new Semaphore[NUMBER_OF_GROUPS];
    private final Semaphore[][] layerMutex;

    private int activeGroup = -1;
    private int workingNumber = 0;
    private final int[] waitingNumber = {0, 0, 0, 0};
    private int groupsWaiting = 0;


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
                    this.state[i][j][k] = i;
                }
            }
        }

        for (int i = 0; i < NUMBER_OF_GROUPS; i++) {
            others[i] = new Semaphore(0, true);
        }

        layerMutex = new Semaphore[3][this.size];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < this.size; j++) {
                layerMutex[i][j] = new Semaphore(1, true);
            }
        }
    }

    private void interruptionHandler(boolean interrupted, int groupNumber) throws InterruptedException {
        if (!interrupted) {
            activeGroup = groupNumber;
        }
        else if (workingNumber == 0 && waitingNumber[groupNumber] == 0) {
            if (groupsWaiting > 0) {
                firstsOnes.release();
            }
            else {
                activeGroup = -1;
                mutex.release();
            }

            throw new InterruptedException();
        }
    }

    private void entryProtocol(int groupNumber) throws InterruptedException {
        mutex.acquire();

        boolean interrupted = false;

        if (activeGroup == -1) {
            activeGroup = groupNumber;
        }
        else if (groupsWaiting > 0 || activeGroup != groupNumber){
            waitingNumber[groupNumber]++;

            if (waitingNumber[groupNumber] == 1) {
                groupsWaiting++;
                mutex.release();
                firstsOnes.acquireUninterruptibly();
                groupsWaiting--;
            }
            else {
                mutex.release();
                others[groupNumber].acquireUninterruptibly();
            }

            waitingNumber[groupNumber]--;
            interrupted = Thread.interrupted();
            interruptionHandler(interrupted, groupNumber);
        }

        if (!interrupted) {
            workingNumber++;
        }

        if (waitingNumber[groupNumber] > 0) {
            others[groupNumber].release();
        }
        else {
            mutex.release();
        }

        if (interrupted) {
            throw new InterruptedException();
        }
    }

    private void exitProtocol() {
        mutex.acquireUninterruptibly();
        workingNumber--;

        if (workingNumber == 0) {
            if (groupsWaiting > 0) {
                firstsOnes.release();
            }
            else {
                activeGroup = -1;
                mutex.release();
            }
        }
        else {
            mutex.release();
        }
    }

    private int getOppositeSideIndex(int side) {
        switch (side) {
            case TOP:
                return BOTTOM;

            case LEFT:
                return RIGHT;

            case FRONT:
                return BACK;

            case RIGHT:
                return LEFT;

            case BACK:
                return FRONT;

            default: //bottom
                return TOP;
        }
    }

    private int getLayerIndex(int side, int layer) {
        if (side == TOP || side == LEFT || side == FRONT) {
            return layer;
        }
        else {
            return size - layer - 1;
        }
    }

    private void rotateFace(int side, int direction) {
        int[][] temp = new int[size][size];

        for (int i = 0; i < size; i++) {
            System.arraycopy(state[side][i], 0, temp[i], 0, size);
        }

        if(direction == 1) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    state[side][j][size - i - 1] = temp[i][j];
                }
            }
        }
        else {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    state[side][size - j - 1][i] = temp[i][j];
                }
            }
        }
    }

    private void rotateSwapHelper(int[][][] indexesToSwap, int[] order, int direction) {
        int[] temp = new int[size];

        int dirHelper = 0;

        if (direction == -1) {
            dirHelper = 3;
        }

        for (int i = 0; i < size; i++) {
            temp[i] = state[order[dirHelper]][indexesToSwap[0][i][0]][indexesToSwap[0][i][1]];
        }

        for (int i = 1; i < 4; i++) {
            for(int j = 0; j < size; j++) {
                int swap = state[order[Math.abs(dirHelper - i)]][indexesToSwap[i][j][0]][indexesToSwap[i][j][1]];
                state[order[Math.abs(dirHelper - i)]][indexesToSwap[i][j][0]][indexesToSwap[i][j][1]] = temp[j];
                temp[j] = swap;
            }
        }

        for (int i = 0; i < size; i++) {
            state[order[dirHelper]][indexesToSwap[0][i][0]][indexesToSwap[0][i][1]] = temp[i];
        }
    }

    private void rotateAxis0(int side, int layer) { //left, right
        int[][][] indexesToSwap = new int[4][size][2];

        if (side == LEFT) {
            for (int i = 0; i < 4; i++) {
                int currentSide = SIDES_TO_CHANGE_AX0[i];

                for (int j = 0; j < size; j++) {
                    switch (currentSide) {
                        case TOP:
                        case FRONT:
                        case BOTTOM: {
                            indexesToSwap[i][j][0] = j;
                            indexesToSwap[i][j][1] = layer;
                            break;
                        }
                        case BACK: {
                            indexesToSwap[i][j][0] = size - j - 1;
                            indexesToSwap[i][j][1] = size - layer - 1;
                            break;
                        }
                    }
                }
            }

            rotateSwapHelper(indexesToSwap, SIDES_TO_CHANGE_AX0, 1);
        }
        else {
            for (int i = 3; i >= 0; i--) {
                int currentSide = SIDES_TO_CHANGE_AX0[3 - i];

                for (int j = 0; j < size; j++) {
                    switch (currentSide) {
                        case TOP:
                        case FRONT:
                        case BOTTOM: {
                            indexesToSwap[i][j][0] = size - j - 1;
                            indexesToSwap[i][j][1] = size - layer - 1;
                            break;
                        }
                        case BACK: {
                            indexesToSwap[i][j][0] = j;
                            indexesToSwap[i][j][1] = layer;
                            break;
                        }
                    }
                }
            }
            rotateSwapHelper(indexesToSwap, SIDES_TO_CHANGE_AX0, -1);

        }
    }

    private void rotateAxis1(int side, int layer) { //back, front
        int[][][] indexesToSwap = new int[4][size][2];

        if (side == FRONT) {
            for (int i = 0; i < 4; i++) {
                int currentSide = SIDES_TO_CHANGE_AX1[i];

                for (int j = 0; j < size; j++) {
                    switch (currentSide) {
                        case TOP: {
                            indexesToSwap[i][j][0] = size - layer - 1;
                            indexesToSwap[i][j][1] = j;
                            break;
                        }
                        case RIGHT: {
                            indexesToSwap[i][j][0] = j;
                            indexesToSwap[i][j][1] = layer;
                            break;
                        }
                        case BOTTOM: {
                            indexesToSwap[i][j][0] = layer;
                            indexesToSwap[i][j][1] = size - j - 1;
                            break;
                        }
                        case LEFT: {
                            indexesToSwap[i][j][0] = size - j - 1;
                            indexesToSwap[i][j][1] = size - layer - 1;
                            break;
                        }
                    }
                }
            }

            rotateSwapHelper(indexesToSwap, SIDES_TO_CHANGE_AX1, 1);
        }
        else {
            for (int i = 3; i >= 0; i--) {
                int currentSide = SIDES_TO_CHANGE_AX1[3 - i];

                for (int j = 0; j < size; j++) {
                    switch (currentSide) {
                        case TOP: {
                            indexesToSwap[i][j][0] = layer;
                            indexesToSwap[i][j][1] = size - j - 1;
                            break;
                        }
                        case RIGHT: {
                            indexesToSwap[i][j][0] = size - j - 1;
                            indexesToSwap[i][j][1] = size - layer - 1;
                            break;
                        }
                        case BOTTOM: {
                            indexesToSwap[i][j][0] = size - layer - 1;
                            indexesToSwap[i][j][1] = j;
                            break;
                        }
                        case LEFT: {
                            indexesToSwap[i][j][0] = j;
                            indexesToSwap[i][j][1] = layer;
                            break;
                        }
                    }
                }
            }

            rotateSwapHelper(indexesToSwap, SIDES_TO_CHANGE_AX1, -1);
        }
    }

    private void rotateAxis2(int side, int layer) { //top, bottom
        int[][][] indexesToSwap = new int[4][size][2];

        if (side == BOTTOM) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < size; j++) {
                    indexesToSwap[i][j][0] = size - layer - 1;
                    indexesToSwap[i][j][1] = j;
                }
            }

            rotateSwapHelper(indexesToSwap, SIDES_TO_CHANGE_AX2, 1);
        }
        else {
            for (int i = 3; i >= 0; i--) {
                for (int j = 0; j < size; j++) {
                    indexesToSwap[i][j][0] = layer;
                    indexesToSwap[i][j][1] = size - j - 1;
                }
            }

            rotateSwapHelper(indexesToSwap, SIDES_TO_CHANGE_AX2, -1);
        }
    }

    public void rotate(int side, int layer) throws InterruptedException {
        int axis = ((side + 2) % 5) % 3;

        entryProtocol(axis);
        layerMutex[axis][getLayerIndex(side, layer)].acquire();
        beforeRotation.accept(side, layer);

        switch (axis) {
            case 0: {
                rotateAxis0(side, layer); //left, right
                break;
            }
            case 1: {
                rotateAxis1(side, layer); //front, back
                break;
            }
            case 2: {
                rotateAxis2(side, layer); //top, bottom
                break;
            }
        }

        if (layer == 0) {
            rotateFace(side, 1);
        }
        else if (layer == size - 1) {
            rotateFace(getOppositeSideIndex(side), -1);
        }

        afterRotation.accept(side, layer);
        layerMutex[axis][getLayerIndex(side, layer)].release();
        exitProtocol();
    }

    public String show() throws InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();

        entryProtocol(NUMBER_OF_SHOW_GROUP);
        beforeShowing.run();

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    stringBuilder.append(state[i][j][k]);
                }
            }
        }

        afterShowing.run();
        exitProtocol();

        return stringBuilder.toString();
    }
}
