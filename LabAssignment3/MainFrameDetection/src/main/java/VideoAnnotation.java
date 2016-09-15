import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.Tag;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Manikanta on 9/14/2016.
 */
public class VideoAnnotation {

    private static String APP_ID = "kCvJ830Yx8N8A6DnwptYoqsJP9thyBZZf_4_A4KI";
    private static String APP_SECRET = "l-hCCwIL4Iq1JqbAiPPVtuISrim8ZnsHnlh5KhD7";
    public static void main(String args[]){
        VideoAnnotation t = new VideoAnnotation();
        String Path= "output/frames/new100.jpg";
        t.getTags(Path);
    }
    public static void getTags(String Path){
        ClarifaiClient clarifai = new ClarifaiClient(APP_ID, APP_SECRET);
        List<RecognitionResult> results = clarifai.recognize(new RecognitionRequest(new File(Path)));

        /*
        for(int i = 0; i < results.size(); i++) {
            System.out.println("results:");
            System.out.println(results.get(i).getTags());
        }
        */

        for(Tag tag : results.get(0).getTags()) {

            //System.out.println(tag.getName() + ": " + tag.getProbability());
            System.out.print(tag.getName() + ", ");
        }
    }


}
