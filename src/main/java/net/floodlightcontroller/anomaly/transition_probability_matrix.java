package net.floodlightcontroller.anomaly;

import java.io.*;
import java.util.*;

public class transition_probability_matrix {
    public static int previous_bin = 100;

    public static float[][] cal_rate(float[][] matrix) {
        float[][] matrix_rate = new float[matrix.length][matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            float total_row = 0;
            for (int j = 0; j < matrix[0].length; j++) {
                total_row += matrix[i][j];
            }
            for (int j = 0; j < matrix[0].length; j++) {
                if (total_row != 0) {
                    // System.out.print(matrix[i][j] / total_row);
                    float rate_index = ((matrix[i][j] / total_row));
                    // System.out.print(rate_trans + " ");
                    matrix_rate[i][j] = rate_index;
                } else {
                    // System.out.print(matrix[i][j] + " ");
                    matrix_rate[i][j] = 0;
                }

            }
            // System.out.println();
        }
        return matrix_rate;
    }

    public static void write_csv(float[][] matrix, String file_name) {
        String csvFilePath = "C:\\\\Users\\\\Admin\\\\OneDrive - Hanoi University of Science and Technology\\\\Desktop\\\\Document\\\\DDoS\\\\Do_an_3\\\\src\\\\result\\\\"
                + file_name + ".csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath))) {
            // Ghi dữ liệu vào file CSV
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    writer.write(String.valueOf(matrix[i][j]));
                    writer.write(","); // Phân tách giá trị bằng dấu phẩy
                }
                writer.newLine(); // Xuống dòng sau mỗi hàng
            }

            System.out.println("Ghi dữ liệu thành công vào file CSV.");
        } catch (IOException e) {
            System.out.println("Đã xảy ra lỗi khi ghi dữ liệu vào file CSV: " + e.getMessage());
        }

    }

    
    public static int get_bin(double number) {
        double bin;
        bin = (number * 20);
        int bin_int = Double.valueOf(bin).intValue();
        if (bin_int == 20) {
            bin_int = 19;
        }
        return bin_int;
    }

    public static int get_bin_2(double number1, double number2) {
        int bin_1, bin_2;
        bin_1 = get_bin(number1);
        bin_2 = get_bin(number2);
        int bin_int = (bin_1 * 20 + bin_2);
        return bin_int;
    }

    public static int get_bin_5(double number1, double number2, double number3, double number4, double number5) {
        int bin_1, bin_2, bin_3, bin_4, bin_5;
        bin_1 = get_bin(number1);
        bin_2 = get_bin(number2);
        bin_3 = get_bin(number3);
        bin_4 = get_bin(number4);
        bin_5 = get_bin(number5);
        int bin_int = (int) (bin_1 * Math.pow(20, 4) + bin_2 * Math.pow(20, 3) + bin_3 * 400 + bin_4 * 20
                + bin_5);
        return bin_int;
    }

    public static int get_bin_3(double number1, double number2, double number3) {
        int bin_1, bin_2, bin_3;
        bin_1 = get_bin(number1);
        bin_2 = get_bin(number2);
        bin_3 = get_bin(number3);
        int bin_int = (int) (bin_1 * Math.pow(20, 2) + bin_2 * Math.pow(bin_2, 1) + bin_3);
        return bin_int;
    }
    
    public static List<Double> write_array(File filename) {
        List<Double> array_train = new ArrayList<>();
        try {
            // File loop = new File("/home/duc/Java_Project/Do_an_3/src/loop_training.csv");
            Scanner sc_loop = new Scanner(filename);
            sc_loop.useDelimiter(",");

            while (sc_loop.hasNext()) {
                String index = sc_loop.nextLine();
                Double index_double = Double.valueOf(index);
                double index_value = index_double.doubleValue();
                array_train.add(index_value);
            }
            sc_loop.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return array_train;
    }

    public static float[][] create_matrix(File name_file, int level_matrix) {
        long len_matrix = (int) Math.pow(20, level_matrix);
        float[][] matrix = new float[(int) len_matrix][(int) len_matrix];
        // Gán các phần tử của ma trận là 0
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                matrix[i][j] = 0;
            }
        }

        try {

            List<Double> arr_train = write_array(name_file);
            // int bin_int = get_bin(arr_train.get(0));

            switch (level_matrix) {
                case 1:
                    int pre_bin1 = get_bin(arr_train.get(0));
                    for (int i = 1; i < arr_train.size(); i++) {
                        int now_bin = get_bin(arr_train.get(i));
                        matrix[pre_bin1][now_bin] += 1;
                        // System.out.println(matrix[previous_bin][now_bin]);
                        pre_bin1 = now_bin;
                        // }
                    }
                    break;
                case 2:
                    double pre_num_1 = arr_train.get(0);
                    double pre_num_2 = arr_train.get(1);
                    int pre_bin2 = get_bin_2(pre_num_1, pre_num_2);

                    for (int i = 1; i < arr_train.size() - 1; i += 1) {
                        double now_num_1 = arr_train.get(i);
                        double now_num_2 = arr_train.get(i + 1);
                        int now_bin = get_bin_2(now_num_1, now_num_2);

                        matrix[pre_bin2][now_bin] += 1;
                        pre_bin2 = now_bin;
                    }
                    break;

                case 3:
                    double num_1 = arr_train.get(0);
                    double num_2 = arr_train.get(1);
                    double num_3 = arr_train.get(2);
                    int bin3 = get_bin_3(num_1, num_2, num_3);

                    for (int i = 1; i < arr_train.size() - 2; i += 1) {
                        double now_num_1 = arr_train.get(i);
                        double now_num_2 = arr_train.get(i + 1);
                        double now_num_3 = arr_train.get(i + 2);
                        int now_bin = get_bin_3(now_num_1, now_num_2, now_num_3);

                        matrix[bin3][now_bin] += 1;
                        bin3 = now_bin;
                    }
                    break;

                case 5:
                    double pre_num1 = arr_train.get(0);
                    double pre_num2 = arr_train.get(1);
                    double pre_num3 = arr_train.get(2);
                    double pre_num4 = arr_train.get(3);
                    double pre_num5 = arr_train.get(4);
                    int pre_bin5 = get_bin_5(pre_num1, pre_num2, pre_num3, pre_num4, pre_num5);

                    for (int i = 1; i < arr_train.size() - 4; i++) {
                        double now_num_1 = arr_train.get(i);
                        double now_num_2 = arr_train.get(i + 1);
                        double now_num_3 = arr_train.get(i + 2);
                        double now_num_4 = arr_train.get(i + 3);
                        double now_num_5 = arr_train.get(i + 4);
                        int now_bin = get_bin_5(now_num_1, now_num_2, now_num_3, now_num_4, now_num_5);

                        matrix[pre_bin5][now_bin] += 1;
                        pre_bin5 = now_bin;
                    }
                    break;
            }
            // for (int i = 0; i < matrix.length; i++) {
            // float total_row = 0;
            // for (int j = 0; j < matrix[0].length; j++) {
            // total_row += matrix[i][j];
            // }
            // for (int j = 0; j < matrix[0].length; j++) {
            // if (total_row != 0) {
            // // System.out.print(matrix[i][j] / total_row);
            // float rate_trans = (matrix[i][j] / total_row) * 100;
            // // System.out.print(rate_trans + " ");
            // matrix[i][j] = (int) rate_trans;
            // } else {
            // // System.out.print(matrix[i][j] + " ");
            // matrix[i][j] = 0;
            // }

            // }
            // // System.out.println();
            matrix = cal_rate(matrix);

            write_csv(matrix, "Matrix_train_lv1");

        } catch (Exception e) {
            System.out.println(e);
        }
        return matrix;
    }
}
