package concurrentcube;


public class Main {
    public static void main(String[] args) {
        Cube cube = new Cube(3, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});

        try {
            System.out.println(cube.show());

            for (int i = 0; i < 1260; i++) {
                cube.rotate(1, 0);
                cube.rotate(2, 2);
                cube.rotate(4, 0);
                cube.rotate(5, 1);
                cube.rotate(0, 0);
                cube.rotate(3, 1);
                cube.rotate(2, 2);

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
