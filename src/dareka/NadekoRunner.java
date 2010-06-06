package dareka;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

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

    public void setText(String s) {
        textArea.setText(messageBuffer + s);
        messageBuffer = textArea.getText();
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

    public Boolean isRunning() {
        return this.isRunning;
    }
    public Boolean isAlive() {
        return this.isAlive;
    }
}
