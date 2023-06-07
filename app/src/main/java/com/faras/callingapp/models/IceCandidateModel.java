package com.faras.callingapp.models;

public class IceCandidateModel {
    public String sdpMid;
    public Double sdpMLineIndex;
    public String sdpCandidate;

    public IceCandidateModel(String sdpMid, Double sdpMLineIndex, String sdpCandidate) {
        this.sdpMid = sdpMid;
        this.sdpMLineIndex = sdpMLineIndex;
        this.sdpCandidate = sdpCandidate;
    }

    public String getSdpMid() {
        return sdpMid;
    }

    public void setSdpMid(String sdpMid) {
        this.sdpMid = sdpMid;
    }

    public Double getSdpMLineIndex() {
        return sdpMLineIndex;
    }

    public void setSdpMLineIndex(Double sdpMLineIndex) {
        this.sdpMLineIndex = sdpMLineIndex;
    }

    public String getSdpCandidate() {
        return sdpCandidate;
    }

    public void setSdpCandidate(String sdpCandidate) {
        this.sdpCandidate = sdpCandidate;
    }
}
