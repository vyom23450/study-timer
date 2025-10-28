# study-timer
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class StudyTimer extends JFrame {
    private JTextField subjectField;
    private JLabel timerLabel;
    private JButton startButton, stopButton;
    private JTextArea sessionArea, summaryArea;
    private long startTime;
    private Timer timer;
    private java.util.List<Session> sessions = new ArrayList<>();
    public StudyTimer() {
        setTitle("Study Session Timer");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        // --- Top panel ---
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(3, 1, 5, 5));
        subjectField = new JTextField();
        subjectField.setToolTipText("Enter subject (e.g., Math)");
        timerLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 36));
        JPanel buttonPanel = new JPanel();
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        topPanel.add(subjectField);
        topPanel.add(timerLabel);
        topPanel.add(buttonPanel);
        add(topPanel, BorderLayout.NORTH);
        // --- Center panel for sessions ---
        sessionArea = new JTextArea();
        sessionArea.setEditable(false);
        sessionArea.setBorder(BorderFactory.createTitledBorder("Past Sessions"));
        add(new JScrollPane(sessionArea), BorderLayout.CENTER);
        // --- Bottom panel for summary ---
        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setBorder(BorderFactory.createTitledBorder("This Week's Summary"));
        add(new JScrollPane(summaryArea), BorderLayout.SOUTH);
        // --- Button actions ---
        startButton.addActionListener(e -> startSession());
        stopButton.addActionListener(e -> stopSession());
        loadSessionsFromFile();
        updateSessionDisplay();
        updateSummary();
    }
    private void startSession() {
        String subject = subjectField.getText().trim();
        if (subject.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a subject!");
            return;
        }
        startTime = System.currentTimeMillis();
        timer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                long elapsed = System.currentTimeMillis() - startTime;
                timerLabel.setText(formatTime(elapsed));
            }
        });
        timer.start();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        subjectField.setEnabled(false);
    }
    private void stopSession() {
        if (timer != null) timer.stop();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        String subject = subjectField.getText().trim();
        Session s = new Session(subject, duration, new Date());
        sessions.add(s);
        saveSessionsToFile();
        subjectField.setText("");
        subjectField.setEnabled(true);
        timerLabel.setText("00:00:00");
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        updateSessionDisplay();
        updateSummary();
    }
    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    private void updateSessionDisplay() {
        sessionArea.setText("");
        for (int i = sessions.size() - 1; i >= 0; i--) {
            Session s = sessions.get(i);
            sessionArea.append(s.toString() + "\n");
        }
    }
    private void updateSummary() {
        Map<String, Long> totals = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        Date oneWeekAgo = cal.getTime();
        for (Session s : sessions) {
            if (s.date.after(oneWeekAgo)) {
                totals.put(s.subject, totals.getOrDefault(s.subject, 0L) + s.duration);
            }
        }
        summaryArea.setText("");
        for (String subject : totals.keySet()) {
            double hours = totals.get(subject) / 3600000.0;
            summaryArea.append(subject + ": " + String.format("%.2f", hours) + " hrs this week\n");
        }
    }
    private void saveSessionsToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("sessions.txt"))) {
            for (Session s : sessions) {
                pw.println(s.subject + "," + s.duration + "," + s.date.getTime());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void loadSessionsFromFile() {
        File file = new File("sessions.txt");
        if (!file.exists()) return;
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String[] parts = sc.nextLine().split(",");
                if (parts.length == 3) {
                    String subject = parts[0];
                    long duration = Long.parseLong(parts[1]);
                    Date date = new Date(Long.parseLong(parts[2]));
                    sessions.add(new Session(subject, duration, date));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static class Session {
        String subject;
        long duration;
        Date date;
        Session(String subject, long duration, Date date) {
            this.subject = subject;
            this.duration = duration;
            this.date = date;
        }
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
            long minutes = Math.round(duration / 60000.0);
            return subject + " — " + minutes + " min on " + sdf.format(date);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new StudyTimer().setVisible(true);
        });
    }
}
