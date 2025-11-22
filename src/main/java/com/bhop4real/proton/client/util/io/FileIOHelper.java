package com.bhop4real.proton.client.util.io;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * General-purpose helper for file IO routines. Encapsulates repeated boilerplate around
 * directory creation, JSON read/write, atomic moves, and timestamped backups.
 */
public final class FileIOHelper
{
    private FileIOHelper()
    {
    }

    /**
     * Ensures the given directory exists. Returns {@code true} if it already exists or could be created.
     */
    public static boolean ensureDirectory(File directory, Logger logger)
    {
        if (directory == null)
        {
            return false;
        }

        if (directory.exists())
        {
            if (directory.isDirectory())
            {
                return true;
            }

            if (logger != null)
            {
                logger.error("Path {} exists but is not a directory", directory.getAbsolutePath());
            }
            return false;
        }

        boolean created = directory.mkdirs();
        if (!created && !directory.exists())
        {
            if (logger != null)
            {
                logger.error("Failed to create directory {}", directory.getAbsolutePath());
            }
            return false;
        }
        return true;
    }

    /**
     * Reads a JSON object from the supplied file. Returns {@code null} if the file does not exist.
     *
     * @throws IOException        if reading the file fails
     * @throws JsonParseException if the file does not contain valid JSON
     */
    public static JsonObject readJsonObject(File file, Gson gson, Logger logger) throws IOException
    {
        if (file == null || !file.exists())
        {
            return null;
        }

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8))
        {
            JsonElement element = gson.fromJson(reader, JsonElement.class);
            if (element == null || element.isJsonNull())
            {
                return new JsonObject();
            }
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        }
        catch (JsonParseException ex)
        {
            if (logger != null)
            {
                logger.error("Failed to parse JSON from {}", file.getAbsolutePath(), ex);
            }
            throw ex;
        }
    }

    /**
     * Writes the provided JSON element to the target file. Attempts an atomic move when possible.
     * Returns {@code true} when the target file was replaced successfully. Returns {@code false}
     * when the write only produced a fallback copy (e.g. because the target was locked).
     */
    public static boolean writeJsonElement(File target, JsonElement element, Gson gson, Logger logger)
    {
        if (target == null)
        {
            return false;
        }

        File parent = target.getParentFile();
        if (parent != null && !parent.exists())
        {
            if (!ensureDirectory(parent, logger))
            {
                return false;
            }
        }

        Path tempPath = null;
        boolean success = false;

        try
        {
            Path baseDir = parent != null ? parent.toPath() : target.toPath().toAbsolutePath().getParent();
            if (baseDir == null)
            {
                throw new IOException("Unable to determine parent directory for " + target.getAbsolutePath());
            }

            tempPath = Files.createTempFile(baseDir, target.getName(), ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8))
            {
                gson.toJson(element != null ? element : JsonNull.INSTANCE, writer);
            }

            try
            {
                Files.move(tempPath, target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                success = true;
            }
            catch (AtomicMoveNotSupportedException unsupported)
            {
                Files.move(tempPath, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                success = true;
            }
            catch (IOException locked)
            {
                if (logger != null)
                {
                    logger.warn("Unable to replace {} ({}), writing fallback copy instead.", target.getAbsolutePath(), locked.getMessage());
                }
                Path fallback = buildTimestampedSibling(target.toPath(), "fallback");
                Files.move(tempPath, fallback, StandardCopyOption.REPLACE_EXISTING);
                if (logger != null)
                {
                    logger.info("Wrote fallback file {}", fallback.getFileName());
                }
            }
        }
        catch (Exception ex)
        {
            if (logger != null)
            {
                logger.error("Failed to write {}", target.getAbsolutePath(), ex);
            }
        }
        finally
        {
            if (tempPath != null)
            {
                try
                {
                    Files.deleteIfExists(tempPath);
                }
                catch (IOException ignored)
                {
                }
            }
        }

        return success;
    }

    /**
     * Creates a timestamped copy of the source file in the same directory. Returns the new file, or {@code null}
     * if the copy failed.
     */
    public static File createTimestampedCopy(File source, String label, Logger logger)
    {
        if (source == null || !source.exists())
        {
            return null;
        }

        Path copyPath = buildTimestampedSibling(source.toPath(), label != null ? label : "backup");
        try
        {
            Files.copy(source.toPath(), copyPath, StandardCopyOption.REPLACE_EXISTING);
            if (logger != null)
            {
                logger.info("Created backup {} for {}", copyPath.getFileName(), source.getAbsolutePath());
            }
            return copyPath.toFile();
        }
        catch (Exception ex)
        {
            if (logger != null)
            {
                logger.error("Failed to create backup for {}", source.getAbsolutePath(), ex);
            }
            return null;
        }
    }

    private static Path buildTimestampedSibling(Path origin, String label)
    {
        String originalName = origin.getFileName().toString();
        String timeSuffix = "-" + (label != null && !label.isEmpty() ? label + "-" : "") + System.currentTimeMillis();

        int dotIndex = originalName.lastIndexOf('.');
        String candidate;
        if (dotIndex >= 0)
        {
            String base = originalName.substring(0, dotIndex);
            String extension = originalName.substring(dotIndex);
            candidate = base + timeSuffix + extension;
        }
        else
        {
            candidate = originalName + timeSuffix;
        }

        Path parent = origin.getParent();
        return parent != null ? parent.resolve(candidate) : origin.resolveSibling(candidate);
    }
}

