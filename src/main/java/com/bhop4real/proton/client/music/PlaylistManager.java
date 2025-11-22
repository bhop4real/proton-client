package com.bhop4real.proton.client.music;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class PlaylistManager {
	private final List<Path> originalTracks = new ArrayList<Path>();
	private final List<Path> orderedTracks = new ArrayList<Path>();
	private PlayOrder playOrder = PlayOrder.DEFAULT;
	private int currentIndex = -1;

	public synchronized void setTracks(final List<Path> tracks) {
		originalTracks.clear();
		originalTracks.addAll(tracks);
		rebuildOrdered();
		currentIndex = orderedTracks.isEmpty() ? -1 : 0;
	}

	public synchronized void setOrder(final PlayOrder order) {
		if (order == null) return;
		if (this.playOrder == order) return;
		this.playOrder = order;
		rebuildOrdered();
		if (!orderedTracks.isEmpty()) {
			currentIndex = Math.max(0, Math.min(currentIndex, orderedTracks.size() - 1));
		} else {
			currentIndex = -1;
		}
	}

	public synchronized PlayOrder getOrder() {
		return playOrder;
	}

	public synchronized List<Path> getOrderedTracksSnapshot() {
		return new ArrayList<Path>(orderedTracks);
	}

	public synchronized int size() {
		return orderedTracks.size();
	}

	public synchronized int getCurrentIndex() {
		return currentIndex;
	}

	public synchronized void setCurrentIndex(final int index) {
		if (index < 0 || index >= orderedTracks.size()) return;
		currentIndex = index;
	}

	public synchronized Path getCurrent() {
		if (currentIndex < 0 || currentIndex >= orderedTracks.size()) return null;
		return orderedTracks.get(currentIndex);
	}

	public synchronized Path getNext() {
		if (orderedTracks.isEmpty()) return null;
		if (playOrder == PlayOrder.RANDOM) {
			final int next = ThreadLocalRandom.current().nextInt(orderedTracks.size());
			currentIndex = next;
			return orderedTracks.get(currentIndex);
		}
		currentIndex++;
		if (currentIndex >= orderedTracks.size()) currentIndex = 0;
		return orderedTracks.get(currentIndex);
	}

	public synchronized Path getPrevious() {
		if (orderedTracks.isEmpty()) return null;
		if (playOrder == PlayOrder.RANDOM) {
			final int prev = ThreadLocalRandom.current().nextInt(orderedTracks.size());
			currentIndex = prev;
			return orderedTracks.get(currentIndex);
		}
		currentIndex--;
		if (currentIndex < 0) currentIndex = orderedTracks.size() - 1;
		return orderedTracks.get(currentIndex);
	}

	private void rebuildOrdered() {
		orderedTracks.clear();
		orderedTracks.addAll(originalTracks);
		if (playOrder == PlayOrder.REVERSE) {
			Collections.reverse(orderedTracks);
		} else if (playOrder == PlayOrder.RANDOM) {
			// Fisher–Yates shuffle
			final Random r = ThreadLocalRandom.current();
			for (int i = orderedTracks.size() - 1; i > 0; i--) {
				int j = r.nextInt(i + 1);
				final Path tmp = orderedTracks.get(i);
				orderedTracks.set(i, orderedTracks.get(j));
				orderedTracks.set(j, tmp);
			}
		}
	}
}


