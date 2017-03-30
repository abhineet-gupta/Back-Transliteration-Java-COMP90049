import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GED {   
    public static void main(String[] args) {
        //Path to files
        String train_filepath = "D:\\GDrive\\Docs\\2017\\Code\\Java\\KT_Proj1_BackTransliteration\\data\\train.txt";
        String dict_filepath = "D:\\GDrive\\Docs\\2017\\Code\\Java\\KT_Proj1_BackTransliteration\\data\\names.txt";
        String DELIMITER = "\t";
        
        int p_idx = 0;  //Persian name index
        @SuppressWarnings("unused")
        int l_idx = 1;  //Latin name index
        List<String[]> nameList = new ArrayList<>();
        List<String> dictList = new ArrayList<String>();

        //import training data
        try {
            //Open file as a stream (Java 8 feature)
            Stream<String> trainStream = Files.lines(Paths.get(train_filepath));
                
            //parse each line in the stream to store as Persian and Latin name
            nameList = trainStream.map(line -> line.split(DELIMITER))
                            .collect(Collectors.toList());
            trainStream.close();
            
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Number of training data read: " + nameList.size());
        
        //import dictionary names
        try {
            //Open file as a stream (Java 8 feature)
            Stream<String> dictStream = Files.lines(Paths.get(dict_filepath));
                
            //parse each line
            dictList = dictStream.collect(Collectors.toList());
            dictStream.close();
            
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Number of dict names read: " + dictList.size());
        //-----------------------
//        System.out.println(CalculateGED("crat", "arts"));

        System.out.println(CalculateGED(nameList.get(0)[p_idx].toLowerCase(), dictList.get(1)));

//        for (String[] names : nameList) {
//            System.out.println(CalculateGED(names[p_idx], dict));
//        }
    }

    private static int CalculateGED(String lhs, String rhs) {
        int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];        
        
        int m_cost = 1;
        int i_cost = -1;
        int d_cost = -1;
        int r_cost = -1;
        
        for (int i = 0; i <= lhs.length(); i++)                                 
            distance[i][0] = i * i_cost;                                                  
        
        for (int j = 1; j <= rhs.length(); j++)                                 
            distance[0][j] = j * d_cost;                                                  
                                                                                 
        for (int i = 1; i <= lhs.length(); i++)                                 
            for (int j = 1; j <= rhs.length(); j++)                             
                distance[i][j] = maximum(                                        
                        distance[i - 1][j] + i_cost,                                  
                        distance[i][j - 1] + d_cost,                                  
                        distance[i - 1][j - 1] + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? m_cost : r_cost));
                                                                                 
        return distance[lhs.length()][rhs.length()];
    }
    
    private static int maximum(int a, int b, int c) {                            
        return Math.max(Math.max(a, b), c);                                      
    }
}
