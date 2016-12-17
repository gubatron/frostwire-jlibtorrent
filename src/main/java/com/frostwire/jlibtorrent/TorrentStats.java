package com.frostwire.jlibtorrent;

import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentAddedAlert;
import sun.invoke.empty.Empty;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;


public final class TorrentStats {


    private SessionManager sessionManager;
    private TorrentHandle torrentHandle;
    private long samplingIntervalInMs;
    private long maxHistoryInMs;
    public static final String UPLOAD = "UPLOAD";
    public static final String DOWNLOAD = "DOWNLOAD";
    private Queue<Integer> downloadRate = new LinkedList<Integer>();
    private Queue<Integer> uploadRate = new LinkedList<Integer>();
    private long tStart; //for collecting sampling interval time

    public TorrentStats(final SessionManager sessionManager, TorrentHandle torrentHandle, long samplingIntervalInMs, long maxHistoryInMs) {
        this.sessionManager = sessionManager;
        this.torrentHandle = torrentHandle;
        this.samplingIntervalInMs = samplingIntervalInMs;
        this.maxHistoryInMs = maxHistoryInMs;
        this.tStart = System.currentTimeMillis();

        startAlertListener();
    }


    private void startAlertListener() {

        sessionManager.addListener(new AlertListener() {
            @Override
            public int[] types() {
                return null;
            }

            @Override
            public void alert(Alert<?> alert) {
                AlertType type = alert.type();

                switch (type) {

                    case TORRENT_ADDED:
                        System.out.println("Torrent added ");
                        ((TorrentAddedAlert) alert).handle().resume();
                        break;

                    case BLOCK_FINISHED:
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
                        break;

                    case TORRENT_FINISHED:
                        System.out.println("Torrent finished");
                        break;
                }
            }
        });

    }


    //Type = DOWNLOAD / UPLOAD
    //limit = number of download speed samples or less if available
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
                //System.out.println("element: "+element.intValue());
                rateHistory[i] = element.intValue();

                i++;
            }

            return rateHistory;
        }

        //return empty queue is empty
        return new int[10];

    }

    public int[] get(String type, int limit) {
        Queue<Integer> resultQueue = null;
        int[] rateHistory=null;
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
