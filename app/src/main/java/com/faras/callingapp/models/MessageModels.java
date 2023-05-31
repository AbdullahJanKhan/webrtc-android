package com.faras.callingapp.models;

import androidx.annotation.NonNull;

public class MessageModels {
    public String type, name, target;
    public Object data;

    public MessageModels(String type, String name, String target, Object data) {
        this.type = type;
        this.name = name;
        this.target = target;
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    public String getTarget() {
        return target;
    }

    public String getType() {
        return type;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setType(String type) {
        this.type = type;
    }

    @NonNull
    @Override
    public String toString() {
        return "Type: " + type + " Name: " + name + " Target: " + target + " Data: " + (data == null ? "" : data.toString());
    }
}

