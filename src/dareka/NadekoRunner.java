package dareka;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class NadekoRunner {
    private String dir;
    private String[] cmd;
    private JTextArea textArea;
    private Process process;
    private String messageBuffer = "";
    private Boolean isRunning = true;
    private Boolean isAlive = true;
    private String logFile;

    public NadekoRunner(String dir, String[] cmd, JTextArea textArea) {
        this.cmd = cmd;
        this.dir = dir;
        this.textArea = textArea;
    }

    public void start() {
        new Thread() {
            @Override
            public void run() {
                try {
                    process = Runtime.getRuntime().exec(cmd, null, new File(dir));
                    isRunning = true;
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
                writeLog(new BufferedReader(new InputStreamReader(process.getInputStream())));
                writeLog(new BufferedReader(new InputStreamReader(process.getErrorStream())));
                monitor();
            }
        }.start();
    }

    private void writeLog(final BufferedReader br) {
        new Thread() {
            @Override
            public void run() {
                try {
                    String line;
                    while ((line = br.readLine()) != null) {
                        final String s = line ;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                setText(s + "\n");
                            }
                        });
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void monitor() {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (isRunning()) {
                        try {
                            process.exitValue();
                            isAlive = false;
                        }
                        catch (Exception e) {
                            isAlive = true;
                        }
                        finally {
                            if (!isAlive) {
                                break;
                            }
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public BufferedWriter getLogWriter() {
        if (logFile == null) return null;

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_froamtLogFileName(logFile), true)));
        }
        catch (FileNotFoundException e) {
            logFile = null;
            e.printStackTrace();
        }
        return writer;
    }

    private String _froamtLogFileName(String logFile) {
        Date now = new Date();
        SimpleDateFormat date = new SimpleDateFormat();
        return dir + '/' + logFile
            .replaceAll("%Y", __dateFormat(date, now, "yyyy"))
            .replace("%M", __dateFormat(date, now, "MM"))
            .replaceAll("%D", __dateFormat(date, now, "dd"));
    }

    private String __dateFormat(SimpleDateFormat dateFormat, Date date, String pattern) {
        dateFormat.applyPattern(pattern);
        return dateFormat.format(date);
    }

    public void setText(String s) {
        textArea.setText(messageBuffer + s);
        BufferedWriter writer = getLogWriter();
        if (writer != null) {
            try {
                logging(writer, s);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        messageBuffer = textArea.getText();
    }

    private void logging(BufferedWriter writer, String buff) throws IOException {
        SimpleDateFormat dateForamt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        writer.write("[" + dateForamt.format(new Date()) + "] " + buff);
        writer.flush();
        writer.close();
    }

    public void kill() {
        setText("*** Process Stopping...");
        process.destroy();

        try {
            process.waitFor();
            this.isRunning = false;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        setText("done\n");
    }

    public void clear() {
        messageBuffer = "";
        textArea.setText("");
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public Boolean isRunning() {
        return this.isRunning;
    }
    public Boolean isAlive() {
        return this.isAlive;
    }
}
