package concurrentcube;

import java.util.function.BiConsumer;


public class Main {
    private static class Utils {
        public static final BS beforeShowing = new BS();
        public static final AS afterShowing = new AS();

        public static BiConsumer<Integer, Integer> beforeRotation = (integer, integer2) -> {

        };

        public static BiConsumer<Integer, Integer> afterRotation = (integer, integer2) -> {

        };

        private static class BS implements Runnable {
            @Override
            public void run() {

            }
        }

        private static class AS implements Runnable {
            @Override
            public void run() {

            }
        }
    }

    public static void main(String[] args) {
        Cube cube = new Cube(3, Utils.beforeRotation, Utils.afterRotation, Utils.beforeShowing, Utils.afterShowing);

        try {
            System.out.println(cube.show());
            cube.rotate(0, 1);
            System.out.println(cube.show());
            cube.rotate(5, 1);
            System.out.println(cube.show());
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
