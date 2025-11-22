package com.bhop4real.proton.client.music;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MusicService {
	private static final MusicService INSTANCE = new MusicService();

	public static MusicService get() {
		return INSTANCE;
	}

	public static final class NowPlayingInfo {
		public final String title;
		public final String author;
		public final long positionMs;
		public final long durationMs;
		public NowPlayingInfo(String title, String author, long positionMs, long durationMs) {
			this.title = title != null ? title : "";
			this.author = author != null ? author : "";
			this.positionMs = Math.max(0L, positionMs);
			this.durationMs = Math.max(0L, durationMs);
		}
	}

	private final PlaylistManager playlistManager = new PlaylistManager();

	private volatile int volume = 50;
	private volatile Clip clip;
	private volatile Path currentPath;
	private volatile boolean paused;
	private final ExecutorService playExecutor = Executors.newSingleThreadExecutor();

	private MusicService() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					shutdown();
				} catch (Throwable ignored) {
				}
			}
		}, "Proton-Music-ShutdownHook"));
	}

	public void loadLocal(final List<Path> files) {
		playlistManager.setTracks(files);
	}

	public void playIndex(final int index) {
		playlistManager.setCurrentIndex(index);
		final Path path = playlistManager.getCurrent();
		if (path != null) {
			asyncLoad(path);
		}
	}

	public void play() {
		if (playlistManager.getCurrent() == null && playlistManager.size() > 0) {
			playlistManager.setCurrentIndex(0);
		}
		final Path path = playlistManager.getCurrent();
		if (path != null) {
			asyncLoad(path);
		}
	}

	public void pause() {
		final Clip c = this.clip;
		if (c != null && c.isOpen() && c.isActive()) {
			long pos = c.getMicrosecondPosition();
			c.stop();
			c.setMicrosecondPosition(pos);
			paused = true;
		}
	}

	public void resume() {
		final Clip c = this.clip;
		if (c != null && c.isOpen() && paused) {
			c.start();
			paused = false;
		}
	}

	public void stop() {
		stopClip();
		paused = false;
	}

	public void next() {
		final Path next = playlistManager.getNext();
		if (next != null) {
			asyncLoad(next);
		}
	}

	public void previous() {
		final Path prev = playlistManager.getPrevious();
		if (prev != null) {
			asyncLoad(prev);
		}
	}

	public void setOrder(final PlayOrder order) {
		playlistManager.setOrder(order);
	}

	public PlayOrder getOrder() {
		return playlistManager.getOrder();
	}

	public void setVolume(final int vol) {
		final int clamped = Math.max(0, Math.min(100, vol));
		this.volume = clamped;
		applyVolume();
	}

	public int getVolume() {
		return volume;
	}

	public void seek(final long ms) {
		final Clip c = this.clip;
		if (c != null && c.isOpen()) {
			long targetUs = Math.max(0L, Math.min(ms, getDurationMsInternal())) * 1000L;
			try {
				c.setMicrosecondPosition(targetUs);
			} catch (Exception ignored) {
			}
		}
	}

	void playNextFromScheduler() {
		// Not used in this simplified implementation; handled by LineListener
	}

	private void loadAndPlay(final Path path) {
		stop();
		this.currentPath = path;
		playWithClip(path);
	}

	private void playWithClip(final Path path) {
		Clip newClip = null;
		AudioInputStream ais = null;
		try {
			BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path));
			ais = AudioSystem.getAudioInputStream(bis);
			AudioFormat baseFormat = ais.getFormat();
			AudioFormat decoded = toPcmSignedStereo16(baseFormat);
			if (!baseFormat.matches(decoded)) {
				ais = AudioSystem.getAudioInputStream(decoded, ais);
			}
			newClip = AudioSystem.getClip();
			newClip.open(ais);
			final Clip clipRef = newClip;
			newClip.addLineListener(new LineListener() {
				@Override
				public void update(LineEvent event) {
					if (event.getType() == LineEvent.Type.STOP) {
						long pos = clipRef.getMicrosecondPosition();
						long len = clipRef.getMicrosecondLength();
						boolean ended = Math.abs(len - pos) <= 15000L || pos >= len;
						if (ended) {
							try {
								clipRef.close();
							} catch (Exception ignored) {
							}
							tryPlayNextDistinctAsync();
						}
					}
				}
			});
			this.clip = newClip;
			this.paused = false;
			applyVolume();
			newClip.start();
		} catch (Throwable ignored) {
			// Skip on any issue
			tryPlayNextDistinctAsync();
		}
	}

	private void stopClip() {
		final Clip c = this.clip;
		if (c != null) {
			try {
				c.stop();
			} catch (Exception ignored) {
			}
			try {
				c.close();
			} catch (Exception ignored) {
			}
		}
		this.clip = null;
	}

	private AudioFormat toPcmSignedStereo16(AudioFormat in) {
		// Ensure JavaSound-friendly format
		int channels = Math.max(1, in.getChannels());
		// Keep channel count; do not force stereo if source is mono
		return new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				in.getSampleRate(),
				16,
				channels,
				channels * 2,
				in.getSampleRate(),
				false
		);
	}

	private void applyVolume() {
		final Clip c = this.clip;
		if (c == null) return;
		try {
			FloatControl gain = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
			// Map 0-100 to control range in dB
			float volLinear = Math.max(0.0f, Math.min(1.0f, volume / 100.0f));
			float min = gain.getMinimum();
			float max = gain.getMaximum();
			// Avoid -Inf at zero: map [0..1] to [min..max] logarithmically but clamp
			float dB;
			if (volLinear <= 0.001f) {
				dB = min;
			} else {
				// Standard conversion to dB
				dB = (float)(20.0 * Math.log10(volLinear));
				if (Float.isNaN(dB) || dB < min) dB = min;
				if (dB > max) dB = max;
			}
			gain.setValue(dB);
		} catch (IllegalArgumentException ignored) {
			// Control not supported; ignore
		}
	}

	public void shutdown() {
		try {
			stop();
		} catch (Exception ignored) {
		}
		try {
			playExecutor.shutdownNow();
		} catch (Exception ignored) {
		}
	}

	// Visualization / Info hooks
	public NowPlayingInfo getNowPlaying() {
		final String title = currentPath != null ? currentPath.getFileName().toString() : "";
		final Clip c = this.clip;
		if (c != null && c.isOpen()) {
			return new NowPlayingInfo(
					title,
					"",
					c.getMicrosecondPosition() / 1000L,
					c.getMicrosecondLength() / 1000L
			);
		}
		return new NowPlayingInfo(title, "", 0L, 0L);
	}

	public float[] getLastRmsStereo() {
		// Not computed in simplified path
		return new float[] { 0.0f, 0.0f };
	}

	private long getDurationMsInternal() {
		final Clip c = this.clip;
		if (c == null || !c.isOpen()) return 0L;
		return c.getMicrosecondLength() / 1000L;
	}

	private void tryPlayNextDistinctAsync() {
		final Path nxt = playlistManager.getNext();
		if (nxt == null) return;
		if (isSameTrack(nxt, currentPath)) return;
		asyncLoad(nxt);
	}

	private boolean isSameTrack(Path a, Path b) {
		if (a == null || b == null) return false;
		try {
			return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
		} catch (Throwable ignored) {
			return a.equals(b);
		}
	}

	private void asyncLoad(final Path path) {
		try {
			playExecutor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						loadAndPlay(path);
					} catch (Throwable ignored) {
					}
				}
			});
		} catch (Throwable ignored) {
		}
	}

	// MP3SPI path needs no special handling here; AudioSystem will decode MP3.
}


