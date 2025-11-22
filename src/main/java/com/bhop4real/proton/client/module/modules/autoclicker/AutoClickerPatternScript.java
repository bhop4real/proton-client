package com.bhop4real.proton.client.module.modules.autoclicker;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.util.RandomUtil;
import com.bhop4real.proton.client.util.SecureRandomUtil;

import javax.script.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * JavaScript execution engine for auto clicker pattern scripts.
 * Uses Java 8's Nashorn JavaScript engine for dynamic script loading.
 */
public final class AutoClickerPatternScript
{
    private static final ScriptEngineManager engineManager = new ScriptEngineManager();
    private static final ScriptEngine engine;
    
    static
    {
        ScriptEngine tempEngine = engineManager.getEngineByName("nashorn");
        if (tempEngine == null)
        {
            // Try alternative names
            tempEngine = engineManager.getEngineByName("javascript");
            if (tempEngine == null)
            {
                tempEngine = engineManager.getEngineByExtension("js");
            }
        }
        engine = tempEngine;
        if (engine == null)
        {
            Proton.logger.warn("Nashorn JavaScript engine not available. Pattern scripting will be disabled.");
        }
    }
    
    private CompiledScript compiledScript;
    private String currentScriptPath;
    private long sessionStartTime = 0;
    private long lastClickTime = 0;
    private long lastDelay = 0;
    
    private final Invocable invocable;
    
    public AutoClickerPatternScript()
    {
        if (engine == null)
        {
            throw new IllegalStateException("JavaScript engine not available. Pattern scripting is disabled.");
        }
        this.invocable = (Invocable) engine;
        setupScriptBindings();
    }
    
    /**
     * Sets up all the functions and objects exposed to scripts
     */
    private void setupScriptBindings()
    {
        try
        {
            // Create Java objects that can be called from JavaScript
            Bindings bindings = engine.createBindings();
            
            // Create wrapper objects with callable methods for Nashorn
            Object randomUtil = createRandomUtilWrapper();
            Object secureRandomUtil = createSecureRandomUtilWrapper();
            Object mathUtil = createMathUtilWrapper();
            
            bindings.put("RandomUtil", randomUtil);
            bindings.put("SecureRandomUtil", secureRandomUtil);
            bindings.put("MathUtil", mathUtil);
            // TimeUtil will be created dynamically in getNextDelay
            
            // Expose Java utilities via eval'd JavaScript functions
            String jsSetup = 
                "var randomInt = function(min, max) { return RandomUtil.nextInt(min, max); };" +
                "var randomDouble = function() { return RandomUtil.nextDouble(); };" +
                "var randomDoubleMinMax = function(min, max) { return RandomUtil.nextDoubleMinMax(min, max); };" +
                "var randomLong = function() { return RandomUtil.nextLong(); };" +
                "var randomBoolean = function() { return RandomUtil.nextBoolean(); };" +
                "var randomGaussian = function() { return RandomUtil.nextGaussian(); };" +
                
                "var secureRandomInt = function(min, max) { return SecureRandomUtil.nextInt(min, max); };" +
                "var secureRandomDouble = function() { return SecureRandomUtil.nextDouble(); };" +
                "var secureRandomDoubleMinMax = function(min, max) { return SecureRandomUtil.nextDoubleMinMax(min, max); };" +
                "var secureRandomLong = function() { return SecureRandomUtil.nextLong(); };" +
                "var secureRandomBoolean = function() { return SecureRandomUtil.nextBoolean(); };" +
                "var secureRandomGaussian = function() { return SecureRandomUtil.nextGaussian(); };" +
                
                "var average = function(arr) { return MathUtil.average(arr); };" +
                "var min = function(arr) { return MathUtil.min(arr); };" +
                "var max = function(arr) { return MathUtil.max(arr); };" +
                "var clamp = function(val, min, max) { return MathUtil.clamp(val, min, max); };" +
                "var lerp = function(a, b, t) { return MathUtil.lerp(a, b, t); };" +
                "var normalize = function(val, min, max) { return MathUtil.normalize(val, min, max); };" +
                "var round = function(n) { return MathUtil.round(n); };" +
                "var floor = function(n) { return MathUtil.floor(n); };" +
                "var ceil = function(n) { return MathUtil.ceil(n); };" +
                "var abs = function(n) { return MathUtil.abs(n); };" +
                "var pow = function(base, exp) { return MathUtil.pow(base, exp); };" +
                "var sqrt = function(n) { return MathUtil.sqrt(n); };" +
                "var sin = function(n) { return MathUtil.sin(n); };" +
                "var cos = function(n) { return MathUtil.cos(n); };" +
                "var tan = function(n) { return MathUtil.tan(n); };" +
                "var log = function(n) { return MathUtil.log(n); };" +
                "var log10 = function(n) { return MathUtil.log10(n); };" +
                
                // Time functions - will be updated dynamically in getNextDelay
                "var getCurrentTime = function() { return TimeUtil.getCurrentTime(); };" +
                "var getLastDelay = function() { return TimeUtil.getLastDelay(); };" +
                "var getElapsedTime = function() { return TimeUtil.getElapsedTime(); };" +
                "var getLastClickTime = function() { return TimeUtil.getLastClickTime(); };" +
                "var delay = function(ms) { return TimeUtil.delay(ms); };";
            
            engine.eval(jsSetup, bindings);
            engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        }
        catch (ScriptException e)
        {
            Proton.logger.error("Failed to setup script bindings", e);
        }
    }
    
    private Object createRandomUtilWrapper()
    {
        Bindings wrapper = engine.createBindings();
        wrapper.put("nextInt", (BiFunction<Integer, Integer, Integer>) (min, max) -> RandomUtil.nextInt(min, max));
        wrapper.put("nextDouble", (java.util.function.Supplier<Double>) RandomUtil::nextDouble);
        wrapper.put("nextDoubleMinMax", (BiFunction<Double, Double, Double>) (min, max) -> RandomUtil.nextDouble(min, max));
        wrapper.put("nextLong", (java.util.function.Supplier<Long>) RandomUtil::nextLong);
        wrapper.put("nextBoolean", (java.util.function.Supplier<Boolean>) RandomUtil::nextBoolean);
        wrapper.put("nextGaussian", (java.util.function.Supplier<Double>) RandomUtil::nextGaussian);
        return wrapper;
    }
    
    private Object createSecureRandomUtilWrapper()
    {
        Bindings wrapper = engine.createBindings();
        wrapper.put("nextInt", (BiFunction<Integer, Integer, Integer>) (min, max) -> SecureRandomUtil.nextInt(min, max));
        wrapper.put("nextDouble", (java.util.function.Supplier<Double>) SecureRandomUtil::nextDouble);
        wrapper.put("nextDoubleMinMax", (BiFunction<Double, Double, Double>) (min, max) -> SecureRandomUtil.nextDouble(min, max));
        wrapper.put("nextLong", (java.util.function.Supplier<Long>) SecureRandomUtil::nextLong);
        wrapper.put("nextBoolean", (java.util.function.Supplier<Boolean>) SecureRandomUtil::nextBoolean);
        wrapper.put("nextGaussian", (java.util.function.Supplier<Double>) SecureRandomUtil::nextGaussian);
        return wrapper;
    }
    
    private Object createMathUtilWrapper()
    {
        final AutoClickerPatternScript self = this;
        Bindings wrapper = engine.createBindings();
        wrapper.put("average", (Function<Object, Double>) self::average);
        wrapper.put("min", (Function<Object, Double>) self::min);
        wrapper.put("max", (Function<Object, Double>) self::max);
        wrapper.put("clamp", (TriFunction<Number, Number, Number, Number>) self::clamp);
        wrapper.put("lerp", (TriFunction<Number, Number, Number, Double>) self::lerp);
        wrapper.put("normalize", (TriFunction<Number, Number, Number, Double>) self::normalize);
        wrapper.put("round", (Function<Number, Long>) n -> Math.round(n.doubleValue()));
        wrapper.put("floor", (Function<Number, Long>) n -> (long)Math.floor(n.doubleValue()));
        wrapper.put("ceil", (Function<Number, Long>) n -> (long)Math.ceil(n.doubleValue()));
        wrapper.put("abs", (Function<Number, Double>) n -> Math.abs(n.doubleValue()));
        wrapper.put("pow", (BiFunction<Number, Number, Double>) (base, exp) -> Math.pow(base.doubleValue(), exp.doubleValue()));
        wrapper.put("sqrt", (Function<Number, Double>) n -> Math.sqrt(n.doubleValue()));
        wrapper.put("sin", (Function<Number, Double>) n -> Math.sin(n.doubleValue()));
        wrapper.put("cos", (Function<Number, Double>) n -> Math.cos(n.doubleValue()));
        wrapper.put("tan", (Function<Number, Double>) n -> Math.tan(n.doubleValue()));
        wrapper.put("log", (Function<Number, Double>) n -> Math.log(n.doubleValue()));
        wrapper.put("log10", (Function<Number, Double>) n -> Math.log10(n.doubleValue()));
        return wrapper;
    }
    
    @FunctionalInterface
    private interface TriFunction<T, U, V, R>
    {
        R apply(T t, U u, V v);
    }
    
    /**
     * Loads and compiles a script from a file path
     */
    public boolean loadScript(String scriptPath)
    {
        if (scriptPath == null || scriptPath.trim().isEmpty())
        {
            compiledScript = null;
            currentScriptPath = null;
            return false;
        }
        
        try
        {
            Path path = Paths.get(scriptPath);
            if (!Files.exists(path))
            {
                Proton.logger.error("Pattern script file not found: " + scriptPath);
                compiledScript = null;
                currentScriptPath = null;
                return false;
            }
            
            File file = path.toFile();
            try (FileReader reader = new FileReader(file))
            {
                CompiledScript compiled = ((Compilable) engine).compile(reader);
                compiledScript = compiled;
                currentScriptPath = scriptPath;
                Proton.logger.info("Pattern script loaded successfully: " + scriptPath);
                return true;
            }
        }
        catch (ScriptException | IOException e)
        {
            Proton.logger.error("Failed to load pattern script: " + scriptPath, e);
            compiledScript = null;
            currentScriptPath = null;
            return false;
        }
    }
    
    /**
     * Executes the script's getNextDelay function
     */
    public long getNextDelay(long lastDelay, AutoClickerRandomization.Config config, PatternState state)
    {
        if (compiledScript == null)
        {
            return lastDelay > 0 ? lastDelay : 50L; // Default fallback
        }
        
        try
        {
            // Update dynamic state
            this.lastDelay = lastDelay;
            this.lastClickTime = state.lastClickTime;
            
            // Update bindings with current state
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            
            // Create/update TimeUtil with current state
            Bindings timeUtil = engine.createBindings();
            timeUtil.put("getCurrentTime", (java.util.function.Supplier<Long>) () -> System.currentTimeMillis());
            timeUtil.put("getLastDelay", (java.util.function.Supplier<Long>) () -> this.lastDelay);
            timeUtil.put("getElapsedTime", (java.util.function.Supplier<Long>) () -> sessionStartTime > 0 ? System.currentTimeMillis() - sessionStartTime : 0L);
            timeUtil.put("getLastClickTime", (java.util.function.Supplier<Long>) () -> this.lastClickTime);
            timeUtil.put("delay", (java.util.function.Function<Long, Void>) ms -> {
                try {
                    Thread.sleep(Math.max(0, ms));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
            bindings.put("TimeUtil", timeUtil);
            
            // Create config and state objects for script
            bindings.put("config", createConfigObject(config));
            bindings.put("state", createStateObject(state));
            
            // Execute script to set up functions
            compiledScript.eval();
            
            // Call getNextDelay function
            Object result = invocable.invokeFunction("getNextDelay", lastDelay, bindings.get("config"), bindings.get("state"));
            
            if (result == null)
            {
                Proton.logger.warn("Pattern script getNextDelay returned null, using fallback");
                return lastDelay > 0 ? lastDelay : 50L;
            }
            
            double delayValue = ((Number) result).doubleValue();
            long delay = Math.max(1L, Math.round(delayValue));
            
            return delay;
        }
        catch (ScriptException | NoSuchMethodException e)
        {
            Proton.logger.error("Failed to execute pattern script getNextDelay", e);
            return lastDelay > 0 ? lastDelay : 50L;
        }
    }
    
    private Object createConfigObject(AutoClickerRandomization.Config config)
    {
        Bindings configObj = engine.createBindings();
        configObj.put("minCps", config.minCps());
        configObj.put("maxCps", config.maxCps());
        configObj.put("minDelay", config.minDelayMs());
        configObj.put("maxDelay", config.maxDelayMs());
        configObj.put("offsetMin", config.offsetMinMs());
        configObj.put("offsetMax", config.offsetMaxMs());
        return configObj;
    }
    
    private Object createStateObject(PatternState state)
    {
        Bindings stateObj = engine.createBindings();
        stateObj.put("clickCount", state.clickCount);
        stateObj.put("sessionTime", state.sessionTime);
        stateObj.put("lastClickTime", state.lastClickTime);
        return stateObj;
    }
    
    public void startSession()
    {
        sessionStartTime = System.currentTimeMillis();
        lastClickTime = 0;
    }
    
    public void recordClick()
    {
        lastClickTime = System.currentTimeMillis();
    }
    
    public void reset()
    {
        sessionStartTime = 0;
        lastClickTime = 0;
        lastDelay = 0;
    }
    
    public String getCurrentScriptPath()
    {
        return currentScriptPath;
    }
    
    // Helper functions for JavaScript exposure
    
    private Double average(Object array)
    {
        if (!(array instanceof Object[]))
        {
            return 0.0;
        }
        Object[] arr = (Object[]) array;
        if (arr.length == 0)
        {
            return 0.0;
        }
        double sum = 0.0;
        for (Object o : arr)
        {
            if (o instanceof Number)
            {
                sum += ((Number) o).doubleValue();
            }
        }
        return sum / arr.length;
    }
    
    private Double min(Object array)
    {
        if (!(array instanceof Object[]))
        {
            return Double.MAX_VALUE;
        }
        Object[] arr = (Object[]) array;
        double min = Double.MAX_VALUE;
        for (Object o : arr)
        {
            if (o instanceof Number)
            {
                double val = ((Number) o).doubleValue();
                if (val < min)
                {
                    min = val;
                }
            }
        }
        return min == Double.MAX_VALUE ? 0.0 : min;
    }
    
    private Double max(Object array)
    {
        if (!(array instanceof Object[]))
        {
            return Double.MIN_VALUE;
        }
        Object[] arr = (Object[]) array;
        double max = Double.MIN_VALUE;
        for (Object o : arr)
        {
            if (o instanceof Number)
            {
                double val = ((Number) o).doubleValue();
                if (val > max)
                {
                    max = val;
                }
            }
        }
        return max == Double.MIN_VALUE ? 0.0 : max;
    }
    
    private Number clamp(Number value, Number min, Number max)
    {
        double val = value.doubleValue();
        double minVal = min.doubleValue();
        double maxVal = max.doubleValue();
        if (val < minVal)
        {
            return minVal;
        }
        if (val > maxVal)
        {
            return maxVal;
        }
        return val;
    }
    
    private Double lerp(Number a, Number b, Number t)
    {
        double aVal = a.doubleValue();
        double bVal = b.doubleValue();
        double tVal = t.doubleValue();
        return aVal + (bVal - aVal) * tVal;
    }
    
    private Double normalize(Number value, Number min, Number max)
    {
        double val = value.doubleValue();
        double minVal = min.doubleValue();
        double maxVal = max.doubleValue();
        if (maxVal == minVal)
        {
            return 0.0;
        }
        return (val - minVal) / (maxVal - minVal);
    }
    
    
    /**
     * Pattern state passed to scripts
     */
    public static final class PatternState
    {
        public final int clickCount;
        public final long sessionTime;
        public final long lastClickTime;
        
        public PatternState(int clickCount, long sessionTime, long lastClickTime)
        {
            this.clickCount = clickCount;
            this.sessionTime = sessionTime;
            this.lastClickTime = lastClickTime;
        }
    }
}

