package com.example;

import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.rest.api.v2010.account.Call;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CallLogs extends HttpServlet {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/calldb";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "amlahmad12345";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String callLogsHtml;
        try {
            callLogsHtml = retrieveCallLogFromDatabase();
        } catch (SQLException ex) {
            Logger.getLogger(CallLogs.class.getName()).log(Level.SEVERE, null, ex);

            callLogsHtml = "Error retrieving call logs: " + ex.getMessage();
        }

        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Call Logs</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Call Logs</h1>");
            out.println(callLogsHtml);
            out.println("</body>");
            out.println("</html>");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Call Logs Servlet";
    }

    private static void saveCallsToDatabase() throws SQLException {
        Twilio.init("AC71dd281f4ec71ca0e5905b3013166ef5", "272bf2f3bce0b829d3fec3bc2413a941");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(CallLogs.class.getName()).log(Level.SEVERE, null, ex);
        }
        ResourceSet<Call> calls = Call.reader().limit(1).read();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            if (calls.iterator().hasNext()) {
                Call record = calls.iterator().next();

                long unixTimestamp = record.getDateCreated().toInstant().toEpochMilli();
                java.sql.Timestamp dateCreated = new java.sql.Timestamp(unixTimestamp);

                String sql = "SELECT * FROM call_log WHERE call_sid = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setString(1, record.getSid());
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (!resultSet.next()) {
                            
                            try (PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO call_log (call_sid, duration, price, status, direction, to_phone, date_created) VALUES (?, CAST(? AS INTEGER), CAST(? AS DOUBLE PRECISION), ?, ?, ?, ?)")) {
                                insertStatement.setString(1, record.getSid());
                                insertStatement.setObject(2, record.getDuration());
                                insertStatement.setObject(3, record.getPrice());
                                insertStatement.setString(4, record.getStatus().toString());
                                insertStatement.setString(5, record.getDirection());
                                insertStatement.setString(6, record.getTo());
                                insertStatement.setTimestamp(7, dateCreated);
                                insertStatement.executeUpdate();
                                System.out.println("Call details saved to the database.");
                            }
                        } else {
                            System.out.println("Call SID already exists in the database, skipping insertion.");
                        }
                    }
                }
            } else {
                System.out.println("No calls found.");
            }
        }
    }

    private static String retrieveCallLogFromDatabase() throws SQLException {
        saveCallsToDatabase();
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(CallLogs.class.getName()).log(Level.SEVERE, null, ex);
        }

        StringBuilder htmlBuilder = new StringBuilder();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT * FROM call_log";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String callSid = resultSet.getString("call_sid");
                        int duration = resultSet.getInt("duration");
                        double price = resultSet.getDouble("price");
                        String status = resultSet.getString("status");
                        String direction = resultSet.getString("direction");
                        String toPhone = resultSet.getString("to_phone");
                        java.sql.Timestamp dateCreated = resultSet.getTimestamp("date_created");

                        ZonedDateTime zdt = Instant.ofEpochMilli(dateCreated.getTime()).atZone(ZoneId.systemDefault());

                        htmlBuilder.append("<p>");
                        htmlBuilder.append("Call SID: ").append(callSid).append("<br>");
                        htmlBuilder.append("Duration: ").append(duration).append("<br>");
                        htmlBuilder.append("Price: ").append(price).append("<br>");
                        htmlBuilder.append("Status: ").append(status).append("<br>");
                        htmlBuilder.append("Direction: ").append(direction).append("<br>");
                        htmlBuilder.append("To Phone: ").append(toPhone).append("<br>");
                        htmlBuilder.append("Date Created: ").append(zdt).append("<br>");
                        htmlBuilder.append("--------------").append("<br>");
                        htmlBuilder.append("</p>");
                    }
                }
            }
        }

        return htmlBuilder.toString();
    }
}
