package net.floodlightcontroller.anomaly;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.projectfloodlight.openflow.protocol.OFAnomalyDetection;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.anomaly.transition_probability_matrix;

public class AnomalyDetection implements IFloodlightModule, IOFMessageListener {

	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	String csvFilePath = "/home/duc/anomaly/loop_score.csv";
	
	public static double loop_1 = 0;
    public static double loop_2 = 0;
    public static double loop_3 = 0;
    public static double loop_4 = 0;
    public static double loop_5 = 0;
    public static double loop_6 = 0;

    public static File train = new File(
            "C:\\Users\\Admin\\OneDrive - Hanoi University of Science and Technology\\Desktop\\Document\\DDoS\\Do_an_3\\src\\result\\LOOP_python.csv");

    public static String csvFile = "/home/duc/anomaly/anomaly_score.csv";

    public static float[][] matrix_train = transition_probability_matrix.create_matrix(train, 3);

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "ANOMALY_DETECTION";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}
	
    public static int median_filter(int[] values) {
        int[] sortedValues = Arrays.copyOf(values, values.length);
        Arrays.sort(sortedValues);

        if (sortedValues.length % 2 == 0) {
            // Nếu mảng có số phần tử là chẵn
            int midIndex1 = sortedValues.length / 2 - 1;
            int midIndex2 = sortedValues.length / 2;
            return (sortedValues[midIndex1] + sortedValues[midIndex2]) / 2;
        } else {
            // Nếu mảng có số phần tử là lẻ
            int midIndex = sortedValues.length / 2;
            return sortedValues[midIndex];
        }
    }
	
	public static String[] push(String[] array, String push) {
        String[] longer = new String[array.length + 1];
        System.arraycopy(array, 0, longer, 0, array.length);
        longer[array.length] = push;
        return longer;
    }

    public static String[] pop(String[] array, String push) {
        String[] longer = new String[array.length - 1];
        System.arraycopy(array, 0, longer, 0, array.length);
        for (int i = 1; i < array.length; i++)
            longer[i] = array[i];
        return longer;
    }

    public static double cal_expectation(float[][] matrix, int bin) {
        // int bin_r = transition_probability_matrix.get_bin(r);
        double E = 0;
        // System.out.println("matrix_length = " + matrix.length + "bin = " + bin);
        for (int i = 0; i < matrix.length; i++) {
            E = E + (matrix[bin][i] * i);
        }
        // System.out.println("E = " + E);
        return E;
    }

    public static double cal_sigma(float[][] matrix, int bin) {
        // int bin_r = transition_probability_matrix.get_bin(r);
        double E = cal_expectation(matrix, bin);
        double average = E / matrix.length;
        double VX = 0;

        for (int i = 0; i < matrix.length; i++) {
            VX += matrix[bin][i] * bin * bin - average * average;

        }
        double sigma = Math.sqrt(VX);
        return sigma;
    }

    public static int get_median(double[] arr) {
        int[] median = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            median[i] = transition_probability_matrix.get_bin(arr[i]);
        }
        int med = median_filter(median);
        return med;
    }

    public static int get_score(int bin) {
        int score = 0;
        score = (bin % 400) % 20;
        return score;
    }

    public static double predict_score_proposed(int pre_bin, int now_bin,
            float[][] matrix) {

        // calculate expectation of the row corresponds to the row r(n).
        double E = cal_expectation(matrix, pre_bin);
        // System.out.println("E = " + E);

        // calculate standard deviation of the row corresponds to the row bin.
        double sigma = cal_sigma(matrix, pre_bin);
        // System.out.println("sigma = " + sigma);

        // int now_score = get_score(now_bin);
        // calculate prediction error e
        // double e = Math.abs(now_score - E);
        double e = Math.abs(now_bin - E);

        // calculate normalized error b
        double b = e / sigma;

        double temp = b * matrix[pre_bin][now_bin];
        matrix[pre_bin][now_bin] = (int) temp;

        double sum = 0;
        for (int i = 0; i < matrix.length; i++) {
            sum += matrix[pre_bin][i]; // Cộng giá trị vào tổng
        }

        for (int i = 0; i < matrix.length; i++) {
            matrix[pre_bin][i] = (int) (matrix[pre_bin][i] * 100 / sum);
        }

        float max = matrix[now_bin][0];
        // double pred = cal_expectation(matrix, now_bin);
        double pred = 0;
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[now_bin][i] > max) {
                max = matrix[now_bin][i];
                pred = i;

            }
        }
        // pred = get_score(pred);
        // get pred score
        return pred;
    }

    public static double predict_score_paper(int pre_bin, int now_bin, float[][] matrix) {

        // calculate expectation of the row corresponds to the row r(n).
        double E = cal_expectation(matrix, pre_bin);
        // System.out.println("E = " + E);

        // calculate standard deviation of the row corresponds to the row bin.
        double sigma = cal_sigma(matrix, pre_bin);
        // System.out.println("sigma = " + sigma);

        // int now_score = get_score(now_bin);
        // calculate prediction error e
        // double e = Math.abs(now_score - E);
        double e = Math.abs(now_bin - E);

        // calculate normalized error b
        double b = e / sigma;

        double temp = b * matrix[now_bin][now_bin];
        matrix[now_bin][now_bin] = (float) temp;

        double sum = 0;
        for (int i = 0; i < matrix.length; i++) {
            sum += matrix[now_bin][i]; // Cộng giá trị vào tổng
        }

        for (int i = 0; i < matrix.length; i++) {
            matrix[now_bin][i] = (float) (matrix[now_bin][i] * 100 / sum);
        }

        float max = matrix[now_bin][0];
        double pred = 0;
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[now_bin][i] > max) {
                max = matrix[now_bin][i];
                pred = i;

            }
        }
        // pred = get_score(pred);
        // get pred score
        return pred;
    }

    public static void running(double loop_score) {

        int anomaly_count = 0;
        double num_pred = 0;

        // loop_6 = loop_score;

        int pre_bin1 = transition_probability_matrix.get_bin_3(loop_1, loop_2, loop_3);
        int pre_bin2 = transition_probability_matrix.get_bin_3(loop_2, loop_3, loop_4);
        int pre_bin3 = transition_probability_matrix.get_bin_3(loop_3, loop_4, loop_5);
        int pre_bin4 = transition_probability_matrix.get_bin_3(loop_4, loop_5, loop_6);

        int now_bin = transition_probability_matrix.get_bin_3(loop_5, loop_6, loop_score);

        int[] arr_state = new int[] { pre_bin1, pre_bin2, pre_bin3, pre_bin4, now_bin };
        int pre_state = 0;
        int state = median_filter(arr_state);
        double pred = predict_score_proposed(pre_state, state, matrix_train);
        num_pred = pred;

        if (state > num_pred) {
//            System.out.println("Error:" + " threshold = " + num_pred + " state = " +
//                    state);
//            System.out.println("Count = " + anomaly_count);
            anomaly_count += 1;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile,true))) {
                // Ghi dữ liệu vào file CSV

                writer.write(String.valueOf(num_pred));
                writer.write(","); // Phân tách giá trị bằng dấu phẩy
                writer.write(String.valueOf(state));
                writer.newLine(); // Xuống dòng sau mỗi hàng

            } catch (IOException e) {
                System.out.println("Đã xảy ra lỗi khi ghi dữ liệu vào file CSV: " +
                        e.getMessage());
            }
        }
        loop_1 = loop_2;
        loop_2 = loop_3;
        loop_3 = loop_4;
        loop_4 = loop_5;
        loop_5 = loop_6;
        loop_6 = loop_score;

    }
	
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		switch (msg.getType()) {
	    case ANOMALY_DETECTION:

	        /* Retrieve the deserialized packet in message */
//	        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
	    	
	    	// Ép kiểu msg thành OFAnomalyDetection để truy cập phương thức getData()
	    	OFAnomalyDetection anomalyMsg = (OFAnomalyDetection) msg;
	        
	    	// Lấy dữ liệu từ bản tin ANOMALY_DETECTION
            byte[] data = anomalyMsg.getData();
            running(Double.valueOf(String.valueOf(data)));
	    	
	        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath,
                    true))) {
                // Ghi dữ liệu vào file CSV
                writer.write(String.valueOf(data));
                writer.newLine(); // Xuống dòng sau mỗi hàng
               

            } catch (IOException e) {
                System.out.println("Đã xảy ra lỗi khi ghi dữ liệu vào file CSV: " +
                        e.getMessage());
            }
	        /* We will fill in the rest here shortly */

	        break;
	    default:
	        break;
	    }
	    return Command.CONTINUE;

	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
	    Collection<Class<? extends IFloodlightService>> l =
	            new ArrayList<>();
	        l.add(IFloodlightProviderService.class);
	        return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
	    floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
	    macAddresses = new ConcurrentSkipListSet<>();
	    logger = LoggerFactory.getLogger(AnomalyDetection.class);

	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider.addOFMessageListener(OFType.ANOMALY_DETECTION, this);

	}



}
