package com.bhop4real.proton.client.learning.click;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.settings.SettingsManager;
import com.bhop4real.proton.client.util.io.FileIOHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Singleton facade providing access to click samples, training, and inference.
 */
public final class ClickMLService
{
    private static final ClickMLService INSTANCE = new ClickMLService();
    private static final String MODEL_FILE_NAME = "click-model.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ClickSampleRepository sampleRepository = new ClickSampleRepository(gson);
    private final ClickModelTrainer trainer = new ClickModelTrainer();
    private final AtomicReference<ClickModel> cachedModel = new AtomicReference<>();
    private final ExecutorService executor;

    private ClickMLService()
    {
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory()
        {
            @Override
            public Thread newThread(Runnable r)
            {
                Thread thread = new Thread(r, "Proton-ClickML");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public static ClickMLService get()
    {
        return INSTANCE;
    }

    public List<ClickSample> loadSamples()
    {
        return sampleRepository.loadAll();
    }

    public void addSample(ClickSample sample)
    {
        sampleRepository.saveSample(sample);
    }

    public CompletableFuture<ClickTrainingResult> trainAsync(boolean clearSamplesAfter)
    {
        return CompletableFuture.supplyAsync(() -> trainAndPersistInternal(clearSamplesAfter), executor);
    }

    public ClickTrainingResult trainAndPersist(boolean clearSamplesAfter)
    {
        return trainAndPersistInternal(clearSamplesAfter);
    }

    private ClickTrainingResult trainAndPersistInternal(boolean clearSamplesAfter)
    {
        List<ClickSample> samples = loadSamples();
        if (samples.isEmpty())
        {
            Proton.logger.warn("Cannot train click model without samples.");
            return ClickTrainingResult.failure("Failed to train click model: no samples recorded.", ClickTrainingData.empty());
        }

        ClickTrainingData trainingData = trainer.train(samples);
        if (!trainingData.hasModel())
        {
            Proton.logger.warn("Training produced an empty click model.");
            return ClickTrainingResult.failure("Failed to train click model: dataset is empty.", trainingData);
        }

        ClickModel model = trainingData.getModel();
        if (persistModel(model))
        {
            cachedModel.set(model);
            if (clearSamplesAfter)
            {
                sampleRepository.deleteAll();
            }
            Proton.logger.info("Trained click model: {}", trainingData.getSummary().toLogSummary());
            return ClickTrainingResult.success(trainingData, clearSamplesAfter);
        }

        return ClickTrainingResult.failure("Failed to persist click model to disk.", trainingData);
    }

    public OptionalLong sampleDelay()
    {
        ClickModel model = ensureModelLoaded();
        if (model == null || !model.isReady())
        {
            return OptionalLong.empty();
        }
        return OptionalLong.of(model.sampleDelay());
    }

    public ClickModel getCachedModel()
    {
        return ensureModelLoaded();
    }

    private ClickModel ensureModelLoaded()
    {
        ClickModel cached = cachedModel.get();
        if (cached != null && cached.isReady())
        {
            return cached;
        }

        ClickModel loaded = loadModelFromDisk();
        if (loaded != null && loaded.isReady())
        {
            cachedModel.set(loaded);
        }
        return cachedModel.get();
    }

    private boolean persistModel(ClickModel model)
    {
        File target = new File(resolveModelDirectory(), MODEL_FILE_NAME);
        JsonObject json = new JsonObject();
        json.addProperty("trainedAt", model.getTrainedAt());
        json.addProperty("sampleCount", model.getSampleCount());
        json.addProperty("mean", model.getMean());
        json.addProperty("stdDeviation", model.getStdDeviation());
        json.addProperty("bandwidth", model.getBandwidth());

        JsonArray kernels = new JsonArray();
        for (Double kernel : model.getKernels())
        {
            if (kernel != null)
            {
                kernels.add(new JsonPrimitive(kernel));
            }
        }
        json.add("kernels", kernels);

        return FileIOHelper.writeJsonElement(target, json, gson, Proton.logger);
    }

    private ClickModel loadModelFromDisk()
    {
        File target = new File(resolveModelDirectory(), MODEL_FILE_NAME);
        if (!target.exists())
        {
            return null;
        }

        try
        {
            JsonObject json = FileIOHelper.readJsonObject(target, gson, Proton.logger);
            if (json == null)
            {
                return null;
            }

            int count = json.has("sampleCount") ? json.get("sampleCount").getAsInt() : 0;
            double mean = json.has("mean") ? json.get("mean").getAsDouble() : 0.0;
            double std = json.has("stdDeviation") ? json.get("stdDeviation").getAsDouble() : 0.0;
            double bandwidth = json.has("bandwidth") ? json.get("bandwidth").getAsDouble() : 0.0;
            long trainedAt = json.has("trainedAt") ? json.get("trainedAt").getAsLong() : target.lastModified();

            List<Double> kernels = Collections.emptyList();
            if (json.has("kernels") && json.get("kernels").isJsonArray())
            {
                JsonArray array = json.getAsJsonArray("kernels");
                kernels = new java.util.ArrayList<>(array.size());
                for (int i = 0; i < array.size(); i++)
                {
                    try
                    {
                        kernels.add(array.get(i).getAsDouble());
                    }
                    catch (Exception ignored)
                    {
                    }
                }
            }

            ClickModel model = new ClickModel(trainedAt, count, kernels, mean, std, bandwidth);
            if (!model.isReady())
            {
                Proton.logger.warn("Loaded click model is empty, ignoring.");
                return null;
            }
            return model;
        }
        catch (Exception e)
        {
            Proton.logger.error("Failed to load click model from disk", e);
            return null;
        }
    }

    private File resolveModelDirectory()
    {
        SettingsManager settings = ProtonClient.getInstance().getSettingsManager();
        File baseDir = settings != null ? settings.getConfigDirectory() : null;

        if (baseDir == null)
        {
            File mcDir = net.minecraft.client.Minecraft.getMinecraft().mcDataDir;
            baseDir = new File(mcDir, "proton");
        }

        File directory = new File(baseDir, "models/click");
        FileIOHelper.ensureDirectory(directory, Proton.logger);
        return directory;
    }
}

