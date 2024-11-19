import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;

public class CalendarApp extends JFrame {

    private JLabel monthLabel;
    private JButton[] dayButtons;
    private JComboBox<Integer> yearComboBox;
    private int currentMonth;
    private int currentYear;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/jaydb?password=1409";
    private static final String DB_USER = "Jaysurieya";
    private static final String DB_PASSWORD = "1409";

    public CalendarApp() {
        setTitle("Simple Calendar Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 400);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(headerPanel, BorderLayout.NORTH);

        JButton prevButton = new JButton("<");
        JButton nextButton = new JButton(">");
        monthLabel = new JLabel("", JLabel.CENTER);
        monthLabel.setFont(new Font("Arial", Font.BOLD, 24));

        yearComboBox = new JComboBox<>();
        for (int i = 1900; i <= 2100; i++) {
            yearComboBox.addItem(i);
        }
        yearComboBox.addActionListener(new ActionListener() {
           
            public void actionPerformed(ActionEvent e) {
                currentYear = (int) yearComboBox.getSelectedItem();
                updateCalendar(currentMonth, currentYear);
            }
        });

        headerPanel.add(prevButton, BorderLayout.WEST);
        headerPanel.add(monthLabel, BorderLayout.CENTER);
        headerPanel.add(nextButton, BorderLayout.EAST);
        headerPanel.add(yearComboBox, BorderLayout.SOUTH);

        JPanel daysPanel = new JPanel();
        daysPanel.setLayout(new GridLayout(7, 7));
        add(daysPanel, BorderLayout.CENTER);

        String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : daysOfWeek) {
            JLabel dayLabel = new JLabel(day, JLabel.CENTER);
            dayLabel.setFont(new Font("Arial", Font.BOLD, 12));
            daysPanel.add(dayLabel);
        }

        dayButtons = new JButton[42];
        for (int i = 0; i < 42; i++) {
            dayButtons[i] = new JButton();
            dayButtons[i].setFont(new Font("Arial", Font.PLAIN, 12));
            daysPanel.add(dayButtons[i]);
        }

        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH);
        currentYear = cal.get(Calendar.YEAR);
        yearComboBox.setSelectedItem(currentYear);

        updateCalendar(currentMonth, currentYear);

        prevButton.addActionListener(new ActionListener() {
           
            public void actionPerformed(ActionEvent e) {
                currentMonth--;
                if (currentMonth < 0) {
                    currentMonth = 11;
                    currentYear--;
                    yearComboBox.setSelectedItem(currentYear);
                }
                updateCalendar(currentMonth, currentYear);
            }
        });

        nextButton.addActionListener(new ActionListener() {
           
            public void actionPerformed(ActionEvent e) {
                currentMonth++;
                if (currentMonth > 11) {
                    currentMonth = 0;
                    currentYear++;
                    yearComboBox.setSelectedItem(currentYear);
                }
                updateCalendar(currentMonth, currentYear);
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void updateCalendar(int month, int year) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        monthLabel.setText(months[month] + " " + year);

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        int startDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int numberOfDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < 42; i++) {
            dayButtons[i].setText("");
            dayButtons[i].setEnabled(false);

            for (ActionListener al : dayButtons[i].getActionListeners()) {
                dayButtons[i].removeActionListener(al);
            }
        }

        for (int i = 0; i < numberOfDays; i++) {
            final int day = i + 1;
            dayButtons[i + startDay].setText(String.valueOf(day));
            dayButtons[i + startDay].setEnabled(true);

            dayButtons[i + startDay].addActionListener(new ActionListener() {
               
                public void actionPerformed(ActionEvent e) {
                    showTaskDialog(year, month, day);
                }
            });
        }
    }

    private void showTaskDialog(int year, int month, int day) {
        ArrayList<String> tasks = getTasksFromDatabase(year, month, day);
        JList<String> taskList = new JList<>(tasks.toArray(new String[0]));
        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.setPreferredSize(new Dimension(200, 100));

        JTextField taskField = new JTextField();
        Object[] message = {
                "Tasks for " + day + "/" + (month + 1) + "/" + year + ":",
                scrollPane,
                "Enter new task:",
                taskField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Task Manager", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION && !taskField.getText().trim().isEmpty()) {
            saveTaskToDatabase(year, month, day, taskField.getText().trim());
        }
    }

    private void saveTaskToDatabase(int year, int month, int day, String task) {
        String query = "INSERT INTO todo_list (task_date, task_text) VALUES (?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setDate(1, Date.valueOf(year + "-" + (month + 1) + "-" + day));
            stmt.setString(2, task);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Task saved successfully!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error saving task: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ArrayList<String> getTasksFromDatabase(int year, int month, int day) {
        ArrayList<String> tasks = new ArrayList<>();
        String query = "SELECT task_text FROM todo_list WHERE task_date = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setDate(1, Date.valueOf(year + "-" + (month + 1) + "-" + day));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tasks.add(rs.getString("task_text"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error retrieving tasks: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return tasks;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalendarApp());
    }
}
