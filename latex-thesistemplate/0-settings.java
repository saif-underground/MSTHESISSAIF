package tools;

import java.util.Arrays;
import java.util.LinkedList;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.rules.M5Rules;
import weka.classifiers.trees.M5P;
import weka.classifiers.trees.REPTree;

public class Settings {

public static final int NUM_FOLD_CROSS_VALIDATION = 4;
// stats files.
// Phase 1 : separate training instances of slot based files based on
// clusters
// Phase 3: combine training instances of slot based files
static String CUSTOMER_NAME = "cluster-test";
public static String TRAINING_ROOT_FOLDER_NAME = "TrainingData";
public static String OVERALL_STATS_FOLDER_NAME = "OverallStats";
static String OVERALL_STATS_HEADER_LOCATION = "header/header-overallstats.txt";
public static String CLUSTER_FOLDER_NAME = "Clusters";
public static String SLOT_BASED_TRAINING_SET_FOLDER_NAME = "SlotBasedTrainingInstances";
public static String INDIVIDUAL_PREDICTOR_FOLDER_NAME = "IndividualPredictors";
// log extracted file's setting
public static String LOG_EXTRACTED_ROOT_FOLDER_NAME = "SimulationData"; //
// public static String OVERALL_STATS_FOLDER_NAME = "OverAllStats"; //
public static String AVERAGE_FOLDER_NAME = "AverageUsage";
public static String zScoreUsageFolder = "ZScoreUsage";
public static String bootStrapFolder = "BootStrapUsage";
public static String slotBasedFolderName = "SlotBasedUsage";
public static String CLUSTER_PREDICTOR_FOLDER_NAME = "ClusterPredictors";
// public static String SLOT_BASED_HEADER_LOCATION =
// "header/header_V6-feature-set-1.txt";
public static String SLOT_BASED_HEADER_LOCATION = "header/header_V6-feature-set-1-red.txt";

public static String[] CLASSIFER_NAME = { "M5RULES", "M5P", "REPTree",
// "AdditiveRegression", "KStar",
"LinearRegression"
// , "MultiLayerPerceptron"
};
public static LinkedList<Classifier> CLASSIFIER_LIST = new LinkedList<Classifier>(
Arrays.asList(new M5Rules(), new M5P(), new REPTree(),
// new AdditiveRegression(), new KStar(),
new LinearRegression()
// , new MultilayerPerceptron()
));
}// end class
