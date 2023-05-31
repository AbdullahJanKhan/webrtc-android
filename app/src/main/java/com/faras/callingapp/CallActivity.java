package com.faras.callingapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.faras.callingapp.databinding.ActivityCallBinding;
import com.faras.callingapp.models.IceCandidateModel;
import com.faras.callingapp.models.MessageModels;
import com.faras.callingapp.utils.PeerConnectionObserver;
import com.faras.callingapp.utils.RTCAudioManager;
import com.google.gson.Gson;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.SessionDescription;

import java.util.HashMap;

public class CallActivity extends AppCompatActivity {

    private ActivityCallBinding binding;
    private String userName;
    private SocketRepository socketRepository;
    private RTCClient rtcClient;
    private final String TAG = "CallActivity";
    private String target;
    private final Gson gson = new Gson();
    private Boolean isMute = false;
    private Boolean isCameraPause = false;
    private RTCAudioManager rtcAudioManager;

    private Boolean isSpeakerMode = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
    }

    public void init() {
        userName = getIntent().getStringExtra("username");
        socketRepository = new SocketRepository(userName) {
            @Override
            public void onNewMessage(MessageModels message) {
                newMessage(message);
            }
        };
        rtcClient = new RTCClient(getApplication(), userName, socketRepository, new PeerConnectionObserver() {
            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                mediaStream.videoTracks.get(0).addSink(binding.remoteView);
                Log.d(TAG, "onAddStream: " + mediaStream);
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                rtcClient.addIceCandidate(iceCandidate);
                HashMap<String, Object> candidate = new HashMap<>();
                candidate.put("sdpMid", iceCandidate.sdpMid);
                candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                candidate.put("sdpCandidate", iceCandidate.sdp);
                socketRepository.sendMessageToSocket(
                        new MessageModels("ice_candidate", userName, target, candidate)
                );
            }
        });
        rtcAudioManager = RTCAudioManager.create(this);
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE);

        binding.callBtn.setOnClickListener(view -> {
            socketRepository.sendMessageToSocket(
                    new MessageModels(
                            "start_call", userName, binding.targetUserNameEt.getText().toString(), null
                    )
            );
            target = binding.targetUserNameEt.getText().toString();
        });

        binding.switchCameraButton.setOnClickListener(view -> rtcClient.switchCamera());

        binding.micButton.setOnClickListener(view -> {
            if (isMute) {
                isMute = false;
                binding.micButton.setImageResource(R.drawable.ic_baseline_mic_off_24);
            } else {
                isMute = true;
                binding.micButton.setImageResource(R.drawable.ic_baseline_mic_24);
            }
            rtcClient.toggleAudio(isMute);
        });

        binding.videoButton.setOnClickListener(view -> {
            if (isCameraPause) {
                isCameraPause = false;
                binding.videoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24);
            } else {
                isCameraPause = true;
                binding.videoButton.setImageResource(R.drawable.ic_baseline_videocam_24);
            }
            rtcClient.toggleCamera(isCameraPause);
        });

        binding.audioOutputButton.setOnClickListener(view -> {
            if (isSpeakerMode) {
                isSpeakerMode = false;
                binding.audioOutputButton.setImageResource(R.drawable.ic_baseline_hearing_24);
                rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE);
            } else {
                isSpeakerMode = true;
                binding.audioOutputButton.setImageResource(R.drawable.ic_baseline_speaker_up_24);
                rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE);

            }
        });

        binding.endCallButton.setOnClickListener(view -> {
            setCallLayoutGone();
            setWhoToCallLayoutVisible();
            setIncomingCallLayoutGone();
            rtcClient.endCall();
        });
    }

    private void setIncomingCallLayoutGone() {
        binding.incomingCallLayout.setVisibility(View.GONE);
    }

    private void setIncomingCallLayoutVisible() {
        binding.incomingCallLayout.setVisibility(View.VISIBLE);
    }

    private void setCallLayoutGone() {
        binding.callLayout.setVisibility(View.GONE);
    }

    private void setCallLayoutVisible() {
        binding.callLayout.setVisibility(View.VISIBLE);
    }

    private void setWhoToCallLayoutGone() {
        binding.whoToCallLayout.setVisibility(View.GONE);
    }

    private void setWhoToCallLayoutVisible() {
        binding.whoToCallLayout.setVisibility(View.VISIBLE);
    }

    private void newMessage(MessageModels message) {
        Log.d("SocketRepository", "onNewMessage: " + message.toString());
        if ("call_response".equals(message.type)) {
            if (message.data.toString().equals("user is not online")) {
                // user offline
                runOnUiThread(() ->
                        Toast.makeText(CallActivity.this, "User not Reachable", Toast.LENGTH_LONG).show()
                );
            } else {
                // user ready, we start the call
                runOnUiThread(
                        () -> {
                            setWhoToCallLayoutGone();
                            setCallLayoutVisible();
                            rtcClient.initializeSurfaceView(binding.localView);
                            rtcClient.initializeSurfaceView(binding.remoteView);
                            rtcClient.startLocalVideo(binding.localView);
                            rtcClient.call(binding.targetUserNameEt.getText().toString());
                        }
                );
            }
        } else if ("answer_received".equals(message.type)) {
            SessionDescription sessionDescription = new SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
            );
            rtcClient.onRemoteSessionReceived(sessionDescription);
            runOnUiThread(
                    () ->
                            binding.remoteViewLoading.setVisibility(View.GONE)
            );
        } else if ("offer_received".equals(message.type)) {
            runOnUiThread(() -> {
                setIncomingCallLayoutVisible();
                binding.incomingNameTV.setText(new StringBuffer(message.name + " is Calling You"));
                binding.acceptButton.setOnClickListener(view -> {
                    setIncomingCallLayoutGone();
                    setCallLayoutVisible();
                    setWhoToCallLayoutGone();
                    rtcClient.initializeSurfaceView(binding.localView);
                    rtcClient.initializeSurfaceView(binding.remoteView);
                    rtcClient.startLocalVideo(binding.localView);

                    SessionDescription sessionDescription = new SessionDescription(
                            SessionDescription.Type.OFFER,
                            message.data.toString()
                    );
                    rtcClient.onRemoteSessionReceived(sessionDescription);
                    rtcClient.answer(message.name);
                    target = message.name;
                    binding.remoteViewLoading.setVisibility(View.GONE);
                });
                binding.rejectButton.setOnClickListener(view -> setIncomingCallLayoutGone());
            });
        } else if ("ice_candidate".equals(message.type)) {
            IceCandidateModel receivingCandidate = gson.fromJson(
                    gson.toJson(message.data),
                    IceCandidateModel.class
            );
            rtcClient.addIceCandidate(
                    new IceCandidate(
                            receivingCandidate.sdpMid,
                            Math.toIntExact(receivingCandidate.sdpMLineIndex.longValue()),
                            receivingCandidate.sdpCandidate
                    )
            );
        }
    }
}