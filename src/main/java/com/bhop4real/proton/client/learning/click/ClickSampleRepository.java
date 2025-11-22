package com.bhop4real.proton.client.learning.click;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.settings.SettingsManager;
import com.bhop4real.proton.client.util.io.FileIOHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles persistence of click samples to disk.
 */
public class ClickSampleRepository
{
    private static final String SAMPLES_FOLDER = "click-samples";
    private final Gson gson;

    public ClickSampleRepository(Gson gson)
    {
        this.gson = gson;
    }

    private File resolveDirectory()
    {
        SettingsManager settings = ProtonClient.getInstance().getSettingsManager();
        File baseDir = settings != null ? settings.getConfigDirectory() : null;

        if (baseDir == null)
        {
            File mcDir = net.minecraft.client.Minecraft.getMinecraft().mcDataDir;
            baseDir = new File(mcDir, "proton");
        }

        File directory = new File(baseDir, SAMPLES_FOLDER);
        FileIOHelper.ensureDirectory(directory, Proton.logger);
        return directory;
    }

    public List<ClickSample> loadAll()
    {
        File root = resolveDirectory();
        List<File> files = new ArrayList<>();
        collectSampleFiles(root, files);
        if (files.isEmpty())
        {
            return Collections.emptyList();
        }

        List<ClickSample> samples = new ArrayList<>(files.size());
        for (File file : files)
        {
            try
            {
                JsonObject json = FileIOHelper.readJsonObject(file, gson, Proton.logger);
                if (json == null)
                {
                    continue;
                }

                String id = json.has("id") ? json.get("id").getAsString() : file.getName();
                long createdAt = json.has("createdAt") ? json.get("createdAt").getAsLong() : file.lastModified();
                List<Long> intervals = new ArrayList<>();
                if (json.has("intervals") && json.get("intervals").isJsonArray())
                {
                    JsonArray array = json.getAsJsonArray("intervals");
                    for (JsonElement element : array)
                    {
                        try
                        {
                            intervals.add(Math.max(1L, element.getAsLong()));
                        }
                        catch (Exception ignored)
                        {
                        }
                    }
                }

                List<Long> holdDurations = new ArrayList<>();
                if (json.has("holdDurations") && json.get("holdDurations").isJsonArray())
                {
                    JsonArray holds = json.getAsJsonArray("holdDurations");
                    for (JsonElement element : holds)
                    {
                        try
                        {
                            holdDurations.add(Math.max(1L, element.getAsLong()));
                        }
                        catch (Exception ignored)
                        {
                        }
                    }
                }

                if (!intervals.isEmpty() || !holdDurations.isEmpty())
                {
                    samples.add(new ClickSample(id, createdAt, intervals, holdDurations));
                }
            }
            catch (Exception e)
            {
                Proton.logger.error("Failed to read click sample file {}", file.getName(), e);
            }
        }

        return samples;
    }

    private void collectSampleFiles(File directory, List<File> out)
    {
        if (directory == null || out == null)
        {
            return;
        }

        File[] entries = directory.listFiles();
        if (entries == null)
        {
            return;
        }

        for (File entry : entries)
        {
            if (entry.isDirectory())
            {
                collectSampleFiles(entry, out);
            }
            else if (entry.isFile() && entry.getName().toLowerCase().endsWith(".json"))
            {
                out.add(entry);
            }
        }
    }

    public void saveSample(ClickSample sample)
    {
        if (sample == null || sample.size() == 0)
        {
            return;
        }

        File directory = resolveDirectory();
        String fileName = sample.getId() + ".json";
        File target = new File(directory, fileName);

        JsonObject json = new JsonObject();
        json.addProperty("id", sample.getId());
        json.addProperty("createdAt", sample.getCreatedAt());

        JsonArray intervals = new JsonArray();
        for (Long value : sample.getIntervals())
        {
            if (value != null)
            {
                intervals.add(new JsonPrimitive(value));
            }
        }
        json.add("intervals", intervals);
        JsonArray holds = new JsonArray();
        for (Long value : sample.getHoldDurations())
        {
            if (value != null)
            {
                holds.add(new JsonPrimitive(value));
            }
        }
        json.add("holdDurations", holds);
        json.addProperty("count", sample.size());

        FileIOHelper.writeJsonElement(target, json, gson, Proton.logger);
    }

    public void deleteAll()
    {
        File directory = resolveDirectory();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            if (!file.delete())
            {
                Proton.logger.warn("Unable to delete click sample {}", file.getName());
            }
        }
    }
}

