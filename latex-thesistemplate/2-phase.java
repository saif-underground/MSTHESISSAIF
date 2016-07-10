package tools;

/*
 * Created by Saiful Abu on May 2016.
 * Run several phases.
 *  Phase 0 create training instances from over all -->
 do clustering manually using weka on this data
 phase 1 separates slot based training instances
 based on the cluster
 phase 2: manually check if there exists any outlier in the training set.
 then run phase 2 to combines the slot
 based files stored in cluster folders. This phase will figure out the
 best performing classifier for each cluster
 phase 3 --- separate customer data in separate folder and combine them
 phase 4 --- after checking manually the aggregated data, make prediction
 models that makes best
 prediction for that customer.
 RUN MovingAvgPerformanceLogger.java AFTER ALL THE PHASE ARE COMPLETE
 * 
 * */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.lazy.KStar;
import weka.classifiers.meta.AdditiveRegression;
import weka.classifiers.rules.M5Rules;
import weka.classifiers.trees.M5P;
import weka.classifiers.trees.REPTree;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class FileCombiner_ClusterVersion {
static int PHASE = 4; // Phase 0 create training instances from over all -->
// do clustering manually using weka on this data
// phase 1 separates slot based training instances
// based on the cluster
// phase 2: manually check if there exists any outlier in the training set.
// then run phase 2 to combines the slot
// based files stored in cluster folders. This phase will figure out the
// best performing classifier for each cluster
// phase 3 --- separate customer data in separate folder and combine them
// phase 4 --- after checking manually the aggregated data, make prediction
// models that makes best
// prediction for that customer.
// RUN MovingAvgPerformanceLogger.java AFTER ALL THE PHASE ARE COMPLETE
static String OUT_FILE_NAME_START_PORTION = "training_set_"
+ Settings.CUSTOMER_NAME;

// static String OUT_FILE_NAME_START_PORTION =
// "error_office_complex1_ns_base_";

public static void main(String[] args) {
// combine();
if (PHASE == 0) {
phase0();
} else if (PHASE == 1) {
phase1();
} else if (PHASE == 2) {
phase2();
} else if (PHASE == 3) {
phase3();
} else if (PHASE == 4) {
phase4();
}
}// end of main

public static void phase0() {
String tempFolderName = "temp";
if (Files.exists(Paths.get(Settings.TRAINING_ROOT_FOLDER_NAME)) == false) {
try {
Files.createDirectory(Paths
.get(Settings.TRAINING_ROOT_FOLDER_NAME));
} catch (IOException e) {
// TODO Auto-generated catch block
System.out.println("Could not create root folder");
e.printStackTrace();
}// end try catch
}// end if

if (Files.exists(Paths.get(Settings.TRAINING_ROOT_FOLDER_NAME + "/"
+ tempFolderName))) {
try {
FileUtils.forceDelete(new File(
Settings.TRAINING_ROOT_FOLDER_NAME + "/"
+ tempFolderName));

} catch (IOException e) {
// TODO Auto-generated catch block
System.out.println("Could not delete training folder");
e.printStackTrace();
}// end try catch
}// end if

try {
Files.createDirectory(Paths.get(Settings.TRAINING_ROOT_FOLDER_NAME
+ "/" + tempFolderName));
} catch (IOException e1) {
// TODO Auto-generated catch block
e1.printStackTrace();
System.out.println("could not create temp folder");
}
File[] files = new File(Settings.LOG_EXTRACTED_ROOT_FOLDER_NAME)
.listFiles();
for (File file : files) {
// go inside overall stats folder

File[] foldersInsideOverAllStats = new File(file.toString() + "/"
+ Settings.OVERALL_STATS_FOLDER_NAME).listFiles();
for (File fileOverAllStats : foldersInsideOverAllStats) {
String powerTypeFolderName = fileOverAllStats.getName();
if (Files.exists(Paths.get(Settings.TRAINING_ROOT_FOLDER_NAME
+ "/" + tempFolderName + "/" + powerTypeFolderName)) == false) {
try {
Files.createDirectory(Paths
.get(Settings.TRAINING_ROOT_FOLDER_NAME + "/"
+ tempFolderName + "/"
+ powerTypeFolderName));
} catch (IOException e) {
// TODO Auto-generated catch block
System.out
.println("Could not create power type folder inside temp folder");
e.printStackTrace();
}// end try catch
}// end if
String destinationFolder = Settings.TRAINING_ROOT_FOLDER_NAME
+ "/" + tempFolderName + "/" + powerTypeFolderName;
// String sourceFolder = fileOverAllStats.toString() + "/zscr";
String sourceFolder = fileOverAllStats.toString() + "/avg";
copyFromDirectoryToDirectory(sourceFolder, destinationFolder);
}// end for
}// end for

// upto this point all the files are in temp folder based on the power
// type
String overAllStatsTrainingFolder = Settings.TRAINING_ROOT_FOLDER_NAME
+ "/" + Settings.OVERALL_STATS_FOLDER_NAME;
// create clustered folder
String clusterFolder = Settings.TRAINING_ROOT_FOLDER_NAME + "/"
+ Settings.CLUSTER_FOLDER_NAME;

ifExistDeleteThenCreate(overAllStatsTrainingFolder);
ifExistDeleteThenCreate(clusterFolder);

files = new File(Settings.TRAINING_ROOT_FOLDER_NAME + "/"
+ tempFolderName).listFiles();
for (File file : files) {
String powerType = file.getName();
ifExistDeleteThenCreate(overAllStatsTrainingFolder + "/"
+ powerType);
// create powertype folders in cluster directory
ifExistDeleteThenCreate(clusterFolder + "/" + powerType);
combine(file.toString(), overAllStatsTrainingFolder + "/"
+ powerType, Settings.OVERALL_STATS_HEADER_LOCATION);
}// end for

deleteIfExists(Settings.TRAINING_ROOT_FOLDER_NAME + "/"
+ tempFolderName);

}// end of method

/*
 * loads the cluster model files. separates training instances of slot based
 * files based on the cluster assignments.
 */
public static void phase1() {
System.out.println("running phase 1");
String clusterFolder = Settings.TRAINING_ROOT_FOLDER_NAME + "/"
+ Settings.CLUSTER_FOLDER_NAME;
File[] files = new File(clusterFolder).listFiles();
ifExistDeleteThenCreate(Settings.TRAINING_ROOT_FOLDER_NAME + "/"
+ Settings.SLOT_BASED_TRAINING_SET_FOLDER_NAME);
ifExistDeleteThenCreate(Settings.TRAINING_ROOT_FOLDER_NAME + "/"
+ Settings.CLUSTER_PREDICTOR_FOLDER_NAME);

for (File cluster : files) {
String clusterType = cluster.getName();
String slotBasedTrainingInstanceFolder = Settings.TRAINING_ROOT_FOLDER_NAME
+ "/"
+ Settings.SLOT_BASED_TRAINING_SET_FOLDER_NAME
+ "/"
+ clusterType;
String predictorStorerFolder = Settings.TRAINING_ROOT_FOLDER_NAME
+ "/" + Settings.CLUSTER_PREDICTOR_FOLDER_NAME + "/"
+ clusterType;
ifExistDeleteThenCreate(slotBasedTrainingInstanceFolder);
ifExistDeleteThenCreate(predictorStorerFolder);
for (File file : cluster.listFiles()) {
if (file == null) {
continue;
}
if (file.listFiles().length == 0) {
continue; // don't do anything if the folder does not have a
// cluster model in it
}// end if
String powerType = file.getName();
// System.out.println("has cluster " + powerType);
// create folder for the training instance with appropriate
// power
// type
ifExistDeleteThenCreate(slotBasedTrainingInstanceFolder + "/"
+ powerType);
// creating predictor storage folder
ifExistDeleteThenCreate(predictorStorerFolder + "/" + powerType);

// read corresponding .inst file and make a map that points to
// instance to file location
// TrainingData\OverallStats\CONSUMPTION\CONSUMPTION.inst
String instFileLocation = "TrainingData/OverallStats/"
+ powerType + "/" + powerType + ".inst";
HashMap<Integer, String> instanceToFileLocationMap = new HashMap<Integer, String>();

FileInputStream fis;
try {
fis = new FileInputStream(new File(instFileLocation));
// Construct BufferedReader from InputStreamReader
// BufferedReader
BufferedReader br = new BufferedReader(
new InputStreamReader(fis));

String line = null;
while ((line = br.readLine()) != null) {
// System.out.println(line);
String[] parts = line.split(",");
int instanceNumber = Integer.parseInt(parts[0]);
String logFolderName = parts[1].substring(1,
parts[1].length() - 1);
String customerName = parts[3].substring(1,
parts[3].length() - 1);
int simulationFileNumber = Integer
.parseInt(logFolderName.substring(0, 2));
String arffFileName = customerName + "--game"
+ simulationFileNumber + ".extracted";
String logFileLocation = Settings.LOG_EXTRACTED_ROOT_FOLDER_NAME
+ "/" + logFolderName + "/"

+ Settings.slotBasedFolderName + "/"

+ powerType + "/" + arffFileName;
instanceToFileLocationMap.put(instanceNumber,
logFileLocation);
}// end of while

br.close();
} catch (Exception e) {
// TODO Auto-generated catch block
e.printStackTrace();
System.out.println("could not read inst file");
System.exit(0);
}// end of try catch
// hashmap is ready for use now: it has instance --> file
// name
// in it

// now load the cluster file
// TrainingData\Clusters\CONSUMPTION\CONSUMPTION.model
String clusterPath = Settings.TRAINING_ROOT_FOLDER_NAME + "/"
+ Settings.CLUSTER_FOLDER_NAME + "/"
+ cluster.getName() + "/" + powerType + "/" + powerType
+ ".model";
SimpleKMeans model = null;
try {
// Vector v = (Vector) SerializationHelper.read(MODEL_PATH);
// model = (Classifier) v.get(0);
// Instances header = (Instances) v.get(1);
model = (SimpleKMeans) weka.core.SerializationHelper
.read(clusterPath);
// System.out.println("loaded " + clusterPath);

int clusterCounts = model.getNumClusters();
int[] assignments = model.getAssignments();

// create cluster folder in each powertype folders
for (int i = 0; i < clusterCounts; i++) {
ifExistDeleteThenCreate(slotBasedTrainingInstanceFolder
+ "/" + powerType + "/" + "cluster-" + i);
ifExistDeleteThenCreate(predictorStorerFolder + "/"
+ powerType + "/" + "cluster-" + i);
}// end for

// now copy slot based files in proper cluster folders
for (int instIndex = 0; instIndex < assignments.length; instIndex++) {
int assignedCluster = assignments[instIndex];
String sourceFileLocation = instanceToFileLocationMap
.get(instIndex);
String destFolderLocation = slotBasedTrainingInstanceFolder
+ "/"
+ powerType
+ "/"
+ "cluster-"
+ assignedCluster;
// System.out.println(sourceFile + " --- " + destFile);
copyFileToDirectory(sourceFileLocation,
destFolderLocation);
}// end for
} catch (Exception e) {
// TODO Auto-generated catch block
System.out.println("Error Message: " + e.getMessage());
System.exit(0);
}// end of try catch

}// end for
}// end for
}// end of method phase 1

/*
 * combines the slot based information files stored in different cluster
 * folders.
 */
public static void phase2() {

String slotBasedFolderName = combineFileNamesToMakePathLocation(
Settings.TRAINING_ROOT_FOLDER_NAME,
Settings.SLOT_BASED_TRAINING_SET_FOLDER_NAME);
for (File clusterType : new File(slotBasedFolderName).listFiles()) {
String clusterTypeName = clusterType.getName();
System.out.println("******************************");
System.out.println("Cluster Type " + clusterTypeName);
for (File powerTypeDir : clusterType.listFiles()) {

String powerType = powerTypeDir.getName();
for (File clusterDir : new File(
combineFileNamesToMakePathLocation(slotBasedFolderName,
clusterTypeName, powerType)).listFiles()) {
String clusterName = clusterDir.getName();

System.out.println(powerType + " " + clusterName);
String outputFilePath = combineFileNamesToMakePathLocation(
clusterDir.getAbsolutePath(), "Combined");
deleteIfExists(outputFilePath);
combine(clusterDir.getAbsolutePath(), outputFilePath,
Settings.SLOT_BASED_HEADER_LOCATION);

String predictorFile = combineFileNamesToMakePathLocation(
Settings.TRAINING_ROOT_FOLDER_NAME,
Settings.CLUSTER_PREDICTOR_FOLDER_NAME,
clusterTypeName, powerType, clusterName,
"bestPredictor.model");
String arffFileLocation = new File(outputFilePath)
.listFiles(new FilenameFilter() {
public boolean accept(File dir, String name) {
return name.toLowerCase().endsWith(".arff");
}
})[0].getAbsolutePath();
// System.out.println(predictorFile);
makeBestClassifierForEachCluster(arffFileLocation,
predictorFile);
}// end for
}// end for
System.out.println("******************************");
}// end for

}// end of method phase 2

public static void makeBestClassifierForEachCluster(
String instanceFileLocation, String outputFileLocation) {
// System.out.println("Examining " + instanceFileLocation);
try {
BufferedReader reader = null;
reader = new BufferedReader(new FileReader(instanceFileLocation));
Instances inst = null;
inst = new Instances(reader);
inst.setClassIndex(inst.numAttributes() - 1);
// code for reduction from big feat to reduced feat

// Instances instNew;
// Remove remove;
//
// remove = new Remove();
// remove.setAttributeIndices("5,6,7,32,33,37");
// remove.setInvertSelection(new Boolean(true));
// remove.setInputFormat(inst);
// instNew = Filter.useFilter(inst, remove);
//
// instNew.setClassIndex(instNew.numAttributes() - 1);
//
// ArffSaver saver = new ArffSaver();
// saver.setInstances(instNew);
//
// deleteIfExists(Paths.get(instanceFileLocation + "--reduced.arff")
// .toString());
// saver.setFile(new File(instanceFileLocation + "--reduced.arff"));
// saver.writeBatch();
// end of red feat set

// test classifier creation from the instance set
// we are going to use 7 classifiers and choosing the best among
// them
Classifier bestClassifier = null;
double minRelativePercentError = Double.POSITIVE_INFINITY;
double minMeanAbsError = Double.POSITIVE_INFINITY;

LinkedList<Classifier> classifierList = (LinkedList<Classifier>) Settings.CLASSIFIER_LIST
.clone();

int i = 0;
int bestClassiferINdex = -1;
for (Classifier classifier : classifierList) {

// classifier.buildClassifier(instNew);
// Evaluation eval = new Evaluation(instNew);
// eval.crossValidateModel(classifier, instNew, 4, new
// Random(1));
classifier.buildClassifier(inst);
Evaluation eval = new Evaluation(inst);
eval.crossValidateModel(classifier, inst, 4, new Random(1));
double relativeAbsError = eval.relativeAbsoluteError();
double meanAbsError = eval.meanAbsoluteError();
System.out.println("Making " + Settings.CLASSIFER_NAME[i]);
if (relativeAbsError < minRelativePercentError) {
bestClassiferINdex = i;
bestClassifier = classifier;
minRelativePercentError = relativeAbsError;
minMeanAbsError = meanAbsError;
}// end if else
System.out.println(Settings.CLASSIFER_NAME[i]
+ " relative error : " + relativeAbsError
+ " abs error : " + meanAbsError);
i++;

}// end for

// System.out.println("For customer " + instanceFileLocation
// + " The best classifier "
// + Settings.CLASSIFER_NAME[bestClassiferINdex]
// + " with error " + minRelativePercentError);

// write down the classifer model to file
weka.core.SerializationHelper.write(outputFileLocation,
bestClassifier);
} catch (Exception e) {
// TODO Auto-generated catch block
e.printStackTrace();
}// end of try catch

}// end of method

// combines information of customers with same name together and then
// creates the best
// predictor model out of it.
public static void phase3() {
String temporaryFolder = combineFileNamesToMakePathLocation(
Settings.TRAINING_ROOT_FOLDER_NAME, "TempIndiv");
String indivFolder = combineFileNamesToMakePathLocation(
Settings.TRAINING_ROOT_FOLDER_NAME,
Settings.INDIVIDUAL_PREDICTOR_FOLDER_NAME);
ifExistDeleteThenCreate(temporaryFolder);
ifExistDeleteThenCreate(indivFolder);

HashMap<String, Boolean> customerNameMap = new HashMap<String, Boolean>();
File[] files = new File(Settings.LOG_EXTRACTED_ROOT_FOLDER_NAME)
.listFiles();
for (File file : files) {
// go inside slotbased usage folder
File[] consumptionFolder = new File(file.toString() + "/"
+ Settings.slotBasedFolderName + "/" + "CONSUMPTION")
.listFiles();
for (File customerLog : consumptionFolder) {
String customerName = customerLog.getName().substring(0,
customerLog.getName().indexOf("--"));
customerNameMap.put(customerName, true);
// System.out.println(customerName);
if (Files.exists(Paths
.get(temporaryFolder + "/" + customerName)) == false) {
try {
Files.createDirectory(Paths.get(temporaryFolder + "/"
+ customerName));
} catch (IOException e) {
// TODO Auto-generated catch block
System.out
.println("Could not create separate folders for individual customers");
e.printStackTrace();
}// end try catch
}// end if
String destinationFolder = temporaryFolder + "/" + customerName;
// System.out.println(destinationFolder);
String sourceFile = customerLog.toString();
copyFileToDirectory(sourceFile, destinationFolder);
}// end for
}// end for
// at this point cusotmers of consumption type are copied to folders
// containing the customername
// now combine them to form training instance
File[] customerLogFolder = new File(temporaryFolder).listFiles();
for (File customerLog : customerLogFolder) {
String customerName = customerLog.getName();
combine(customerLog.getAbsolutePath(),
combineFileNamesToMakePathLocation(indivFolder,
customerName), customerName,
Settings.SLOT_BASED_HEADER_LOCATION);
}// end for

// at this point we have all the customers data combined together
// now make best performing classifier for each customer type

}// end of method

// loads the arff file for each customer.
// ignores unneccsary attributes
// checks which prediciton model among 7 models has the least absolute error
// write that predictor to file
public static void phase4() {
// go to each customer folder and load the arff file
String individualPredictorFolder = combineFileNamesToMakePathLocation(
Settings.TRAINING_ROOT_FOLDER_NAME,
Settings.INDIVIDUAL_PREDICTOR_FOLDER_NAME);
File[] indivCustomerFolder = new File(individualPredictorFolder)
.listFiles();
for (File customerFolder : indivCustomerFolder) {
// System.out.println(customerFolder.getName());
// load the arff file
try {
BufferedReader reader = null;
reader = new BufferedReader(new FileReader(
customerFolder.getAbsolutePath() + "/"
+ customerFolder.getName() + ".arff"));
Instances inst = null;
inst = new Instances(reader);
inst.setClassIndex(inst.numAttributes() - 1);

// Instances instNew;
// Remove remove;
// remove = new Remove();
// remove.setAttributeIndices("5,6,7,32,33,37");
// remove.setInvertSelection(new Boolean(true));
// remove.setInputFormat(inst);
// instNew = Filter.useFilter(inst, remove);
// instNew.setClassIndex(instNew.numAttributes() - 1);
//
// ArffSaver saver = new ArffSaver();
// saver.setInstances(instNew);
//
// deleteIfExists(Paths.get(
// customerFolder.getAbsolutePath() + "/"
// + customerFolder.getName() + "--reduced.arff")
// .toString());
// ;
// saver.setFile(new File(customerFolder.getAbsolutePath() + "/"
// + customerFolder.getName() + "--reduced.arff"));
// saver.writeBatch();

// test classifier creation from the instance set
// we are going to use 7 classifiers and choosing the best among
// them
Classifier bestClassifier = null;
double minRelativePercentError = Double.POSITIVE_INFINITY;
double minMeanAbsError = Double.POSITIVE_INFINITY;
LinkedList<Classifier> classifierList = (LinkedList<Classifier>) Settings.CLASSIFIER_LIST
.clone();

int i = 0;
int bestClassiferINdex = -1;
for (Classifier classifier : classifierList) {
// classifier.buildClassifier(instNew);
// Evaluation eval = new Evaluation(instNew);
// eval.crossValidateModel(classifier, instNew, 10,
// new Random(1));
classifier.buildClassifier(inst);
Evaluation eval = new Evaluation(inst);
eval.crossValidateModel(classifier, inst,
Settings.NUM_FOLD_CROSS_VALIDATION, new Random(1));
double relativeAbsError = eval.relativeAbsoluteError();

if (relativeAbsError < minRelativePercentError) {
bestClassiferINdex = i;
bestClassifier = classifier;
minRelativePercentError = relativeAbsError;
minMeanAbsError = eval.meanAbsoluteError();
}// end if else
i++;
}// end for

System.out.println("For customer " + customerFolder.getName()
+ " The best classifier "
+ Settings.CLASSIFER_NAME[bestClassiferINdex]
+ " with error " + minRelativePercentError
+ " mean abs error " + minMeanAbsError);

// write down the classifer model to file
weka.core.SerializationHelper.write(
customerFolder.getAbsolutePath() + "/"
+ customerFolder.getName() + ".model",
bestClassifier);
} catch (Exception e) {
// TODO Auto-generated catch block
e.printStackTrace();
}// end of try catch
}
}// end of method

public static void testAttribDeletetion() throws Exception {
// for test make a classifier based reduced training set
BufferedReader reader = null;

reader = new BufferedReader(
new FileReader(
"TrainingData\\IndividualPredictors\\BrooksideHomes\\BrooksideHomes.arff"));

Instances inst = null;
inst = new Instances(reader);

Instances instNew;
Remove remove;

remove = new Remove();
remove.setAttributeIndices("5,6,7,32,33,37");
remove.setInvertSelection(new Boolean(true));
remove.setInputFormat(inst);
instNew = Filter.useFilter(inst, remove);

instNew.setClassIndex(instNew.numAttributes() - 1);

ArffSaver saver = new ArffSaver();
saver.setInstances(instNew);
deleteIfExists(Paths
.get("TrainingData\\IndividualPredictors\\BrooksideHomes\\BrooksideHomes-reduced-auto.arff")
.toString());
;
saver.setFile(new File(
"TrainingData\\IndividualPredictors\\BrooksideHomes\\BrooksideHomes-reduced-auto.arff"));
saver.writeBatch();

// test classifier creation from the instance set
// we are going to use 7 classifiers and choosing the best among them
Classifier bestClassifier = null;
double minRelativePercentError = Double.POSITIVE_INFINITY;

LinkedList<Classifier> classifierList = new LinkedList<Classifier>();

String[] classifierName = { "M5RULES", "M5P", "REPTree",
"AdditiveRegression", "KStar", "LinearRegression",
"MultiLayerPerceptron" };
classifierList.add(new M5Rules());
classifierList.add(new M5P());
classifierList.add(new REPTree());
classifierList.add(new AdditiveRegression());
classifierList.add(new KStar());
classifierList.add(new LinearRegression());
classifierList.add(new MultilayerPerceptron());

int i = 0;
int bestClassiferINdex = -1;
for (Classifier classifier : classifierList) {
classifier.buildClassifier(instNew);
Evaluation eval = new Evaluation(instNew);
eval.crossValidateModel(classifier, instNew, 10, new Random(1));
double relativeAbsError = eval.relativeAbsoluteError();

if (relativeAbsError < minRelativePercentError) {
bestClassiferINdex = i;
bestClassifier = classifier;
minRelativePercentError = relativeAbsError;
}// end if else
i++;
}// end for

System.out.println("The best classifier "
+ classifierName[bestClassiferINdex] + " with error "
+ minRelativePercentError);

}// end of method

public static String combineFileNamesToMakePathLocation(String... strings) {
if (strings.length <= 0) {
return null;
}
;
String output = strings[0];
for (int i = 1; i < strings.length; i++) {
output += "/" + strings[i];
}// end for
return output;
}// end of method

public static void deleteIfExists(String filePath) {
if (Files.exists(Paths.get(filePath))) {
try {
FileUtils.forceDelete(new File(filePath));

} catch (IOException e) {
// TODO Auto-generated catch block
System.out.println("Could not delete folder " + filePath);
e.printStackTrace();
}// end try catch
}// end if
}

/*
 * source -- folder that contains files to be combined destination -- the
 * combined file name headerLocation -- location of the header file
 * generates a random name for the output file name
 */
public static void combine(String source, String destination,
String headerLocation) {
Path outputFilePath = Paths
.get(new File(getOutPutFileName(destination)).getAbsolutePath());

List<Path> inputs = new LinkedList<Path>();
inputs.add(Paths.get(new File(headerLocation).getAbsolutePath())); // add
// header
// file's
// location
// at
// the
// beginning
inputs.addAll(getPathofAllFilesFromFolder(source));
concatenateFiles(inputs, outputFilePath);

}// end of emethod

/*
 * source -- folder that contains files to be combined destination -- the
 * combined file name headerLocation -- location of the header file
 * generates a random name for the output file name outputFileName --
 * creates the outputfile in the destination Directory
 */
public static void combine(String source, String destination,
String outputFileName, String headerLocation) {
Path outputFilePath = Paths.get(new File(destination + "/"
+ outputFileName + ".arff").getAbsolutePath());

List<Path> inputs = new LinkedList<Path>();
inputs.add(Paths.get(new File(headerLocation).getAbsolutePath())); // add
// header
// file's
// location
// at
// the
// beginning
inputs.addAll(getPathofAllFilesFromFolder(source));
concatenateFiles(inputs, outputFilePath);

}// end of emethod

public static void ifExistDeleteThenCreate(String filePath) {

deleteIfExists(filePath);

try {
Files.createDirectory(Paths.get(filePath));
} catch (IOException e1) {
// TODO Auto-generated catch block
e1.printStackTrace();
System.out.println("could not create folder " + filePath);
}
}// end of method

// copies a file to a Directory
public static void copyFileToDirectory(String srcFileLocation,
String destinationDirectoryLocation) {
Path file = Paths.get(srcFileLocation);
Path to = Paths.get(destinationDirectoryLocation);
try {
Files.copy(file, to.resolve(file.getFileName()));
} catch (IOException e) {
// TODO Auto-generated catch block
System.out.println("could not copy file to a directory");
e.printStackTrace();
}
}// end of method copy file to directory

// copies content of one directory to antoher directory
public static void copyFromDirectoryToDirectory(String src,
String destination) {
File source = new File(src);
File dest = new File(destination);
try {
FileUtils.copyDirectory(source, dest);
} catch (IOException e) {
e.printStackTrace();
}
}// end of method

public static void combine() {
int headerVersion = 6;

String featureSet = "-feature-set-1";
String headerFolder = "header";
// String headerFileName = "header_V" + headerVersion + "" + featureSet
// + ".txt"; //
String headerFileName = "header-overallstats.txt";
// header.txt
// String headerFileName = "header-error.txt"; // header.txt

// String featureFolder = "error-customer";
// String featureFolder = "test-set-extracted-feature-set-1" + "-v"
// + headerVersion;
String featureFolder = "extractedFeatureFile-feature-set-1-V"
+ headerVersion;
// String trainingSetFolder = "trainingSet";
String trainingSetFolder = "trainingSet-feature-set-1-v"
+ headerVersion;
// String trainingSetFolder = "error-training-set";
String outputFileName = getOutPutFileName(trainingSetFolder);
Path outputFilePath = Paths.get(new File(outputFileName)
.getAbsolutePath());

List<Path> inputs = new LinkedList<Path>();
inputs.add(Paths.get(new File(headerFolder + "/" + headerFileName)
.getAbsolutePath())); // add header file's location at the
// beginning
inputs.addAll(getPathofAllFilesFromFolder(featureFolder));

// testOutputFileGen(outputFilePath.toString());
print("Started Conacatenating...");
concatenateFiles(inputs, outputFilePath);
print("...Finished Concatenating");
}// end of emethod

public static void concatenateFiles(List<Path> inputs, Path output) {
if (Files.notExists(output.getParent())) {
try {
Files.createDirectories(output.getParent());

} catch (Exception e) {
print("Could not create file");
e.printStackTrace();
}// end try catchh
}// end if

Path instanceInfoFile = Paths.get(output.getParent().toAbsolutePath()
.toString()
+ "/" + output.getParent().getFileName().toString() + ".inst");
try { // always create a new file
Files.createFile(output);
Files.createFile(instanceInfoFile);
} catch (IOException e1) {
// TODO Auto-generated catch block
e1.printStackTrace();
}// end try catch

// instance file

// System.out.println("instance file " + instanceInfoFile.toString());

// Charset for read and write
Charset charset = StandardCharsets.UTF_8;
// Join files (lines)
int curIndex = 0;
for (Path path : inputs) {

try {
List<String> lines = Files.readAllLines(path, charset);
Files.write(output, lines, charset, StandardOpenOption.CREATE,
StandardOpenOption.APPEND);

if (path.getFileName().toString().endsWith(".txt")) { // skip
// for
// header
continue;
}// end if
// the instance file will be garbase for folders other than
// overallstats
String[] words = lines.get(0).split(",");
List<String> instanceLine = new LinkedList<String>();
instanceLine.add(curIndex + "," + words[0] + "," + words[1]
+ "," + words[2]);
Files.write(instanceInfoFile, instanceLine, charset,
StandardOpenOption.CREATE, StandardOpenOption.APPEND);
curIndex++;
} catch (IOException e) {
print("Error reading files");
// TODO Auto-generated catch block
e.printStackTrace();
}// end of try catch
}// end of for
}// end of method

public static void testOutputFileGen(String str) {
print(str);
}// end of method

public static String getOutPutFileName(String trainingSetFolder) {
DateFormat dateFormat = new SimpleDateFormat("MM_dd_HH_mm_yyyy");
Date date = new Date();
// System.out.println(dateFormat.format(date)); //2014/08/06 15:59:48
String endPortion = dateFormat.format(date);
String OUT_FILE_NAME = trainingSetFolder + "/"
+ OUT_FILE_NAME_START_PORTION + "_" + endPortion + ".arff";
return OUT_FILE_NAME;
}// end of method

public static List<Path> getPathofAllFilesFromFolder(String directory) {
List<Path> path = new LinkedList<Path>();

File[] files = new File(directory).listFiles();
for (File file : files) {
if (file.isFile()) {
path.add(Paths.get(file.getAbsolutePath()));
}// end if
}// end for
return path;
}// end of method

public static void print(String str) {
System.out.println(str);
}// end of method
}// end of method
