package org.logstash.skunk.config;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.Set;

import static javax.lang.model.SourceVersion.RELEASE_8;

@SupportedSourceVersion(RELEASE_8)
@SupportedAnnotationTypes("org.logstash.skunk.config.Config")
public class ConfigParser extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "HERE!!!!!!");


        for (Element element : roundEnv.getElementsAnnotatedWith(Config.class)) {

            if (element.getKind() != ElementKind.PARAMETER) {

                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Config must be applied to a field");

                return true;
            }

            VariableElement configItem = (VariableElement) element;
            String classType = configItem.asType().toString();
            String configItemKey = configItem.getAnnotationMirrors().get(0).getElementValues().values().iterator().next().toString();

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, configItem.getSimpleName()+ " of type " + classType + " with key " + configItemKey);

        }
        return true;
    }
}
