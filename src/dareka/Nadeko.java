package dareka;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import org.ho.yaml.Yaml;

public class Nadeko {
    private static final String AppName = "Nadeko";
    private static final String Version = "0.01";
    private static final String defaultConfFile = "config.yaml";
    private static final String iconPath = "img";
    private static final String execIcon = "exec.png";
    private String trayIcon = "tray.png";
    private JFrame frame;
    private JTabbedPane tabPane;
    private NadekoRunner[] processes;

    private static final String WINDOWS = "Windows";
    private static final String LINUX   = "Linux";
    private static final String MAC     = "Mac";
    private Map<String, String> ImagePath = new HashMap<String, String>() {
        {
            put(WINDOWS, "win");
            put(LINUX, "linux");
            put(MAC, "mac");
        }
    };

    public static void main(String[] args) throws Exception {
        final String confFile = args.length > 0 ? args[0] : defaultConfFile;
        Map conf = loadConfig(confFile);

        Nadeko nadeko = new Nadeko();
        nadeko.createWindowAndTrayIcon(conf);
    }

    @SuppressWarnings("unchecked")
    private static Map loadConfig(String confFile) throws UnsupportedEncodingException, FileNotFoundException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(confFile), "UTF-8"));
        return (Map)Yaml.load(reader);
    }

    private void createWindowAndTrayIcon(Map conf) throws IOException, AWTException {
        this.tabPane = new JTabbedPane();
        List executes =(List)conf.get("executes");
        processes = new NadekoRunner[executes.size()];
        for (int i = 0; i < executes.size(); i++) {
            Map exe = (Map)executes.get(i);
            String name = (String)exe.get("name");
            String dir = (String)exe.get("dir");
            List cmd = (List)exe.get("cmd");
            String[] command = new String[cmd.size()];
            for (int j = 0; j < cmd.size(); j++) {
                command[j] = (String)cmd.get(j);
            }
            JTextArea textArea = new JTextArea();
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(false);

            JScrollPane scrollpane = new JScrollPane(textArea);
            scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            tabPane.addTab(name, scrollpane);
            tabPane.setTabComponentAt(i, new JLabel(name, new ImageIcon(readIconImage(getIconPath(execIcon))), JLabel.TRAILING));

            NadekoRunner runner = new NadekoRunner(dir, command, textArea);
            runner.start();
            processes[i] = runner;

            addRightClickMenu(i, runner);
            recoveryProcess(runner);
        }

        this.frame = new JFrame(AppName);
        this.frame.getContentPane().add(tabPane, BorderLayout.CENTER);

        String trayIconPath = getTrayIconPath();

        this.frame.setSize(600, 300);
        this.frame.setIconImage(readIconImage(trayIconPath));
        this.frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                frame.setVisible(false);
            }
        });

        this.frame.setLocationRelativeTo(null);

        createTrayIcon();
    }

    private String getIconPath(String iconName) {
        return iconPath + "/" + iconName;
    }

    private String getTrayIconPath() {
        String osName = System.getProperty("os.name");
        String basePath;
        if(osName.indexOf(WINDOWS) >= 0) {
            basePath = ImagePath.get(WINDOWS);
        }
        else if(osName.indexOf(LINUX) >= 0) {
            basePath = ImagePath.get(LINUX);
        }
        else if(osName.indexOf(MAC) >= 0) {
            basePath = ImagePath.get(MAC);
        }
        else {
            basePath = ImagePath.get(WINDOWS);
        }
        return getIconPath(basePath + "/" + trayIcon);
    }

    private void addRightClickMenu(final int index, final NadekoRunner runner) {
        final JPopupMenu popup = new JPopupMenu();

        JMenuItem startMI = new JMenuItem("Start");
        startMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (runner.isRunning()) return;
                runner.start();
            }
        });

        JMenuItem stopMI = new JMenuItem("Stop");
        stopMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (runner.isRunning()) {
                    runner.kill();
                }
            }
        });

        JMenuItem resetMI = new JMenuItem("Reset");
        resetMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runner.kill();
                runner.clear();
                runner.start();
            }
        });

        JMenuItem clearMI = new JMenuItem("Clear");
        clearMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runner.clear();
            }
        });

        popup.add(startMI);
        popup.add(stopMI);
        popup.add(resetMI);
        popup.addSeparator();
        popup.add(clearMI);

        tabPane.getTabComponentAt(index).addMouseListener(new MouseListener() {
            @Override
            public void mouseReleased(MouseEvent e) {
                tabPane.setSelectedIndex(index);
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (e.isPopupTrigger()) {
                        popup.show(e.getComponent(), e.getX() + 10, e.getY());
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                tabPane.setSelectedIndex(index);
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (e.isPopupTrigger()) {
                        popup.show(e.getComponent(), e.getX() + 10, e.getY());
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseClicked(MouseEvent e) {}
        });
    }

    private void createTrayIcon() throws IOException, AWTException {
        TrayIcon trayIcon = new TrayIcon(this.frame.getIconImage(), AppName);
        trayIcon.addActionListener(active());

        addPopupMenu(trayIcon);
        SystemTray.getSystemTray().add(trayIcon);
    }

    private ActionListener active() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setExtendedState(JFrame.NORMAL);
                frame.setAlwaysOnTop(true);
                frame.setVisible(true);
                frame.setAlwaysOnTop(false);
            }
        };
    }

    private Image readIconImage(String iconPath) throws IOException {
        return ImageIO.read(getClass().getResourceAsStream(iconPath));
    }

    private void addPopupMenu(TrayIcon icon) {
        PopupMenu pmenu = new PopupMenu();

        MenuItem viewMI = new MenuItem("Open");
        viewMI.addActionListener(active());

        MenuItem exitMI = new MenuItem("Exit");
        exitMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                killProcess();
                System.exit(0);
            }
        });

        MenuItem aboutMI = new MenuItem("About");
        aboutMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message =
                      "<html>"
                    + "<body>"
                    + "<h1 style='text-align: center'>" + AppName + "</h1>"
                    + "<table>"
                    + "<tr><th style='text-align: left'>Version</th><td>" + Version + "</td></tr>"
                    + "<tr><th style='text-align: left'>Author</th><td>xaicron</td></tr>"
                    + "<tr><th style='text-align: left'>Visit</th><td><a href='http://blog.livedoor.jp/xaicron/'>http://blog.livedoor.jp/xaicron/</a>"
                    + "</table>"
                    + "</body>"
                    + "</html>"
                ;

                JEditorPane text = new JEditorPane("text/html", message);
                text.setEditable(false);
                text.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
                text.setFont(new JLabel().getFont());

                text.addHyperlinkListener(new HyperlinkListener() {
                    @Override
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if (e.getEventType() == EventType.ACTIVATED) {
                            URL url = e.getURL();
                            Desktop dp = Desktop.getDesktop();
                            try {
                                dp.browse(url.toURI());
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            } catch (URISyntaxException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });

                JFrame f = new JFrame("About");
                f.getContentPane().add(text, BorderLayout.CENTER);
                f.setSize(250, 160);
                f.setBackground(Color.white);
                f.setAlwaysOnTop(true);
                f.setIconImage(frame.getIconImage());
                f.setLocationRelativeTo(null);
                f.setResizable(false);
                f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                f.setVisible(true);
            }
        });

        pmenu.add(viewMI);
        pmenu.addSeparator();
        pmenu.add(aboutMI);
        pmenu.addSeparator();
        pmenu.add(exitMI);
        icon.setPopupMenu(pmenu);
    }

    private void killProcess() {
        for (int i = 0; i < processes.length; i++) {
            processes[i].kill();
        }
    }

    private void recoveryProcess(final NadekoRunner runner) {
        new Thread() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!runner.isAlive()) {
                        runner.setText("*** Recovery...");
                        runner.start();
                        runner.setText("done.\n");
                    }
                }
            }
        }.start();
    }
}
