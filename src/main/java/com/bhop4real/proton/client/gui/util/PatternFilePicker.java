package com.bhop4real.proton.client.gui.util;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.settings.SettingsManager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.function.Consumer;

/**
 * File picker utility for selecting pattern script files.
 * Uses Swing JFileChooser on EDT thread to avoid blocking Minecraft's main thread.
 */
public final class PatternFilePicker
{
    private PatternFilePicker() {}
    
    /**
     * Opens a file picker dialog for selecting a pattern script file.
     * The callback will be invoked on the EDT thread with the selected file path, or null if cancelled.
     */
    public static void pickPatternFile(Consumer<String> callback)
    {
        // Run file chooser on Swing EDT thread
        SwingUtilities.invokeLater(() -> {
            try
            {
                JFileChooser fileChooser = new JFileChooser();
                
                // Set default directory to proton/patterns/
                SettingsManager settingsManager = ProtonClient.getInstance() != null ? 
                    ProtonClient.getInstance().getSettingsManager() : null;
                if (settingsManager != null)
                {
                    File configDir = settingsManager.getConfigDirectory();
                    File patternsDir = new File(configDir, "patterns");
                    if (!patternsDir.exists())
                    {
                        patternsDir.mkdirs();
                    }
                    fileChooser.setCurrentDirectory(patternsDir);
                }
                
                // Filter for .js files
                FileNameExtensionFilter filter = new FileNameExtensionFilter("JavaScript Files (*.js)", "js");
                fileChooser.setFileFilter(filter);
                fileChooser.setAcceptAllFileFilterUsed(false);
                
                // Set dialog title
                fileChooser.setDialogTitle("Select Pattern Script File");
                
                int result = fileChooser.showOpenDialog(null);
                
                if (result == JFileChooser.APPROVE_OPTION)
                {
                    File selectedFile = fileChooser.getSelectedFile();
                    String filePath = selectedFile.getAbsolutePath();
                    callback.accept(filePath);
                }
                else
                {
                    callback.accept(null);
                }
            }
            catch (Exception e)
            {
                Proton.logger.error("Error opening file picker", e);
                callback.accept(null);
            }
        });
    }
}

