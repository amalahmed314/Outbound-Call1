package com.example;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Say;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;

/**
 *
 * @author Amal ELF
 */
public class MakeOutBound extends HttpServlet {


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String toPhoneNumber = request.getParameter("toPhoneNumber");
        String message = request.getParameter("message");

        try {
            makeCall(toPhoneNumber, message);
            
            response.getWriter().write("<h1>Call made successfully!</h1>");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error making the call.");
        }
    }

    private static void makeCall(String toPhoneNumber, String message) {
        Twilio.init("AC71dd281f4ec71ca0e5905b3013166ef5", "272bf2f3bce0b829d3fec3bc2413a941");

        VoiceResponse voiceResponse = new VoiceResponse.Builder()
                .say(new Say.Builder(message).build())
                .build();

        Call call = Call.creator(
                new com.twilio.type.PhoneNumber(toPhoneNumber),
                new com.twilio.type.PhoneNumber("+12676622499"),
                new com.twilio.type.Twiml(voiceResponse.toXml())
        ).create();

        //System.out.println("Call SID: " + call.getSid());
    }

   
}
