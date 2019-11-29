package streamanalytic;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSink;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.flink.util.Collector;

public class TaxiStreamAnalytic {
	public static void main(String[] args) throws Exception {
		// using flink ParameterTool to parse input parameters
		final String inputQueue = "inputQueue";
		final String outputQueue = "outputQueue";
		final int parallelismDegree = 3;

		// the following is for setting up the execution getExecutionEnvironment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();

		// checkpoint can be used for different levels of message guarantees
		// select one of the following modes
//		final CheckpointingMode checkpointingMode = CheckpointingMode.AT_LEAST_ONCE;
//		// final checkpointMode = CheckpointingMode.AT_LEAST_ONCE;
//		env.enableCheckpointing(1000 * 60, checkpointingMode);
//		// define the event time
		env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

		final RMQConnectionConfig connectionConfig = new RMQConnectionConfig.Builder().setHost("localhost")
				.setPort(5672).setVirtualHost("/").setUserName("admin").setPassword("password").build();
		// declare rabbit mq as a source of data and set parallelism degree
		RMQSource<String> btsdatasource = new RMQSource(connectionConfig, inputQueue, true, new SimpleStringSchema());
		final DataStream<String> btsdatastream = env.addSource(btsdatasource).setParallelism(1);
		DataStream<String> taxiEvents = btsdatastream.flatMap(new BTSParser()).keyBy(new TaxiKeySelector())
				.timeWindow(Time.seconds(5)).process(new MyProcessWindowFunction()).setParallelism(20);
		RMQSink<String> sink =new RMQSink<String>(
				connectionConfig,
				outputQueue,
				new SimpleStringSchema());
		
		taxiEvents.addSink(sink);
		
		taxiEvents.print();
		btsdatastream.print();

		JobExecutionResult res = env.execute("test");
		System.out.println(res.toString());
	}

	public static class TaxiKeySelector implements KeySelector<BTSTaxiEvent, String> {

		public String getKey(BTSTaxiEvent value) throws Exception {
			// TODO Auto-generated method stub
			return value.puLocation;
		}

	}

	public static class BTSParser implements FlatMapFunction<String, BTSTaxiEvent> {

		public void flatMap(String value, Collector<BTSTaxiEvent> out) {
			String[] data = value.split(" ");
			BTSTaxiEvent taxiEvent = new BTSTaxiEvent();
			if (data.length<19) {
				return;
			}
			taxiEvent.venderID = data[5].split(":")[1];
			taxiEvent.doLocation = data[0].split(":")[1];
			taxiEvent.puLocation = data[19].split(":")[1];
			SimpleDateFormat smf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				taxiEvent.dropoffTime = smf.parse(data[3].split(":")[1] + " " + data[4]);
				taxiEvent.pickupTime = smf.parse(data[17].split(":")[1] + " " + data[18]);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			out.collect(taxiEvent);
		}

	}

	private static class MyProcessWindowFunction
			extends ProcessWindowFunction<BTSTaxiEvent, String, String, TimeWindow> {

		@Override
		public void process(String key, ProcessWindowFunction<BTSTaxiEvent, String, String, TimeWindow>.Context context,
				Iterable<BTSTaxiEvent> elements, Collector<String> out) throws Exception {
			HashMap<String, Integer> locationPickupMap = new HashMap<String, Integer>();
			SimpleDateFormat smf = new SimpleDateFormat("MM-dd-HH-");
			for (BTSTaxiEvent taxiRecord : elements) {
				String mapKey = smf.format(taxiRecord.pickupTime) + taxiRecord.puLocation;
				if (locationPickupMap.containsKey(mapKey)) {
					locationPickupMap.put(mapKey, locationPickupMap.get(mapKey) + 1);
				} else {
					locationPickupMap.put(mapKey, 1);
				}
			}
			Iterator keys = locationPickupMap.keySet().iterator();
			if (keys.hasNext()) {
				String outputkey = (String) keys.next();
				String output = outputkey + "," + locationPickupMap.get(outputkey);
				out.collect(output);
			}
		}

	}

}
