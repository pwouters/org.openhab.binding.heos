package org.openhab.binding.heos.resources;

import java.lang.reflect.Type;
import java.util.HashMap;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class HeosDeserializerEvent implements JsonDeserializer<HeosResponseEvent> {

    private HeosResponseEvent responseHeos = new HeosResponseEvent();

    private String rawCommand = null;
    private String rawResult = null;
    private String rawMessage = null;

    private String errorCode = null;
    private String errorMessage = null;

    private String eventType = null;
    private String commandType = null;
    private HashMap<String, String> messages = new HashMap<String, String>();

    @Override
    public HeosResponseEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        final JsonObject jsonObject = json.getAsJsonObject();
        final JsonObject jsonHeos = jsonObject.get("heos").getAsJsonObject();

        // sets the Basic command String and decodes it afterwards to eventType and commandType
        this.rawCommand = jsonHeos.get("command").getAsString();
        decodeCommand(rawCommand);
        responseHeos.setCommand(rawCommand);
        responseHeos.setEventType(eventType);
        responseHeos.setCommandType(commandType);

        // not all Messages has a result field. Field is only set if check is true
        if (jsonHeos.has("result")) {
            this.rawResult = jsonHeos.get("result").getAsString();
            responseHeos.setResult(rawResult);
        } else {
            responseHeos.setResult("null");
        }
        // not all Messages has a message field. Field is only set if check is true
        if (jsonHeos.has("message")) {
            if (!jsonHeos.get("message").getAsString().isEmpty()) {
                this.rawMessage = jsonHeos.get("message").getAsString();
                responseHeos.setMessage(rawMessage); // raw Message
                decodeMessage(rawMessage);
                responseHeos.setMessagesMap(messages);
            } else {
                this.messages.put("command under process", "false"); // noch �berarbeiten!!!!
                responseHeos.setMessagesMap(messages);
            }
        }
        if (rawResult.equals("fail")) {
            responseHeos.setErrorCode(messages.get("eid"));
            responseHeos.setErrorMessage(messages.get("text"));
        }

        return responseHeos;
    }

    private void decodeMessage(String message) {

        if (message.contains("command under")) {
            this.messages.put("command under process", "true");
            return;
        }

        this.messages.put("command under process", "false");

        String input = "&" + message;
        int start = 0;
        int stop = 0;

        while (stop >= 0) {
            start = input.indexOf("&", start) + 1;
            stop = input.indexOf("=", start);
            if (stop < 0) {
                this.messages.put("message", message);
                return;
            }
            String key = input.substring(start, stop);
            start = stop + 1;
            stop = input.indexOf("&", start);
            String value;
            if (stop < 0) {
                value = input.substring(start, input.length());
            } else {
                value = input.substring(start, stop);
            }
            this.messages.put(key, value);

        }

    }

    private void decodeCommand(String command) {

        int start = 0;
        int stop = 0;
        if (command.indexOf("/") > 0) {
            stop = command.indexOf("/", start);
            this.eventType = command.substring(start, stop);
            this.commandType = command.substring(stop + 1);
        } else {
            this.eventType = "Error";
            this.commandType = command;
        }
    }
}
