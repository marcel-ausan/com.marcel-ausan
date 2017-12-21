package algosummary;

import com.ullink.ulbridge2.ULMessage;
import com.ullink.ultools.tags.Tags;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlgoReport {
    private static Map<String, Object> ApamaHKD = new HashMap<String, Object>();
    private static Map<String, Object> ApamaCNY = new HashMap<String, Object>();
    private static Map<String, Object> MarvelHKD = new HashMap<String, Object>();
    private static Map<String, Object> MarvelCNY = new HashMap<String, Object>();
    private static Map<String, Object> TradebookHKD = new HashMap<String, Object>();
    private static Map<String, Object> TradebookCNY = new HashMap<String, Object>();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please provide the logs folder as argument.");
            return;
        }

        try (FileWriter fw = new FileWriter(new File("AlgoSummary.csv")); BufferedWriter out = new BufferedWriter(fw);) {
            out.write("Transaction Date,Algo System,Number of Parent Orders,Number of Child Orders,Number of Executions,Currency,Total Turnover");
            out.newLine();

            String inputFolderName = args[0];
            for (File f : listFilesForFolder(new File(inputFolderName))) {
                System.out.println("Reading file " + f.getName());
                initializeContainer(ApamaHKD);
                initializeContainer(ApamaCNY);
                initializeContainer(MarvelHKD);
                initializeContainer(MarvelCNY);
                initializeContainer(TradebookHKD);
                initializeContainer(TradebookCNY);
                extractData(f);
                writeDailyData(out, f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Done");
    }

    private static void writeDailyData(BufferedWriter out, File f) throws IOException {
        String date = f.getName().split("\\.")[0];
        date = date.substring(0, 4) + "/" + Integer.valueOf(date.substring(4, 6)) + "/" + Integer.valueOf(date.substring(6));
        if ((((int) ApamaHKD.get("parents")) + ((int) ApamaHKD.get("children")) + ((int) ApamaHKD.get("executions")) + (int) ((double) ApamaHKD.get("turnover"))) != 0) {
            out.write(reportLine(date, ApamaHKD, "Apama", "HKD"));
            out.newLine();
        }
        if ((((int) ApamaCNY.get("parents")) + ((int) ApamaCNY.get("children")) + ((int) ApamaCNY.get("executions")) + (int) ((double) ApamaCNY.get("turnover"))) != 0) {
            out.write(reportLine(date, ApamaCNY, "Apama", "CNY"));
            out.newLine();
        }
        if ((((int) MarvelHKD.get("parents")) + ((int) MarvelHKD.get("children")) + ((int) MarvelHKD.get("executions")) + (int) ((double) MarvelHKD.get("turnover"))) != 0) {
            out.write(reportLine(date, MarvelHKD, "MarvelSoft", "HKD"));
            out.newLine();
        }
        if ((((int) MarvelCNY.get("parents")) + ((int) MarvelCNY.get("children")) + ((int) MarvelCNY.get("executions")) + (int) ((double) MarvelCNY.get("turnover"))) != 0) {
            out.write(reportLine(date, MarvelCNY, "MarvelSoft", "CNY"));
            out.newLine();
        }
        if ((((int) TradebookHKD.get("parents")) + ((int) TradebookHKD.get("children")) + ((int) TradebookHKD.get("executions")) + (int) ((double) TradebookHKD.get("turnover"))) != 0) {
            out.write(reportLine(date, TradebookHKD, "Tradebook", "HKD"));
            out.newLine();
        }
        if ((((int) TradebookCNY.get("parents")) + ((int) TradebookCNY.get("children")) + ((int) TradebookCNY.get("executions")) + (int) ((double) TradebookCNY.get("turnover"))) != 0) {
            out.write(reportLine(date, TradebookCNY, "Tradebook", "CNY"));
            out.newLine();
        }
    }

    private static String reportLine(String date, Map<String, Object> algoMap, String algoEngine, String currency) {
        DecimalFormat formatter = new DecimalFormat("#0.00");
        return date + ',' + algoEngine + ',' + (int) algoMap.get("parents") + ',' + (int) algoMap.get("children") + ',' + (int) algoMap.get("executions") + ',' + currency + ',' + formatter.format((double) algoMap.get("turnover"));
    }

    private static void extractData(File f) {
        try (BufferedReader in = getBufferedReaderForCompressedFile(f.getAbsolutePath())) {
            String line = "";
            while ((line = in.readLine()) != null) {
                parseLine(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initializeContainer(Map<String, Object> map) {
        map.put("parents", 0);
        map.put("children", 0);
        map.put("executions", 0);
        map.put("turnover", 0d);
    }

    private static void parseLine(String line) throws Exception {
        int ulmIndex = line.indexOf("PushMessage");
        if (ulmIndex > -1) {
            ULMessage ulm = ULMessage.valueOf(line.substring(ulmIndex + 14));
            if ("new".equals(ulm.getString(Tags.MSGTYPE))) {
                /*
                 * Parent order if going to outbound algo plugin
                 */
                if ("O_APAMA".equals(ulm.getString(Tags.ULTOSESSIONNAME))) {
                    if ("HKD".equals(ulm.getString(Tags.CURRENCY))) processParent(ulm, ApamaHKD);
                    else if ("CNY".equals(ulm.getString(Tags.CURRENCY))) processParent(ulm, ApamaCNY);
                    else throw new Exception("Currency not identified");
                } else if ("O_Bloomberg_Tradebook_FIX42".equals(ulm.getString(Tags.ULTOSESSIONNAME))) {
                    if ("HKD".equals(ulm.getString(Tags.CURRENCY))) processParent(ulm, TradebookHKD);
                    else if ("CNY".equals(ulm.getString(Tags.CURRENCY))) processParent(ulm, TradebookCNY);
                    else throw new Exception("Currency not identified");
                } else if ("O_Marvel_FIX44".equals(ulm.getString(Tags.ULTOSESSIONNAME))) {
                    if ("HKD".equals(ulm.getString(Tags.CURRENCY))) processParent(ulm, MarvelHKD);
                    else if ("CNY".equals(ulm.getString(Tags.CURRENCY))) processParent(ulm, MarvelCNY);
                    else throw new Exception("Currency not identified");
                    /*
                     * Child order if coming from inbound algo plugin
                     */
                } else if ("I_APAMA".equals(ulm.getString(Tags.ULFROMSESSIONNAME))) {
                    if ("HKD".equals(ulm.getString(Tags.CURRENCY))) processChild(ulm, ApamaHKD);
                    else if ("CNY".equals(ulm.getString(Tags.CURRENCY))) processChild(ulm, ApamaCNY);
                    else throw new Exception("Currency not identified");
                } else if ("I_Bloomberg_Tradebook_FIX42".equals(ulm.getString(Tags.ULFROMSESSIONNAME))) {
                    if ("HKD".equals(ulm.getString(Tags.CURRENCY))) processChild(ulm, TradebookHKD);
                    else if ("CNY".equals(ulm.getString(Tags.CURRENCY))) processChild(ulm, TradebookCNY);
                    else throw new Exception("Currency not identified");
                } else if ("I_Marvel_FIX44".equals(ulm.getString(Tags.ULFROMSESSIONNAME))) {
                    if ("HKD".equals(ulm.getString(Tags.CURRENCY))) processChild(ulm, MarvelHKD);
                    else if ("CNY".equals(ulm.getString(Tags.CURRENCY))) processChild(ulm, MarvelCNY);
                    else throw new Exception("Currency not identified");
                }
            } else if ("executionreport".equals(ulm.getString(Tags.MSGTYPE)) && "trade".equals(ulm.getString(Tags.EXECTYPE))) {
                /*
                 * Only capturing executions on the children, to avoid double counts
                 */
                if ("I_APAMA".equals(ulm.getString(Tags.ULTOSESSIONNAME))) {
                    if ("HKD".equals(ulm.getString(Tags.CURRENCY))) processExecution(ulm, ApamaHKD);
                    else if ("CNY".equals(ulm.getString(Tags.CURRENCY))) processExecution(ulm, ApamaCNY);
                    else throw new Exception("Currency not identified");
                } else if ("I_Bloomberg_Tradebook_FIX42".equals(ulm.getString(Tags.ULTOSESSIONNAME))) {
                    if ("HKD".equals(ulm.getString(Tags.CURRENCY))) processExecution(ulm, TradebookHKD);
                    else if ("CNY".equals(ulm.getString(Tags.CURRENCY))) processExecution(ulm, TradebookCNY);
                    else throw new Exception("Currency not identified");
                } else if ("I_Marvel_FIX44".equals(ulm.getString(Tags.ULTOSESSIONNAME))) {
                    if ("HKD".equals(ulm.getString(Tags.CURRENCY))) processExecution(ulm, MarvelHKD);
                    else if ("CNY".equals(ulm.getString(Tags.CURRENCY))) processExecution(ulm, MarvelCNY);
                    else throw new Exception("Currency not identified");
                }
            }
        }
    }

    private static void processExecution(ULMessage ulm, Map<String, Object> algoMap) {
        if (!ulm.exist("iscopy")) {
            algoMap.put("executions", ((int) algoMap.get("executions")) + 1);
            algoMap.put("turnover", ((double) algoMap.get("turnover") + ulm.getInt(Tags.LASTSHARES) * ulm.getDouble(Tags.LASTPX)));
        }
    }

    private static void processChild(ULMessage ulm, Map<String, Object> algoMap) {
        if (!ulm.exist("iscopy")) algoMap.put("children", ((int) algoMap.get("children")) + 1);
    }

    private static void processParent(ULMessage ulm, Map<String, Object> algoMap) {
        if (!ulm.exist("iscopy")) algoMap.put("parents", ((int) algoMap.get("parents")) + 1);
    }

    public static BufferedReader getBufferedReaderForCompressedFile(String fileIn) throws FileNotFoundException, CompressorException {
        FileInputStream fin = new FileInputStream(fileIn);
        BufferedInputStream bis = new BufferedInputStream(fin);
        CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
        return new BufferedReader(new InputStreamReader(input));
    }

    public static List<File> listFilesForFolder(File folder) {
        List<File> result = new ArrayList<File>();
        for (File fileEntry : folder.listFiles()) {
            result.add(fileEntry);
        }
        return result;
    }
}
