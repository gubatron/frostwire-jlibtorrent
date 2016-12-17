package com.frostwire.jlibtorrent;

import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentAddedAlert;

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

    //for collecting sampling interval time
    private long tStart;

    public TorrentStats(final SessionManager sessionManager, TorrentHandle torrentHandle, long samplingIntervalInMs, long maxHistoryInMs) {
        this.sessionManager = sessionManager;
        this.torrentHandle = torrentHandle;
        this.samplingIntervalInMs = samplingIntervalInMs;
        this.maxHistoryInMs = maxHistoryInMs;
        tStart = System.currentTimeMillis();

        //start internal alert listener
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
                        //only true if samplingIntervalInMs, margin of error 5-90 ms
                        if ((System.currentTimeMillis() - tStart) >= samplingIntervalInMs) {
                            tStart = System.currentTimeMillis();

                            BlockFinishedAlert a = (BlockFinishedAlert) alert;
                            int p = (int) (a.handle().status().progress() * 100);

                            //if !paused && !finished
                            if (!torrentHandle.status().isFinished() && !torrentHandle.status().isPaused()) {

                                downloadRate.add(torrentHandle.status().downloadRate());
                                uploadRate.add(torrentHandle.status().uploadRate());


                                System.out.println("Sampled time: " + downloadRate.size() * samplingIntervalInMs + " / maxHistory: "+ maxHistoryInMs);
                                //if the sampling # exceeded the limit
                                if(downloadRate.size() * samplingIntervalInMs > maxHistoryInMs){
                                    downloadRate.poll();
                                    uploadRate.poll();
                                }

                                System.out.println("#of elements = "+ downloadRate.size());

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
    public int[] get(String type, int limit) {

        return new int[1];
    }


}
