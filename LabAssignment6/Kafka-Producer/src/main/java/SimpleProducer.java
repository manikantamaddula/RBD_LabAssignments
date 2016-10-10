import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.migcomponents.migbase64.Base64;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.FastBasicKeypointMatcher;
import org.openimaj.feature.local.matcher.LocalFeatureMatcher;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.math.geometry.transforms.estimation.RobustAffineTransformEstimator;
import org.openimaj.math.model.fit.RANSAC;
import org.openimaj.video.Video;
import org.openimaj.video.xuggle.XuggleVideo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class SimpleProducer {
    private static Producer<Integer, String> producer;
    private final Properties properties = new Properties();
    static Video<MBFImage> Newvideo;
    static List<MBFImage> imageList = new ArrayList<MBFImage>();
    static List<Long> timeStamp = new ArrayList<Long>();
    static List<Double> mainPoints = new ArrayList<Double>();
    static Double x;

    public SimpleProducer() {
        properties.put("metadata.broker.list", "localhost:9092");
        properties.put("serializer.class", "kafka.serializer.StringEncoder");
        properties.put("request.required.acks", "1");
        properties.put("message.max.bytes", "10000000");
        producer = new Producer<Integer, String>(new ProducerConfig(properties));
    }


    public static String MetaDataVideo(String file){
        String encodedString = null;
        InputStream inputStream = null, inputStream2 = null;
        int i = 0, j = 1;
        byte[] bytes;
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();



        //For Assignment6
        try {
            Newvideo = new XuggleVideo(new File(file));

            for (MBFImage mbfImage : Newvideo) {
                BufferedImage bufferedFrame = ImageUtilities.createBufferedImageForDisplay(mbfImage);
                i++;
                String name = "output/frames/"+"frame"    + i + ".jpg";
                File outputFile = new File(name);
                try {

                    ImageIO.write(bufferedFrame, "jpg", outputFile);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                MBFImage b = mbfImage.clone();
                imageList.add(b);
                timeStamp.add(Newvideo.getTimeStamp());
                x=Newvideo.getFPS();
            }

            System.out.println("Video fps: "+x);
            System.out.println("No. of frames in the Video: "+imageList.size());
            System.out.println("Video length: "+(timeStamp.get(timeStamp.size()-1)-timeStamp.get(0))+"ms");

            for (int a = 0; a < 10; a++) {
                String imgName = "output/frames/" + "frame" + j + ".jpg";
                System.out.println(imgName);
                BufferedImage bufferedFrame = ImageUtilities.createBufferedImageForDisplay(imageList.get(a + 1));
                j++;
                inputStream2 = new FileInputStream(imgName);
                while ((bytesRead = inputStream2.read(buffer)) != -1) {
                    //output.write(buffer, 0, bytesRead);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        try{
            System.out.println("Inside Try catch");
            for (int l=0; l<imageList.size() - 1; l++)
            {
                MBFImage image1 = imageList.get(l);
                MBFImage image2 = imageList.get(l+1);
                DoGSIFTEngine engine = new DoGSIFTEngine();
                LocalFeatureList<Keypoint> queryKeypoints = engine.findFeatures(image1.flatten());
                LocalFeatureList<Keypoint> targetKeypoints = engine.findFeatures(image2.flatten());
                RobustAffineTransformEstimator modelFitter = new RobustAffineTransformEstimator(5.0, 1500,
                        new RANSAC.PercentageInliersStoppingCondition(0.5));
                LocalFeatureMatcher<Keypoint> matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(
                        new FastBasicKeypointMatcher<Keypoint>(8), modelFitter);
                matcher.setModelFeatures(queryKeypoints);
                matcher.findMatches(targetKeypoints);
                double size = matcher.getMatches().size();
                mainPoints.add(size);
                System.out.println(size);
            }
            Double max = Collections.max(mainPoints);
            for(int m=0, n=0; m<mainPoints.size(); m++){
                if(((mainPoints.get(m))/max < 0.01) || m==0){
                    Double name1 = mainPoints.get(m)/max;
                    BufferedImage bufferedFrame = ImageUtilities.createBufferedImageForDisplay(imageList.get(m+1));
                    String name = "output/mainframes/keyframe" + n + ".jpg";
                    File outputFile = new File(name);
                    try {
                        ImageIO.write(bufferedFrame, "jpg", outputFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }



            for (int a = 0; a < 100; a++) {
                String imgName = "output/mainframes/" + "keyframe" + j + ".jpg";
                System.out.println(imgName);
                //BufferedImage bufferedFrame = ImageUtilities.createBufferedImageForDisplay(imageList.get(a + 1));
                j++;
                inputStream2 = new FileInputStream(imgName);
                while ((bytesRead = inputStream2.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }


        }
        catch(Exception e){
            e.printStackTrace();
        }



        bytes = output.toByteArray();
        encodedString = Base64.encodeToString(bytes, true);
        return encodedString;
    }

    public static void main(String[] args) {
        new SimpleProducer(); //Setting properties for kafka producer
        String topic = args[0];  //Topic Name
        String msg = MetaDataVideo(args[1]); //Encoding the metadata and keyframes
        Iterable<String> result = Splitter.fixedLength(100000).split(msg); //Splitting the video file
        String[] parts = Iterables.toArray(result, String.class); //Parts of video file
        for(int i=0; i<parts.length; i++){
            KeyedMessage<Integer, String> data = new KeyedMessage<Integer, String>(topic, parts[i]);
            //System.out.println(parts[i]);
            producer.send(data);
        }
        System.out.println("Metadata Sent");
        producer.close();
    }
}


