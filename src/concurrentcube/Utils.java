package concurrentcube;

public class Utils {
    private static final int TOP = 0;
    private static final int LEFT = 1;
    private static final int FRONT = 2;

    public static int getLayerIndex(int side, int layer, int size) {
        if (side == TOP || side == LEFT || side == FRONT) {
            return layer;
        }
        else {
            return size - layer - 1;
        }
    }
}
