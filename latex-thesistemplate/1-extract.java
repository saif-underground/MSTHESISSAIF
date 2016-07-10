package org.powertac.trialmodule;

/*14, June 2016*/
/*14 June 2016, written by Saiful Abu
 * Extracts only five features from the log files.
 * 
 * Extracts data from all the customers. and put in this format
 * SimulationData
 * |
 * | --- SimulationFileName
 * | ----------AverageUsage
 * | ----------------customer1
 * | ----------------custumer2
 *      | ----------SlotBasedUsage
 *      | ----------------customer1 ...
 *      
 *      Original file was Clustering_Data_Extractor
 * 
 * */

import java.awt.Toolkit;
import java.io.File;
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
import org.powertac.trialmodule.CustomerUsageStorer.SlotRecord;

import tools.Settings;

/**
 * Logtool Analyzer that reads ClearedTrade instances as they arrive and builds
 * an array for each timeslot giving all the market clearings for that
 * timeslot,s indexed by leadtime. The output data file has one line/timeslot
 * formatted as<br>
 * [mwh price] [mwh price] ...<br>
 * Each line has 24 entries, assuming that each timeslot is open for trading 24
 * times.
 * 
 * Usage: MktPriceStats state-log-filename output-data-filename
 * 
 * @author John Collins
 */
public class ReducedFeatureExtractor extends LogtoolContext implements Analyzer {

static int TEMP_TEST_TIME_SLOT = 0;
int START_FROM_SLOT = 600;
static String[] weekDay = { "mon", "tue", "wed", "thu", "fri", "sat", "sun" };
static String[] hourDay = { "0", "1", "2", "3", "4", "5", "6", "7", "8",
"9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
"20", "21", "22", "23", };

// public static String[] customerName = { "SolarLeasing@1",
// "SunnyhillSolar2", "MedicalCenter@2", };
// CustomerUsageStorer custUsage = new CustomerUsageStorer(customerName[0],
// null);
static String logFileNames[] = { "", "01.powertac-sim-1",
"02.powertac-sim-2", "03.powertac-sim-3", "04.powertac-sim-4",
"05.powertac-sim-5", "06.powertac-sim-6", "07.powertac-sim-7",
"08.powertac-sim-8", "09.powertac-sim-9", "10.powertac-sim-10",
"11.powertac-sim-11", "12.powertac-sim-12", "13.powertac-sim-13",
"14.powertac-sim-14", "15.powertac-sim-15", "16.powertac-sim-16",
"17.powertac-sim-17", "18.powertac-sim-18", "19.powertac-sim-19",
"20.powertac-sim-20", "21.powertac-sim-21", "22.powertac-sim-22",
"23.powertac-sim-23", "24.powertac-sim-24", "25.powertac-sim-25",
"26.powertac-sim-26-my-sim", "27.powertac-sim-27-my-sim",

};
static int fileIndex = 10;
static String logFileName = logFileNames[fileIndex];
static private Logger log = Logger.getLogger(MktPriceStats.class.getName());

// service references
private static TimeslotRepo timeslotRepo;
private TimeService timeService;
private WeatherReportRepo weatherReportRepo;
private OrderbookRepo OrderbookRepo;
private DomainObjectReader dor;
private Timeslot timeslot;
int counter = 0;

private static CustomerRepo customerRepo;

// Data
private TreeMap<Integer, ClearedTrade[]> data;
private TreeMap<Integer, CustomMarketTransaction> marketData;

private int ignoreInitial = 0; // timeslots to ignore at the beginning
private int ignoreCount = 0;
private int indexOffset = 0; // should be
// Competition.deactivateTimeslotsAhead - 1

// private PrintWriter output = null;
private String dataFilename = "clearedTrades.data";
public double brokerID;
private BrokerRepo brokerRepo;

// saif
int totalSlot = 360;
int previousTimeSlot = 0;
int totalTariffTransaction = 0;
Map<Integer, WeatherReport> weatherData;
// only extract from customers of this type
static PowerType[] powerType = { PowerType.CONSUMPTION };
static HashMap<String, CustomerInformationHolder> customerInformationHolder;

// /////////
int MAXIMUM_PREVIOUS_WEEK_OFFSET = 6;

// PowerUsageRepo powerUsageRepo;

/**
 * Main method just creates an instance and passes command-line args to its
 * inherited cli() method.
 */
public static void endProgramExecution() {

// close all file headers
for (String customerName : customerInformationHolder.keySet()) {
customerInformationHolder.get(customerName).output.close();
}// end for
// any additional file writing goes here
// write average file

for (String customerName : customerInformationHolder.keySet()) {
// constitue average usage string
// game#, powerType, customer name
String headerInfo = "\""
+ logFileName
+ "\""
+ ","
+ "\""
+ customerInformationHolder.get(customerName).customerInfo
.getPowerType().toString() + "\"" + "," + "\""
+ customerName + "\"";
String averageUsage = headerInfo;
String zScoreUsage = headerInfo;
for (int slot = 0; slot < 24 * 7; slot++) {
double avgUsageValue = customerInformationHolder
.get(customerName).custUsage.weekSlot[slot].stats
.getMean();
double zScoreValue = customerInformationHolder
.get(customerName).custUsage.getZScore(slot / 24 + 1,
slot % 24);
if (averageUsage.isEmpty()) {
averageUsage += avgUsageValue;
} else {
averageUsage = averageUsage + "," + avgUsageValue;
}

if (zScoreUsage.isEmpty()) {
zScoreUsage += zScoreValue;
} else {
zScoreUsage = zScoreUsage + "," + zScoreValue;
}
}// end for
// write the average usage to file
CustomerInfo customerInfo = customerInformationHolder
.get(customerName).customerInfo;
writeOverAllStats(customerInfo, averageUsage, "avg");
writeOverAllStats(customerInfo, zScoreUsage, "zscr");

}// end for
System.out.println("... Data Extraction finished from " + logFileName);
}// end of method

public static void writeOverAllStats(CustomerInfo customerInfo,
String overAllStatsString, String extension) {
String customerName = customerInfo.getName();

// create file now

String outputFileLocation = Settings.LOG_EXTRACTED_ROOT_FOLDER_NAME
+ "/" + logFileName + "/" + Settings.OVERALL_STATS_FOLDER_NAME
+ "/" + customerInfo.getPowerType().toString() + "/"
+ extension + "/" + customerName + "--game" + fileIndex + "."
+ extension;
try {
File file = new File(outputFileLocation);
if (file.getParentFile().exists() == false) {
file.getParentFile().mkdirs();
}// end if
PrintWriter output = new PrintWriter(new File(outputFileLocation));
output.write(overAllStatsString + "\n");
output.flush();
output.close();
// Files.createFile(Paths.get(outputFileLocation));
} catch (Exception e) {
// TODO Auto-generated catch block
System.out.println("could not create the file "
+ outputFileLocation);
}// end try catch
}// end of method

public static void main(String[] args) {
System.out.println("Data Extracting from log file " + logFileName
+ " ...");
new ReducedFeatureExtractor().cli(args);
endProgramExecution();
Toolkit.getDefaultToolkit().beep();
}// end of main

public static int[] converDayHour(int timeSlot) {

DateTime dt = timeslotRepo.getDateTimeForIndex(timeSlot);
int dayOfWeek = dt.getDayOfWeek();
int hourOfDay = dt.getHourOfDay();
int[] retVal = { dayOfWeek, hourOfDay };
return retVal;
}// end of method

/**
 * Takes two args, input filename and output filename
 * 
 * 
 */
private void cli(String[] args) {

// create folder by name
// SimulationData <SimulationFileName> [AverageUsage SlotBasedUsage]
if (Files.exists(Paths.get(Settings.LOG_EXTRACTED_ROOT_FOLDER_NAME)) == false) {
try {
Files.createDirectory(Paths
.get(Settings.LOG_EXTRACTED_ROOT_FOLDER_NAME));
} catch (IOException e) {
// TODO Auto-generated catch block
System.out.println("Could not create root folder");
e.printStackTrace();
}// end try catch
}// end if

try {
if (Files.exists(Paths.get(Settings.LOG_EXTRACTED_ROOT_FOLDER_NAME
+ "/" + logFileName))) {
FileUtils.forceDelete(new File(
Settings.LOG_EXTRACTED_ROOT_FOLDER_NAME + "/"
+ logFileName));
}
FileUtils
.forceMkdir(new File(
Settings.LOG_EXTRACTED_ROOT_FOLDER_NAME + "/"
+ logFileName));
FileUtils.forceMkdir(new File(
Settings.LOG_EXTRACTED_ROOT_FOLDER_NAME + "/" + logFileName
+ "/" + Settings.OVERALL_STATS_FOLDER_NAME));
FileUtils.forceMkdir(new File(
Settings.LOG_EXTRACTED_ROOT_FOLDER_NAME + "/" + logFileName
+ "/" + Settings.slotBasedFolderName));
// Files.createDirectory(Paths.get(rootFolder + "/" + logFileName));
// Files.createDirectory(Paths.get(rootFolder + "/" + logFileName
// + "/" + avgFolderName));
// Files.createDirectory(Paths.get(rootFolder + "/" + logFileName
// + "/" + slotBasedFolderName));
} catch (IOException e) {
// TODO Auto-generated catch block
System.out.println("Could not create avg files folder");
e.printStackTrace();
}// end try catch

String logFolder = "log";

String logFileExtension = "state";
String logFileLocation = logFolder + "/" + logFileName + "."
+ logFileExtension;
super.cli(logFileLocation, this);
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

// saif
marketData = new TreeMap<Integer, CustomMarketTransaction>();
customerInformationHolder = new HashMap<String, CustomerInformationHolder>();
// ///////////////////
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

}

}

/*********** Handlers Begin ********/
// -----------------------------------
// catch TimeslotUpdate events
class TimeslotUpdateHandler implements NewObjectListener {

@Override
public void handleNewObject(Object thing) {

int currentTimeSlot = timeslotRepo.currentSerialNumber();

// update previous slot
int prevSlotBefore = currentTimeSlot - 1;
int[] dayHour = converDayHour(prevSlotBefore);

int slotOfInterest = prevSlotBefore;// currentTimeSlot - 8; // - 2;
if (slotOfInterest % 100 == 0) {
System.out.println("Processing Time Slot " + slotOfInterest
+ " ...");
}// end if
if (slotOfInterest > START_FROM_SLOT) {
for (String customerName : customerInformationHolder.keySet()) {
processSlot(
slotOfInterest,
customerInformationHolder.get(customerName).custUsage,
customerInformationHolder.get(customerName).output);
}
}// end if
// System.out.println("will write to file for slto " +
// slotOfInterest);

if (prevSlotBefore >= 360) {
// Update record for all the customers
for (String customerName : customerInformationHolder.keySet()) {
customerInformationHolder.get(customerName).custUsage
.updateRecord(dayHour[0], dayHour[1]);
}// end for
}// end if
}// end of method

/*
 * Forms supervised learning example for a timeSlotIndex.
 * [temporalString] [temperature of 5 slots starting from the slot of
 * interest] [previous week's usage 5 features centered at the same slot
 * as the slot of interest] [actual usage]
 */
void processSlot(int timeSlotIndex, CustomerUsageStorer custUsage,
PrintWriter output) {
String instance = "";
DateTime dt = timeslotRepo.getDateTimeForIndex(timeSlotIndex);
String dateTime = "\"";
dateTime += dt.getYear() + "-" + dt.getMonthOfYear() + "-"
+ dt.getDayOfMonth() + " " + dt.getHourOfDay() + "\"";

int dayOfWeek = converDayHour(timeSlotIndex)[0];
int hourOfDay = converDayHour(timeSlotIndex)[1];
int slotOfWeek = (dayOfWeek - 1) * 24 + hourOfDay;
String day = weekDay[dayOfWeek - 1];
String hour = hourDay[hourOfDay];
SlotRecord record = custUsage.getRecord(dayOfWeek, hourOfDay);

double totalUsage = record.completeUsage;

// excluding time related feats
// instance += dateTime + "," + slotOfWeek + "," + day + "," + hour
// + ",";

// weather instance
String weatherString = formWeatherString(weatherData,
timeSlotIndex, 1);
instance += weatherString;

String zScoreString = "";
int totalZScores = 24;
int startSlot = (int) ((dayOfWeek - 1) * 24 + hourOfDay);
for (int i = 0; i < totalZScores; i++) {
int currentSlot = (168 + startSlot - i) % 168;
int dayOfWeekTemp = currentSlot / 24 + 1;
int hourOfDayTemp = currentSlot % 24;
double zScore = custUsage.getZScore(dayOfWeekTemp,
hourOfDayTemp);
zScoreString += zScore + ",";
}// end for
// excluding zscore related feats
// instance += zScoreString;
double meanOfSlot = custUsage.getMean(dayOfWeek, hourOfDay);
double stdDevOfSlot = custUsage.getStdDev(dayOfWeek, hourOfDay);
double overAllMean = custUsage.getOverAllMean();
double overAllStdDev = custUsage.getOverAllStdDev();
double overAllZ = custUsage.overAllZ();
// excluding statis related feats
// instance += meanOfSlot + "," + stdDevOfSlot + "," + overAllMean
// + "," + overAllStdDev + "," + overAllZ + ",";
instance += meanOfSlot + "," + stdDevOfSlot + ",";
double avgUsage = 0;
if (record.completePopulation > 0) {
avgUsage = totalUsage / record.completePopulation;
}// end if
// instance += avgUsage + ",";
instance += avgUsage;
double recentUsageZScore = custUsage.getZScoreRecentUsage(
dayOfWeek, hourOfDay);
// exluding recent z score
// instance += recentUsageZScore;

writeALine(instance, output);
if (timeSlotIndex == 601) {
System.out.println(instance);
}

}// end of method

void writeALine(String string, PrintWriter output) {
output.write(string + "\n");
output.flush();
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
double cloudCover = weatherReport.getCloudCover();
double temperature = weatherReport.getTemperature();
double windspeed = weatherReport.getWindSpeed();
weatherReportString = weatherReportString + cloudCover + ","
+ temperature + "," + windspeed + ",";
}// end for
return weatherReportString;
}// end of method

/*
 * forms a string of the following data: <consumption slot 0>
 * <consumption slot -1> .... <consumption slot - (totalslots-1)>
 */
String formPreviousAvgUsageString(
PowerUsageRepoForOfficeComplex powerUsageRepo,
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

}// end of class

class WeatherReportHandler implements NewObjectListener {

@Override
public void handleNewObject(Object thing) {

WeatherReport wr = (WeatherReport) thing;

// System.out.println("In the weather report handler");

int timeSlotIndex = wr.getTimeslotIndex();
// System.out.println("Putting weather " + timeSlotIndex);
weatherData.put(timeSlotIndex, wr);

}// end of method
}// end of class

class TariffTransactionHandler implements NewObjectListener {

@Override
public void handleNewObject(Object thing) {

TariffTransaction tariffTransaction = (TariffTransaction) thing;
DateTime dt = timeslotRepo.getDateTimeForIndex(tariffTransaction
.getPostedTimeslotIndex());
int dayOfWeek = dt.getDayOfWeek();
int hourOfDay = dt.getHourOfDay();
int month = dt.getMonthOfYear();
int dayOfMonth = dt.getDayOfMonth();

CustomerInfo customerInfo = tariffTransaction.getCustomerInfo();
if (customerInfo == null) {
// System.out.println("customer info is null");
return;
}// end if

// boolean containsValidType = false;
// for (PowerType pt : powerType) {
// if (customerInfo.getPowerType() == pt) {
// containsValidType = true;
// }// end if
// }// end for
// if (containsValidType == false) {
// return;
// }// end if

double usageKwh = Math.abs(tariffTransaction.getKWh());
int userCount = tariffTransaction.getCustomerCount();

if (tariffTransaction.getPostedTimeslotIndex() == 420) {
System.out.println("at 420 slot for customer "
+ tariffTransaction.getCustomerInfo().getName()
+ " population " + tariffTransaction.getCustomerCount()
+ " broker name "
+ tariffTransaction.getBroker().getUsername());
}// end if
String customerName = tariffTransaction.getCustomerInfo().getName();
int reportOfSlot = tariffTransaction.getPostedTimeslotIndex();
if (customerInformationHolder.containsKey(customerName) == false) {
// create file now
String outputFileLocation = Settings.LOG_EXTRACTED_ROOT_FOLDER_NAME
+ "/"
+ logFileName
+ "/"
+ Settings.slotBasedFolderName
+ "/"
+ customerInfo.getPowerType().toString()
+ "/"
+ customerName + "--game" + fileIndex + ".extracted";
try {
File file = new File(outputFileLocation);
if (file.getParentFile().exists() == false) {
file.getParentFile().mkdirs();
}
// Files.createFile(Paths.get(outputFileLocation));
} catch (Exception e) {
// TODO Auto-generated catch block
System.out.println("could not create the file "
+ outputFileLocation);
}
customerInformationHolder.put(customerName,
new CustomerInformationHolder(customerName,
outputFileLocation, customerInfo));
}
CustomerUsageStorer custUsage = customerInformationHolder
.get(customerName).custUsage;
customerInformationHolder.get(customerName).custUsage.addUsage(
dayOfWeek, hourOfDay, month, dayOfMonth, usageKwh,
userCount, reportOfSlot);

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
