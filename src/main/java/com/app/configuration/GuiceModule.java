package com.app.configuration;

import com.app.annotations.Intercept;
import com.app.annotations.Step;
import com.app.contexts.IScenarioContext;
import com.app.contexts.ScenarioContext;
import com.app.listners.FunctionListner;
import com.app.utils.PropertyMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import cucumber.api.guice.CucumberModules;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.But;
import cucumber.runtime.java.guice.InjectorSource;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.aspectj.weaver.patterns.SimpleScope;
import org.junit.internal.runners.TestMethod;
import org.openqa.selenium.WebDriver;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class GuiceModule extends AbstractModule implements InjectorSource {

    private final String stepDefsPackage;

    private String scenarioContextsPackage;

    private String utilsPackage;

    private String stepsPackage;

    private Injector injector;

    /**
     * Guice Module with default package values
     */
    public GuiceModule() {
        super();
        this.stepDefsPackage = "com.app.tests";
        this.scenarioContextsPackage = "com.app.contexts";
        this.utilsPackage = "com.app.utils";
        this.stepsPackage = "com.app.steps";
    }

    /**
     * GuiceModule with the specific packages for the user defined framework
     * @param stepDefsPackage
     */
    public GuiceModule(String stepDefsPackage) {
        super();
        this.stepDefsPackage = stepDefsPackage;
    }

    /**
     * GuiceModule with the specific packages for the user defined framework
     * @param stepDefsPackage
     * @param scenarioContextsPackage
     */
    public GuiceModule(String stepDefsPackage, String scenarioContextsPackage) {
        super();
        this.stepDefsPackage = stepDefsPackage;
        this.scenarioContextsPackage = scenarioContextsPackage;
    }

    /**
     * GuiceModule with the specific packages for the user defined framework
     * @param stepDefsPackage
     * @param scenarioContextsPackage
     * @param utilsPackage
     */
    public GuiceModule(String stepDefsPackage, String scenarioContextsPackage, String utilsPackage) {
        super();
        this.stepDefsPackage = stepDefsPackage;
        this.scenarioContextsPackage = scenarioContextsPackage;
        this.utilsPackage = utilsPackage;
    }

    /**
     * GuiceModule with the specific packages for the user defined framework
     * @param stepDefsPackage
     * @param scenarioContextsPackage
     * @param stepsPackage
     * @param utilsPackage
     */
    public GuiceModule(String stepDefsPackage, String scenarioContextsPackage, String stepsPackage, String utilsPackage) {
        super();
        this.stepDefsPackage = stepDefsPackage;
        this.scenarioContextsPackage = scenarioContextsPackage;
        this.utilsPackage = utilsPackage;
        this.stepsPackage = stepsPackage;
    }

    @Override
    protected void configure() {
        bind(WebDriver.class).toProvider(DriverProvider.class).in(ScenarioScoped.class);
        getAllStepDefinitionClasses().forEach(stepDef->bind(stepDef).in(ScenarioScoped.class));
        getScenarioContexts().forEach(context->bind(context).in(ScenarioScoped.class));
        getAllSteps().forEach(step->bind(step).in(ScenarioScoped.class));
        bind(IScenarioContext.class).to(ScenarioContext.class).in(ScenarioScoped.class);
        getAllUtilClasses().forEach(this::bind);
        Names.bindProperties(binder(), getProperties());
        bindListener(Matchers.any(), new PageListner());
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Intercept.class), new FunctionListner());
    }


    /**
     * gets all Step Definition Classes
     * @return list of Step Definition class types
     */
    private List<Class<?>> getAllStepDefinitionClasses(){
        List<Class<?>> stepDefinitions = new ArrayList<>();
        List<Class<? extends Annotation>> cucumberAnnotations = new ArrayList<>();
        cucumberAnnotations.add(And.class);
        cucumberAnnotations.add(Given.class);
        cucumberAnnotations.add(Then.class);
        cucumberAnnotations.add(When.class);
        cucumberAnnotations.add(But.class);
        cucumberAnnotations.add(After.class);
        cucumberAnnotations.add(Before.class);
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.forPackages(stepDefsPackage).setScanners(new MethodAnnotationsScanner());
        Reflections reflections = new Reflections(builder);
        for(Class<? extends Annotation> annotation: cucumberAnnotations){
            Set<Method> methods = reflections.getMethodsAnnotatedWith(annotation);
            if(!methods.isEmpty()){
                Class<?> stepDefinitionClass = methods.iterator().next().getDeclaringClass();
                if(!stepDefinitions.contains(stepDefinitionClass)) {
                    stepDefinitions.add(stepDefinitionClass);
                }
            }
        }
        return stepDefinitions;
    }

    /**
     * gets all step classes
     * @return list of steps classes
     */
    private List<Class<?>> getAllSteps(){
        List<Class<?>> stepClasses = new ArrayList<>();
        if(stepsPackage != null && !stepsPackage.equals("")) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.forPackages(stepsPackage).setScanners(new TypeAnnotationsScanner(), new SubTypesScanner());
            Reflections reflections = new Reflections(builder);
            stepClasses = reflections.getTypesAnnotatedWith(Step.class).stream().collect(Collectors.toList());
        }
        return stepClasses;
    }

    /**
     * gets all the scenario context classes
     * @return list of scenario context classes
     */
    private List<Class<?>> getScenarioContexts() {
        List<Class<?>> contextClasses = new ArrayList<>();
        if(scenarioContextsPackage != null && !scenarioContextsPackage.equals("")) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.forPackages(scenarioContextsPackage).setScanners(new SubTypesScanner());
            Reflections reflections = new Reflections(builder);
            contextClasses = reflections.getSubTypesOf(IScenarioContext.class).stream().collect(Collectors.toList());
        }
        return contextClasses;
    }

    /**
     * gets all the util classes
     * @return list of utility classes
     */
    private List<Class<?>> getAllUtilClasses() {
        List<Class<?>> utilClasses = new ArrayList<>();
        if(utilsPackage != null && !utilsPackage.equals("")) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.forPackages(utilsPackage);
            Reflections reflections = new Reflections(builder);
            utilClasses = reflections.getSubTypesOf(Object.class).stream().collect(Collectors.toList());
        }
        return utilClasses;
    }
    /**
     * returns the properties object
     * @return
     */
    private Properties getProperties() {
        return PropertyMap.getInstance().getLoadedProperties();
    }

    @Override
    public Injector getInjector() {
        if(injector == null) {
            injector = Guice.createInjector(Stage.PRODUCTION, CucumberModules.SCENARIO,
                     this);
        }
        return injector;
    }
}
