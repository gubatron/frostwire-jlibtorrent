package com.frostwire.jlibtorrent;
import com.frostwire.jlibtorrent.alerts.Alert;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * It will start an alertEvent listener to keep track of the download
 * and upload Speed (bytes/sec). The granularity selected will depend
 * on the speed in which the results of the speed of the Alert event
 * listener is retrieved, which can have a delay in milliseconds.
 * @author haperlot
 */

public final class TorrentStats {

    private SessionManager sessionManager;
    private TorrentHandle torrentHandle;
    private long samplingIntervalInMs;
    private long maxHistoryInMs;
    public static final String UPLOAD = "UPLOAD";
    public static final String DOWNLOAD = "DOWNLOAD";
    private Queue<Integer> downloadRate = new LinkedList<>();
    private Queue<Integer> uploadRate = new LinkedList<>();
    private long tStart; //for collecting sampling interval time

    public TorrentStats(final SessionManager sessionManager, TorrentHandle torrentHandle, long samplingIntervalInMs, long maxHistoryInMs) {
        this.sessionManager = sessionManager;
        this.torrentHandle = torrentHandle;
        this.samplingIntervalInMs = samplingIntervalInMs;
        this.maxHistoryInMs = maxHistoryInMs;
        this.tStart = System.currentTimeMillis();
        startAlertListener();
    }

    /**
     * It will start the alert event listener, as soon as the sampling interval
     * meets the requested time it will save the values on queues. If the queues
     * reach max size denoted by the maxHistory time, they will poll() the head
     * element, in our case the oldest inserted.
     */
    private void startAlertListener() {

        sessionManager.addListener(new AlertListener() {
            @Override
            public int[] types() {
                int[] a = new int[1];
                a[0] = 30; //getting BLOCK_FINISHED
                return a;
            }

            @Override
            public void alert(Alert<?> alert) {
                //only true if samplingIntervalInMs,as soon as I get it from the alert margin of error 5-90 ms.
                if ((System.currentTimeMillis() - tStart) >= samplingIntervalInMs) {
                    tStart = System.currentTimeMillis();

                    //if !paused && !finished, it could be paused during the download
                    if (!torrentHandle.status().isFinished() && !torrentHandle.status().isPaused()) {
                        
                        downloadRate.add(torrentHandle.status().downloadRate());
                        uploadRate.add(torrentHandle.status().uploadRate());

                        //if the sampling # exceeded the limit, remove head
                        if (downloadRate.size() * samplingIntervalInMs > maxHistoryInMs) {
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
     * @param type type of speed (bytes/sec) data to retrieved
     * @return all the elements tracked by the event listener limited by the maxHistory
     */

    public int[] get(String type) {
        Queue<Integer> resultQueue = null;
        int i;

        if (type.compareToIgnoreCase(this.UPLOAD) == 0)
            resultQueue = uploadRate;
        else if (type.compareToIgnoreCase(this.DOWNLOAD) == 0)
            resultQueue = uploadRate;

        if (!resultQueue.isEmpty()) {
            int[] rateHistory = new int[resultQueue.size()];
            //returning available items
            i = 0;
            for (Integer element : resultQueue) {
                rateHistory[i] = element.intValue();
                i++;
            }
            return rateHistory;
        }
        //return empty queue is empty
        return new int[10];
    }

    /**
     * Will return all available items on the queue, depending on the type selected.
     * It will also limit the result returned.
     * @param type  type of speed (bytes/sec) data to retrieve
     * @param limit number of results to retrieve
     * @return all the elements tracked by the event listener limited by the maxHistory
     */

    public int[] get(String type, int limit) {
        Queue<Integer> resultQueue = null;
        int[] rateHistory = null;
        int i, j;

        if (type.compareToIgnoreCase(this.UPLOAD) == 0)
            resultQueue = uploadRate;
        else if (type.compareToIgnoreCase(this.DOWNLOAD) == 0)
            resultQueue = uploadRate;

        if (!resultQueue.isEmpty()) {

            //returning available items if limit > itemList
            if (limit >= resultQueue.size()) {
                rateHistory = this.get(type);

            } else if (limit < resultQueue.size()) {
                rateHistory = new int[limit];
                i = j = 0;

                for (Integer element : resultQueue) {
                    if (j >= resultQueue.size() - limit) {
                        rateHistory[i] = element.intValue();
                        i++;
                    }
                    j++;
                }
            }
            return rateHistory;
        }
        //return empty queue is empty
        return new int[limit];
    }
}