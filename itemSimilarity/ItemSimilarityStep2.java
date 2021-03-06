package itemSimilarity;

import hdfs.HDFSDao;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;

/*
 * 输入数据格式为【userID,itemID,pref】
 * map后输出【itemID,userID】
 * reduce后输出【itemID:userID1,userID2...】
 * 即统计每一部电影被哪些用户观看过
 */
public class ItemSimilarityStep2 {

	public static final Pattern DELIMITER = Pattern.compile("[\t:,]");

	public static class Step2MapClass extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, Text> {

		private Text k = new Text();
		private Text v = new Text();

		public void map(LongWritable key, Text value,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {

			String[] tokens = DELIMITER.split(value.toString());
			k.set(tokens[1]);
			v.set(tokens[0]);
			output.collect(k, v);
		}

	}

	public static class Step2ReduceClass extends MapReduceBase implements
			Reducer<Text, Text, Text, Text> {

		private Text v = new Text();

		public void reduce(Text key, Iterator<Text> values,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {

			StringBuilder sBuilder = new StringBuilder();
			while (values.hasNext()) {
				sBuilder.append("," + values.next().toString());
			}
			v.set(sBuilder.toString().replaceFirst(",", ""));
			output.collect(key, v);
		}

	}

	public static void run(Map<String, String> path) throws IOException {

		String input = path.get("SimilarityStep2Input");
		String output = path.get("SimilarityStep2Output");

		Configuration configuration = new Configuration();
		JobConf job = new JobConf(configuration, ItemSimilarityStep2.class);

		HDFSDao hdfsDao = new HDFSDao(configuration);
		hdfsDao.rmr(output);

		FileInputFormat.setInputPaths(job, new Path(input));
		FileOutputFormat.setOutputPath(job, new Path(output));

		job.setInputFormat(TextInputFormat.class);
		job.setOutputFormat(TextOutputFormat.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		job.setMapperClass(Step2MapClass.class);
		job.setReducerClass(Step2ReduceClass.class);

		job.set("mapred.textoutputformat.separator", ":");

		RunningJob runningJob = JobClient.runJob(job);
		while (!runningJob.isComplete()) {
			runningJob.waitForCompletion();
		}

	}

}
