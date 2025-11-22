package com.bhop4real.proton.client.music;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LocalLibraryScanner {
	private static final String[] SUPPORTED_EXT = new String[] { "wav", "mp3" };

	public List<Path> scan(final Path root) {
		final List<Path> result = new ArrayList<Path>();
		if (root == null) return result;
		if (!Files.exists(root)) return result;
		try {
			Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if (attrs.isRegularFile() && isSupported(file)) {
						result.add(file);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException ignored) {
		}
		return result;
	}

	private boolean isSupported(final Path file) {
		final String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
		for (final String ext : SUPPORTED_EXT) {
			if (name.endsWith("." + ext)) return true;
		}
		return false;
	}
}


