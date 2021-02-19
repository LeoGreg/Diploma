import java.io.*;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class Main implements MainLogic {

    private final static String address = "C:\\Users\\User\\Desktop\\Grigor.gdt";

    private String FINAL_SHIFTED_RIGHT_COLUMN;
    private String middle_column = "";
    private String right_column = "";


    class AsyncMiddle implements Callable<String> {
        String[] changing = middle_column.split("\n");
        private Integer multiplier;

        public AsyncMiddle(int multiplier) {

            this.multiplier = multiplier;
        }

        @Override
        public String call() throws Exception {

            return breedMiddleColumn(changing, multiplier);
        }
    }


    private final static String READ;
    private static String main[];

    static {
        String main_4x4_layouts = "";
        READ = FileHandler.getInstance().read(address);
        String[] arr1 = READ.split("SRAM2RW4x4");
        String arr2[] = arr1[1].split("SRAM2RW4x8");
        Pattern pattern = Pattern.compile(NumberPattern.layout_pattern);
        Matcher matcher = pattern.matcher(arr2[0]);
        while (matcher.find()) {
            main_4x4_layouts += matcher.group() + "\n";
        }

        main = main_4x4_layouts.split("\n");

    }


    static class InvalidWordOrBitInputs extends Exception {
        public InvalidWordOrBitInputs(String message) {
            super(message);
        }

        public static void isValid(boolean ex, String message) throws InvalidWordOrBitInputs {
            if (ex) {
                throw new InvalidWordOrBitInputs(message);
            }
        }
    }

    private String asynchronousCallForMiddleColumn(int bit) throws ExecutionException, InterruptedException {
        int num = bit / 4;
        int thread_n = 10;
        ExecutorService executor = Executors.newFixedThreadPool(thread_n);
        List<AsyncMiddle> list = new ArrayList<>();

        for (int i = 0; i < num; i++) {
            AsyncMiddle r = new AsyncMiddle(i);
            list.add(r);
        }
        List<Future<String>> future = null;
        future = executor.invokeAll(list);
        System.out.println("almost there hang on a bit...");
        executor.shutdown();
        StringBuilder current = new StringBuilder();
        for (long i = 0; i < future.size(); i++) {
            current.append(future.get((int) i).get());
        }

        return current.toString();

    }


    private String shiftRightColumn(String right_column, int word, int bit) {

        int num = (bit / 4) - 1;
        final String[] shifting = right_column.split("\n");
        String[] finalChanged = new String[shifting.length];
        String FINAL_RIGHT_COLUMN = "";
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(3);

        for (int i = 0; i < shifting.length; i++) {
            String numsXY = shifting[i].replaceAll(NumberPattern.num_pattern, "").split(" ", 2)[1];
            double x = Double.parseDouble(numsXY.split(" ")[0]);
            double y = Double.parseDouble(numsXY.split(" ")[1]);
            double finalX = x + num * ShiftSize.dx;
            String fx = nf.format(finalX);
            String fy = nf.format(y);
            String first_part = shifting[i].split(" xy")[0];
            finalChanged[i] = first_part + " " + "xy(" + fx + " " + fy + ")}";
            FINAL_RIGHT_COLUMN += finalChanged[i] + "\n";
        }
        return FINAL_RIGHT_COLUMN;

    }


    private String breedMiddleColumn(String[] changing, int d) {
        String FINAL_MIDDLE_COLUMN = "";
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(3);
        for (int i = 0; i < changing.length; i++) {
            String numsXY = changing[i].replaceAll(NumberPattern.num_pattern, "").split(" ", 2)[1];
            double x = Double.parseDouble(numsXY.split(" ")[0]);
            double y = Double.parseDouble(numsXY.split(" ")[1]);
            double finalX = x + d * ShiftSize.dx;
            String first_part = changing[i].split(" xy")[0];
            FINAL_MIDDLE_COLUMN += first_part + " " + "xy(" + nf.format(finalX) + " " + nf.format(y) + ")}" + "\n";

        }

        return FINAL_MIDDLE_COLUMN;
    }


    private String breedMatrices(String main_matrix, int word) {
        StringBuilder final_matrices = new StringBuilder();
        String first_part = main_matrix.split(" xy")[0];
        int num = word / 4;
        String matrix_nums = main_matrix.replaceAll(NumberPattern.num_pattern, "").split(" ", 2)[1];
        double xmatrix = Double.parseDouble(matrix_nums.split(" ")[0]);
        double ymatrix = Double.parseDouble(matrix_nums.split(" ")[1]);

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(3);

        for (int i = 0; i < num; i++) {
            double finalY = ymatrix + i * ShiftSize.dy;
            String fy = nf.format(finalY);
            String fx = nf.format(xmatrix);
            final_matrices.append(first_part + " " + "xy(" + fx + " " + fy + ")}" + "\n");
        }

        return final_matrices.toString();
    }


    private String[] topTripletPulUp(int word) {
        String[] top_triplet = {main[MainPosition.left_top], main[MainPosition.middle_top], main[MainPosition.right_top]};
        String finalTopHorizontalRow[] = new String[top_triplet.length];
        int matrix_number = word / 4;
        int endY_multiply = matrix_number - 1;

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(3);

        for (int i = 0; i < top_triplet.length; i++) {
            String numXy = top_triplet[i].replaceAll(NumberPattern.num_pattern, "").split(" ", 2)[1];
            double x = Double.parseDouble(numXy.split(" ")[0]);
            double y = Double.parseDouble(numXy.split(" ")[1]);
            double finalY = y + endY_multiply * ShiftSize.dy;
            String fxx = nf.format(x);
            String fy = nf.format(finalY);
            finalTopHorizontalRow[i] = top_triplet[i].split(" xy")[0] + " " + "xy(" + fxx + " " + fy + ")}" + "\n";

        }

        return finalTopHorizontalRow;
    }


    private String makeAllChanges(int word, int bit) throws ExecutionException, InterruptedException {
        String FINAL_LEFT_COLUMN = "";
        String finalTopHorizontalRow[] = new String[3];
        String final_matrices;
        int top_triplet_right = 2;
        int top_triplet_middle = 1;
        int top_triplet_left = 0;
        int leftNum = AsideSolution.log2(word);//3 4 5 6
        String left_decoder = Decoders.getLeftByName(Integer.toString(leftNum), Integer.toString(word));
        String right_decoder = Decoders.getRightByName(Integer.toString(leftNum), Integer.toString(word));

        finalTopHorizontalRow = topTripletPulUp(word);
        final_matrices = breedMatrices(main[MainPosition.middle_below_top], word);

        middle_column += finalTopHorizontalRow[top_triplet_middle] + final_matrices + main[MainPosition.middle_above_bottom] + "\n" +
                main[MainPosition.middle_bottom] + "\n";
        right_column += finalTopHorizontalRow[top_triplet_right] + right_decoder + "\n" + main[MainPosition.right_above_bottom] + "\n" +
                main[MainPosition.right_bottom] + "\n";


        String MIDDLE_FINAL_COLUMN = asynchronousCallForMiddleColumn(bit);//


        Thread t2 = new Thread() {
            @Override
            public void run() {
                FINAL_SHIFTED_RIGHT_COLUMN = shiftRightColumn(right_column, word, bit);
            }
        };

        t2.start();
        try {

            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        FINAL_LEFT_COLUMN += finalTopHorizontalRow[top_triplet_left] + left_decoder + "\n" + main[MainPosition.left_above_bottom] + "\n" + main[MainPosition.left_bottom] + "\n";
        return MIDDLE_FINAL_COLUMN + FINAL_LEFT_COLUMN + FINAL_SHIFTED_RIGHT_COLUMN;

    }


    @Override
    public void buildLayouts() {
        Scanner in = new Scanner(System.in);
        System.out.println("write down word_line size ");
        int word = in.nextInt();
        System.out.println("write down bit_line size");
        int bit = in.nextInt();
        try {
            InvalidWordOrBitInputs.isValid(word < 4 || bit < 4, "invalid inputs");
            InvalidWordOrBitInputs.isValid(word % 4 != 0 || bit % 4 != 0, "invalid inputs");
            InvalidWordOrBitInputs.isValid(word > 1024, "invalid inputs");
            InvalidWordOrBitInputs.isValid(AsideSolution.check_log2(word) % 1 != 0, "wrong word size,can't find decoder");
        } catch (InvalidWordOrBitInputs e) {
            System.out.println(e.getMessage());
            System.out.println("try again later");
            System.exit(1);
        }
        System.out.println("please wait....");
        String needed = AsideSolution.removeLastCharacter(READ);
        FileHandler.getInstance().write(address, needed, false);
        String full_design;

        String cell_name = AsideSolution.SRAM_name_generator(word, bit);
        long start = new Date().getTime();
        String cell_layouts = null;
        try {
            cell_layouts = makeAllChanges(word, bit);
        } catch (Throwable e) {
            System.out.println("nothing has been written because of the problem");
            System.exit(1);
        }
        long end = new Date().getTime();
        String extension = "}" + "\n}" + "\n";
        full_design = cell_name + "\n" + cell_layouts + extension;
        FileHandler.getInstance().write(address, full_design, true);
        System.out.println(Message.SUCCESS_MESSAGE);
        System.out.println("needed time in sec : " + (end - start) / 1000);
        System.exit(0);
    }


    public static void main(String[] args) {
        new Main().buildLayouts();

    }
}


class FileHandler implements FileInOutProtocol {

    private static final FileHandler INSTANCE = new FileHandler();

    private FileHandler() {

    }

    public static FileHandler getInstance() {
        if (INSTANCE == null) {
            return new FileHandler();
        }
        return INSTANCE;
    }

    private static void exists(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(Message.STH_WRONG_MESSAGE);
            }
        }
    }

    @Override
    public String read(String address) {
        File file = new File(address);
        FileInputStream fileInputStream = null;
        byte[] buffer;
        try {
            if (!file.canRead()) {
                throw new IOException();
            }
            fileInputStream = new FileInputStream(file);
            buffer = new byte[fileInputStream.available()];
            fileInputStream.read(buffer);
            fileInputStream.close();
            return new String(buffer);
        } catch (IOException e) {
            throw new RuntimeException(Message.STH_WRONG_MESSAGE);
        }
    }

    @Override
    public void write(String address, String text, boolean append) {
        File file = null;
        FileOutputStream fileOutputStream = null;
        try {

            file = new File(address);
            exists(file);
            fileOutputStream = new FileOutputStream(file, append);
            byte[] array = text.getBytes();
            fileOutputStream.write(array);
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(Message.STH_WRONG_MESSAGE);
        }
    }
}


interface FileInOutProtocol {

    String read(String address);

    void write(String address, String text, boolean append);

}

interface MainLogic {

    void buildLayouts();

}

final class AsideSolution {
    private final static String FIRST_PART = "MYSRAM2RW";

    public static String SRAM_name_generator(int words, int bits) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String celDes = "cell{c=2017-09-25 13:22:51" + " " + "m=" + dateFormat.format(new Date()) + " " + "\'" + FIRST_PART + words + "x" + bits + "\'";
        return celDes;
    }//last } does't generate

    public static String removeLastCharacter(String str) {
        String result = null;
        if ((str != null) && (str.length() > 0)) {
            result = str.substring(0, str.length() - 2);
        }
        return result;
    }

    public static int log2(int N) {

        int result = (int) (Math.log(N) / Math.log(2));

        return result;
    }

    public static double check_log2(int N) {

        double result = (double) (Math.log(N) / Math.log(2));

        return result;
    }
}

final class Decoders {
    private static final String dec2x4_left = "s{'sgd_dec2x4_left' xy(0 12.929)}";
    private static final String dec2x4_right = "s{'sgd_dec2x4_right' xy(17.783 12.929)}";

    private static final String dec3x8_left = "s{'sgd_dec3x8_left' xy(0 12.929)}";
    private static final String dec3x8_right = "s{'sgd_dec3x8_right' xy(17.783 12.929)}";

    private static final String dec4x16_left = "s{'sgd_dec4x16_left' xy(0 12.929)}";
    private static final String dec4x16_right = "s{'sgd_dec4x16_right' xy(17.783 12.929)}";

    private static final String dec5x32_left = "s{'sgd_dec5x32_left' xy(0 12.929)}";
    private static final String dec5x32_right = "s{'sgd_dec5x32_right' xy(17.783 12.929)}";

    private static final String dec6x64_left = "s{'sgd_dec6x64_left' xy(0 12.929)}";
    private static final String dec6x64_right = "s{'sgd_dec6x64_right' xy(17.783 12.929)}";

    private static final String dec7x128_left = "s{'sgd_dec7x128_left' xy(0 12.929)}";
    private static final String dec7x128_right = "s{'sgd_dec7x128_right' xy(17.783 12.929)}";

    private static final String dec8x256_left = "s{'sgd_dec_8x256_left' xy(0 12.929)}";
    private static final String dec8x256_right = "s{'sgd_dec_8x256_right' xy(17.783 12.929)}";

    private static final String dec9x512_left = "s{'sgd_dec_9X512_left' xy(0 12.929)}";
    private static final String dec9x512_right = "s{'sgd_dec_9X512_Right' xy(17.783 12.929)}";

    private static final String dec10x1024_left = "s{'sgd_dec_10X1024_left' xy(0 12.929)}";
    private static final String dec10x1024_right = "s{'sgd_dec_10X1024_Right' xy(17.783 12.929)}";

    public static String getLeftByName(String leftN, String rightNum) {
        String name = "dec" + leftN + "x" + rightNum + "_left";
        Field field = null;
        Object o = null;
        try {
            field = Decoders.class.getDeclaredField(name);
            field.setAccessible(true);
            o = field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.out.println(e.toString());
        }
        return o.toString();

    }


    public static String getRightByName(String leftN, String rightNum) {
        String name = "dec" + leftN + "x" + rightNum + "_right";
        Field field = null;
        Object o = null;
        try {
            field = Decoders.class.getDeclaredField(name);
            field.setAccessible(true);
            o = field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.out.println(e.toString());
        }
        return o.toString();
    }

}

final class Message {
    public static final String SUCCESS_MESSAGE = "finished successfully";
    public static final String STH_WRONG_MESSAGE = "something went wrong";
    public static final String WRONG_INPUTS = "wrong inputs try again...";
}

final class NumberPattern {
    public final static String layout_pattern = "s(\\{)([A-Za-z_0-9_\\s_[().,' '])]){0,}(\\})";
    public final static String num_pattern = "[^0-9.\\s+]";
}

final class MainPosition {

    public final static int middle_top = 0;
    public final static int middle_below_top = 1;
    public final static int middle_above_bottom = 4;
    public final static int middle_bottom = 3;

    public final static int right_top = 11;
    public final static int right_below_top = 5;
    public final static int right_above_bottom = 9;
    public final static int right_bottom = 10;

    public final static int left_top = 7;
    public final static int left_below_top = 8;
    public final static int left_above_bottom = 6;
    public final static int left_bottom = 2;
}

final class ShiftSize {
    public final static double dx = 3.626;
    public final static double dy = 2.4;
}