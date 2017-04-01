import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BackTransliteration {   
    public static void main(String[] args) {
        //Path to files
        String train_filepath = "D:\\GDrive\\Docs\\2017\\Code\\Java\\KT_Proj1_BackTransliteration\\data\\train.txt";
        String dict_filepath = "D:\\GDrive\\Docs\\2017\\Code\\Java\\KT_Proj1_BackTransliteration\\data\\names.txt";
        String DELIMITER = "\t";
        
        int p_idx = 0;  //Persian name index
        int l_idx = 1;  //Latin name index
        Integer num_names;  //number of names to process; can be used to limit processing during testing
        
        List<String[]> nameList = new ArrayList<>();    //to store data from train.txt
        List<String> dictList = new ArrayList<>();      //to store data from names.txt 
//------------------------------------------------------------------------------
        //import training data
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
        System.out.println("Number of training data read: " + nameList.size());
        
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
        System.out.println("Number of dict names read: " + dictList.size());
//------------------------------------------------------------------------------
            //for benchmarking time performance
        System.out.println("Processing...");
        long startTime = System.currentTimeMillis();
        
        num_names = nameList.size();    //how many records to analyse
        Map<String, ArrayList<String>> scoreNamesMap = new HashMap<>(); //to store the results/scores
        int temp_score, temp_min = 99;
        String temp_name;
        
        for (int i = 0; i < num_names; i++) {   //for each Persian name
            temp_min = 99;
            temp_name = nameList.get(i)[p_idx].toLowerCase();
            
            for (int j = 0; j < dictList.size(); j++) {    //find its GED for each dictionary name
                temp_score = calculateLevenshteinDistance(temp_name, dictList.get(j));
                
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
//                //Print out current Persian name and its best suited Latin names
//            System.out.print(nameList.get(i)[p_idx]);
//            for (String bestName : scoreNamesMap.get(nameList.get(i)[p_idx])) {
//                System.out.print("\t" + bestName);
//            }
//            System.out.println();
        }
//------------------------------------------------------------------------------
        //Analysis
        int correct_predicted = 0;
        int total_predicted = 0;
        
        for (int i = 0; i < num_names; i++) {            
            if (scoreNamesMap.get(nameList.get(i)[p_idx]).contains(nameList.get(i)[l_idx])){
                correct_predicted++;
            }
            total_predicted += scoreNamesMap.get(nameList.get(i)[p_idx]).size();
        }
        System.out.println("\nPrecision: " + correct_predicted*100/total_predicted + "%");
        System.out.println("Recall: " + correct_predicted*100/nameList.size() + "%");
        System.out.println("Average number of predictions per name: " + total_predicted/nameList.size());
//------------------------------------------------------------------------------        
        
        //end benchmark timing
        long endTime = System.currentTimeMillis();
        System.out.println("\nCalculations took " + (endTime - startTime)/1000.0/60.0 + " min.");
    }

    private static int calculateLevenshteinDistance(String lhs, String rhs) {
        int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];        
        
    //Score matrix for Levenshtein Distance
        int m_cost = 0;
        int i_cost = 1;
        int d_cost = 1;
        int r_cost = 1;
        
        for (int i = 0; i <= lhs.length(); i++)                                 
            distance[i][0] = i * i_cost;                                                  
        
        for (int j = 1; j <= rhs.length(); j++)                                 
            distance[0][j] = j * d_cost;                                                  
                                                                                 
        for (int i = 1; i <= lhs.length(); i++)                                 
            for (int j = 1; j <= rhs.length(); j++)                             
                distance[i][j] = minimum(                                        
                        distance[i - 1][j] + i_cost,                                  
                        distance[i][j - 1] + d_cost,                                  
                        distance[i - 1][j - 1] + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? m_cost : r_cost));
                                                                                 
        return distance[lhs.length()][rhs.length()];
    }
    
    private static int minimum(int a, int b, int c) {                            
        return Math.min(Math.min(a, b), c);                                      
    }
}
