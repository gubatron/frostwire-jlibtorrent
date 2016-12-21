package com.frostwire.jlibtorrent;

import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.TorrentAlert;

import java.security.InvalidParameterException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * It will start an alertEvent listener to keep track of the download
 * and upload Speed (bytes/sec). The granularity selected will depend
 * on the speed in which the results of the speed of the Alert event
 * listener is retrieved, which can have a delay in milliseconds.
 *
 * @author haperlot
 */

public final class TorrentStats {

    private final SessionManager sessionManager;
    private final TorrentHandle torrentHandle;
    private final long samplingIntervalInMs;
    private final int[] TYPES;
    private final int MAX_SAMPLES;
    private final Queue<Sample> samples;
    private long tStart; //for collecting sampling interval time

    public enum Metric {
        UploadRate,
        DownloadRate
    }

    private class Sample {
        final int downloadRate;
        final int uploadRate;

        Sample(int downloadRate, int uploadRate) {
            this.downloadRate = downloadRate;
            this.uploadRate = uploadRate;
        }
    }


    public TorrentStats(final SessionManager sessionManager, TorrentHandle torrentHandle, long samplingIntervalInMs, long maxHistoryInMs) {
        this.sessionManager = sessionManager;
        this.torrentHandle = torrentHandle;
        this.samplingIntervalInMs = samplingIntervalInMs;
        this.tStart = System.currentTimeMillis();
        this.MAX_SAMPLES = (int) (maxHistoryInMs / samplingIntervalInMs);
        this.TYPES = new int[] { AlertType.STATS.swig() };
        this.samples = new LinkedList<>();
        startAlertListener();
    }

    /**
     * It will start the alert event listener, as soon as the sampling interval
     * meets the requested time it will save the values on queues. If the queues
     * reach max size denoted by the maxHistory time, they will poll() the head
     * element, in our case the oldest inserted.
     */
    public void startAlertListener() {
        sessionManager.addListener(new AlertListener() {
            @Override
            public int[] types() {
                return TYPES;
            }

            @Override
            public void alert(Alert<?> alert) {

                //if not eq to the torrentHandle return.
                if (!((TorrentAlert<?>) alert).handle().swig().op_eq(torrentHandle.swig())) {
                    return;
                }

                //if !paused, it will take both stats when uploading / downloading
                final TorrentStatus status = torrentHandle.status();
                if (status != null && !status.isPaused()) {

                    //only true if samplingIntervalInMs
                    if ((System.currentTimeMillis() - tStart) >= samplingIntervalInMs) {
                        tStart = System.currentTimeMillis();

                        //add stats
                        samples.add(new Sample(status.downloadRate(), status.uploadRate()));

                        //if the sampling # exceeded the limit, remove. It keeps max size.
                        while (MAX_SAMPLES < samples.size()) {
                            samples.poll();
                        }
                    }

                }
            }
        });
    }

    /**
     * Will return all available items on the queue, depending on the type selected
     * @param type of metric data to retrieved
     * @return all the elements tracked by the event listener limited by the maxHistory
     *
     * NOTE: since you have to do java acrobatics to create a T[] and native datatypes can't
     * be considered as generics, I'm naming this on purpose like this as a hint for future
     * getFloatSamples, getBooleanSamples, getLongSamples methods.
     */

    public int[] getIntSamples(Metric type) {
        if (type != Metric.DownloadRate && type != Metric.UploadRate) {
            throw new InvalidParameterException("TorrentStats.getIntSamples("+type.toString()+"). Invalid metric type passed, it is not a metric that tracks int samples.");
        }

        if (!samples.isEmpty()) {
            final Iterator<Sample> iterator = samples.iterator();
            int[] results = new int[Math.min(MAX_SAMPLES, samples.size())];
            int i=0;
            while (iterator.hasNext() || i < MAX_SAMPLES) {
                Sample sample = iterator.next();
                switch (type) {
                    case DownloadRate:
                        results[i] = sample.downloadRate;
                        break;
                    case UploadRate:
                        results[i] = sample.uploadRate;
                        break;
                }
                i++;
            }
            return results;
        }
        return new int[0];
    }
}