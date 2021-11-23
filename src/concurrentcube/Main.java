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
        Cube cube = new Cube(3, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});

        try {
            for (int i = 0; i < 6; i++) {
                cube.rotate(i, 1);
                System.out.println(i + ", 1\n");
                System.out.println(cube.show());

                for (int j = 0; j < 3; j++) {
                    cube.rotate(i, 1);
                }
            }

            System.out.println(cube.show());

            for (int i = 0; i < 1260; i++) {
                cube.rotate(1, 1);
                cube.rotate(2, 3);
                cube.rotate(4, 1);
                cube.rotate(5, 2);
                cube.rotate(0, 1);
                cube.rotate(3, 2);
                cube.rotate(2, 3);

                if (i == 300) {
                    System.out.println(cube.show());
                }
            }

            System.out.println(cube.show());
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
