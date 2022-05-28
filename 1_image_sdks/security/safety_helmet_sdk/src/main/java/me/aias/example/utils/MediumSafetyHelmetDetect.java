package me.aias.example.utils;

import ai.djl.Device;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安全帽检测
 * https://github.com/njvisionpower/Safety-Helmet-Wearing-Dataset
 */

public final class MediumSafetyHelmetDetect {

  private static final Logger logger = LoggerFactory.getLogger(MediumSafetyHelmetDetect.class);

  public MediumSafetyHelmetDetect() {}

  public Criteria<Image, DetectedObjects> criteria(Image image) {
    Map<String, Object> arguments = new ConcurrentHashMap<>();
    int[] size = scale(image.getHeight(), image.getWidth());
    arguments.put("width", size[1]);
    arguments.put("height", size[0]);
    arguments.put("resize", true);
    arguments.put("rescale", true);
    arguments.put("normalize", true);
    arguments.put("threshold", 0.2);

    Translator<Image, DetectedObjects> translator = YoloTranslator.builder(arguments).build();
    
    Criteria<Image, DetectedObjects> criteria =
        Criteria.builder()
            .setTypes(Image.class, DetectedObjects.class)
            .optModelUrls("https://aias-home.oss-cn-beijing.aliyuncs.com/models/sec_models/mobilenet1.0.zip")
            // .optModelUrls("/Users/calvin/Desktop/Download/browser/Safety-Helmet-Wearing-Dataset-master/symbol/darknet53/")
            .optTranslator(translator)
            .optProgress(new ProgressBar())
            .optEngine("MXNet")
            .build();

    return criteria;
  }

  private static int[] scale(int h, int w) {
    int min = Math.min(h, w);
    float scale = 1.0F;

    scale = (float) 416 * 1.0F / (float) min;

    return new int[] {(int) ((float) h * scale), (int) ((float) w * scale)};
  }
}
