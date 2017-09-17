package org.logstash.skunk;


import org.logstash.skunk.api.config.Config;
import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.EventQueue;
import org.logstash.skunk.api.plugin.Input;
import org.logstash.skunk.api.plugin.Output;
import org.logstash.skunk.api.plugin.Plugin;
import org.logstash.skunk.api.plugin.Processor;
import org.reflections.Reflections;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class App {
    public static void main(String[] args) throws IllegalAccessException, InstantiationException, MalformedURLException, ClassNotFoundException, URISyntaxException {

        Queue queue = new ArrayBlockingQueue<Event>(1024);
        EventQueue eventQueue = new EventQueue(queue);
        EventQueueInputConsumer inputConsumer = new EventQueueInputConsumer(eventQueue);


        //emulate a giant map of configuration, keyed by plugin class name
        Map<String, Map<String, ?>> configuration = new HashMap();
        Map<String, Object> translateConfig = new HashMap<>();

        configuration.put("org.logstash.skunk.plugin.processors.Translate", translateConfig);
        //common
        translateConfig.put("destination", "destValue");
        translateConfig.put("exact", true);
        translateConfig.put("override", false);
        translateConfig.put("regex", false);
        translateConfig.put("fallback", "fallbackValue");

        //change this to use the other constructor
        boolean useFile = true;

        if (useFile) {
            translateConfig.put("dictionary_path", new File("/tmp").toPath());
            translateConfig.put("refresh_interval", 99);
        } else {
            translateConfig.put("dictionary", new HashMap<String, Object>());
        }


        int workers = 2;


        //This strategy can be adapted to loading jars from .zip files in well known named directories, then programmatically un-zip to a staging location, add the .jar files to
        // the class path via the the URL class loader.

        //Instead of 1 class loader per inputs and and outputs, there should be 1 class loader per .zip, A zip may contain inputs, outputs, and processors and thus
        // should all use the same loader. Logic will need to be added to know the grouping so that we can correctly set the thread Context class loader.


        //By default this (Java) is a parent first class loader, meaning that any all classes be attempted be loaded from the parent class loader first . This means that while the
        // plugins are isolated from each other, they are not isolated from the dependencies of the main app. e.g. if core depends on jackson 2.7, then all plugins will
        // have to use that version, and any version they ship with will be ignored. We should provide an option to use a child first class loader strategy to allow
        // plugins to optionally override versions in core.

//        URLClassLoader loader = URLClassLoader.newInstance(new URL[] {new URL("file:///Users/jake/workspace/skunkstash/input/target/input-1.0-SNAPSHOT.jar")}); //via jar

        URI baseURI = App.class.getClassLoader().getResource(".").toURI().resolve("../../../");

        //via .class, don't forget the trailing /
        URLClassLoader inputLoader = URLClassLoader.newInstance(new URL[]{baseURI.resolve("input/target/classes/").toURL()});
        URLClassLoader outputLoader = URLClassLoader.newInstance(new URL[]{
                baseURI.resolve("processors/target/classes/").toURL(),
                baseURI.resolve("output/target/classes/").toURL()});

        //Find plugin via reflections
        Reflections reflections = new Reflections("org.logstash.skunk.plugin", inputLoader, outputLoader);
        Set<Class<?>> plugins = reflections.getTypesAnnotatedWith(Plugin.class);
        Set<Class<?>> inputClasses = new HashSet<>();
        Set<Class<?>> processorClasses = new HashSet<>();
        Set<Class<?>> outputClasses = new HashSet<>();


        for (Class<?> clazz : plugins) {
            Class<?>[] interfaces = clazz.getInterfaces();
            Plugin plugin = clazz.getAnnotation(Plugin.class);
            String type = null;
            for (Class<?> i : interfaces) {
                if ("org.logstash.skunk.api.plugin.Input".equals(i.getCanonicalName())) {
                    inputClasses.add(clazz);
                    type = "input";
                } else if ("org.logstash.skunk.api.plugin.Processor".equals(i.getCanonicalName())) {
                    processorClasses.add(clazz);
                    type = "processors";
                } else if ("org.logstash.skunk.api.plugin.Output".equals(i.getCanonicalName())) {
                    outputClasses.add(clazz);
                    type = "output";
                }

                System.out.println("Found " + type + " plugin \"" + plugin.value() + "\"");
            }
        }


        List<Processor> processors = new ArrayList<>();
        //Reflections based constructor injection for configuration.
        for (Class<?> clazz : processorClasses) {
            boolean constructed = false;
            Constructor<?>[] constructors = clazz.getConstructors();
            Map<String, ?> pluginConfig = configuration.get(clazz.getCanonicalName());

            //zero arg default constructor
            if (constructors.length == 1 && constructors[0].getParameterCount() == 0) {
                Processor processor = (Processor) clazz.newInstance();
                processors.add(processor);
                constructed = true;
            } else {
                //Find the matching constructor
                for (Constructor constructor : constructors) {
                    Annotation[][] annotations = constructor.getParameterAnnotations();

                    //pull the config annotations
                    List<String> annotationParams = new ArrayList<>();
                    for (int i = 0; i < annotations.length; i++) {

                        //ensure that non-default constructors only have the @Config paramaters
                        if (annotations[i].length == 0 || !Config.class.equals(annotations[i][0].annotationType())) {
                            throw new IllegalStateException("Non default constructors must only have @Config(\"name\") parameters");
                        }
                        annotationParams.add(((Config) annotations[i][0]).value());
                    }

                    //check based on size
                    if (constructor.getParameterCount() != annotationParams.size() || constructor.getParameterCount() != pluginConfig.size()) {
                        //not a match based on size...move along
                        System.out.println("No match based on count. parameter count: " + constructor.getParameterCount() + ", annotation count: " + annotationParams.size() +
                                ", plugin config count: " + pluginConfig.size());
                        continue;
                    }

                    //check based on keys
                    Set<String> configKeys = pluginConfig.keySet();
                    Set<String> annotationKeys = new HashSet<>(annotationParams);
                    if (!configKeys.equals(annotationKeys)) {
                        //not a match based on keys ... move along
                        System.out.println("No match based on keys. configKeys: " + Arrays.toString(configKeys.toArray()) + " annotationKeys: " + Arrays.toString(annotationKeys
                                .toArray()));
                        continue;
                    }

                    boolean match = false;
                    //check based on type, and grab the values if types are good
                    List<Object> params = new ArrayList<>();
                    for (int i = 0; i < constructor.getParameterTypes().length; i++) {
                        Class paramaterType = primitiveToObject(constructor.getParameterTypes()[i]);
                        String keyName = annotationParams.get(i);
                        Object configValue = pluginConfig.get(keyName);
                        if (paramaterType.isAssignableFrom(primitiveToObject(configValue.getClass()))) {
                            match = true;
                        } else {
                            System.out.println("No match based on type. parameter type: '" + paramaterType.getCanonicalName() + "', config value: '" + configValue.getClass()
                                    .getCanonicalName() + "'");
                            continue;
                        }
                        params.add(configValue);
                    }

                    if (match) {
                        //if it made it this far, we found a match!
                        System.out.println("FOUND A MATCH! constructing....");
                        try {
                            System.out.println("Construction with constructor keys: " + Arrays.toString(annotationParams.toArray()) + ", parameters: " + Arrays.toString(params
                                    .toArray()));
                            processors.add((Processor) constructor.newInstance(params.toArray()));
                            constructed = true;
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (!constructed) {
                System.out.println("****************** WARNING '" + clazz.getCanonicalName() + "' is not constructed ***************");
            }
        }

        List<Output> outputs = new ArrayList<>();
        for (
                Class<?> clazz : outputClasses)

        {
            Output output = (Output) clazz.newInstance();
            outputs.add(output);
        }
        System.out.println(String.format("Found %d inputs, %d processors, %d outputs", inputClasses.size(), processorClasses.size(), outputClasses.size()));

        List<Input> inputs = new ArrayList<>();
        //Each input gets its own thread
        ExecutorService inputExecutorService = Executors.newFixedThreadPool(inputClasses.size(), new PluginThreadFactory("input", inputLoader));
        for (
                Class<?> clazz : inputClasses)

        {
            Input input = (Input) clazz.newInstance();
            inputs.add(input);
            inputExecutorService.execute(() -> input.start(inputConsumer));
        }

        List<Sink> sinks = new ArrayList<>();
        //a thread for each worker
        ExecutorService processingExecutorService = Executors.newFixedThreadPool(workers, new PluginThreadFactory("output", outputLoader));
        IntStream.of(workers).

                forEach(__ ->

                {
                    Sink sink = new Sink(processors, outputs, eventQueue);
                    sinks.add(sink);
                    processingExecutorService.execute(() -> sink.start());
                });


        //Shutdown hook to allow for call to stop
        Runtime.getRuntime().

                addShutdownHook(new Thread(() ->

                {

                    System.out.println("Shutting down inputs...");
                    inputs.forEach(input -> input.stop());
                    shutdown(inputExecutorService, "inputs");

                    System.out.println("Shutting down outputs...");
                    sinks.forEach(p -> p.stop());
                    shutdown(processingExecutorService, "outputs");

                    System.out.println("Bye Bye...");
                }));

    }


    private static Class<?> primitiveToObject(Class<?> type) {
        if (!type.isPrimitive())
            return type;
        else if (int.class.equals(type))
            return Integer.class;
        else if (double.class.equals(type))
            return Double.class;
        else if (char.class.equals(type))
            return Character.class;
        else if (boolean.class.equals(type))
            return Boolean.class;
        else if (long.class.equals(type))
            return Long.class;
        else if (float.class.equals(type))
            return Float.class;
        else if (short.class.equals(type))
            return Short.class;
        else if (byte.class.equals(type))
            return Byte.class;
        else
            //should't happen
            throw new IllegalArgumentException("primitive type not supported " + type.getName());
    }

    /**
     * Attempt to clean shutdown, after a bit, interrupt the thread
     */
    public static void shutdown(ExecutorService service, String name) {
        try {
            service.shutdown();
            int max_wait = 10;
            int i = 0;
            while (i++ < max_wait) {
                System.out.println("Waiting for the " + name + " to terminate...");
                if (service.awaitTermination(1, TimeUnit.SECONDS)) {
                    break;
                }
            }
            if (i >= max_wait) {
                //knock knock, whose there? , interrupting thread, interrupting thread who ?
                service.shutdownNow();
                if (!service.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.out.println("Shut down of " + name + " not stop cleanly after " + max_wait + " seconds");
                }
            }
        } catch (InterruptedException e) {
            //do nothing
        }
    }

}
