package org.logstash.skunk;


import org.logstash.skunk.api.config.Config;
import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.EventQueue;
import org.logstash.skunk.api.plugin.Deprecated;
import org.logstash.skunk.api.plugin.*;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
        translateConfig.put("legacy", true);
        translateConfig.put("regular_expression", true);

        //change this to use the other constructor
        boolean useFile = true;

        if (useFile) {
            translateConfig.put("dictionary_path", "tmp");
            translateConfig.put("refresh_interval", 99);
        } else {
            translateConfig.put("dictionary", new HashMap<String, Object>());
        }


        Map<String, Object> dropperConfig = new HashMap<>();
        configuration.put("org.logstash.skunk.plugin.processors.Dropper", dropperConfig);
        dropperConfig.put("toggle_start", false);


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
        Reflections reflections = new Reflections("org.logstash.skunk.plugin", new SubTypesScanner(), new TypeAnnotationsScanner(), new FieldAnnotationsScanner(), inputLoader,
                outputLoader);
        Set<Class<?>> plugins = reflections.getTypesAnnotatedWith(Plugin.class);
        Set<Class<?>> inputClasses = new HashSet<>();
        Set<Class<?>> processorClasses = new HashSet<>();
        Set<Class<?>> outputClasses = new HashSet<>();


        //TODO: Handle deprecated and obsoleted for plugins
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
            Constructor<?>[] constructors = clazz.getConstructors();

            //zero arg default constructor
            if (constructors.length == 1 && constructors[0].getParameterCount() == 0) {
                Processor processor = (Processor) clazz.newInstance();
                //set the configuration
                Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);

                Map<String, ?> configMap = configuration.get(clazz.getCanonicalName());
                fields.stream().filter(f -> f.getDeclaringClass().equals(clazz)).forEach(field ->
                        {
                            field.setAccessible(true);

                            try {
                                String obsoletedMessage = field.getAnnotation(Obsoleted.class) == null ? "" : field.getAnnotation(Obsoleted.class).value();
                                if (!obsoletedMessage.isEmpty()) {
                                    System.out.println("Not setting value for " + field.getName() + " : " + obsoletedMessage);

                                } else {
                                    String deprecatedMessage = field.getAnnotation(Deprecated.class) == null ? "" : field.getAnnotation(Deprecated.class).value();
                                    if (!deprecatedMessage.isEmpty()) {
                                        System.out.println("Warning deprecated value: " + field.getName() + " : " + deprecatedMessage);

                                    }
                                    field.set(processor, configMap.get(field.getAnnotation(Config.class).value()));
                                }

                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                );

                processor.initialize();
                processors.add(processor);

            } else {
                //Non default
                throw new IllegalStateException("Non default constructors are not allowed");

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
