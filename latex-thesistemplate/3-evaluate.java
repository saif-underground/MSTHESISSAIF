package org.powertac.trialmodule;

/*
 * Created by Saiful Abu
 On April 19, 2016
 This program writes prediction and true value for moving average prediction scheme for all 
 customers.

 For each time slot > 360 + 168, this program writes down the following in a file
 time slot --- customerName --- powerType --- true avg usage --- predicted avg usage

 It works on some test files. For each file you have to run the program once. Output will be
 appended to a output file. If the output file is not empty already. For the first (fileIndex = 1) file
 the output file if exists deleted then created, for other files, the program simply appends 
 predictions to the file.

 Name of the output file is movingAvgPerformance.csv

 At the end, if analysis of the file is true, it loads the csv file and figures out the prediction for each
 of the predictors. And finally writes down teh prediction errors.
 * */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.powertac.common.ClearedTrade;
import org.powertac.common.CustomerInfo;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.logtool.LogtoolContext;
import org.powertac.logtool.common.DomainObjectReader;
import org.powertac.logtool.common.NewObjectListener;
import org.powertac.logtool.example.MktPriceStats;
import org.powertac.logtool.ifc.Analyzer;

import tools.Settings;
import weka.classifiers.Classifier;
import weka.clusterers.Clusterer;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class MovingAvgPerformanceLogger extends LogtoolContext implements
Analyzer {

HashMap<String, PowerUsageRepo> customerPowerUsageMap;
HashMap<String, CustomerUsageStorer> customerStatisticsStorer;
int START_FROM_SLOT = 600;
int agentIdForSanityCheck = 4470; // agent ude
int slotAgentSanityCheck = 610; // 441; // 610;

static boolean ANALYZE_PERFORMANCE_IN_THE_END = true;
// debug purpose variables

static String logFileNames[] = {

"", "01.test-01", "02.test-02", "03.test-03", "04.test-04", "05.test-05",

};
static int fileIndex = 3;
static String logFileName = logFileNames[fileIndex];
static private Logger log = Logger.getLogger(MktPriceStats.class.getName());

// service references
private TimeslotRepo timeslotRepo;
private TimeService timeService;
private WeatherReportRepo weatherReportRepo;
private OrderbookRepo OrderbookRepo;
private DomainObjectReader dor;
private Timeslot timeslot;
int counter = 0;

private CustomerRepo customerRepo;

// Data
private TreeMap<Integer, ClearedTrade[]> data;
private TreeMap<Integer, CustomMarketTransaction> marketData;

private int ignoreInitial = 0; // timeslots to ignore at the beginning
private int ignoreCount = 0;
private int indexOffset = 0; // should be
// Competition.deactivateTimeslotsAhead - 1

private PrintWriter output = null;
private static String dataFilename = "clearedTrades.data";
public double brokerID;
private BrokerRepo brokerRepo;

// saif
int totalSlot = 360;
int previousTimeSlot = 0;
int totalTariffTransaction = 0;
Map<Integer, WeatherReport> weatherData;

// /////////
int MAXIMUM_PREVIOUS_WEEK_OFFSET = 6;
// PowerUsageRepo powerUsageRepo;
HashMap<Long, PowerUsageRepo> brokerProfile;

// stores predictorType (eg simplekmeans-2 ---> Hashmap(powertype, cluster)
static HashMap<String, HashMap<String, HashMap<Integer, Classifier>>> clusterPredictorMap = new HashMap<String, HashMap<String, HashMap<Integer, Classifier>>>();
static HashMap<String, Classifier> individualPredictorMap = new HashMap<String, Classifier>();
static HashMap<String, HashMap<String, Clusterer>> clusterModelMap = new HashMap<String, HashMap<String, Clusterer>>();

public static void calculateError() throws Exception {
FileReader fileReader = new FileReader(
"OfflinePerformanceEvaluation\\MovingAvgOffline.csv");

// Always wrap FileReader in BufferedReader.
BufferedReader bufferedReader = new BufferedReader(fileReader);
String line = bufferedReader.readLine(); // read the first line
String[] predictors = line.split(","); // the last slot is not predictor
int numberPredictors = predictors.length - 2;

double[] totalPercentError = new double[numberPredictors];
double[] totalError = new double[numberPredictors];
double[] predictedValue = new double[numberPredictors];
int numInstances = 0;

// debug variable
int numPercentWhereKmeansWasBetter = 0;
double KMeansPercentError = 0;
while ((line = bufferedReader.readLine()) != null) {
if (line.isEmpty()) {
continue;
}// end if
String[] tokens = line.split(",");
double trueValue = Double.parseDouble(tokens[tokens.length - 1]);
// checking the effect of the true value. for now avoiding the true
// value if less than 1.
if (trueValue < 1) {
continue;
}
// end if

for (int predNum = 0; predNum < numberPredictors; predNum++) {
predictedValue[predNum] = Double
.parseDouble(tokens[predNum + 1]);
double error = Math.abs((trueValue - predictedValue[predNum])
/ trueValue) * 100;
totalPercentError[predNum] += (error);
totalError[predNum] += Math.abs(trueValue
- predictedValue[predNum]);
// debug to check why kemans-2 percent error is inferior
// than
// moving avg
if (predNum == 1) {
KMeansPercentError = error;
}// end if
if (predNum == 4) {
if (error > KMeansPercentError) {
numPercentWhereKmeansWasBetter++;
}
}// end if
// end debug
}// end for

numInstances++;
}// end while

if (numInstances != 0) {
for (int predNum = 0; predNum < numberPredictors; predNum++) {
double avgPercentError = totalPercentError[predNum]
/ numInstances;
double avgError = totalError[predNum] / numInstances;
System.out.print(predictors[predNum + 1] + " :: ");
System.out.print("Avg abs error "
+ String.format("%.2f", avgError));
System.out.println(" -- Avg % abs error "
+ String.format("%.2f", avgPercentError) + " %");
}// end for
}// end if

// debug to check kmeans-2 vs movign avg
// System.out.println("K-Means-2 was better "
// + numPercentWhereKmeansWasBetter + " out of " + numInstances);
// debug
bufferedReader.close();
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

public static void loadPredictors() {
String clusterTypeFolderRoot = combineFileNamesToMakePathLocation(
Settings.TRAINING_ROOT_FOLDER_NAME,
Settings.CLUSTER_PREDICTOR_FOLDER_NAME);
for (File clusterTypeFolder : new File(clusterTypeFolderRoot)
.listFiles()) {
String clusterTypeName = clusterTypeFolder.getName();
HashMap<String, HashMap<Integer, Classifier>> powerTypeMap = new HashMap<String, HashMap<Integer, Classifier>>();
clusterPredictorMap.put(clusterTypeName, powerTypeMap);
for (File powerTypeFolder : clusterTypeFolder.listFiles()) {
String powerTypeName = powerTypeFolder.getName();
HashMap<Integer, Classifier> classfierMap = new HashMap<Integer, Classifier>();
powerTypeMap.put(powerTypeName, classfierMap);
for (File clusterNumberFolder : powerTypeFolder.listFiles()) {
File classiferFile = clusterNumberFolder.listFiles()[0];
// System.out.println(classiferFileName);
int clusterNumber = Integer
.parseInt(clusterNumberFolder.getName()
.substring(
clusterNumberFolder.getName()
.indexOf('-') + 1));
try {
Classifier classifer = (Classifier) weka.core.SerializationHelper
.read(classiferFile.getAbsolutePath());
classfierMap.put(clusterNumber, classifer);
// System.out.println("loaded one classifier");
} catch (Exception e) {
// TODO Auto-generated catch block
System.out
.println("Could not load the classifier for cluster");
System.out.println("file name "
+ classiferFile.toString());
System.exit(0);
}// end of try catch
}// end for
}// end for
}// end for

// now load the classiiers for individual predictors
String individualPredictorFolder = combineFileNamesToMakePathLocation(
Settings.TRAINING_ROOT_FOLDER_NAME,
Settings.INDIVIDUAL_PREDICTOR_FOLDER_NAME);
for (File customerFolder : new File(individualPredictorFolder)
.listFiles()) {
File classiferFile = customerFolder.listFiles(new FilenameFilter() {
public boolean accept(File dir, String name) {
return name.toLowerCase().endsWith(".model");
}
})[0];

try {
Classifier classifer = (Classifier) weka.core.SerializationHelper
.read(classiferFile.getAbsolutePath());
individualPredictorMap.put(customerFolder.getName(), classifer);
System.out.println(classiferFile.getName());
} catch (Exception e) {
// TODO Auto-generated catch block
System.out.println("Could not load the classifier for cluster");
System.out.println("file name " + classiferFile.toString());
System.exit(0);
}// end of try catch

}// end for

// now load clusterModels
String clusterRootFolder = combineFileNamesToMakePathLocation(
Settings.TRAINING_ROOT_FOLDER_NAME,
Settings.CLUSTER_FOLDER_NAME);
for (File clusterFolder : new File(clusterRootFolder).listFiles()) {
String clusterTypeName = clusterFolder.getName();

HashMap<String, Clusterer> powerTypeClusterMap = new HashMap<String, Clusterer>();
clusterModelMap.put(clusterTypeName, powerTypeClusterMap);
for (File powerTypeFolderFile : clusterFolder.listFiles()) {
if (powerTypeFolderFile.listFiles().length == 0) {
continue;
}// end if
String powerTypeName = powerTypeFolderFile.getName();
try {
Clusterer clusterModel = (Clusterer) weka.core.SerializationHelper
.read(powerTypeFolderFile.listFiles()[0]
.getAbsolutePath());
powerTypeClusterMap.put(powerTypeName, clusterModel);
System.out.println("loaded cluster model");
} catch (Exception e) {
// TODO Auto-generated catch block
System.out.println("Could not load the cluster model");

System.out.println("file name "
+ powerTypeFolderFile.listFiles()[0].toString());
System.exit(0);

}// end of try catch
}// end for
}// end for

}// end of method

/**
 * Main method just creates an instance and passes command-line args to its
 * inherited cli() method.
 * 
 * @throws Exception
 */
public static void main(String[] args) throws Exception {
System.out.println("Data Extracting from log file " + logFileName
+ " ...");
new MovingAvgPerformanceLogger().cli(args);

if (ANALYZE_PERFORMANCE_IN_THE_END) {
calculateError();
}// end if
System.out.println("... Data Extraction finished from " + logFileName);
}// end of main

/**
 * Takes two args, input filename and output filename
 */
private void cli(String[] args) {
// load cluster predictors and individual predictors
loadPredictors();
// create the csv file for tariff transaction inspection
// String tariffTranspectionFileHeader =
// "Transaction_Type,Tariff_Id,Tariff_Type,Customer_Name,Customer_PowerType,Individual_Count,Total_Population,Usage_Kwh,Avg_Usage_Kwh";
// csvWriter = new CSVWriter(
// "C:/Users/iasrluser/Google Drive/Research/result/tariff_transaction_inspection_slot_"
// + slotAgentSanityCheck
// + "_agent_"
// + agentIdForSanityCheck + ".csv",
// tariffTranspectionFileHeader);

String logFolder = "log";

String logFileExtension = "state";
String logFileLocation = logFolder + "/" + logFileName + "."
+ logFileExtension;

String outputFolderBase = "OfflinePerformanceEvaluation";
String outputFileName = "MovingAvgOffline.csv";
String outputFileLocation = outputFolderBase + "/" + outputFileName;

/*
 * if (args.length != 2) {
 * System.out.println("Usage: <analyzer> input-file output-file");
 * return; }//end of if dataFilename = args[1]; super.cli(args[0],
 * this);
 */
if (fileIndex == 1) {
if (Files.exists(Paths.get(outputFolderBase)) == true) {
try {
// Files.deleteIfExists(Paths.get(outputFolderBase));
FileUtils.forceDelete(new File(outputFolderBase));
} catch (IOException e) {
// TODO Auto-generated catch block
e.printStackTrace();
}// end try catch
}// end if

// now create the directory
try {
Files.createDirectory(Paths.get(outputFolderBase));
Files.createFile(Paths.get(outputFileLocation));
} catch (IOException e) {
// TODO Auto-generated catch block
e.printStackTrace();
}
}// end of folder creation
dataFilename = outputFileLocation;
super.cli(logFileLocation, this);
output.close();
// csvWriter.closeWriter();
}// end of method

/*
 * (non-Javadoc)
 * 
 * @see org.powertac.logtool.ifc.Analyzer#setup()
 */
@Override
public void setup() {
dor = (DomainObjectReader) SpringApplicationContext.getBean("reader");
timeslotRepo = (TimeslotRepo) getBean("timeslotRepo");
// weatherReportRepo = (WeatherReportRepo) getBean("WeatherReportRepo");
timeService = (TimeService) getBean("timeService");
brokerRepo = (BrokerRepo) SpringApplicationContext
.getBean("brokerRepo");
// registerNewObjectListener(new BrokerHandler(), Broker.class);

registerNewObjectListener(new TimeslotUpdateHandler(),
TimeslotUpdate.class);
registerNewObjectListener(new TariffTransactionHandler(),
TariffTransaction.class);
// registerNewObjectListener(new MarketTransactionHandler(),
// MarketTransaction.class);
// registerNewObjectListener(new BalancingTransactionHandler(),
// BalancingTransaction.class);
// registerNewObjectListener(new TimeslotHandler(), Timeslot.class);
registerNewObjectListener(new WeatherReportHandler(),
WeatherReport.class);
// registerNewObjectListener(new OrderbookHandler(), Orderbook.class);

ignoreCount = ignoreInitial;
data = new TreeMap<Integer, ClearedTrade[]>();
// saif
weatherData = new HashMap<Integer, WeatherReport>();
// powerUsageRepo = new PowerUsageRepo();
brokerProfile = new HashMap<Long, PowerUsageRepo>();
// saif
marketData = new TreeMap<Integer, CustomMarketTransaction>();
customerPowerUsageMap = new HashMap<String, PowerUsageRepo>();
customerStatisticsStorer = new HashMap<String, CustomerUsageStorer>();
// ///////////////////

try {

// output = new PrintWriter(new File(dataFilename));
output = new PrintWriter(new FileOutputStream(
new File(dataFilename), true /* append = true */));
if (fileIndex == 1) {
String header = "CustomerName,";
for (String clusterTypeName : clusterModelMap.keySet()) {
header += clusterTypeName + ",";
}
header = header + "IndivPredictor, MovingAvg, True-Usage";
output.println(header);
}// end if

} catch (FileNotFoundException e) {
log.error("Cannot open file " + dataFilename);
}
}

/*
 * (non-Javadoc)
 * 
 * @see org.powertac.logtool.ifc.Analyzer#report()
 */
@Override
public void report() {
boolean ret;
ret = true;
if (ret) {
// for now wont use the report method
return;
}// end if

for (Map.Entry<Integer, CustomMarketTransaction> entry : marketData
.entrySet()) {
String delim = "";
Integer timeslot = entry.getKey();
CustomMarketTransaction trades = entry.getValue();

// if (trades.length != 24)
// log.error("short array " + trades.length);
// for (int i = 0; i < trades.length; i++) {

if (null == trades) {
output.print(delim + "[0.0 0.0]");
} else {

output.format("%s %d,%d,%.2f,%.2f, %.2f, %.2f, %.2f", delim,
trades.day, trades.hour, trades.temp,
trades.cloudCoverage, trades.windDirection,
trades.windSpeed, trades.clearingPrice);
}

delim = " ";
// }

output.println();

}
output.close();

}

/*********** Handlers Begin ********/
// -----------------------------------
// catch TimeslotUpdate events
class TimeslotUpdateHandler implements NewObjectListener {

@Override
public void handleNewObject(Object thing) {

int slotOfInterest = timeslotRepo.currentSerialNumber() - 1;
if (slotOfInterest % 100 == 0) {
System.out.println("Processing Time Slot " + slotOfInterest
+ " ...");
}// end if
// if (slotOfInterest < START_FROM_SLOT) {
if (slotOfInterest < 361) {
return;
}// end if
processSlot(slotOfInterest);
}// end of method

/*
 * Forms supervised learning example for a timeSlotIndex.
 * [temporalString] [temperature of 5 slots starting from the slot of
 * interest] [previous week's usage 5 features centered at the same slot
 * as the slot of interest] [actual usage]
 */
void processSlot(int timeSlotIndex) {
// for (PowerUsageRepo powerUsageRepo : brokerProfile.values()) {
// for (Long brokerIdentifier : brokerProfile.keySet()) {
for (String customerName : customerPowerUsageMap.keySet()) {
PowerUsageRepo powerUsageRepo = customerPowerUsageMap
.get(customerName);
if (powerUsageRepo == null) {
// System.out.println("No record for broker "
// + brokerIdentifier);
continue;
}

double trueAvgUsage = powerUsageRepo.getAvgUsage(timeSlotIndex);
int day = timeslotRepo.getDateTimeForIndex(timeSlotIndex)
.getDayOfWeek();
int hour = timeslotRepo.getDateTimeForIndex(timeSlotIndex)
.getHourOfDay();
double movingAvgUsage = powerUsageRepo.getMovingAvgUsage(day,
hour);

powerUsageRepo.addtoMovingAvgUsage(day, hour, trueAvgUsage);
String instance = customerName + ",";
// System.out.println(instance);

// load the proper customer Usage storage
CustomerUsageStorer custUsage = customerStatisticsStorer
.get(customerName);

FastVector attribs = new FastVector(6);
attribs.addElement(new Attribute("cloudcover"));
attribs.addElement(new Attribute("temperature"));
attribs.addElement(new Attribute("windspeed"));
attribs.addElement(new Attribute("mean"));
attribs.addElement(new Attribute("stddev"));
attribs.addElement(new Attribute("prediction"));
// form instance string for predictor
Instances dataset = new Instances("TestInstances", attribs, 0);
// Assign the prediction attribute to the dataset. This
// attribute will
// be used to make a prediction.
dataset.setClassIndex(dataset.numAttributes() - 1);

double[] attributes = new double[6];
WeatherReport weatherReport = weatherData.get(timeSlotIndex);
attributes[0] = weatherReport.getCloudCover();
attributes[1] = weatherReport.getTemperature();
attributes[2] = weatherReport.getWindSpeed();
attributes[3] = custUsage.getMean(day, hour);
attributes[4] = custUsage.getStdDev(day, hour);

Instance instanceToPredict = new Instance(1.0, attributes);

dataset.add(instanceToPredict);
instanceToPredict.setDataset(dataset);

// form cluster string for cluster
double[] weeklyAvgUsage = custUsage.getWeeklyAvgOfAllSlots();
Instance instanceToCluster = new Instance(1.0, weeklyAvgUsage);

// predict for each cluster instance
String customerPowerType = powerUsageRepo.customerInfo
.getPowerType().toString();
for (String clusterTypeName : clusterModelMap.keySet()) {
Clusterer clusterer = (Clusterer) clusterModelMap.get(
clusterTypeName).get(customerPowerType);

try {
int clusterIndex = clusterer
.clusterInstance(instanceToCluster);
Classifier classifer = (Classifier) clusterPredictorMap
.get(clusterTypeName).get(customerPowerType)
.get(clusterIndex);
double prediction = classifer
.classifyInstance(instanceToPredict);
// System.out.println("successfully predicted");
instance += prediction + ",";

} catch (Exception e) {
// TODO Auto-generated catch block
System.out.println("could not predict the instance "
+ e.getMessage());
}
}// end for

// predict using the individual predictors
Classifier classifer = individualPredictorMap.get(customerName);
try {
double prediction = classifer
.classifyInstance(instanceToPredict);
// System.out.println("successfully predicted");
instance += prediction + ",";
} catch (Exception e) {
// TODO Auto-generated catch block
System.out.println("could not predict the instance");
}

instance += movingAvgUsage + "," + trueAvgUsage;

// all prediction has been made so update record now
custUsage.updateRecord(day, hour);
if (timeSlotIndex > START_FROM_SLOT)
writeALine(instance);
}// end for
}// end of method

void debug_avg_usage(int slot, double usage, long brID, String instance) {
if ((slot != slotAgentSanityCheck))
return;
if (brID != agentIdForSanityCheck) {
return;
}// end if
DateTime dateTime = timeslotRepo
.getDateTimeForIndex(slotAgentSanityCheck);
int year = dateTime.getYear();
int month = dateTime.getMonthOfYear();
int dayOfMOnth = dateTime.getDayOfMonth();
int dayOfWeek = dateTime.getDayOfWeek();
int hourOfDay = dateTime.getHourOfDay();
WeatherReport weatherReport2 = weatherData
.get(slotAgentSanityCheck);
WeatherReport weatherReport1 = weatherData
.get(slotAgentSanityCheck - 1);
WeatherReport weatherReport0 = weatherData
.get(slotAgentSanityCheck - 2);

System.out.println(instance);
}// end of method

void writeALine(String string) {
output.write(string + "\n");
output.flush();
}// end of method

void testWriteLine(int currentSlot, int slotToTest, String str) {
if (currentSlot == slotToTest) {
writeALine(str);
}// end if
}// end of method

/*
 * weatherString = <temp-0><temp-1>...<temp-(n-1)> temp-(i) means
 * temperature of t-i slots temperature where t is the current slot.
 * 
 * @param weatherData = list of weather report for each slot i
 * 
 * @param currentTimeSlot = the slot we are predicting for
 * 
 * @param totalReports = number of reports we want to incorporate in the
 * training example
 */
String formWeatherString(Map<Integer, WeatherReport> weatherData,
int currentTimeSlot, int totalReports) {
String weatherReportString = "";
for (int i = 0; i < totalReports; i++) {
int timeSlot = currentTimeSlot - i;
WeatherReport weatherReport = weatherData.get(timeSlot);
double temperature = weatherReport.getTemperature();
weatherReportString = weatherReportString + temperature + ",";
}// end for
return weatherReportString;
}// end of method

String formInstanceString(int timeSlot, PowerUsageRepo powerUsageRepo) {
String instance = "";
int previousSlot = timeSlot - 1;
String temporalString = formTemporalString(timeSlot);
String weatherString = formWeatherString(weatherData, timeSlot, 6);
String immediateUsage = formPreviousAvgUsageString(powerUsageRepo,
previousSlot, 12);
String previousWeekUsage = formPreviousAvgUsageString(
powerUsageRepo, timeSlot - 24 * 7
+ MAXIMUM_PREVIOUS_WEEK_OFFSET, 13);
double currentSlotUsage = powerUsageRepo.getAvgUsage(timeSlot);
instance = temporalString + weatherString + immediateUsage
+ previousWeekUsage + currentSlotUsage;
return instance;
}// end of method

void testFormInstanceString(int currentSlot, int slotToTest,
PowerUsageRepo powerUsageRepo) {
if (currentSlot == slotToTest) {
System.out.println(formInstanceString(slotToTest,
powerUsageRepo));
}// end if
}// end of method

/*
 * forms a string of the following data: <consumption slot 0>
 * <consumption slot -1> .... <consumption slot - (totalslots-1)>
 */
String formPreviousAvgUsageString(PowerUsageRepo powerUsageRepo,
int startingIndex, int totalSlots) {
String usageStr = "";
for (int i = 0; i < totalSlots; i++) {
int slot = startingIndex - i;
// double averageUsage = powerUsageRepo.getUsage(slot) /
// powerUsageRepo.getPopulation(slot);
double averageUsage = powerUsageRepo.getAvgUsage(slot);
usageStr = usageStr + averageUsage + ",";
}// end for
return usageStr;
}// end of method

/*
 * @param timeSlot forms the string of the following format <month
 * [1-12]> <dayOfMonth[1-31]> <dayOfWeek[1-7]> <hour[0-23]>
 */
String formTemporalString(int timeSlot) {

String temporalString = "";
int monthOfYear = timeslotRepo.getDateTimeForIndex(timeSlot)
.getMonthOfYear();
int dayOfMonth = timeslotRepo.getDateTimeForIndex(timeSlot)
.getDayOfMonth();
int dayOfWeek = timeslotRepo.getDateTimeForIndex(timeSlot)
.getDayOfWeek();
int hourOfDay = timeslotRepo.getDateTimeForIndex(timeSlot)
.getHourOfDay();
temporalString = monthOfYear + "," + dayOfMonth + "," + dayOfWeek
+ "," + hourOfDay + ",";
return temporalString;
}// end of method

void testFormTemporalStringMethod(int currentSlot, int slotToTest) {
if (currentSlot == slotToTest) {
System.out.println(formTemporalString(slotToTest));
}// end if
}// end of method

void testFormUsageString(int currentSlot, int slotToTest,
PowerUsageRepo powerUsageRepo) {
if (currentSlot == slotToTest) {
System.out.println(formPreviousAvgUsageString(powerUsageRepo,
currentSlot, 1));
System.out
.println(formPreviousAvgUsageString(powerUsageRepo,
currentSlot - 24 * 7
- MAXIMUM_PREVIOUS_WEEK_OFFSET, 13));

TimeSlotRecord record = powerUsageRepo.getRecord(currentSlot);
System.out.println(record.toString());
System.out.println(formPreviousAvgUsageString(powerUsageRepo,
currentSlot, 1));
}// end if
}// end of method

void testWeatherFormMethod(int currentSlot, int slotToTest) {
if (currentSlot == slotToTest) {
String str = formWeatherString(weatherData, slotToTest, 2);
System.out.println(str);
WeatherReport report = weatherData.get(slotToTest);
report.toString();
report = weatherData.get(slotToTest - 1);
report.toString();
}// end if
}// end of test method

void testDayFormats(int currentSlot) {
int previousSlot = currentSlot - 1;

//
int monthOfYear = timeslotRepo.getDateTimeForIndex(currentSlot)
.getMonthOfYear();
int dayOfMonth = timeslotRepo.currentTimeslot().getStartTime()
.getDayOfMonth();
int dayOfWeek = timeslotRepo.currentTimeslot().dayOfWeek();
int dayHour = timeslotRepo.currentTimeslot().slotInDay();
System.out.print(monthOfYear + " " + dayOfMonth + " " + dayOfWeek
+ " " + dayHour + " : ");

monthOfYear = timeslotRepo.getDateTimeForIndex(previousSlot)
.getMonthOfYear();
dayOfMonth = timeslotRepo.getDateTimeForIndex(previousSlot)
.getDayOfMonth();
dayOfWeek = timeslotRepo.getDateTimeForIndex(previousSlot)
.getDayOfWeek();
dayHour = timeslotRepo.getDateTimeForIndex(previousSlot)
.getHourOfDay();
System.out.println(monthOfYear + " " + dayOfMonth + " " + dayOfWeek
+ " " + dayHour);
}// end of method

void testUsageInsertion(int currentSlot, PowerUsageRepo powerUsageRepo) {
if (currentSlot == 540) {
double usage = powerUsageRepo.getUsage(currentSlot);
int customerCount = powerUsageRepo.getPopulation(currentSlot);
System.out.println("*** usage : " + usage + " customerCount "
+ customerCount + " ***");
}// end if
}// end of method
}// end of class

class WeatherReportHandler implements NewObjectListener {

@Override
public void handleNewObject(Object thing) {

WeatherReport wr = (WeatherReport) thing;

// System.out.println("In the weather report handler");

int timeSlotIndex = wr.getTimeslotIndex();
// System.out.println("Putting weather " + timeSlotIndex);
weatherData.put(timeSlotIndex, wr);
}
}// end of class

class TariffTransactionHandler implements NewObjectListener {

@Override
public void handleNewObject(Object thing) {
TariffTransaction tariffTransaction = (TariffTransaction) thing;

// if (tariffTransaction.getTxType() !=
// TariffTransaction.Type.CONSUME) {

CustomerInfo customerInfo = tariffTransaction.getCustomerInfo();
if (customerInfo == null) {
return;
}// end if
if (customerInfo.getPowerType() != PowerType.CONSUMPTION) {
return;
}// end if

PowerUsageRepo powerUsageRepo;
CustomerUsageStorer custUsage;
if (customerPowerUsageMap.containsKey(customerInfo.getName()) == true) {
powerUsageRepo = customerPowerUsageMap.get(customerInfo
.getName());
custUsage = customerStatisticsStorer
.get(customerInfo.getName());
} else {
powerUsageRepo = new PowerUsageRepo();
powerUsageRepo.customerInfo = customerInfo;
customerPowerUsageMap.put(customerInfo.getName(),
powerUsageRepo);

custUsage = new CustomerUsageStorer(customerInfo.getName(),
tariffTransaction.getTariffSpec());
customerStatisticsStorer.put(customerInfo.getName(), custUsage);
}// end if else

double usageKwh = Math.abs(tariffTransaction.getKWh());
int userCount = tariffTransaction.getCustomerCount();
if ((usageKwh == 0) || (userCount == 0)) {
return;
}// end if

int reportOfSlot = tariffTransaction.getPostedTimeslotIndex();
powerUsageRepo.addUsage(reportOfSlot, usageKwh, userCount);
int dayOfWeek = timeslotRepo.getDateTimeForIndex(reportOfSlot)
.getDayOfWeek();
int hourOfDay = timeslotRepo.getDateTimeForIndex(reportOfSlot)
.getHourOfDay();
int month = timeslotRepo.getDateTimeForIndex(reportOfSlot)
.getMonthOfYear();
int dayOfMonth = timeslotRepo.getDateTimeForIndex(reportOfSlot)
.getDayOfMonth();
custUsage.addUsage(dayOfWeek, hourOfDay, month, dayOfMonth,
usageKwh, userCount, reportOfSlot);
}// end of method

}// end of class

/*********************** Handler End ******************************/

class CustomMarketTransaction {
int timeslotIndex;
double boughtMWh;
double soldMWh;
double mWh;
double price;
double boughtprice;
double soldprice;
int count;
double balancingTrans;
int day;
int hour;
double temp;
double cloudCoverage;
double windSpeed;
double windDirection;
double clearingPrice;
double[] clearings = new double[8720];

CustomMarketTransaction() {
timeslotIndex = 0;
boughtMWh = 0.0;
soldMWh = 0.0;
boughtprice = 0.0;
soldprice = 0.0;
mWh = 0.0;
price = 0.0;
count = 0;
balancingTrans = 0.0;
day = 0;
hour = 0;
temp = 0.0;
cloudCoverage = 0.0;
windSpeed = 0.0;
windDirection = 0.0;
clearingPrice = 0.0;
}
}
}

/***************** class definition ************************/
class TimeSlotRecord {
int timeSlotIndex;
double usage;
int population;

TimeSlotRecord(int timeSlotIndex) {
this.timeSlotIndex = timeSlotIndex;
usage = 0;
population = 0;
}// end of constructor

@Override
public String toString() {
return "timeSlot : " + timeSlotIndex + " usage : " + usage
+ " population : " + population;
}// end of method
}// end of class

// this class stores energy usage for each customer
// a hashmap timeslot ---> usage
// also keeps a moving average data structure for the customer
class PowerUsageRepo {
HashMap<Integer, TimeSlotRecord> slotUsageMap;
double[] weeklyMovingAvgUsage = new double[168];
CustomerInfo customerInfo;

double getMovingAvgUsage(int day, int hour) {
int slot = (day - 1) * 24 + hour;
return weeklyMovingAvgUsage[slot];
}// end of method

void addtoMovingAvgUsage(int day, int hour, double recentAvgUsage) {
int slot = (day - 1) * 24 + hour;
weeklyMovingAvgUsage[slot] = .7 * weeklyMovingAvgUsage[slot] + .3
* recentAvgUsage;

}// end of method

PowerUsageRepo() {
slotUsageMap = new HashMap<Integer, TimeSlotRecord>();
}// end of constructor

void addUsage(int slotIndex, double usageAmount, int customerCount) {
usageAmount = Math.abs(usageAmount); // take positive
TimeSlotRecord slotRecord = slotUsageMap.get(slotIndex);
if (slotRecord == null) {
slotRecord = new TimeSlotRecord(slotIndex);
slotUsageMap.put(slotIndex, slotRecord);
}// end if
// modify old record
slotRecord.usage += usageAmount;
slotRecord.population += customerCount;
}// end of method

double getUsage(int timeSlotIndex) {
TimeSlotRecord slotRecord = slotUsageMap.get(timeSlotIndex);
if (slotRecord == null) {
return 0;
} else {
return slotRecord.usage;
}// end if else
}// end of method

int getPopulation(int timeSlotIndex) {
TimeSlotRecord slotRecord = slotUsageMap.get(timeSlotIndex);
if (slotRecord == null) {
return 0;
} else {
return slotRecord.population;
}// end if else
}// end of method

double getAvgUsage(int timeSlotIndex) {
TimeSlotRecord slotRecord = slotUsageMap.get(timeSlotIndex);
if (slotRecord == null) {
// System.out.println("Slot record is null");
return -1; // no slot record
} else {
if (slotRecord.population == 0) {
// System.out.println("population 0 for slot "
// + slotRecord.timeSlotIndex);
return -2; // no user
} else {
return slotRecord.usage / slotRecord.population;
}
}// end if else
}// end of method

TimeSlotRecord getRecord(int timeSlotIndex) {
TimeSlotRecord record;
record = slotUsageMap.get(timeSlotIndex);
return record;
}// end of method

class CustomerInformationFormation {
double prediction;
double trueAvgUsage;
double[] weeklyMovingAvgUsage = new double[168];

}// end of class
}// end of class

