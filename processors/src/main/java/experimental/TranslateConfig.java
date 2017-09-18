package experimental;

import org.logstash.skunk.api.config.Config;

import java.nio.file.Path;
import java.util.Map;

@Config("org.logstash.skunk.plugin.processors.Translate")
public class TranslateConfig {


    private @Config("dictionary_path") Path dictionaryPath;

    private @Config("refresh_interval") int refreshInterval;

    private @Config("dictionary")  Map<String, Object> dictionary;

    private @Config("destination") String destination;

    private @Config("exact") boolean exact;

    private @Config("override") boolean override;

    private @Config("regex") boolean regex;

    private @Config("fallback") String fallBack;

    //more examples

    private @Config(value = "oldstuff", deprecated = "this is no longer used") int foo;
    private @Config(value = "really_oldstuff", obsolete = "if you set this, logstash wont start") int foo2;
    private @Config(value = "something_important", required = true) int foo3;


    /**
     * Business logic in the getters should error/log when there is configuration mismatch.
     *
     */


    public Path getDictionaryPath() {
        if(getDictionary() != null){
            throw new IllegalStateException("Dictionary and Dictionary Path are mutually exclusive");
        }
        return dictionaryPath;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public Map<String, Object> getDictionary() {
        if(getDictionaryPath() != null){
            throw new IllegalStateException("Dictionary and Dictionary Path are mutually exclusive");
        }
        return dictionary;
    }

    public String getDestination() {
        return destination;
    }

    public boolean isExact() {
        return exact;
    }

    public boolean isOverride() {
        return override;
    }

    public boolean isRegex() {
        return regex;
    }

    public String getFallBack() {
        return fallBack;
    }


    //Use reflections to Field::setAccessible to true, set these values these via reflections

}
