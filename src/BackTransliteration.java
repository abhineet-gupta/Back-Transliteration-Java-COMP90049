import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BackTransliteration {   
        //Process entire file or limited records
    private static final Boolean process_entire_file = true; //set to false to process limited records
    private static final Integer proc_limit = 100;  //number of names to process; only valid if above is false
    
    private static final Integer max_row = 26, max_col = 26;    //number of letters in alphabet
    private static Integer[][] matrix = new Integer[max_row][max_col];  //BLOSUM-style matrix to hold scoring matrix
    
        //Path to files
    private static final String train_filepath = "D:\\GDrive\\Docs\\2017\\Code\\Java\\KT_Proj1_BackTransliteration\\data\\train.txt";
    private static final String dict_filepath = "D:\\GDrive\\Docs\\2017\\Code\\Java\\KT_Proj1_BackTransliteration\\data\\names.txt";
    private static final String DELIMITER = "\t";
    
    //Default match and replacement scores
    private static final Integer def_match_score = 0;
    private static final Integer ged_i_cost = 0;
    private static final Integer ged_d_cost = 2;
    private static final Integer def_replace_score = 1;
    private static final Integer useful_replace_score = 0;
    
    
    private static final Integer p_idx = 0;  //Persian name index
    private static final Integer l_idx = 1;  //Latin name index
    
    public static void main(String[] args) {
        
        Integer temp_score_value = 99;
        
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
        
        Integer num_names; 
        if (process_entire_file) {
            num_names = nameList.size();    //how many records to analyse 
        } else {
            num_names = proc_limit;
        }
        Map<String, ArrayList<String>> scoreNamesMap = new HashMap<>(); //to store the results/scores

        Integer temp_score, temp_min;
        String temp_name = "";

        //Initialise scoring matrix - used only for GED
        initialiseScoringMatrix();        
        
        for (int i = 0; i < num_names; i++) {   //for each Persian name
            temp_min = temp_score_value;
            temp_name = nameList.get(i)[p_idx].toLowerCase();
            
            for (int j = 0; j < dictList.size(); j++) {    //find its GED for each dictionary name
                
//                temp_score = calculateLevenshteinDistance(temp_name, dictList.get(j));
                temp_score = calculateGED(temp_name, dictList.get(j));
                
                //if the new GED is less than previous minimum GED for this Persian name...
                if (temp_min > temp_score) {
                    temp_min = temp_score;
                        //...create new list of potential Latin names for that Persian name
                    scoreNamesMap.put(nameList.get(i)[p_idx], new ArrayList<>(Arrays.asList(dictList.get(j))));
                    
                } else if (temp_score == temp_min){
                        //Otherwise, if the score is the same, add current Latin name as a potential name
                    scoreNamesMap.get(nameList.get(i)[p_idx]).add(dictList.get(j));
                }
            }
        }
//        printPredictedNames(nameList, scoreNamesMap);
        
//------------------Analyse----------------------------------------------------
        Integer correct_predicted = 0, total_predicted = 0;
        
        for (int i = 0; i < num_names; i++) {
            if (scoreNamesMap.get(nameList.get(i)[p_idx]).contains(nameList.get(i)[l_idx])){
                correct_predicted++;
            }
            total_predicted += scoreNamesMap.get(nameList.get(i)[p_idx]).size();
        }
        
        System.out.println("\nRecords processed: " + num_names);
        System.out.println("Precision: " + correct_predicted*100/total_predicted + "%");
        System.out.println("Recall: " + correct_predicted*100/num_names + "%");
        System.out.println("Average # predictions/name: " + total_predicted/num_names);
        
//------------------Benchmark program runtime-----------------------------------        
        double endTime = System.currentTimeMillis();
        double time_taken_sec = (endTime-startTime)/1000.0;
        double time_taken_min = time_taken_sec/60.0;
        
        System.out.println("\nDuration: " + 
                            String.format("%.2f", time_taken_sec) + " secs (" +
                            String.format("%.2f", time_taken_min) + " mins)");
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


    private static void initialiseScoringMatrix() {
        for (int row = 0; row < max_row; row++) {
            for (int col = 0; col < max_col; col++) {
                if (row == col) {
                    matrix[row][col] = def_match_score;
                } else {
                    matrix[row][col] = def_replace_score;
                }
            }
        }
        
        //custom replacement scores
        changeScore('a', 'e');
        changeScore('a', 'i');
        changeScore('a', 'o');
        changeScore('a', 'u');
        changeScore('c', 'k');
        changeScore('g', 'j');
        changeScore('p', 'f');
        changeScore('s', 'c');
        changeScore('v', 'w');
        changeScore('v', 'u');
        changeScore('v', 'o');
        changeScore('v', 'w');
        changeScore('o', 'v');
        changeScore('y', 'i');
        changeScore('y', 'e');
        changeScore('z', 's');
    }
    

    private static void changeScore(char c, char d) {
        matrix[Character.getNumericValue(c)-10][Character.getNumericValue(d)-10] = useful_replace_score;
        matrix[Character.getNumericValue(d)-10][Character.getNumericValue(c)-10] = useful_replace_score;
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
            distance[i][0] = i * ged_i_cost;                                                  
        
        for (int j = 1; j <= rhs.length(); j++)                                 
            distance[0][j] = j * ged_d_cost;                                                  
                                                  
        //Calculate each cell based on neighbouring cells
        for (int i = 1; i <= lhs.length(); i++)                                 
            for (int j = 1; j <= rhs.length(); j++)                             
                distance[i][j] = minimum(                                        
                        distance[i - 1][j] + ged_i_cost,                                  
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
                return def_match_score;   //if they are both the same non-[A-Z] characters, it's still a match 
            } else {
                return def_replace_score;   //
            }
        } else if (c2_value < 0 || c2_value > 25) {
            return def_replace_score;
        }        
        return matrix[c1_value][c2_value];
    }

    private static Integer minimum(int a, int b, int c) {                            
        return Math.min(Math.min(a, b), c);                                      
    }
}
