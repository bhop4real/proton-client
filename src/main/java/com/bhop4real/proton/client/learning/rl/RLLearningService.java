package com.bhop4real.proton.client.learning.rl;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.settings.SettingsManager;
import com.bhop4real.proton.client.util.io.FileIOHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Singleton service for reinforcement learning model management
 */
public final class RLLearningService
{
    private static final RLLearningService INSTANCE = new RLLearningService();
    private static final String MODEL_DIR = "rl_models";
    private static final String MODEL_FILE_PREFIX = "rl_model_";
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final AtomicReference<RLModel> currentModel = new AtomicReference<>(new RLModel());
    private final RLTrainer trainer = new RLTrainer();
    private long sessionStartTime = 0;
    private long lastClickTime = 0;
    
    private RLLearningService()
    {
    }
    
    public static RLLearningService get()
    {
        return INSTANCE;
    }
    
    /**
     * Get next click delay using the trained model
     */
    public long getNextClickDelay()
    {
        RLModel model = currentModel.get();
        long sessionDuration = sessionStartTime > 0 ? System.currentTimeMillis() - sessionStartTime : 0;
        int delay = trainer.selectAction(model, sessionDuration);
        return Math.max(RLModel.getMinDelay(), Math.min(RLModel.getMaxDelay(), delay));
    }
    
    /**
     * Record a click and update the model with reward
     */
    public void recordClick(boolean flagged)
    {
        long currentTime = System.currentTimeMillis();
        long interval = lastClickTime > 0 ? currentTime - lastClickTime : 0;
        lastClickTime = currentTime;
        
        trainer.recordInterval(interval);
        
        // Reward: positive for unflagged clicks, negative for flagged
        double reward = flagged ? -10.0 : 1.0;
        
        long sessionDuration = sessionStartTime > 0 ? currentTime - sessionStartTime : 0;
        RLModel model = currentModel.get();
        trainer.updateModel(model, reward, sessionDuration);
        
        // Decay epsilon over time (reduce exploration)
        model.decayEpsilon(0.9995);
    }
    
    /**
     * Start a new training session
     */
    public void startSession()
    {
        sessionStartTime = System.currentTimeMillis();
        lastClickTime = 0;
        trainer.reset();
    }
    
    /**
     * End current session
     */
    public void endSession()
    {
        sessionStartTime = 0;
        lastClickTime = 0;
    }
    
    /**
     * Save current model to disk
     */
    public boolean saveModel()
    {
        try
        {
            SettingsManager settingsManager = ProtonClient.getInstance().getSettingsManager();
            if (settingsManager == null)
            {
                Proton.logger.error("SettingsManager not available for saving RL model");
                return false;
            }
            
            File configDir = settingsManager.getConfigDirectory();
            File modelDir = new File(configDir, MODEL_DIR);
            if (!FileIOHelper.ensureDirectory(modelDir, Proton.logger))
            {
                Proton.logger.error("Failed to create RL models directory");
                return false;
            }
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            File modelFile = new File(modelDir, MODEL_FILE_PREFIX + timestamp + ".json");
            
            RLModel model = currentModel.get();
            JsonObject json = model.toJson();
            
            boolean saved = FileIOHelper.writeJsonElement(modelFile, json, gson, Proton.logger);
            if (saved)
            {
                Proton.logger.info("RL model saved to: " + modelFile.getAbsolutePath());
            }
            return saved;
        }
        catch (Exception e)
        {
            Proton.logger.error("Failed to save RL model", e);
            return false;
        }
    }
    
    /**
     * Load model from file
     */
    public boolean loadModel(File modelFile)
    {
        try
        {
            JsonObject json = FileIOHelper.readJsonObject(modelFile, gson, Proton.logger);
            if (json == null)
            {
                return false;
            }
            
            RLModel model = RLModel.fromJson(json);
            currentModel.set(model);
            trainer.reset();
            Proton.logger.info("RL model loaded from: " + modelFile.getAbsolutePath());
            return true;
        }
        catch (Exception e)
        {
            Proton.logger.error("Failed to load RL model", e);
            return false;
        }
    }
    
    /**
     * Get current model (for inspection)
     */
    public RLModel getCurrentModel()
    {
        return currentModel.get();
    }
    
    /**
     * Reset model to initial state
     */
    public void resetModel()
    {
        currentModel.set(new RLModel());
        trainer.reset();
        sessionStartTime = 0;
        lastClickTime = 0;
    }
}

