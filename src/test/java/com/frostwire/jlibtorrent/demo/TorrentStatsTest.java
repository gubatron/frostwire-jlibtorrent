package com.frostwire.jlibtorrent.demo;
import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import java.io.File;
import java.util.concurrent.CountDownLatch;

/**
 * @author haperlot
 *
 * @samplingIntervalInMs the sampling interval time in milliseconds
 * @maxHistoryInMs max history in milliseconds to be tracked
 */
public final class TorrentStatsTest {

    public static void main(String[] args) throws Throwable {

        // comment this line for a real application
        args = new String[]{"/Users/maximiliamgierschmann/Downloads/Honey_Larochelle_Hijack_FrostClick_FrostWire_MP3_May_06_2016.torrent"};
        File torrentFile = new File(args[0]);

        final SessionManager sessionManager = new SessionManager();
        final CountDownLatch signal = new CountDownLatch(1);

        //starting sessionManager & torrent download
        sessionManager.start();
        TorrentInfo ti = new TorrentInfo(torrentFile);
        sessionManager.download(ti, torrentFile.getParentFile());

        //getting the torrentHandle for the TorrentStats tracker
        final TorrentHandle torrentHandle = sessionManager.find(ti.infoHash());


        long samplingIntervalInMs = 500; // 0.5 second
        long maxHistoryInMs = 1 * 60 * 1000; // 1 minutes
        // This creates a new TorrentStatsKeeper instance and all the internal alert listeners necessary
        final TorrentStats stats = sessionManager.trackStats(torrentHandle, samplingIntervalInMs, maxHistoryInMs);


        //declaring listener to check updated values
        sessionManager.addListener(new AlertListener() {
            @Override
            public int[] types() {
                return null;
            }

            @Override
            public void alert(Alert<?> alert) {
                AlertType type = alert.type();

                switch (type) {

                    case BLOCK_FINISHED:
                        // gets all the available upload speed samples (bytes/sec), in this case that'd be <= 600 elements
                        //int[] speedRate = stats.get(TorrentStats.DOWNLOAD);

                        // gets the last 10 available download speed samples or less if less available
                        int[] speedRate = stats.get(TorrentStats.DOWNLOAD, 10);
                        //int[] speedRate = stats.get(TorrentStats.DOWNLOAD, 1);
                        //int[] speedRate = stats.get(TorrentStats.DOWNLOAD, 5);

                        System.out.println("Speeds(bytes/sec)");
                        if (!torrentHandle.status().isFinished())
                            for (int i = 0; i < speedRate.length; i++) System.out.print(speedRate[i] + " ");

                        break;
                }
            }
        });

        //stop sessionManager
        signal.await();
        sessionManager.stop();


    }


}
