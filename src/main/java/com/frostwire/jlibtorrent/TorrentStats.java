package com.frostwire.jlibtorrent;

import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.TorrentAlert;

import java.util.Arrays;
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
    private final int[] STATS = {AlertType.STATS.swig()};
    private final long MAX_SAMPLES;
    private final Queue<Integer> downloadRate = new LinkedList<>();
    private final Queue<Integer> uploadRate = new LinkedList<>();
    private long tStart; //for collecting sampling interval time

    public enum Metric {
        UploadRate,
        DownloadRate
    }

    public TorrentStats(final SessionManager sessionManager, TorrentHandle torrentHandle, long samplingIntervalInMs, long maxHistoryInMs) {
        this.sessionManager = sessionManager;
        this.torrentHandle = torrentHandle;
        this.samplingIntervalInMs = samplingIntervalInMs;
        this.tStart = System.currentTimeMillis();
        this.MAX_SAMPLES = maxHistoryInMs / samplingIntervalInMs;
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
                return STATS;
            }

            @Override
            public void alert(Alert<?> alert) {

                //if not eq to the torrentHandle return.
                if (!((TorrentAlert<?>) alert).handle().swig().op_eq(torrentHandle.swig())) {
                    return;
                }

                //if !paused, it will take both stats when uploading / downloading
                if (!torrentHandle.status().isPaused()) {

                    //only true if samplingIntervalInMs
                    if ((System.currentTimeMillis() - tStart) >= samplingIntervalInMs) {
                        tStart = System.currentTimeMillis();

                        //add stats
                        downloadRate.add(torrentHandle.status().downloadRate());
                        uploadRate.add(torrentHandle.status().uploadRate());

                        //if the sampling # exceeded the limit, remove. It keeps max size.
                        while (MAX_SAMPLES < downloadRate.size()) {
                            downloadRate.poll();
                            uploadRate.poll();
                        }
                    }

                }
            }
        });
    }

    /**
     * Will return all available items on the queue, depending on the type selected
     *
     * @param type  type of speed (bytes/sec) data to retrieved
     * @param limit optional, to limit the result set
     * @return all the elements tracked by the event listener limited by the maxHistory
     */

    public int[] get(Metric type, Integer... limit) {

        Queue<Integer> resultQueue = null;
        int numberElements = 0;

        if (limit.length >= 1) {
            numberElements = limit[0];
        }

        switch (type) {
            case UploadRate:
                resultQueue = uploadRate;
                break;

            case DownloadRate:
                resultQueue = downloadRate;
                break;
        }

        if (!resultQueue.isEmpty()) {
            if (numberElements == 0) {
                return toIntArray(resultQueue);
            } else {
                if (numberElements >= MAX_SAMPLES || numberElements >= resultQueue.size()) {
                    return toIntArray(resultQueue);
                } else {
                    int[] i = toIntArray(resultQueue);
                    return Arrays.copyOfRange(i, i.length - numberElements, i.length);
                }
            }

        }
        return new int[0];
    }

    /**
     * Helper function that converts Integer[] to int[]
     *
     * @param queue<Integer>
     * @return array of int[]
     */

    int[] toIntArray(Queue<Integer> queue) {

        int[] con = new int[queue.size()];
        int i = 0;
        for (Integer e : queue) {
            con[i++] = e.intValue();
        }
        return con;
    }
}