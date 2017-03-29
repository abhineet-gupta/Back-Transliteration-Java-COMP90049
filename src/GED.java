import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GED {
    
    public static void main(String[] args) {
        
        //Path to the training data
        String FILEPATH = "D:\\GDrive\\Docs\\2017\\Code\\Java\\KT_Proj1_BackTransliteration\\data\\train.txt";
        String DELIMITER = "\t";
        
        int p_idx = 0;  //persian name index
        int l_idx = 1;  //latin name index
        
        List<String[]> nameList = new ArrayList<>();
        
        try {
            //Open file as a stream (Java 8 feature)
            Stream<String> stream = Files.lines(Paths.get(FILEPATH));
                
            //parse each line in the stream to store as Persian and Latin name
            nameList = stream.map(line -> line.split(DELIMITER))
                            .collect(Collectors.toList());
            stream.close();
            
        } catch (Exception e) {
            System.out.println(e);
        }
        
        System.out.println("Number of names read: " + nameList.size());
    }
}
