import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.language.Soundex;

public class BackTransliteration {   
//-----Configurable parameters-------
    //Process entire file or limited records
    private static final Boolean process_entire_file = true; //set to false to process limited records
    private static Integer proc_limit = 10;  //number of names to process; only valid if above is false
    
    //Default match and replacement scores - placed here as it makes for easier manipulation than if inside method
    private static final Integer ged_m_cost = -1;
    private static final Integer ged_i_cost = 2, ged_d_cost = 1, ged_r_cost = 1, ged_modr_cost = 0, ged_modi_cost = 1;

    //filepath variables
    private static String train_filepath;
    private static String dict_filepath;
    private static final String DELIMITER = "\t";
    private static String OUTPUT_FILE;
    private static String method;

//----------------------------------
    //how many lines to skip if sub-sampling the dataset for processing; this is automatically calculated based on "proc_limit" above.
    private static Integer scan_gap = 1;
    
    private static final Integer max_row = 26, max_col = 26;    //number of letters in alphabet
    private static Integer[][] ged_replace_score_matrix = new Integer[max_row][max_col];  //BLOSUM-style matrix to hold scoring matrix
    private static Integer[] ged_insert_score_matrix = new Integer[max_row];
            
    private static final Integer p_idx = 0;  //Persian name index
    private static final Integer l_idx = 1;  //Latin name index
    private static Integer max_ged_score = 99;  //initialisation of GED score
    private static Integer min_sx_score = -1;   //initialisation of Soundex score
    
    public static void main(String[] args) {
        
        train_filepath = args[0];
        dict_filepath = args[1];
        OUTPUT_FILE = args[2];
        
        List<String[]> nameList = new ArrayList<>();    //to store data from train.txt
        List<String> dictList = new ArrayList<>();      //to store data from names.txt
        
//----------------------Import Data---------------------------------------------
        nameList = importTrainData();
        dictList = importDictData();
        System.out.println("Number of training data read: " + nameList.size());
        System.out.println("Number of dict names read: " + dictList.size());
        
//-------------------Process data-----------------------------------------------
            //for benchmarking time performance
        System.out.println("Processing...");
        Long startTime = System.currentTimeMillis();
        
        Integer num_names = nameList.size(); 
        if (!process_entire_file) {
            scan_gap = nameList.size()/proc_limit;  //skip lines to spread out processing over entire file 
        } else {
            proc_limit = num_names;
        }

        Map<String, ArrayList<String>> scoreNamesMap = new HashMap<>(); //to store min Edit Distance results/scores
        Map<String, ArrayList<String>> sxScoreNamesMap = new HashMap<>(); //to store max Soundex results/scores (0-4)
        
        Integer temp_score, temp_min; 
        Integer temp_sx_score, temp_sx_max;
        String temp_name, temp_name_lower, temp_guess, temp_dict_name;
        Soundex sx = new Soundex();
        
        //Initialise scoring matrix - used only for GED
        initialiseReplacementScoringMatrix();  
        initialiseInsertionScoringMatrix();
        
        for (int i = 0; i < num_names; i+= scan_gap) {   //for each Persian name
            temp_min = max_ged_score;
            temp_sx_max = min_sx_score;
            
            temp_name = nameList.get(i)[p_idx];
            temp_name_lower = temp_name.toLowerCase();
            
            for (int j = 0; j < dictList.size(); j++) {    //for each dictionary name
                temp_dict_name = dictList.get(j);
                
////------------GED------------------
//                temp_score = calculateLevenshteinDistance(temp_name, temp_dict_name);
//                method = "GED (Levenshtein)";
                        
                temp_score = calculateGED(temp_name, temp_dict_name);
                method = "GED (Mod-r + Mod-i)";
                
                //if the new GED is less than previous minimum GED for this Persian name...
                if ((temp_score < temp_min) &&
                (temp_name.length() <= temp_dict_name.length())){
                    temp_min = temp_score;
                        //...create new list of potential Latin names for that Persian name
                    scoreNamesMap.put(temp_name, new ArrayList<>(Arrays.asList(temp_dict_name)));
                    
                } else if ((temp_score == temp_min) &&
                        (temp_name.length() <= temp_dict_name.length())){
                        //Otherwise, if the score is the same, add current Latin name as a potential name
                    scoreNamesMap.get(temp_name).add(temp_dict_name);
                }
//---------------------------------
                
//------------SOUNDEX--------------
//                try {
//                    temp_sx_score = sx.difference(temp_name_lower, temp_dict_name);
//                    method = "Soundex";
//                
//                    if ((temp_sx_max < temp_sx_score) &&
//                            (temp_name.length() <= temp_dict_name.length())){//if the new Soundex is more than previous max Soundex value for this Persian name...
//                        temp_sx_max = temp_sx_score;
//                            //...create new list of potential Latin names for that Persian name
//                        sxScoreNamesMap.put(temp_name, new ArrayList<>(Arrays.asList(temp_dict_name)));
//                        
//                    } else if ((temp_sx_score == temp_sx_max) 
//                            && temp_name.length() <= temp_dict_name.length()){  //exclude all names shorter than current Persian name
//                            //Otherwise, if the score is the same, add current Latin name as a potential name
//                        sxScoreNamesMap.get(temp_name).add(temp_dict_name);
//                    }
//                } catch (EncoderException e) {
//                    e.printStackTrace();
//                }
//---------------------------------
            }
            
//------Implement GED on Soundex results
//            temp_min = max_ged_score;
//            method = "Soundex + GED (custom)";
//            
//            for (int k = 0; k < sxScoreNamesMap.get(temp_name).size(); k++) {
//                temp_guess = sxScoreNamesMap.get(temp_name).get(k);
//                
//                temp_score = calculateGED(temp_name_lower, temp_guess);
//              
//              //if the new GED is less than previous minimum GED for this Persian name...
//                if (temp_min > temp_score) {
//                    temp_min = temp_score;
//                          //...create new list of potential Latin names for that Persian name
//                    scoreNamesMap.put(temp_name, new ArrayList<>(Arrays.asList(temp_guess)));
//                      
//                } else if (temp_score == temp_min){
//                    //Otherwise, if the score is the same, add current Latin name as a potential name
//                    scoreNamesMap.get(temp_name).add(temp_guess);
//                }
//            }
//---------------------------------

//------Implement Soundex on GED results
            temp_sx_max = min_sx_score;
            method = "GED (custom) + Soundex";
            
            for (int k = 0; k < scoreNamesMap.get(temp_name).size(); k++) {
                temp_guess = scoreNamesMap.get(temp_name).get(k);
                
                try {
                    temp_sx_score = sx.difference(temp_name_lower, temp_guess);
                    
                    if (temp_sx_max < temp_sx_score) {
                        temp_sx_max = temp_sx_score;
                              //...create new list of potential Latin names for that Persian name
                        sxScoreNamesMap.put(temp_name, new ArrayList<>(Arrays.asList(temp_guess)));
                          
                    } else if (temp_sx_score == temp_sx_max){
                        //Otherwise, if the score is the same, add current Latin name as a potential name
                        sxScoreNamesMap.get(temp_name).add(temp_guess);
                    }
                } catch (EncoderException e) {
                    e.printStackTrace();
                }                 
            }
//---------------------------------
        
        }
//        printPredictedNames(nameList, scoreNamesMap);
        
//------------------Analyse----------------------------------------------------
        Integer correct_predicted = 0, total_predicted = 0;
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_FILE))){
//            String outputLine = method + "\r\n\r\n";
            String outputLine = "Persian,Latin\n";
            bw.write(outputLine);
            
            List<String> tempPredictArray = new ArrayList<>();
            
            for (int i = 0; i < num_names; i+= scan_gap) {
//                outputLine = nameList.get(i)[p_idx].toUpperCase() + "\t" + nameList.get(i)[l_idx] + "\r\n" +
//                        "\t" + sxScoreNamesMap.get(nameList.get(i)[p_idx]).toString() + "\r\n";
                
                outputLine = nameList.get(i)[p_idx].toUpperCase() + ",";                
                tempPredictArray = sxScoreNamesMap.get(nameList.get(i)[p_idx]);
                
                for (int j = 0; j < tempPredictArray.size(); j++) {
                    outputLine += tempPredictArray.get(j);
                    if (j != tempPredictArray.size()-1){
                        outputLine += " ";
                    }
                }
                
                outputLine += "\n";
                
                // write predictions to file only if sub-sampling the dataset - else file is too large!
//                if (!process_entire_file) {
                    bw.write(outputLine);
//                }
                
//                if (sxScoreNamesMap.get(nameList.get(i)[p_idx]).contains(nameList.get(i)[l_idx])){
//                    correct_predicted++;
//                }
                total_predicted += sxScoreNamesMap.get(nameList.get(i)[p_idx]).size();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("\n{m, i, d, r, modr, modi} = {" 
                + ged_m_cost + ", " + ged_i_cost + ", " + ged_d_cost + ", " + ged_r_cost + ", " 
                + ged_modr_cost + ", " + ged_modi_cost + "}");

        System.out.println("Records processed: " + proc_limit);
        System.out.println("Method used: " + method);
        System.out.println("\nPrecision: " + correct_predicted*100/total_predicted + "%");
        System.out.println("Recall: " + correct_predicted*100/proc_limit + "%");
        System.out.println("Average # predictions/name: " + total_predicted/proc_limit);
        System.out.println();
        
//------------------Benchmark program runtime-----------------------------------        
        double endTime = System.currentTimeMillis();
        double time_taken_sec = (endTime-startTime)/1000.0;
        double time_taken_min = time_taken_sec/60.0;
        
        System.out.println("\nDuration: " + 
                            String.format("%.2f", time_taken_sec) + " secs (" +
                            String.format("%.2f", time_taken_min) + " mins)");
    }

    private static void initialiseInsertionScoringMatrix() {
        for (int row = 0; row < max_row; row++) {
            ged_insert_score_matrix[row] = ged_i_cost;
        }
        ged_insert_score_matrix[Character.getNumericValue('a')-10] = ged_modi_cost;
        ged_insert_score_matrix[Character.getNumericValue('e')-10] = ged_modi_cost;
        ged_insert_score_matrix[Character.getNumericValue('i')-10] = ged_modi_cost;
        ged_insert_score_matrix[Character.getNumericValue('o')-10] = ged_modi_cost;
        ged_insert_score_matrix[Character.getNumericValue('u')-10] = ged_modi_cost;
        
        ged_insert_score_matrix[Character.getNumericValue('h')-10] = ged_modi_cost;
        ged_insert_score_matrix[Character.getNumericValue('c')-10] = ged_modi_cost;
        ged_insert_score_matrix[Character.getNumericValue('d')-10] = ged_modi_cost;
        ged_insert_score_matrix[Character.getNumericValue('t')-10] = ged_modi_cost;
    }

    private static void printPredictedNames(List<String[]> nameList, Map<String, ArrayList<String>> scoreNamesMap) {
        for (int i = 0; i < proc_limit; i++) {
          //Print out current Persian name and its best suited Latin names
            System.out.print(nameList.get(i)[p_idx]);
            for (String bestName : scoreNamesMap.get(nameList.get(i)[p_idx])) {
                System.out.print("\t" + bestName);
            }
            System.out.println();
        }
    }

    private static List<String> importDictData() {
        List<String> dictList = new ArrayList<>();
        //import dictionary names
        try {
                //Open file as a stream
            Stream<String> dictStream = Files.lines(Paths.get(dict_filepath));
                //parse each line
            dictList = dictStream.collect(Collectors.toList());
            dictStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }        
        return dictList;
    }

    private static List<String[]> importTrainData() {
        List<String[]> nameList = new ArrayList<String[]>();
        
        try {
            //Open file as a stream
            Stream<String> trainStream = Files.lines(Paths.get(train_filepath));
                //parse each line in the stream to store as Persian and Latin name
            nameList = trainStream.map(line -> line.split(DELIMITER))
                    .collect(Collectors.toList());
            trainStream.close();
        } catch (Exception e) {
            e.printStackTrace();;
        }
        return nameList;
    }

    private static void initialiseReplacementScoringMatrix() {
        for (int row = 0; row < max_row; row++) {
            for (int col = 0; col < max_col; col++) {
                if (row == col) {
                    ged_replace_score_matrix[row][col] = ged_m_cost;
                } else {
                    ged_replace_score_matrix[row][col] = ged_r_cost;
                }
            }
        }

        //custom replacement scores
        changeScore('a', 'e');
        changeScore('a', 'i');
        changeScore('a', 'o');
        changeScore('a', 'u');
        changeScore('c', 'k');
        changeScore('c', 's');
        changeScore('f', 'p');
        changeScore('g', 'j');
        changeScore('h', 'j');
        changeScore('k', 'x');
        changeScore('v', 'u');
        changeScore('v', 'o');
        changeScore('v', 'w');
        changeScore('y', 'i');
        changeScore('y', 'e');
        changeScore('z', 's');
    }
    
    private static void changeScore(char c, char d) {
        ged_replace_score_matrix[Character.getNumericValue(c)-10][Character.getNumericValue(d)-10] = ged_modr_cost;
        ged_replace_score_matrix[Character.getNumericValue(d)-10][Character.getNumericValue(c)-10] = ged_modr_cost;
    }
    
    /**
     * @param lhs: word to compare
     * @param rhs: word to compare against
     * @return Global Edit Distance
     */
    private static int calculateLevenshteinDistance(String lhs, String rhs) {
        int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];        
        
        //Score matrix for Levenshtein Distance
        int m_cost = 0;
        int i_cost = 1;
        int d_cost = 1;
        int r_cost = 1;
         
        //Initialise first row and column of table 
        for (int i = 0; i <= lhs.length(); i++)                                 
            distance[i][0] = i * i_cost;                                                  
        
        for (int j = 1; j <= rhs.length(); j++)                                 
            distance[0][j] = j * d_cost;                                                  
                                                  
        //Calculate each cell based on neighbouring cells
        for (int i = 1; i <= lhs.length(); i++)                                 
            for (int j = 1; j <= rhs.length(); j++)                             
                distance[i][j] = minimum(                                        
                        distance[i - 1][j] + i_cost,                                  
                        distance[i][j - 1] + d_cost,                                  
                        distance[i - 1][j - 1] + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? m_cost : r_cost));
                                                                                 
        return distance[lhs.length()][rhs.length()];
    }

    private static Integer calculateGED(String lhs, String rhs) {
        int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];        
        
        //Initialise first row and column of table 
        for (int i = 0; i <= lhs.length(); i++)                                 
            distance[i][0] = i * ged_d_cost;                                                  
        
        for (int j = 1; j <= rhs.length(); j++)                                 
            distance[0][j] = j * ged_i_cost;                                                  
                                                  
        //Calculate each cell based on neighbouring cells
        for (int i = 1; i <= lhs.length(); i++)                                 
            for (int j = 1; j <= rhs.length(); j++)                             
                distance[i][j] = minimum(                                        
                        distance[i - 1][j] + ged_insert_score_matrix[Character.getNumericValue(rhs.charAt(j-1))-10],                                  
                        distance[i][j - 1] + ged_d_cost,                                  
                        distance[i - 1][j - 1] + matchReplaceCost(lhs.charAt(i - 1), rhs.charAt(j - 1)));
                                                                                 
        return distance[lhs.length()][rhs.length()];
    }
    
    /**
     * @param c1: character 1 to be matched
     * @param c2: character 2 to be matched with
     * @return value of their match/replace score
     */
    private static Integer matchReplaceCost(char c1, char c2) {
        int c1_value = Character.getNumericValue(c1) - 10;
        int c2_value = Character.getNumericValue(c2) - 10;
        
            //Check if characters are outside A-Z; return 0 if so.
        if (c1_value < 0 || c1_value > 25) {
            if (c1 == c2) {
                return ged_m_cost;   //if they are both the same non-[A-Z] characters, it's still a match 
            } else {
                return ged_r_cost;   //
            }
        } else if (c2_value < 0 || c2_value > 25) {
            return ged_r_cost;
        }        
        return ged_replace_score_matrix[c1_value][c2_value];
    }

    private static Integer minimum(int a, int b, int c) {                            
        return Math.min(Math.min(a, b), c);                                      
    }
}